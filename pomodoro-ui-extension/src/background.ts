import { buildDNRRules } from './blockingUtils'
import type { BlockingMessage } from './types'

const TIMER_PAGE   = chrome.runtime.getURL('index.html')
const BLOCKED_PAGE = chrome.runtime.getURL('blocked.html')

chrome.action.onClicked.addListener(async () => {
  const [existing] = await chrome.tabs.query({ url: TIMER_PAGE })
  if (existing?.id != null) {
    await chrome.tabs.update(existing.id, { active: true })
    if (existing.windowId != null) {
      await chrome.windows.update(existing.windowId, { focused: true })
    }
  } else {
    await chrome.tabs.create({ url: TIMER_PAGE })
  }
})

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === 'pomodoro') {
    chrome.notifications.create({
      type: 'basic',
      iconUrl: 'icons/icon128.png',
      title: 'Pomodoro Timer',
      message: 'Time is up! Take a break.',
      priority: 2,
    })
  }
})

chrome.runtime.onMessage.addListener(
  (message: BlockingMessage, _sender, sendResponse) => {
    if (message.type === 'START_BLOCKING') {
      startBlocking().then(() => sendResponse({ success: true }))
      return true
    }
    if (message.type === 'STOP_BLOCKING') {
      stopBlocking().then(() => sendResponse({ success: true }))
      return true
    }
    if (message.type === 'UPDATE_BLOCKING') {
      updateBlocking(message.sites).then(() => sendResponse({ success: true }))
      return true
    }
  }
)

// ---------------------------------------------------------------------------
// Tab URL registry
// Saves the original URL of a tab before we redirect it to blocked.html so
// stopBlocking() can navigate it back.
// ---------------------------------------------------------------------------

async function saveTabUrl(tabId: number, url: string) {
  const { blockedTabUrls = {} } = await chrome.storage.local.get('blockedTabUrls') as {
    blockedTabUrls?: Record<number, string>
  }
  blockedTabUrls[tabId] = url
  await chrome.storage.local.set({ blockedTabUrls })
}

async function restoreBlockedTabs() {
  const { blockedTabUrls = {} } = await chrome.storage.local.get('blockedTabUrls') as {
    blockedTabUrls?: Record<number, string>
  }
  const allTabs = await chrome.tabs.query({})

  for (const tab of allTabs) {
    if (!tab.id || !tab.url?.startsWith(BLOCKED_PAGE)) continue

    const savedUrl = blockedTabUrls[tab.id]
    if (savedUrl) {
      // Navigate back to the page the user was on before blocking started
      await chrome.tabs.update(tab.id, { url: savedUrl })
    } else {
      // Tab was DNR-redirected mid-navigation — go back in history
      await chrome.tabs.goBack(tab.id).catch(() => {})
    }
  }

  await chrome.storage.local.remove('blockedTabUrls')
}

// ---------------------------------------------------------------------------
// Blocking lifecycle
// ---------------------------------------------------------------------------

async function startBlocking() {
  const { blockedSites = [] } = await chrome.storage.local.get('blockedSites') as { blockedSites?: string[] }
  console.log('[pomodoro] startBlocking — sites from storage:', blockedSites)
  if (blockedSites.length === 0) {
    console.log('[pomodoro] startBlocking — no sites, aborting')
    return
  }

  const rules = buildDNRRules(blockedSites, chrome.runtime.id)

  const existingRules = await chrome.declarativeNetRequest.getDynamicRules()
  await chrome.declarativeNetRequest.updateDynamicRules({
    removeRuleIds: existingRules.map((r) => r.id),
    addRules: rules,
  })

  await chrome.storage.local.set({ isBlocking: true })

  // Redirect already-open tabs that are on a blocked domain, saving their URLs first
  const blockedSet = new Set(blockedSites)
  const allTabs = await chrome.tabs.query({})
  console.log('[pomodoro] startBlocking — checking', allTabs.length, 'open tabs')
  for (const tab of allTabs) {
    if (!tab.id || !tab.url) continue
    try {
      const host = new URL(tab.url).hostname.replace(/^www\./, '')
      if (blockedSet.has(host)) {
        await saveTabUrl(tab.id, tab.url)
        await chrome.tabs.update(tab.id, {
          url: `${BLOCKED_PAGE}?site=${encodeURIComponent(host)}`,
        })
      }
    } catch {
      // ignore tabs with non-parseable URLs
    }
  }
}

async function stopBlocking() {
  const existingRules = await chrome.declarativeNetRequest.getDynamicRules()
  if (existingRules.length > 0) {
    await chrome.declarativeNetRequest.updateDynamicRules({
      removeRuleIds: existingRules.map((r) => r.id),
      addRules: [],
    })
  }

  const { pomodoroConfig } = await chrome.storage.local.get('pomodoroConfig') as {
    pomodoroConfig?: { restoreTabs: boolean }
  }
  // Default to false when config is absent (server offline) — matches server default
  if (pomodoroConfig?.restoreTabs ?? false) {
    await restoreBlockedTabs()
  }

  await chrome.storage.local.set({ isBlocking: false })
}

async function updateBlocking(newSites: string[]) {
  const { isBlocking = false } = await chrome.storage.local.get('isBlocking') as { isBlocking?: boolean }
  if (!isBlocking) return

  const existingRules = await chrome.declarativeNetRequest.getDynamicRules()
  await chrome.declarativeNetRequest.updateDynamicRules({
    removeRuleIds: existingRules.map((r) => r.id),
    addRules: buildDNRRules(newSites, chrome.runtime.id),
  })

  const newSiteSet = new Set(newSites)
  const allTabs = await chrome.tabs.query({})
  for (const tab of allTabs) {
    if (!tab.id || !tab.url) continue
    try {
      const host = new URL(tab.url).hostname.replace(/^www\./, '')
      if (newSiteSet.has(host)) {
        await saveTabUrl(tab.id, tab.url)
        await chrome.tabs.update(tab.id, {
          url: `${BLOCKED_PAGE}?site=${encodeURIComponent(host)}`,
        })
      }
    } catch {
      // ignore tabs with non-parseable URLs
    }
  }

  await chrome.storage.local.set({ blockedSites: newSites })
}
