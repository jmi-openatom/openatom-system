const TOKEN_KEY = 'openatom_token'
const REFRESH_TOKEN_KEY = 'openatom_refresh_token'
const USER_KEY = 'openatom_user'
const ROLE_KEY = 'openatom_roles'
const PERMISSION_KEY = 'openatom_permissions'
const REMEMBER_LOGIN_KEY = 'openatom_remember_login'
const SA_TOKEN_QUERY_KEY = 'jmiopenatom'

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

function normalizeObject(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
  return value as Record<string, unknown>
}

function normalizeStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.map((item) => String(item))
}

function normalizeUser(value: unknown): Record<string, unknown> {
  const user = normalizeObject(value)
  const userName = user.userName ?? user.username ?? user.preferred_username
  const realName = user.realName ?? user.name ?? user.nickname
  const studentId = user.studentId ?? user.student_id
  const qqOpenid = user.qqOpenid ?? user.qq_openid
  const onboardingCompletedAt = user.onboardingCompletedAt ?? user.onboarding_completed_at

  return {
    ...user,
    ...(userName !== undefined ? { userName, username: userName } : {}),
    ...(realName !== undefined ? { realName } : {}),
    ...(studentId !== undefined ? { studentId } : {}),
    ...(qqOpenid !== undefined ? { qqOpenid } : {}),
    ...(onboardingCompletedAt !== undefined ? { onboardingCompletedAt } : {}),
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function appendTokenQuery(target: string): string {
  const token = getToken()
  if (!target || !token) return target

  try {
    const isAbsolute = /^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(target)
    const url = new URL(target, window.location.origin)
    url.searchParams.set(SA_TOKEN_QUERY_KEY, token)
    return isAbsolute ? url.toString() : `${url.pathname}${url.search}${url.hash}`
  } catch (_error) {
    return target
  }
}

export function shouldUseFullPageAuthRedirect(target?: string): boolean {
  if (!target) return false
  if (target.startsWith('/api/') || target.startsWith('/oauth/')) return true

  try {
    const url = new URL(target, window.location.origin)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return false
    if (!trustedRedirectOrigins().includes(url.origin)) return false
    return url.pathname.startsWith('/api/') || url.pathname.startsWith('/oauth/')
  } catch (_error) {
    return false
  }
}

function trustedRedirectOrigins(): string[] {
  const origins = new Set<string>([window.location.origin])
  appendOrigin(origins, import.meta.env.VITE_API_BASE_URL)
  appendOrigin(
    origins,
    import.meta.env.VITE_OIDC_AUTHORITY || 'https://oauth.jmi-openatom.cn/api/v1',
  )
  return [...origins]
}

function appendOrigin(origins: Set<string>, value?: string) {
  if (!value) return
  try {
    origins.add(new URL(value, window.location.origin).origin)
  } catch (_error) {
    // Ignore invalid runtime env values.
  }
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

  localStorage.setItem(
    TOKEN_KEY,
    hasAccessToken ? data.accessToken || data.token || '' : localStorage.getItem(TOKEN_KEY) || '',
  )
  localStorage.setItem(
    REFRESH_TOKEN_KEY,
    hasRefreshToken ? data.refreshToken || '' : localStorage.getItem(REFRESH_TOKEN_KEY) || '',
  )
  localStorage.setItem(
    USER_KEY,
    JSON.stringify(normalizeUser(hasUser ? data.user : getCurrentUser())),
  )
  localStorage.setItem(
    ROLE_KEY,
    JSON.stringify(normalizeStringArray(hasRoles ? data.roles : getCurrentRoles())),
  )
  localStorage.setItem(
    PERMISSION_KEY,
    JSON.stringify(normalizeStringArray(hasPermissions ? data.permissions : getCurrentPermissions())),
  )
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(PERMISSION_KEY)
}

export function getCurrentUser(): Record<string, unknown> {
  try {
    return normalizeUser(JSON.parse(localStorage.getItem(USER_KEY) || '{}'))
  } catch (_error) {
    return {}
  }
}

export function getCurrentRoles(): string[] {
  try {
    return normalizeStringArray(JSON.parse(localStorage.getItem(ROLE_KEY) || '[]'))
  } catch (_error) {
    return []
  }
}

export function getCurrentPermissions(): string[] {
  try {
    return normalizeStringArray(JSON.parse(localStorage.getItem(PERMISSION_KEY) || '[]'))
  } catch (_error) {
    return []
  }
}

export function getRememberedLogin(): LoginPayload {
  try {
    return JSON.parse(
      localStorage.getItem(REMEMBER_LOGIN_KEY) || '{"username":"","password":"","remember":false}',
    )
  } catch (_error) {
    return { username: '', password: '', remember: false }
  }
}

export function setRememberedLogin(payload?: LoginPayload): void {
  localStorage.setItem(
    REMEMBER_LOGIN_KEY,
    JSON.stringify({
      username: payload?.username || '',
      password: payload?.password || '',
      remember: Boolean(payload?.remember),
    }),
  )
}

export function clearRememberedLogin(): void {
  localStorage.removeItem(REMEMBER_LOGIN_KEY)
}
