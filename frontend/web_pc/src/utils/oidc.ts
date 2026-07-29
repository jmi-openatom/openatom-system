const STATE_KEY = 'openatom_oidc_state'
const NONCE_KEY = 'openatom_oidc_nonce'
const VERIFIER_KEY = 'openatom_oidc_code_verifier'
const RETURN_TO_KEY = 'openatom_oidc_return_to'

export function getOidcAuthority(): string {
  const configured = import.meta.env.VITE_OIDC_AUTHORITY || 'https://oauth.jmi-openatom.cn/api/v1'
  return configured.replace(/\/+$/, '')
}

export function getOidcClientId(): string {
  return import.meta.env.VITE_OIDC_CLIENT_ID || 'openatom-web'
}

function base64Url(bytes: Uint8Array): string {
  let value = ''
  bytes.forEach((byte) => {
    value += String.fromCharCode(byte)
  })
  return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function decodeBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  const decoded = atob(padded)
  return Uint8Array.from(decoded, (character) => character.charCodeAt(0))
}

function randomValue(): string {
  return base64Url(crypto.getRandomValues(new Uint8Array(32)))
}

export async function buildOidcAuthorizeUrl(redirectPath: string): Promise<string> {
  const verifier = randomValue()
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  const state = randomValue()
  const nonce = randomValue()

  sessionStorage.setItem(STATE_KEY, state)
  sessionStorage.setItem(NONCE_KEY, nonce)
  sessionStorage.setItem(VERIFIER_KEY, verifier)
  sessionStorage.setItem(RETURN_TO_KEY, redirectPath || '/admin/dashboard')

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: getOidcClientId(),
    redirect_uri: `${window.location.origin}/auth/callback`,
    scope: 'openid profile email roles permissions',
    state,
    nonce,
    code_challenge: base64Url(new Uint8Array(digest)),
    code_challenge_method: 'S256',
  })
  return `${getOidcAuthority()}/oauth/authorize?${params.toString()}`
}

export function consumeOidcCallbackState(receivedState?: string): {
  codeVerifier: string
  nonce: string
  returnTo: string
} {
  const expectedState = sessionStorage.getItem(STATE_KEY)
  const codeVerifier = sessionStorage.getItem(VERIFIER_KEY)
  const nonce = sessionStorage.getItem(NONCE_KEY)
  const returnTo = sessionStorage.getItem(RETURN_TO_KEY) || '/admin/dashboard'

  sessionStorage.removeItem(STATE_KEY)
  sessionStorage.removeItem(VERIFIER_KEY)
  sessionStorage.removeItem(NONCE_KEY)
  sessionStorage.removeItem(RETURN_TO_KEY)

  if (!receivedState || !expectedState || receivedState !== expectedState) {
    throw new Error('OAuth state 校验失败')
  }
  if (!codeVerifier || !nonce) throw new Error('OAuth 登录状态已过期')
  return { codeVerifier, nonce, returnTo }
}

export async function verifyOidcIdToken(idToken: string, expectedNonce: string): Promise<void> {
  const parts = idToken.split('.')
  if (parts.length !== 3) throw new Error('OIDC ID Token 格式无效')

  const header = JSON.parse(new TextDecoder().decode(decodeBase64Url(parts[0]))) as {
    alg?: string
    kid?: string
  }
  const claims = JSON.parse(new TextDecoder().decode(decodeBase64Url(parts[1]))) as {
    iss?: string
    aud?: string | string[]
    exp?: number
    nonce?: string
    token_use?: string
  }
  if (header.alg !== 'RS256' || !header.kid) throw new Error('OIDC 签名算法无效')

  const discoveryResponse = await fetch(`${getOidcAuthority()}/.well-known/openid-configuration`)
  if (!discoveryResponse.ok) throw new Error('无法读取 OIDC 配置')
  const discovery = (await discoveryResponse.json()) as { issuer?: string; jwks_uri?: string }
  if (!discovery.jwks_uri || discovery.issuer !== getOidcAuthority()) {
    throw new Error('OIDC Issuer 校验失败')
  }

  const jwksResponse = await fetch(discovery.jwks_uri)
  if (!jwksResponse.ok) throw new Error('无法读取 OIDC 公钥')
  const jwks = (await jwksResponse.json()) as {
    keys?: Array<JsonWebKey & { kid?: string; kty?: string; use?: string }>
  }
  const jwk = jwks.keys?.find((candidate) => candidate.kid === header.kid && candidate.kty === 'RSA')
  if (!jwk) throw new Error('找不到 OIDC 签名公钥')

  const key = await crypto.subtle.importKey(
    'jwk',
    jwk,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['verify'],
  )
  const valid = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5',
    key,
    decodeBase64Url(parts[2]),
    new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
  )
  if (!valid) throw new Error('OIDC ID Token 签名无效')

  const audiences = Array.isArray(claims.aud) ? claims.aud : [claims.aud]
  if (
    claims.iss !== getOidcAuthority() ||
    !audiences.includes(getOidcClientId()) ||
    claims.token_use !== 'id' ||
    claims.nonce !== expectedNonce ||
    !claims.exp ||
    claims.exp <= Math.floor(Date.now() / 1000)
  ) {
    throw new Error('OIDC ID Token Claims 校验失败')
  }
}
