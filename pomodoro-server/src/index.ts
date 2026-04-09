import express from 'express'
import cors from 'cors'
import timerRouter from './timerRouter.js'
import blockedSitesRouter from './blockedSitesRouter.js'
import { registerExpiryCallback, getState, getHistory } from './timerStore.js'
import { broadcast } from './sseManager.js'

const PORT = process.env.PORT ? Number(process.env.PORT) : 4000

const app = express()

app.use(cors({ origin: true, credentials: false }))
app.use(express.json())
app.use('/timer', timerRouter)
app.use('/blocked-sites', blockedSitesRouter)

app.get('/history', (_req, res) => {
  res.json(getHistory())
})

// Broadcast to all SSE clients when the timer expires naturally
registerExpiryCallback(() => {
  broadcast('timerUpdate', getState())
})

app.listen(PORT, () => {
  console.log(`Pomodoro server listening on http://localhost:${PORT}`)
})
