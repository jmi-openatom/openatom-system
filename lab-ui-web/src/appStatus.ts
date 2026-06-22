import { reactive } from 'vue'

let initialized = false
let navigationTimer: number | undefined
let requestTimer: number | undefined

export const appStatus = reactive({
  online: navigator.onLine,
  navigationPending: false,
  navigationSlow: false,
  navigationFailed: false,
  navigationTarget: '',
  activeRequests: 0,
  requestSlow: false,
  requestMessage: '',
})

export function initializeAppStatus() {
  if (initialized) return
  initialized = true
  const sync = () => {
    appStatus.online = navigator.onLine
  }
  window.addEventListener('online', sync)
  window.addEventListener('offline', sync)
}

export function startNavigation(target: string) {
  if (navigationTimer) window.clearTimeout(navigationTimer)
  appStatus.navigationPending = true
  appStatus.navigationSlow = false
  appStatus.navigationFailed = false
  appStatus.navigationTarget = target
  navigationTimer = window.setTimeout(() => {
    appStatus.navigationSlow = appStatus.navigationPending
  }, 900)
}

export function finishNavigation() {
  if (navigationTimer) window.clearTimeout(navigationTimer)
  navigationTimer = undefined
  appStatus.navigationPending = false
  appStatus.navigationSlow = false
  appStatus.navigationFailed = false
}

export function failNavigation() {
  if (navigationTimer) window.clearTimeout(navigationTimer)
  navigationTimer = undefined
  appStatus.navigationPending = false
  appStatus.navigationSlow = false
  appStatus.navigationFailed = true
}

export function beginRequest() {
  appStatus.activeRequests += 1
  if (!requestTimer) {
    requestTimer = window.setTimeout(() => {
      appStatus.requestSlow = appStatus.activeRequests > 0
    }, 1200)
  }
}

export function endRequest() {
  appStatus.activeRequests = Math.max(0, appStatus.activeRequests - 1)
  if (appStatus.activeRequests === 0) {
    if (requestTimer) window.clearTimeout(requestTimer)
    requestTimer = undefined
    appStatus.requestSlow = false
  }
}

export function reportRequestError(message: string) {
  appStatus.requestMessage = message
  window.setTimeout(() => {
    if (appStatus.requestMessage === message) appStatus.requestMessage = ''
  }, 3600)
}
