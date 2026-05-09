// ============================================================
// Model Layer: Global reactive state store (MVVM Model)
// Lightweight alternative to Vuex/Pinia for uni-app
// ============================================================
import { reactive } from 'vue'

const store = reactive({
  // ---- User State ----
  user: {
    token: uni.getStorageSync('token') || '',
    info: null,
    isLogin: false
  },

  // ---- App State ----
  unreadCount: 0,
  tabIndex: 0,

  // ---- Actions ----
  setToken(token) {
    this.user.token = token
    this.user.isLogin = !!token
    uni.setStorageSync('token', token)
  },

  setUserInfo(info) {
    this.user.info = info
  },

  logout() {
    this.user.token = ''
    this.user.info = null
    this.user.isLogin = false
    uni.removeStorageSync('token')
  },

  setUnreadCount(count) {
    this.unreadCount = count
  },

  setTabIndex(index) {
    this.tabIndex = index
  }
})

export default store
