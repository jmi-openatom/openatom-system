const TOKEN_KEY = 'openatom_token'
const REFRESH_TOKEN_KEY = 'openatom_refresh_token'
const USER_KEY = 'openatom_user'
const ROLE_KEY = 'openatom_roles'
const PERMISSION_KEY = 'openatom_permissions'
const REMEMBER_LOGIN_KEY = 'openatom_remember_login'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setSession(payload) {
  const data = payload || {}
  const hasAccessToken = Object.prototype.hasOwnProperty.call(data, 'accessToken') || Object.prototype.hasOwnProperty.call(data, 'token')
  const hasRefreshToken = Object.prototype.hasOwnProperty.call(data, 'refreshToken')
  const hasUser = Object.prototype.hasOwnProperty.call(data, 'user')
  const hasRoles = Object.prototype.hasOwnProperty.call(data, 'roles')
  const hasPermissions = Object.prototype.hasOwnProperty.call(data, 'permissions')

  localStorage.setItem(TOKEN_KEY, hasAccessToken ? (data.accessToken || data.token || '') : (localStorage.getItem(TOKEN_KEY) || ''))
  localStorage.setItem(REFRESH_TOKEN_KEY, hasRefreshToken ? (data.refreshToken || '') : (localStorage.getItem(REFRESH_TOKEN_KEY) || ''))
  localStorage.setItem(USER_KEY, JSON.stringify(hasUser ? (data.user || {}) : getCurrentUser()))
  localStorage.setItem(ROLE_KEY, JSON.stringify(hasRoles ? (data.roles || []) : getCurrentRoles()))
  localStorage.setItem(PERMISSION_KEY, JSON.stringify(hasPermissions ? (data.permissions || []) : getCurrentPermissions()))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(PERMISSION_KEY)
}

export function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch (error) {
    return {}
  }
}

export function getCurrentRoles() {
  try {
    return JSON.parse(localStorage.getItem(ROLE_KEY) || '[]')
  } catch (error) {
    return []
  }
}

export function getCurrentPermissions() {
  try {
    return JSON.parse(localStorage.getItem(PERMISSION_KEY) || '[]')
  } catch (error) {
    return []
  }
}

export function getRememberedLogin() {
  try {
    return JSON.parse(localStorage.getItem(REMEMBER_LOGIN_KEY) || '{"username":"","password":"","remember":false}')
  } catch (error) {
    return { username: '', password: '', remember: false }
  }
}

export function setRememberedLogin(payload) {
  localStorage.setItem(
    REMEMBER_LOGIN_KEY,
    JSON.stringify({
      username: payload?.username || '',
      password: payload?.password || '',
      remember: Boolean(payload?.remember)
    })
  )
}

export function clearRememberedLogin() {
  localStorage.removeItem(REMEMBER_LOGIN_KEY)
}
