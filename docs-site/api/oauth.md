# OpenAtom OAuth 2.0 / OIDC 使用文档

本文档对应当前 `openatom-system` 的认证中心实现。系统支持：

- OAuth 2.0 Authorization Code 授权码模式
- OpenID Connect（OIDC）
- PKCE（推荐使用 `S256`）
- Refresh Token
- UserInfo
- Token Introspection

## 1. 服务地址

生产环境 Issuer：

```text
https://oauth.jmi-openatom.cn/api/v1
```

OIDC Discovery：

```text
GET https://oauth.jmi-openatom.cn/api/v1/.well-known/openid-configuration
```

主要端点：

| 用途 | 方法 | 地址 |
| --- | --- | --- |
| 发起授权 | GET | `/oauth/authorize` |
| 换取或刷新令牌 | POST | `/oauth/token` |
| 获取用户信息 | GET | `/oauth/userinfo` |
| 检查令牌 | POST | `/oauth/introspect` |
| 获取签名密钥描述 | GET | `/oauth/jwks` |

下文使用：

```bash
OIDC_ISSUER=https://oauth.jmi-openatom.cn/api/v1
```

## 2. 注册 OAuth 客户端

管理员进入：

```text
管理后台 -> 认证应用 -> 新增应用
```

需要配置：

| 字段 | 说明 | 示例 |
| --- | --- | --- |
| 应用名称 | 后台展示名称 | `实验室管理系统` |
| Client ID | 客户端唯一标识 | `lab-lms` |
| Client Secret | 机密客户端密钥；纯前端应用留空 | `请使用随机强密钥` |
| 回调地址 | 授权成功后的回调地址，多个地址使用英文逗号分隔 | `https://example.com/auth/callback` |
| Scopes | 空格分隔 | `openid profile email roles permissions` |
| Grant Types | 空格分隔 | `authorization_code refresh_token` |

回调地址采用精确匹配，包括协议、域名、端口和路径。例如：

```text
https://example.com/auth/callback
```

与以下地址均不相同：

```text
http://example.com/auth/callback
https://example.com/auth/callback/
https://www.example.com/auth/callback
```

### 客户端类型

- 浏览器 SPA、桌面端、移动端属于公开客户端：不应保存 `client_secret`，注册时留空，必须使用 PKCE。
- 有安全后端的 Web 应用属于机密客户端：在服务端保存 `client_secret`，不可发送到浏览器。

## 3. 授权码流程

### 3.1 生成 PKCE 参数

客户端生成一个高熵随机字符串作为 `code_verifier`，再计算：

```text
code_challenge = BASE64URL(SHA256(code_verifier))
```

浏览器示例：

```js
function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

export async function createPkce() {
  const random = crypto.getRandomValues(new Uint8Array(32))
  const codeVerifier = base64Url(random)
  const digest = await crypto.subtle.digest(
    'SHA-256',
    new TextEncoder().encode(codeVerifier),
  )

  return {
    codeVerifier,
    codeChallenge: base64Url(new Uint8Array(digest)),
  }
}
```

将 `code_verifier` 临时保存在当前登录会话中，回调换取令牌时使用。

### 3.2 跳转到授权端点

```text
GET /oauth/authorize
```

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `response_type` | 是 | 固定为 `code` |
| `client_id` | 是 | 注册的 Client ID |
| `redirect_uri` | 是 | 必须与注册值完全一致 |
| `scope` | 否 | 默认 `openid profile` |
| `state` | 强烈建议 | 防止 CSRF 的一次性随机值 |
| `nonce` | 建议 | 绑定本次 OIDC 登录 |
| `code_challenge` | 公开客户端必填 | PKCE Challenge |
| `code_challenge_method` | 公开客户端必填 | 推荐并固定使用 `S256` |

示例：

```text
https://oauth.jmi-openatom.cn/api/v1/oauth/authorize
  ?response_type=code
  &client_id=your-client-id
  &redirect_uri=https%3A%2F%2Fexample.com%2Fauth%2Fcallback
  &scope=openid%20profile%20email%20roles%20permissions
  &state=RANDOM_STATE
  &nonce=RANDOM_NONCE
  &code_challenge=PKCE_CODE_CHALLENGE
  &code_challenge_method=S256
```

实际使用时应拼成一行，并对参数进行 URL 编码。

授权成功后，认证中心重定向到：

```text
https://example.com/auth/callback?code=AUTHORIZATION_CODE&state=RANDOM_STATE
```

客户端必须先验证返回的 `state` 与本地保存值一致，再交换令牌。

授权码有效期为 5 分钟，并且只能使用一次。

## 4. 使用授权码换取令牌

```text
POST /oauth/token
Content-Type: application/x-www-form-urlencoded
```

公开客户端：

```bash
curl -X POST "$OIDC_ISSUER/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=authorization_code" \
  --data-urlencode "client_id=your-client-id" \
  --data-urlencode "code=AUTHORIZATION_CODE" \
  --data-urlencode "redirect_uri=https://example.com/auth/callback" \
  --data-urlencode "code_verifier=PKCE_CODE_VERIFIER"
```

机密客户端额外提交：

```bash
--data-urlencode "client_secret=YOUR_CLIENT_SECRET"
```

成功响应示例：

```json
{
  "access_token": "ACCESS_TOKEN",
  "id_token": "ID_TOKEN",
  "refresh_token": "REFRESH_TOKEN",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "openid profile email roles permissions",
  "user": {
    "sub": "1",
    "club_user_id": 1,
    "preferred_username": "username",
    "username": "username",
    "name": "张三",
    "nickname": "张三",
    "email": "user@example.com",
    "phone": "13800000000",
    "phone_number": "13800000000",
    "student_id": "20260001",
    "avatar": "https://example.com/avatar.png",
    "is_lab_member": true,
    "lab_role": 0,
    "roles": ["formal_member"],
    "permissions": ["activity:list"]
  },
  "issuer": "https://oauth.jmi-openatom.cn/api/v1"
}
```

令牌有效期：

- Access Token：1 小时
- ID Token：1 小时
- Refresh Token：7 天

## 5. 调用 UserInfo

```bash
curl "$OIDC_ISSUER/oauth/userinfo" \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

响应字段与换取令牌结果中的 `user` 基本一致。

推荐以后端返回的 `sub` 作为用户稳定唯一标识，不要使用用户名、姓名、邮箱或手机号作为关联主键。

## 6. 刷新令牌

```bash
curl -X POST "$OIDC_ISSUER/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=refresh_token" \
  --data-urlencode "client_id=your-client-id" \
  --data-urlencode "refresh_token=REFRESH_TOKEN"
```

机密客户端同样需要提交：

```bash
--data-urlencode "client_secret=YOUR_CLIENT_SECRET"
```

刷新成功后会返回一组新的 Access Token、ID Token 和 Refresh Token。旧 Refresh Token 会立即失效，因此客户端必须原子地替换整组令牌。

## 7. Token Introspection

```bash
curl -X POST "$OIDC_ISSUER/oauth/introspect" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "token=ACCESS_TOKEN"
```

有效令牌示例：

```json
{
  "active": true,
  "sub": "1",
  "username": "username",
  "name": "张三",
  "client_id": "your-client-id",
  "scope": "openid profile",
  "exp": 1780000000,
  "roles": ["formal_member"],
  "permissions": ["activity:list"]
}
```

无效或过期令牌返回：

```json
{
  "active": false,
  "sub": null,
  "username": null,
  "name": null,
  "client_id": null,
  "scope": null,
  "exp": null,
  "roles": null,
  "permissions": null
}
```

## 8. Scope 与用户字段

支持的 Scope：

| Scope | 用途 |
| --- | --- |
| `openid` | 启用 OIDC；系统会自动保留该 Scope |
| `profile` | 用户名、姓名、头像等基本资料 |
| `email` | 邮箱 |
| `roles` | 系统角色 |
| `permissions` | 系统权限 |

服务端只会授予客户端已注册允许的 Scope。请求未填写 Scope 时，默认申请：

```text
openid profile
```

注意：当前实现的 UserInfo 返回字段尚未按 Scope 逐字段裁剪。客户端仍应只使用业务实际需要的数据。

## 9. 前端接入示例

```js
const issuer = 'https://oauth.jmi-openatom.cn/api/v1'
const clientId = 'your-client-id'
const redirectUri = `${window.location.origin}/auth/callback`

async function login() {
  const { codeVerifier, codeChallenge } = await createPkce()
  const state = crypto.randomUUID()
  const nonce = crypto.randomUUID()

  sessionStorage.setItem('oauth_code_verifier', codeVerifier)
  sessionStorage.setItem('oauth_state', state)
  sessionStorage.setItem('oauth_nonce', nonce)

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: 'openid profile email',
    state,
    nonce,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
  })

  window.location.assign(`${issuer}/oauth/authorize?${params}`)
}

async function handleCallback() {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const state = params.get('state')

  if (!code || state !== sessionStorage.getItem('oauth_state')) {
    throw new Error('OAuth 回调校验失败')
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: clientId,
    code,
    redirect_uri: redirectUri,
    code_verifier: sessionStorage.getItem('oauth_code_verifier') || '',
  })

  const response = await fetch(`${issuer}/oauth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })

  if (!response.ok) throw new Error('换取令牌失败')
  return response.json()
}
```

生产应用应优先采用“后端代理/BFF”保存 Refresh Token。若必须由 SPA 保存令牌，避免使用长期持久化存储，并配合严格的 CSP 和 XSS 防护。

## 10. 常见错误

| 错误 | HTTP 状态 | 常见原因 |
| --- | --- | --- |
| `invalid_client` | 401 | Client ID 不存在、应用被禁用或 Client Secret 错误 |
| `invalid_grant` | 400 | 授权码无效、过期、已使用，回调地址不一致，PKCE 校验失败，或 Refresh Token 无效 |
| `unsupported_grant_type` | 400 | `grant_type` 不是 `authorization_code` 或 `refresh_token` |
| `unsupported_response_type` | 重定向错误 | `response_type` 不是 `code`，或客户端未允许授权码模式 |
| `invalid_token` | 401 | Access Token 缺失、无效或已过期 |

排查重点：

1. `redirect_uri` 是否与后台配置完全一致。
2. Token 请求是否使用 `application/x-www-form-urlencoded`，而不是 JSON。
3. PKCE 的 `code_verifier` 是否与发起授权时属于同一次会话。
4. Refresh Token 是否已被使用过；当前实现会轮换 Refresh Token。
5. 服务器时间是否准确。

## 11. 当前实现的安全注意事项

1. 公开客户端务必使用 PKCE `S256`。服务端目前兼容不带 PKCE 和 `plain` 的请求，这是兼容能力，不是推荐用法。
2. `client_secret` 只能存放在可信后端，不能写入 SPA、移动端安装包或公开仓库。
3. `state` 必须是不可预测的一次性随机值，不能仅用作回跳路径。
4. 当前 `/oauth/introspect` 未要求客户端认证，生产环境如需对公网开放，建议增加机密客户端认证或限制为内网访问。
5. 当前 JWKS 使用对称算法 HS256，并以 `oct` JWK 形式输出签名密钥材料。不要将该端点公开给不可信客户端；生产环境建议改用 RS256/ES256，只发布公钥。
6. 当前实现不提供 OAuth 撤销端点。Access Token 在到期前不能通过标准 `/oauth/revoke` 主动撤销。
