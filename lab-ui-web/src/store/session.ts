import { reactive } from 'vue'

export interface LabUser {
  id: number
  clubUserId: number
  username?: string
  nickname?: string
  avatarUrl?: string
  email?: string
  phone?: string
  labRole: number
  reputationScore: number
}

const TOKEN_KEY = 'lab_token'
const USER_KEY = 'lab_user'

export const session = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),
})

function readUser(): LabUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as LabUser
  } catch {
    return null
  }
}

export function setSession(token: string, user: LabUser) {
  session.token = token
  session.user = user
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  session.token = ''
  session.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function isAdmin() {
  return Boolean(session.user && session.user.labRole >= 1)
}
