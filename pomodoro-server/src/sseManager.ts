import type { Response } from 'express'
import { log } from './logger.js'

const clients = new Set<Response>()

export function addClient(res: Response): void {
  clients.add(res)
  log('SSE', `client connected (total: ${clients.size})`)
  res.on('close', () => {
    clients.delete(res)
    log('SSE', `client disconnected (total: ${clients.size})`)
  })
}

export function broadcast(eventName: string, data: unknown): void {
  log('SSE', `broadcast "${eventName}" to ${clients.size} client(s)`, data)
  const payload = `event: ${eventName}\ndata: ${JSON.stringify(data)}\n\n`
  for (const res of clients) {
    res.write(payload)
  }
}
