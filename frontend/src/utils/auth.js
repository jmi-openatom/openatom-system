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
  localStorage.setItem(TOKEN_KEY, data.accessToken || data.token || '')
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken || '')
  localStorage.setItem(USER_KEY, JSON.stringify(data.user || {}))
  localStorage.setItem(ROLE_KEY, JSON.stringify(data.roles || []))
  localStorage.setItem(PERMISSION_KEY, JSON.stringify(data.permissions || []))
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
