import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  timeout: 12_000,
})

export function getErrorMessage(error: unknown, fallback = '请求失败，请稍后重试') {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.message || (error.code === 'ECONNABORTED' ? '请求超时，请重试' : fallback)
  }
  return error instanceof Error ? error.message : fallback
}
