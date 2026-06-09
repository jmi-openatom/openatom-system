import { getToken } from '@/utils/auth.ts'

export function getOidcAuthority(): string {
  const configured = import.meta.env.VITE_OIDC_AUTHORITY || 'https://oauth.jmi-openatom.cn/api/v1'
  return configured.replace(/\/+$/, '')
}

export function getOidcClientId(): string {
  return import.meta.env.VITE_OIDC_CLIENT_ID || 'openatom-web'
}

export function buildOidcAuthorizeUrl(redirectPath: string): string {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: getOidcClientId(),
    redirect_uri: `${window.location.origin}/auth/callback`,
    scope: 'openid profile email roles permissions',
    state: redirectPath || '/admin/dashboard',
  })
  const token = getToken()
  if (token) {
    params.set('jmiopenatom', token)
  }
  return `${getOidcAuthority()}/oauth/authorize?${params.toString()}`
}
