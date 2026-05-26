import axios, {type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig,} from 'axios'
import {ElMessage} from 'element-plus'
import {clearSession, getToken} from '@/utils/auth.ts'

const UNAUTHORIZED_CODES = new Set([401, 40100])
let redirectingToLogin = false

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 150000,
  withCredentials: true,
})

function currentRedirectPath(): string {
  const { pathname, search, hash } = window.location
  return `${pathname}${search}${hash}` || '/'
}

function redirectToLogin(message?: string) {
  clearSession()
  ElMessage.error(message || '登录已过期，请重新登录')

  if (redirectingToLogin || window.location.pathname === '/admin/login') return

  redirectingToLogin = true
  const redirect = encodeURIComponent(currentRedirectPath())
  window.location.replace(`/admin/login?redirect=${redirect}`)
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
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
    const body = response.data
    // 后端统一返回 ApiResponse，业务失败时直接抛给页面提示。
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      if (UNAUTHORIZED_CODES.has(Number(body.code))) {
        redirectToLogin(body.message)
        return Promise.reject(body)
      }
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(body)
    }
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      redirectToLogin(error.response?.data?.message)
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络请求失败')
    }
    return Promise.reject(error)
  },
)

export default service
