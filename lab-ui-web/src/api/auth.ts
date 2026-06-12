import request from '@/api/request'
import type { LabUser } from '@/store/session'

export interface LoginResponse {
  token: string
  expiresAt: string
  user: LabUser
}

export function getOauthUrl() {
  return request.get<{ authorizeUrl: string; state: string }>('/auth/oauth/url')
}

export function oauthCallback(code: string, state: string) {
  return request.post<LoginResponse>('/auth/oauth/callback', { code, state })
}

export function devLogin(payload: { clubUserId: number; username: string; nickname: string; labRole: number }) {
  return request.post<LoginResponse>('/auth/dev-login', payload)
}

export function me() {
  return request.get<LabUser>('/auth/me')
}
