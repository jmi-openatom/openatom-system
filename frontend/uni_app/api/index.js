// ============================================================
// Model Layer: Business API services
// ============================================================
import { get, post, put } from './request'

// ---- Example: User API ----
export const userApi = {
  login: (data) => post('/api/user/login', data),
  getInfo: () => get('/api/user/info'),
  updateProfile: (data) => put('/api/user/profile', data)
}

// ---- Example: Home API ----
export const homeApi = {
  getBanners: () => get('/api/home/banners'),
  getNoticeList: (page = 1) => get('/api/home/notices', { page })
}

export default { userApi, homeApi }
