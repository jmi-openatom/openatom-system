const TOKEN_KEY = 'openatom_token'
const REFRESH_TOKEN_KEY = 'openatom_refresh_token'
const USER_KEY = 'openatom_user'
const ROLE_KEY = 'openatom_roles'
const PERMISSION_KEY = 'openatom_permissions'
const REMEMBER_LOGIN_KEY = 'openatom_remember_login'

interface SessionPayload {
  accessToken?: string
  token?: string
  refreshToken?: string
  user?: Record<string, unknown>
  roles?: string[]
  permissions?: string[]
}

interface LoginPayload {
  username?: string
  password?: string
  remember?: boolean
}

export function getToken(): string | null {
  return uni.getStorageSync(TOKEN_KEY) || null
}

export function setSession(payload?: SessionPayload): void {
  const data = payload || {}
  const hasAccessToken =
    Object.prototype.hasOwnProperty.call(data, 'accessToken') ||
    Object.prototype.hasOwnProperty.call(data, 'token')
  const hasRefreshToken = Object.prototype.hasOwnProperty.call(data, 'refreshToken')
  const hasUser = Object.prototype.hasOwnProperty.call(data, 'user')
  const hasRoles = Object.prototype.hasOwnProperty.call(data, 'roles')
  const hasPermissions = Object.prototype.hasOwnProperty.call(data, 'permissions')

  uni.setStorageSync(
    TOKEN_KEY,
    hasAccessToken ? data.accessToken || data.token || '' : uni.getStorageSync(TOKEN_KEY) || '',
  )
  uni.setStorageSync(
    REFRESH_TOKEN_KEY,
    hasRefreshToken ? data.refreshToken || '' : uni.getStorageSync(REFRESH_TOKEN_KEY) || '',
  )
  uni.setStorageSync(USER_KEY, JSON.stringify(hasUser ? data.user || {} : getCurrentUser()))
  uni.setStorageSync(ROLE_KEY, JSON.stringify(hasRoles ? data.roles || [] : getCurrentRoles()))
  uni.setStorageSync(
    PERMISSION_KEY,
    JSON.stringify(hasPermissions ? data.permissions || [] : getCurrentPermissions()),
  )
}

export function clearSession(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
  uni.removeStorageSync(ROLE_KEY)
  uni.removeStorageSync(PERMISSION_KEY)
}

export function getCurrentUser(): Record<string, unknown> {
  try {
    return JSON.parse(uni.getStorageSync(USER_KEY) || '{}')
  } catch {
    return {}
  }
}

export function getCurrentRoles(): string[] {
  try {
    return JSON.parse(uni.getStorageSync(ROLE_KEY) || '[]')
  } catch {
    return []
  }
}

export function getCurrentPermissions(): string[] {
  try {
    return JSON.parse(uni.getStorageSync(PERMISSION_KEY) || '[]')
  } catch {
    return []
  }
}

export function getRememberedLogin(): LoginPayload {
  try {
    return JSON.parse(
      uni.getStorageSync(REMEMBER_LOGIN_KEY) || '{"username":"","password":"","remember":false}',
    )
  } catch {
    return { username: '', password: '', remember: false }
  }
}

export function setRememberedLogin(payload?: LoginPayload): void {
  uni.setStorageSync(
    REMEMBER_LOGIN_KEY,
    JSON.stringify({
      username: payload?.username || '',
      password: payload?.password || '',
      remember: Boolean(payload?.remember),
    }),
  )
}

export function clearRememberedLogin(): void {
  uni.removeStorageSync(REMEMBER_LOGIN_KEY)
}
