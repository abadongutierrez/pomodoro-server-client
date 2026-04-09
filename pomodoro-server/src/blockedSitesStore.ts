import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'

const SITES_DIR = join(process.env.HOME ?? process.env.USERPROFILE ?? '.', '.pomodoro')
const SITES_FILE = join(SITES_DIR, 'blocked-sites.json')

function loadSites(): string[] {
  try {
    if (existsSync(SITES_FILE)) {
      return JSON.parse(readFileSync(SITES_FILE, 'utf-8')) as string[]
    }
  } catch {
    // Corrupted file — fall through to empty list
  }
  return []
}

function persistSites(sites: string[]): void {
  try {
    mkdirSync(SITES_DIR, { recursive: true })
    writeFileSync(SITES_FILE, JSON.stringify(sites, null, 2))
  } catch {
    // Non-fatal
  }
}

let sites: string[] = loadSites()

export function getSites(): string[] {
  return sites
}

export function setSites(incoming: string[]): string[] {
  sites = incoming
  persistSites(sites)
  return sites
}
