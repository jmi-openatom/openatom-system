import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getToken } from '../utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    // 后端 Sa-Token 支持自定义 token-name，这里同时兼容常见 Bearer 鉴权。
    config.headers.Authorization = `Bearer ${token}`
    config.headers.jmiopenatom = token
  }
  return config
})

service.interceptors.response.use(
  (response) => {
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
  }
)

export default service
