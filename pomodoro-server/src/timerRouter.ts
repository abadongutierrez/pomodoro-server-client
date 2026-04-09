import { Router } from 'express'
import type { Request, Response } from 'express'
import { getState, startTimer, pauseTimer, resetTimer, setMode } from './timerStore.js'
import { addClient, broadcast } from './sseManager.js'
import { getSites } from './blockedSitesStore.js'
import { log } from './logger.js'
import type { TimerStatus, TimerMode } from './types.js'

const router = Router()

// GET /timer — current state snapshot
router.get('/', (_req: Request, res: Response) => {
  const state = getState()
  log('GET', '/timer', state)
  res.json(state)
})

// GET /timer/events — SSE stream
router.get('/events', (req: Request, res: Response) => {
  log('GET', '/timer/events — new SSE connection')
  res.setHeader('Content-Type', 'text/event-stream')
  res.setHeader('Cache-Control', 'no-cache')
  res.setHeader('Connection', 'keep-alive')
  res.flushHeaders()

  // Send current state immediately so client syncs on connect
  const state = getState()
  const sites = getSites()
  log('SSE', 'initial timerUpdate on connect', state)
  res.write(`event: timerUpdate\ndata: ${JSON.stringify(state)}\n\n`)
  log('SSE', 'initial blockedSitesUpdate on connect', sites)
  res.write(`event: blockedSitesUpdate\ndata: ${JSON.stringify(sites)}\n\n`)

  addClient(res)

  // Keep-alive comment every 20s to prevent proxy/browser from closing the stream
  const keepAlive = setInterval(() => res.write(': ka\n\n'), 20_000)
  req.on('close', () => {
    clearInterval(keepAlive)
    log('GET', '/timer/events — SSE connection closed')
  })
})

// PATCH /timer — state transition
// Body: { "status": "running" | "paused" | "idle", "mode"?: "work" | "short" | "long" }
// Providing "mode" with status "idle" switches the timer mode and resets it.
router.patch('/', (req: Request, res: Response) => {
  const { status, mode } = req.body as { status?: TimerStatus; mode?: TimerMode }
  log('PATCH', `/timer — requested status: "${status}"${mode ? `, mode: "${mode}"` : ''}`)

  if (status !== 'running' && status !== 'paused' && status !== 'idle') {
    log('PATCH', `/timer — invalid status: "${status}"`)
    res.status(400).json({ error: 'status must be "running", "paused", or "idle"' })
    return
  }

  const validModes: TimerMode[] = ['work', 'short', 'long']
  if (mode !== undefined && !validModes.includes(mode)) {
    log('PATCH', `/timer — invalid mode: "${mode}"`)
    res.status(400).json({ error: 'mode must be "work", "short", or "long"' })
    return
  }

  let newState
  if (status === 'idle' && mode !== undefined) {
    newState = setMode(mode)
  } else {
    switch (status) {
      case 'running':
        newState = startTimer()
        break
      case 'paused':
        newState = pauseTimer()
        break
      case 'idle':
        newState = resetTimer()
        break
    }
  }

  log('PATCH', `/timer — new state`, newState)
  broadcast('timerUpdate', newState)
  res.json(newState)
})

export default router
