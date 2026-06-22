import { computed, reactive, readonly } from 'vue'

type RequestToken = symbol

const requestTimers = new Map<RequestToken, number>()
const slowRequests = new Set<RequestToken>()
let navigationTimer: number | undefined
let initialized = false

const state = reactive({
  online: typeof navigator === 'undefined' ? true : navigator.onLine,
  navigationPending: false,
  navigationSlow: false,
  navigationFailed: false,
  navigationTarget: '',
  navigationMessage: '',
  activeRequests: 0,
  slowRequestCount: 0,
})

function syncOnlineStatus() {
  state.online = navigator.onLine
  if (state.online && state.navigationFailed) {
    state.navigationMessage = '网络已恢复，可以重试打开页面'
  }
}

export function initializeAppStatus() {
  if (initialized || typeof window === 'undefined') return
  initialized = true
  window.addEventListener('online', syncOnlineStatus)
  window.addEventListener('offline', syncOnlineStatus)
}

export function startNavigation(target: string) {
  if (navigationTimer) window.clearTimeout(navigationTimer)
  state.navigationPending = true
  state.navigationSlow = false
  state.navigationFailed = false
  state.navigationTarget = target
  state.navigationMessage = ''
  navigationTimer = window.setTimeout(() => {
    if (!state.navigationPending) return
    state.navigationSlow = true
    state.navigationMessage = state.online
      ? '页面加载较慢，正在继续尝试'
      : '网络已断开，恢复连接后可重试'
  }, 900)
}

export function finishNavigation() {
  if (navigationTimer) window.clearTimeout(navigationTimer)
  navigationTimer = undefined
  state.navigationPending = false
  state.navigationSlow = false
  state.navigationFailed = false
  state.navigationMessage = ''
}

export function failNavigation(error?: unknown) {
  if (navigationTimer) window.clearTimeout(navigationTimer)
  navigationTimer = undefined
  state.navigationPending = false
  state.navigationSlow = false
  state.navigationFailed = true
  state.navigationMessage = state.online
    ? '页面加载失败，请重试'
    : '当前网络不可用，连接恢复后请重试'

  if (import.meta.env.DEV && error) {
    console.error('[navigation]', error)
  }
}

export function beginRequest(): RequestToken {
  const token = Symbol('request')
  state.activeRequests += 1
  requestTimers.set(
    token,
    window.setTimeout(() => {
      slowRequests.add(token)
      state.slowRequestCount = slowRequests.size
    }, 1200),
  )
  return token
}

export function endRequest(token?: RequestToken) {
  if (!token) return
  const timer = requestTimers.get(token)
  if (timer) window.clearTimeout(timer)
  requestTimers.delete(token)
  slowRequests.delete(token)
  state.activeRequests = Math.max(0, state.activeRequests - 1)
  state.slowRequestCount = slowRequests.size
}

export function useAppStatus() {
  const busy = computed(() => state.navigationPending || state.activeRequests > 0)
  const showNotice = computed(
    () =>
      !state.online || state.navigationSlow || state.navigationFailed || state.slowRequestCount > 0,
  )
  const noticeMessage = computed(() => {
    if (!state.online) return '网络连接已断开，已保留当前页面'
    if (state.navigationFailed) return state.navigationMessage
    if (state.navigationSlow) return state.navigationMessage
    if (state.slowRequestCount > 0) return '网络响应较慢，正在继续尝试'
    return ''
  })

  return {
    state: readonly(state),
    busy,
    showNotice,
    noticeMessage,
  }
}
