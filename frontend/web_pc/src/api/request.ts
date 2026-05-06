import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getToken } from '@/utils/auth.ts'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
})

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
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(body)
    }
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      clearSession()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/admin/login'
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络请求失败')
    }
    return Promise.reject(error)
  },
)

export default service
