import { http, type ApiResponse } from './http'

export interface CurrentMember {
  id: number
  nickname: string | null
  avatarUrl: string | null
  status: 'ACTIVE' | 'DISABLED'
  currentLevel: string
  totalPoints: number
  profileCompleted: boolean
  onboardingCompleted: boolean
  roles: string[]
  permissions: string[]
}

export async function getSession(): Promise<CurrentMember> {
  const response = await http.get<ApiResponse<CurrentMember>>('/auth/session')
  return response.data.data
}

export async function logout(): Promise<string> {
  const response = await http.post<ApiResponse<{ redirect: string }>>('/auth/logout')
  return response.data.data.redirect
}
