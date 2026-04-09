import { randomUUID } from 'node:crypto'
import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'
import type { TimerMode, TimerState, TimerStateResponse, HistoryEntry, PersistedStore } from './types.js'

const DURATIONS: Record<TimerMode, number> = {
  work: 25 * 60,
  short: 5 * 60,
  long: 15 * 60,
}

const HISTORY_LIMIT = 100

const STATE_DIR = join(process.env.HOME ?? process.env.USERPROFILE ?? '.', '.pomodoro')
const STATE_FILE = join(STATE_DIR, 'pomodoro-state.json')

const DEFAULT_STATE: TimerState = {
  status: 'idle',
  mode: 'work',
  duration: DURATIONS.work,
  startedAt: null,
  elapsed: 0,
  sessionId: randomUUID(),
}

function recoverCurrent(saved: TimerState): TimerState {
  // A persisted 'running' state means the server was killed mid-session.
  // Transition to 'paused' so elapsed is preserved but no phantom countdown starts.
  if (saved.status === 'running') {
    const sinceStart = saved.startedAt
      ? Math.floor((Date.now() - saved.startedAt) / 1000)
      : 0
    return {
      ...saved,
      status: 'paused',
      startedAt: null,
      elapsed: Math.min(saved.elapsed + sinceStart, saved.duration),
    }
  }
  return saved
}

function loadStore(): { state: TimerState; history: HistoryEntry[] } {
  try {
    if (existsSync(STATE_FILE)) {
      const parsed = JSON.parse(readFileSync(STATE_FILE, 'utf-8')) as Record<string, unknown>
      if ('current' in parsed) {
        // New format
        const store = parsed as unknown as PersistedStore
        return {
          state: recoverCurrent(store.current),
          history: Array.isArray(store.history) ? store.history : [],
        }
      } else {
        // Legacy format — bare TimerState, no history
        return {
          state: recoverCurrent(parsed as unknown as TimerState),
          history: [],
        }
      }
    }
  } catch {
    // Corrupted file — fall through to defaults
  }
  return { state: { ...DEFAULT_STATE }, history: [] }
}

function persistStore(): void {
  try {
    mkdirSync(STATE_DIR, { recursive: true })
    const store: PersistedStore = { current: state, history }
    writeFileSync(STATE_FILE, JSON.stringify(store, null, 2))
  } catch {
    // Non-fatal — server continues without persistence
  }
}

function recordHistory(completed: TimerState): void {
  const entry: HistoryEntry = {
    sessionId: completed.sessionId,
    mode: completed.mode,
    duration: completed.duration,
    completedAt: Date.now(),
  }
  history.push(entry)
  if (history.length > HISTORY_LIMIT) history.shift()
}

// Determines the next mode after a natural expiry.
// Called after recordHistory so the just-completed session is already in history.
function computeNextMode(completedMode: TimerMode): TimerMode {
  if (completedMode === 'long') return 'work'
  if (completedMode === 'work') return 'short'

  // Count short breaks since the last long break (including the one just recorded)
  let shortBreakCount = 0
  for (let i = history.length - 1; i >= 0; i--) {
    if (history[i].mode === 'long') break
    if (history[i].mode === 'short') shortBreakCount++
  }
  return shortBreakCount >= 3 ? 'long' : 'work'
}

const loaded = loadStore()
let state: TimerState = loaded.state
let history: HistoryEntry[] = loaded.history

let expiryWatcher: ReturnType<typeof setInterval> | null = null
let onExpiryCallback: (() => void) | null = null

function computeRemaining(s: TimerState): number {
  if (s.status === 'running' && s.startedAt !== null) {
    const sinceStart = Math.floor((Date.now() - s.startedAt) / 1000)
    return Math.max(0, s.duration - s.elapsed - sinceStart)
  }
  return Math.max(0, s.duration - s.elapsed)
}

function clearExpiryWatcher(): void {
  if (expiryWatcher !== null) {
    clearInterval(expiryWatcher)
    expiryWatcher = null
  }
}

function startExpiryWatcher(): void {
  clearExpiryWatcher()
  expiryWatcher = setInterval(() => {
    if (state.status === 'running' && computeRemaining(state) <= 0) {
      recordHistory(state)
      const nextMode = computeNextMode(state.mode)
      state = {
        ...state,
        mode: nextMode,
        duration: DURATIONS[nextMode],
        status: 'idle',
        startedAt: null,
        elapsed: 0,
        sessionId: randomUUID(),
      }
      persistStore()
      clearExpiryWatcher()
      onExpiryCallback?.()
    }
  }, 1000)
}

export function registerExpiryCallback(cb: () => void): void {
  onExpiryCallback = cb
}

export function getState(): TimerStateResponse {
  // Lazy expiry: auto-transition if running and time is up
  if (state.status === 'running' && computeRemaining(state) <= 0) {
    recordHistory(state)
    const nextMode = computeNextMode(state.mode)
    state = {
      ...state,
      mode: nextMode,
      duration: DURATIONS[nextMode],
      status: 'idle',
      startedAt: null,
      elapsed: 0,
      sessionId: randomUUID(),
    }
    persistStore()
    clearExpiryWatcher()
  }
  return { ...state, remaining: computeRemaining(state) }
}

export function getHistory(): HistoryEntry[] {
  return [...history].reverse()
}

export function startTimer(): TimerStateResponse {
  if (state.status === 'running') return getState()
  state = {
    ...state,
    status: 'running',
    startedAt: Date.now(),
  }
  persistStore()
  startExpiryWatcher()
  return { ...state, remaining: computeRemaining(state) }
}

export function pauseTimer(): TimerStateResponse {
  if (state.status !== 'running') return getState()
  const elapsed = state.elapsed + Math.floor((Date.now() - (state.startedAt ?? Date.now())) / 1000)
  state = {
    ...state,
    status: 'paused',
    startedAt: null,
    elapsed,
  }
  persistStore()
  clearExpiryWatcher()
  return { ...state, remaining: computeRemaining(state) }
}

export function resetTimer(): TimerStateResponse {
  clearExpiryWatcher()
  state = {
    ...state,
    status: 'idle',
    startedAt: null,
    elapsed: 0,
    sessionId: randomUUID(),
  }
  persistStore()
  return { ...state, remaining: computeRemaining(state) }
}

export function setMode(mode: TimerMode): TimerStateResponse {
  clearExpiryWatcher()
  state = {
    ...state,
    mode,
    duration: DURATIONS[mode],
    status: 'idle',
    startedAt: null,
    elapsed: 0,
    sessionId: randomUUID(),
  }
  persistStore()
  return { ...state, remaining: computeRemaining(state) }
}
