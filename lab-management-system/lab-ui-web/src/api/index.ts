import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('lms-token')
  if (token) {
    config.headers['lms-token'] = token
  }
  return config
})

export const authAPI = {
  login: (cmsToken: string) => api.post('/auth/login', { cmsToken }),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout')
}

export const problemAPI = {
  getToday: () => api.get('/problems/today'),
  getById: (id: number) => api.get(`/problems/${id}`)
}

export const submissionAPI = {
  submit: (data: any) => api.post('/submissions', data),
  getMy: () => api.get('/submissions/my')
}

export const checkinAPI = {
  checkin: () => api.post('/checkin')
}

export default api
