import { reactive } from 'vue'
import {clearSession, getToken, setSession} from '@/utils/auth'

interface UserState {
  token: string
  info: Record<string, unknown> | null
  isLogin: boolean
}

interface StoreState {
  user: UserState
  unreadCount: number
  tabIndex: number
  setToken: (token: string) => void
  setUserInfo: (info: Record<string, unknown> | null) => void
  logout: () => void
  setUnreadCount: (count: number) => void
  setTabIndex: (index: number) => void
}

const initialToken = getToken()

const store = reactive<StoreState>({
  user: {
    token: typeof initialToken === 'string' ? initialToken : '',
    info: null,
    isLogin: !!initialToken,
  },
  unreadCount: 0,
  tabIndex: 0,
  setToken(token) {
    this.user.token = token
    this.user.isLogin = !!token
    setSession({accessToken: token})
  },
  setUserInfo(info) {
    this.user.info = info
  },
  logout() {
    this.user.token = ''
    this.user.info = null
    this.user.isLogin = false
    clearSession()
  },
  setUnreadCount(count) {
    this.unreadCount = count
  },
  setTabIndex(index) {
    this.tabIndex = index
  },
})

export default store
