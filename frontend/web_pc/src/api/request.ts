import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus/es/components/message/index'
import { beginRequest, endRequest } from '@/composables/useAppStatus'
import { clearSession, getToken } from '@/utils/auth.ts'

const UNAUTHORIZED_CODES = new Set([401, 40100])
const RETRYABLE_STATUS = new Set([408, 425, 429, 500, 502, 503, 504])
const DEFAULT_RETRY_COUNT = 1
let redirectingToLogin = false
let lastMessage = ''
let lastMessageAt = 0

type ResilientRequestConfig = InternalAxiosRequestConfig & {
  __retryCount?: number
  __requestToken?: symbol
  retry?: number
  silent?: boolean
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 20000,
  withCredentials: true,
})

function currentRedirectPath(): string {
  const { pathname, search, hash } = window.location
  return `${pathname}${search}${hash}` || '/'
}

function redirectToLogin(message?: string) {
  clearSession()
  showError(message || '登录已过期，请重新登录')

  if (redirectingToLogin || window.location.pathname === '/login') return

  redirectingToLogin = true
  const redirect = encodeURIComponent(currentRedirectPath())
  window.location.replace(`/login?redirect=${redirect}`)
}

function showError(message: string) {
  const now = Date.now()
  if (message === lastMessage && now - lastMessageAt < 1800) return
  lastMessage = message
  lastMessageAt = now
  ElMessage.error(message)
}

function isRetryable(error: AxiosError, config: ResilientRequestConfig): boolean {
  if ((config.method || 'get').toUpperCase() !== 'GET') return false
  const status = error.response?.status
  return (
    !error.response ||
    error.code === 'ECONNABORTED' ||
    error.code === 'ETIMEDOUT' ||
    error.code === 'ERR_NETWORK' ||
    (typeof status === 'number' && RETRYABLE_STATUS.has(status))
  )
}

function retryDelay(attempt: number): Promise<void> {
  const delay = Math.min(2400, 500 * 2 ** Math.max(0, attempt - 1)) + Math.random() * 250
  return new Promise((resolve) => window.setTimeout(resolve, delay))
}

function waitForOnline(maxWait = 8000): Promise<void> {
  if (navigator.onLine) return Promise.resolve()
  return new Promise((resolve) => {
    const timeout = window.setTimeout(done, maxWait)
    window.addEventListener('online', done, { once: true })

    function done() {
      window.clearTimeout(timeout)
      window.removeEventListener('online', done)
      resolve()
    }
  })
}

function networkErrorMessage(error: AxiosError): string {
  if (!navigator.onLine) return '网络连接已断开，请检查网络后重试'
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
    return '网络响应超时，请稍后重试'
  }
  if (error.response?.status === 429) return '请求过于频繁，请稍后重试'
  if (error.response && error.response.status >= 500) return '服务暂时不可用，请稍后重试'
  return (
    (error.response?.data as { message?: string } | undefined)?.message ||
    error.message ||
    '网络请求失败'
  )
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const resilientConfig = config as ResilientRequestConfig
    if (!resilientConfig.__requestToken) {
      resilientConfig.__requestToken = beginRequest()
    }
    const token = getToken()
    if (token) {
      config.headers.jmiopenatom = token
      config.headers.Authorization = `Bearer ${token}` // 兼容常见 Bearer 鉴权
    }
    return config
  },
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const config = response.config as ResilientRequestConfig
    endRequest(config.__requestToken)
    const body = response.data
    // 后端统一返回 ApiResponse，业务失败时直接抛给页面提示。
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      if (UNAUTHORIZED_CODES.has(Number(body.code))) {
        redirectToLogin(body.message)
        return Promise.reject(body)
      }
      if (!config.silent) showError(body.message || '操作失败')
      return Promise.reject(body)
    }
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
  },
  async (error: AxiosError) => {
    const config = error.config as ResilientRequestConfig | undefined
    if (axios.isCancel(error)) {
      endRequest(config?.__requestToken)
      return Promise.reject(error)
    }

    if (config && isRetryable(error, config)) {
      const currentRetry = config.__retryCount || 0
      const maxRetries = config.retry ?? DEFAULT_RETRY_COUNT
      if (currentRetry < maxRetries) {
        config.__retryCount = currentRetry + 1
        await waitForOnline()
        await retryDelay(config.__retryCount)
        return service.request(config)
      }
    }

    endRequest(config?.__requestToken)
    const status = error.response?.status
    if (status === 401) {
      redirectToLogin((error.response?.data as { message?: string } | undefined)?.message)
    } else if (!config?.silent) {
      showError(networkErrorMessage(error))
    }
    return Promise.reject(error)
  },
)

export default service
