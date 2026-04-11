import { useEffect, useRef, useState } from 'react'
import type { Mode, BlockingMessage, ServerTimerState, ServerConfig } from './types'
import { parseLines } from './blockingUtils'
import { apiStart, apiPause, apiReset, apiSetMode, apiSaveBlockedSites, connectSSE } from './timerClient'
import './App.css'

const DURATIONS: Record<Mode, number> = {
  work: 25 * 60,
  short: 5 * 60,
  long: 15 * 60,
}

const LABELS: Record<Mode, string> = {
  work: 'Focus',
  short: 'Short Break',
  long: 'Long Break',
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

function sendBlocking(msg: BlockingMessage) {
  chrome.runtime.sendMessage(msg).catch(() => {})
}

export default function App() {
  const [mode, setMode] = useState<Mode>('work')
  const [timeLeft, setTimeLeft] = useState(DURATIONS.work)
  const [running, setRunning] = useState(false)
  const [blockedSitesText, setBlockedSitesText] = useState('')
  const [showBlocked, setShowBlocked] = useState(false)
  const [serverOnline, setServerOnline] = useState(false)
  const [sitesSaved, setSitesSaved] = useState(false)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const prevStatusRef = useRef<ServerTimerState['status']>('idle')
  const serverOnlineRef = useRef(false)

  // Keep ref in sync so SSE callback always reads latest value
  useEffect(() => {
    serverOnlineRef.current = serverOnline
  }, [serverOnline])

  // Load blocked sites from chrome.storage as offline fallback on mount
  useEffect(() => {
    chrome.storage.local.get('blockedSites').then((result) => {
      const sites = (result.blockedSites as string[] | undefined) ?? []
      // Only apply if SSE hasn't already provided sites from the server
      setBlockedSitesText((prev) => prev || sites.join('\n'))
    })

    const handleUnload = () => sendBlocking({ type: 'STOP_BLOCKING' })
    window.addEventListener('beforeunload', handleUnload)
    return () => window.removeEventListener('beforeunload', handleUnload)
  }, [])

  // SSE connection — syncs timer state from server
  useEffect(() => {
    const cleanup = connectSSE(
      (state) => {
        // Trigger blocking messages on status transitions
        const prev = prevStatusRef.current
        const now = state.status
        if (state.mode === 'work') {
          if (prev !== 'running' && now === 'running') {
            sendBlocking({ type: 'START_BLOCKING' })
          } else if (prev === 'running' && now !== 'running') {
            sendBlocking({ type: 'STOP_BLOCKING' })
          }
        }
        prevStatusRef.current = now

        setServerOnline(true)
        setTimeLeft(state.remaining)
        setRunning(state.status === 'running')
        setMode(state.mode as Mode)
      },
      (sites) => {
        setBlockedSitesText(sites.join('\n'))
        chrome.storage.local.set({ blockedSites: sites })
        sendBlocking({ type: 'UPDATE_BLOCKING', sites })
      },
      (config: ServerConfig) => {
        chrome.storage.local.set({ pomodoroConfig: config })
      },
      () => setServerOnline(false),
    )
    return cleanup
  }, [])

  // Local countdown tick — display smoothing when server online, authoritative when offline
  useEffect(() => {
    if (!running) return
    intervalRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current!)
          setRunning(false)
          sendBlocking({ type: 'STOP_BLOCKING' })
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(intervalRef.current!)
  }, [running])


  async function handleModeChange(newMode: Mode) {
    if (newMode === mode) return
    clearInterval(intervalRef.current!)
    setMode(newMode)
    if (serverOnlineRef.current) {
      try {
        await apiSetMode(newMode)
        // timeLeft and running come via SSE
        return
      } catch {
        // Fall through to local fallback
      }
    }
    // Offline fallback
    setRunning(false)
    setTimeLeft(DURATIONS[newMode])
    sendBlocking({ type: 'STOP_BLOCKING' })
  }

  async function handleStartPause() {
    if (timeLeft === 0) return
    if (serverOnlineRef.current) {
      try {
        if (running) {
          await apiPause()
        } else {
          await apiStart()
        }
        // State update comes via SSE — do not set running directly here
        return
      } catch {
        // Fall through to local fallback
      }
    }
    // Offline fallback
    const next = !running
    setRunning(next)
    if (mode === 'work') {
      sendBlocking({ type: next ? 'START_BLOCKING' : 'STOP_BLOCKING' })
    }
  }

  async function handleReset() {
    clearInterval(intervalRef.current!)
    if (serverOnlineRef.current) {
      try {
        await apiReset()
        // State update comes via SSE
        return
      } catch {
        // Fall through to local fallback
      }
    }
    // Offline fallback
    setRunning(false)
    setTimeLeft(DURATIONS[mode])
    sendBlocking({ type: 'STOP_BLOCKING' })
  }

  function handleSitesChange(value: string) {
    setBlockedSitesText(value)
    setSitesSaved(false)
  }

  async function handleSaveBlockedSites() {
    const sites = parseLines(blockedSitesText)
    if (serverOnlineRef.current) {
      try {
        await apiSaveBlockedSites(sites)
        // SSE blockedSitesUpdate will sync all clients including this one
        setSitesSaved(true)
        return
      } catch {
        // Fall through to local save
      }
    }
    // Offline fallback: save to chrome.storage only
    chrome.storage.local.set({ blockedSites: sites })
    sendBlocking({ type: 'UPDATE_BLOCKING', sites })
    setSitesSaved(true)
    clearTimeout(debounceRef.current!)
    debounceRef.current = setTimeout(() => setSitesSaved(false), 2000)
  }

  const circumference = 2 * Math.PI * 54
  const progress = 1 - timeLeft / DURATIONS[mode]
  const strokeDashoffset = circumference * (1 - progress)
  const siteCount = parseLines(blockedSitesText).length

  return (
    <div className={`container mode-${mode}`}>
      <div className="title-row">
        <h1 className="title">Pomodoro</h1>
        <span className={`connection-badge ${serverOnline ? 'online' : 'offline'}`}>
          <span className="connection-dot" />
          {serverOnline ? 'online' : 'offline'}
        </span>
      </div>

      <div className="mode-tabs">
        {(Object.keys(DURATIONS) as Mode[]).map((m) => (
          <button
            key={m}
            className={`tab ${mode === m ? 'active' : ''}`}
            onClick={() => handleModeChange(m)}
          >
            {LABELS[m]}
          </button>
        ))}
      </div>

      <div className="timer-ring">
        <svg viewBox="0 0 120 120" width="280" height="280">
          <circle cx="60" cy="60" r="54" className="ring-bg" />
          <circle
            cx="60"
            cy="60"
            r="54"
            className="ring-progress"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            transform="rotate(-90 60 60)"
          />
        </svg>
        <span className="time-display">{formatTime(timeLeft)}</span>
      </div>

      <div className="controls">
        <button className="btn btn-primary" onClick={handleStartPause} disabled={timeLeft === 0}>
          {running ? 'Pause' : 'Start'}
        </button>
        <button className="btn btn-secondary" onClick={handleReset}>
          Reset
        </button>
      </div>

      <p className="status">
        {running ? LABELS[mode] : timeLeft === 0 ? 'Done!' : 'Ready'}
      </p>

      <div className="blocked-section">
        <button
          className="blocked-toggle"
          onClick={() => setShowBlocked((v) => !v)}
        >
          <span>Blocked Sites{siteCount > 0 ? ` (${siteCount})` : ''}</span>
          <span className={`chevron ${showBlocked ? 'open' : ''}`}>▾</span>
        </button>

        {showBlocked && (
          <div className="blocked-body">
            <textarea
              className="blocked-textarea"
              value={blockedSitesText}
              onChange={(e) => handleSitesChange(e.target.value)}
              placeholder={'twitter.com\nyoutube.com\nreddit.com'}
              rows={5}
              spellCheck={false}
            />
            <div className="blocked-footer">
              <p className="blocked-hint">One domain per line. Applied during Focus sessions.</p>
              <div className="blocked-actions">
                <button
                  className={`btn-save ${sitesSaved ? 'saved' : ''}`}
                  onClick={handleSaveBlockedSites}
                >
                  {sitesSaved ? 'Saved!' : 'Save'}
                </button>
                <button
                  className="btn-save"
                  onClick={() => sendBlocking({ type: 'UPDATE_BLOCKING', sites: parseLines(blockedSitesText) })}
                >
                  Refresh
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
