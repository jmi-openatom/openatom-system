import axios, { type AxiosRequestConfig } from 'axios'
import { clearSession, session } from '@/store/session'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  withCredentials: true,
})

service.interceptors.request.use((config) => {
  if (session.token) {
    config.headers.Authorization = `Bearer ${session.token}`
    config.headers.jmilab = session.token
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
      if (Number(body.code) === 40100) clearSession()
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
  },
  (error) => {
    if (error.response?.status === 401) clearSession()
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
