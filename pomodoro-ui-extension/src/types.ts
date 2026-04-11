export type Mode = 'work' | 'short' | 'long'

export type BlockingMessage =
  | { type: 'START_BLOCKING' }
  | { type: 'STOP_BLOCKING' }
  | { type: 'UPDATE_BLOCKING'; sites: string[] }

export type TimerStatus = 'idle' | 'running' | 'paused'

export interface ServerTimerState {
  status: TimerStatus
  mode: Mode
  duration: number
  startedAt: number | null
  elapsed: number
  sessionId: string
  remaining: number
}

export interface ServerConfig {
  restoreTabs: boolean
}
