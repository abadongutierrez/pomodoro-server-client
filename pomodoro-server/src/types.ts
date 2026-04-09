export type TimerMode = 'work' | 'short' | 'long'
export type TimerStatus = 'idle' | 'running' | 'paused'

export interface TimerState {
  status: TimerStatus
  mode: TimerMode
  duration: number
  startedAt: number | null
  elapsed: number
  sessionId: string
}

export interface TimerStateResponse extends TimerState {
  remaining: number
}

export interface HistoryEntry {
  sessionId: string
  mode: TimerMode
  duration: number
  completedAt: number
}

export interface PersistedStore {
  current: TimerState
  history: HistoryEntry[]
}
