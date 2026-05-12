const DOMAIN_PAGE_RULES: Array<{ keyword: string; page: string }> = [
  { keyword: 'apply', page: '/pages/recruitment/index' },
  { keyword: 'recruit', page: '/pages/recruitment/index' },
  { keyword: 'activity', page: '/pages/activity/index' },
  { keyword: 'message', page: '/pages/messages/index' },
  { keyword: 'profile', page: '/pages/profile/index' },
]

let routed = false

export function routeByDomain() {
  if (routed || typeof window === 'undefined') return
  const host = window.location.hostname.toLowerCase()
  const matched = DOMAIN_PAGE_RULES.find((rule) => host.includes(rule.keyword))
  if (!matched) return
  routed = true
  uni.reLaunch({ url: matched.page })
}
