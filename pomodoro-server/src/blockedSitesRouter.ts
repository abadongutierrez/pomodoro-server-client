import { Router } from 'express'
import type { Request, Response } from 'express'
import { getSites, setSites } from './blockedSitesStore.js'
import { broadcast } from './sseManager.js'
import { log } from './logger.js'

const router = Router()

// GET /blocked-sites
router.get('/', (_req: Request, res: Response) => {
  const sites = getSites()
  log('GET', '/blocked-sites', sites)
  res.json(sites)
})

// PUT /blocked-sites — replace the full list
// Body: string[]
router.put('/', (req: Request, res: Response) => {
  const body = req.body as unknown
  log('PUT', '/blocked-sites — received', body)
  if (!Array.isArray(body) || body.some((s) => typeof s !== 'string')) {
    log('PUT', '/blocked-sites — invalid body')
    res.status(400).json({ error: 'body must be an array of strings' })
    return
  }
  const updated = setSites(body as string[])
  log('PUT', '/blocked-sites — saved', updated)
  broadcast('blockedSitesUpdate', updated)
  res.json(updated)
})

export default router
