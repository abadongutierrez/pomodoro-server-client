import type { ServerTimerState, ServerConfig } from './types'

const SERVER_BASE = 'http://localhost:4000'

async function patch(body: Record<string, unknown>): Promise<ServerTimerState> {
  const res = await fetch(`${SERVER_BASE}/timer`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return res.json() as Promise<ServerTimerState>
}

export function apiStart(): Promise<ServerTimerState> {
  return patch({ status: 'running' })
}

export function apiPause(): Promise<ServerTimerState> {
  return patch({ status: 'paused' })
}

export function apiSetMode(mode: ServerTimerState['mode']): Promise<ServerTimerState> {
  return patch({ status: 'idle', mode })
}

export function apiReset(): Promise<ServerTimerState> {
  return patch({ status: 'idle' })
}

export async function apiGetState(): Promise<ServerTimerState> {
  const res = await fetch(`${SERVER_BASE}/timer`)
  return res.json() as Promise<ServerTimerState>
}

export async function apiGetBlockedSites(): Promise<string[]> {
  const res = await fetch(`${SERVER_BASE}/blocked-sites`)
  return res.json() as Promise<string[]>
}

export async function apiSaveBlockedSites(sites: string[]): Promise<string[]> {
  const res = await fetch(`${SERVER_BASE}/blocked-sites`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(sites),
  })
  return res.json() as Promise<string[]>
}

export function connectSSE(
  onState: (state: ServerTimerState) => void,
  onSites: (sites: string[]) => void,
  onConfig: (config: ServerConfig) => void,
  onOffline: () => void,
): () => void {
  let es: EventSource | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let destroyed = false

  function connect() {
    if (destroyed) return
    es = new EventSource(`${SERVER_BASE}/timer/events`)

    es.addEventListener('timerUpdate', (e: MessageEvent) => {
      try {
        onState(JSON.parse(e.data) as ServerTimerState)
      } catch {
        // ignore malformed payloads
      }
    })

    es.addEventListener('blockedSitesUpdate', (e: MessageEvent) => {
      try {
        onSites(JSON.parse(e.data) as string[])
      } catch {
        // ignore malformed payloads
      }
    })

    es.addEventListener('configUpdate', (e: MessageEvent) => {
      try {
        onConfig(JSON.parse(e.data) as ServerConfig)
      } catch {
        // ignore malformed payloads
      }
    })

    es.onerror = () => {
      es?.close()
      es = null
      onOffline()
      if (!destroyed) {
        reconnectTimer = setTimeout(connect, 3_000)
      }
    }
  }

  connect()

  return () => {
    destroyed = true
    if (reconnectTimer) clearTimeout(reconnectTimer)
    es?.close()
  }
}
