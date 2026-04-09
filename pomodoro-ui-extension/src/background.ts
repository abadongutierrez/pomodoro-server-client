import { buildDNRRules } from './blockingUtils'
import type { BlockingMessage } from './types'

const TIMER_PAGE = chrome.runtime.getURL('index.html')

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

async function startBlocking() {
  const { blockedSites = [] } = await chrome.storage.local.get('blockedSites') as { blockedSites?: string[] }
  console.log('[pomodoro] startBlocking — sites from storage:', blockedSites)
  if (blockedSites.length === 0) {
    console.log('[pomodoro] startBlocking — no sites, aborting')
    return
  }

  const rules = buildDNRRules(blockedSites, chrome.runtime.id)
  console.log('[pomodoro] startBlocking — DNR rules to install:', JSON.stringify(rules, null, 2))

  const existingRules = await chrome.declarativeNetRequest.getDynamicRules()
  const existingIds = existingRules.map((r) => r.id)
  console.log('[pomodoro] startBlocking — removing existing rule IDs:', existingIds)

  await chrome.declarativeNetRequest.updateDynamicRules({
    removeRuleIds: existingIds,
    addRules: rules,
  })

  const installedRules = await chrome.declarativeNetRequest.getDynamicRules()
  console.log('[pomodoro] startBlocking — installed rules after update:', JSON.stringify(installedRules, null, 2))

  // Mark blocking as active before redirecting tabs so UPDATE_BLOCKING
  // messages arriving concurrently see the correct state
  await chrome.storage.local.set({ isBlocking: true })

  // Redirect any already-open tab on a blocked site
  const blockedSet = new Set(blockedSites)
  const allTabs = await chrome.tabs.query({})
  console.log('[pomodoro] startBlocking — checking', allTabs.length, 'open tabs')
  for (const tab of allTabs) {
    if (!tab.id || !tab.url) continue
    try {
      const host = new URL(tab.url).hostname.replace(/^www\./, '')
      console.log('[pomodoro] startBlocking — tab host:', host, '| blocked?', blockedSet.has(host))
      if (blockedSet.has(host)) {
        await chrome.tabs.update(tab.id, {
          url: chrome.runtime.getURL(`blocked.html?site=${encodeURIComponent(host)}`),
        })
      }
    } catch {
      // ignore tabs with non-parseable URLs
    }
  }
}

async function stopBlocking() {
  const existingRules = await chrome.declarativeNetRequest.getDynamicRules()
  const existingIds = existingRules.map((r) => r.id)
  if (existingIds.length === 0) return
  await chrome.declarativeNetRequest.updateDynamicRules({
    removeRuleIds: existingIds,
    addRules: [],
  })
  await chrome.storage.local.set({ isBlocking: false })
}

async function updateBlocking(newSites: string[]) {
  const { isBlocking = false } = await chrome.storage.local.get('isBlocking') as { isBlocking?: boolean }
  if (!isBlocking) return

  const existingRules = await chrome.declarativeNetRequest.getDynamicRules()
  const existingIds = existingRules.map((r) => r.id)
  const rules = buildDNRRules(newSites, chrome.runtime.id)
  await chrome.declarativeNetRequest.updateDynamicRules({ removeRuleIds: existingIds, addRules: rules })

  const newSiteSet = new Set(newSites)
  const allTabs = await chrome.tabs.query({})
  for (const tab of allTabs) {
    if (!tab.id || !tab.url) continue
    try {
      const host = new URL(tab.url).hostname.replace(/^www\./, '')
      if (newSiteSet.has(host)) {
        await chrome.tabs.update(tab.id, {
          url: chrome.runtime.getURL(`blocked.html?site=${encodeURIComponent(host)}`),
        })
      }
    } catch {
      // ignore tabs with non-parseable URLs
    }
  }

  await chrome.storage.local.set({ blockedSites: newSites })
}
