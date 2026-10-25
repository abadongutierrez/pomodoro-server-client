const RULE_ID_BASE = 1000

export function parseLines(text: string): string[] {
  return [...new Set(
    text.split('\n')
      .map((line) => normalizeDomain(line.trim()))
      .filter((d): d is string => d !== null)
  )]
}

export function normalizeDomain(input: string): string | null {
  let s = input.toLowerCase()
  s = s.replace(/^https?:\/\//, '')
  s = s.replace(/^www\./, '')
  s = s.split('/')[0].trim()
  if (!s || !/^[a-z0-9.-]+\.[a-z]{2,}$/i.test(s)) return null
  return s
}

// Returns true if host is a blocked domain or one of its subdomains,
// mirroring the `||domain` urlFilter used by the DNR rules.
export function isHostBlocked(host: string, blockedSet: Set<string>): boolean {
  const h = host.toLowerCase().replace(/^www\./, '')
  for (const domain of blockedSet) {
    if (h === domain || h.endsWith(`.${domain}`)) return true
  }
  return false
}

export function buildDNRRules(
  domains: string[],
  extensionId: string,
): chrome.declarativeNetRequest.Rule[] {
  const blockedUrl = `chrome-extension://${extensionId}/blocked.html`
  return domains.map((domain, i) => ({
    id: RULE_ID_BASE + i,
    priority: 1,
    action: {
      type: chrome.declarativeNetRequest.RuleActionType.REDIRECT,
      redirect: { url: `${blockedUrl}?site=${encodeURIComponent(domain)}` },
    },
    condition: {
      urlFilter: `||${domain}`,
      resourceTypes: [
        chrome.declarativeNetRequest.ResourceType.MAIN_FRAME,
        chrome.declarativeNetRequest.ResourceType.SUB_FRAME,
      ],
    },
  }))
}
