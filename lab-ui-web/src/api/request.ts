import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { beginRequest, endRequest, reportRequestError } from '@/appStatus'
import { clearSession, session } from '@/store/session'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000,
  withCredentials: true,
})

type RetryConfig = InternalAxiosRequestConfig & {
  __retryCount?: number
  __tracked?: boolean
  retry?: number
}

service.interceptors.request.use((config) => {
  const retryConfig = config as RetryConfig
  if (!retryConfig.__tracked) {
    retryConfig.__tracked = true
    beginRequest()
  }
  if (session.token) {
    config.headers.Authorization = `Bearer ${session.token}`
    config.headers.jmilab = session.token
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    endRequest()
    const body = response.data
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      if (Number(body.code) === 40100) {
        clearSession()
        window.location.replace(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
      } else {
        reportRequestError(body.message || '请求失败')
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
  },
  async (error: AxiosError) => {
    const config = error.config as RetryConfig | undefined
    const status = error.response?.status
    const retryable =
      config &&
      (config.method || 'get').toUpperCase() === 'GET' &&
      (!error.response || [408, 425, 429, 500, 502, 503, 504].includes(status || 0))
    const retryCount = config?.__retryCount || 0
    if (retryable && retryCount < (config.retry ?? 1)) {
      config.__retryCount = retryCount + 1
      await new Promise((resolve) => window.setTimeout(resolve, 500 * 2 ** retryCount))
      return service.request(config)
    }

    endRequest()
    if (status === 401) {
      clearSession()
      window.location.replace(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
    } else {
      reportRequestError(
        !navigator.onLine
          ? '网络已断开，请检查连接'
          : error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
            ? '网络响应超时，请重试'
            : '网络请求失败，请重试',
      )
    }
    return Promise.reject(error)
  },
)

type ApiClient = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T>
}

export default service as unknown as ApiClient
