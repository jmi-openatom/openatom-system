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
| `mail` | 邮件系统兼容 Scope |
| `roles` | 系统角色 |
| `permissions` | 系统权限 |

服务端只会授予客户端已注册允许的 Scope。请求未填写 Scope 时，默认申请：

```text
openid profile
```

注意：当前实现的 UserInfo 返回字段尚未按 Scope 逐字段裁剪。客户端仍应只使用业务实际需要的数据。

## 9. 推荐接入架构（Web 应用）

生产 Web 应用推荐采用 BFF（Backend for Frontend）模式：浏览器只持有业务系统自己的 `HttpOnly` 会话 Cookie，OAuth 的授权码交换、令牌校验、刷新和令牌保存全部在业务后端完成。

```text
浏览器 -> 业务后端 /auth/login -> OpenAtom /oauth/authorize
浏览器 <- 302 回调 /auth/callback?code=...&state=...
业务后端 -> OpenAtom /oauth/token -> 校验 ID Token -> 建立本地会话
浏览器 -> 业务后端 /api/me（仅携带本地 HttpOnly Cookie）
```

业务系统至少实现以下路由：

| 路由 | 行为 |
| --- | --- |
| `GET /auth/login` | 生成 `state`、`nonce`、PKCE 参数，保存到服务端短期会话并跳转授权端点 |
| `GET /auth/callback` | 校验 `state`，使用 `code_verifier` 换取令牌，校验 ID Token 后创建本地会话 |
| `GET /api/me` | 返回当前业务用户资料和本系统权限 |
| `POST /auth/logout` | 删除本地会话及服务端保存的 OAuth 令牌 |

回调处理成功后，以 `(issuer, sub)` 作为外部身份唯一键查询或创建本地用户。主站返回的 `roles` 和 `permissions` 只描述主站权限，不应自动映射成业务系统管理员权限。

校验 ID Token 时至少检查：RS256 签名、`iss`、`aud`、`exp`、`nbf` 和本次登录保存的 `nonce`。公钥从 Discovery 返回的 `jwks_uri` 获取并按 `kid` 选择，允许缓存并在遇到未知 `kid` 时刷新一次。

本地会话 Cookie 建议使用 `HttpOnly; Secure; SameSite=Lax; Path=/`。OAuth 回调会发生顶层跨站跳转，使用 `SameSite=Strict` 可能导致登录事务 Cookie 无法随回调发送。

当前认证中心没有标准 OIDC `end_session_endpoint` 或 OAuth Token Revocation Endpoint。业务系统退出时应销毁自己的会话和令牌；这不会同时退出主站登录态，也不能提前撤销已经签发的 Access Token。

## 10. 纯前端接入示例

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

此示例只展示协议交互。完整实现还必须使用成熟的 OIDC/JWT 库按上一节规则校验 `id_token`，尤其是签名、`iss`、`aud`、有效期和 `nonce`。不要只解码 JWT 后直接信任其中字段。

生产应用应优先采用 BFF 保存 Refresh Token。若必须由 SPA 保存令牌，应使用内存或当前标签页的短期存储，避免将 Refresh Token 放入 `localStorage`，并配合严格的 CSP 和 XSS 防护。

## 11. 常见错误

| 错误 | HTTP 状态 | 常见原因 |
| --- | --- | --- |
| `invalid_client` | 401 | Client ID 不存在、应用被禁用或 Client Secret 错误 |
| `invalid_grant` | 400 | 授权码无效、过期、已使用，回调地址不一致，PKCE 校验失败，或 Refresh Token 无效 |
| `invalid_request` | 授权重定向错误 | 公开客户端未提供 PKCE，或 `code_challenge_method` 不是 `S256` |
| `unsupported_grant_type` | 400 | `grant_type` 不是 `authorization_code` 或 `refresh_token` |
| `unsupported_response_type` | 重定向错误 | `response_type` 不是 `code`，或客户端未允许授权码模式 |
| `invalid_token` | 401 | Access Token 缺失、无效或已过期 |

排查重点：

1. `redirect_uri` 是否与后台配置完全一致。
2. Token 请求是否使用 `application/x-www-form-urlencoded`，而不是 JSON。
3. PKCE 的 `code_verifier` 是否与发起授权时属于同一次会话。
4. Refresh Token 是否已被使用过；当前实现会轮换 Refresh Token。
5. 服务器时间是否准确。

## 12. 当前实现边界

1. 公开客户端必须使用 PKCE `S256`；缺少 `code_challenge` 或使用其他 Challenge Method 会被拒绝。
2. `client_secret` 只能存放在可信后端，不能写入 SPA、移动端安装包或公开仓库。
3. `state` 必须是不可预测的一次性随机值，不能仅用作回跳路径。
4. 当前 `/oauth/introspect` 未要求客户端认证，生产环境如需对公网开放，建议增加机密客户端认证或限制为内网访问。
5. ID Token 和 Access Token 使用 RS256 签名，JWKS 只发布 RSA 公钥。客户端应通过 Discovery 和 JWKS 验签，不要内置固定公钥。
6. 当前实现不提供 OAuth 撤销端点。Access Token 在到期前不能通过标准 `/oauth/revoke` 主动撤销。

## 13. 给 Vibe Coding 的完整实施提示词

将下面内容整体复制给 Cursor、Codex、Claude Code 等编程工具，并把尖括号占位符替换成你的实际信息：

```text
请在当前项目中完成 OpenAtom OAuth 2.0 / OIDC 登录接入，并直接实现、运行检查和修复发现的问题。

认证中心：
- Issuer: https://oauth.jmi-openatom.cn/api/v1
- Discovery: https://oauth.jmi-openatom.cn/api/v1/.well-known/openid-configuration
- 使用 Authorization Code Flow + PKCE S256
- client_id: <CLIENT_ID>
- client_secret: <CLIENT_SECRET；公开客户端留空，严禁写入前端>
- redirect_uri: <完整回调地址，必须与认证中心登记值逐字符一致>
- scope: openid profile email

实现要求：
1. 先检查当前项目的技术栈、路由、会话和环境变量规范，复用已有依赖；不要另建演示项目。
2. 如果项目有可信后端，采用 BFF：OAuth token 只保存在服务端，浏览器只保存 HttpOnly、Secure、SameSite=Lax 的本地会话 Cookie。如果确实是纯 SPA，使用公开客户端且必须使用 PKCE，不得使用 client_secret。
3. 实现 GET /auth/login：生成密码学随机的 state、nonce、code_verifier，计算 S256 code_challenge；将前三者绑定到一次性、短期登录事务（建议 5 分钟）；跳转 authorization_endpoint。
4. 实现 GET /auth/callback：处理 OAuth error；校验 code 和 state；原子消费登录事务；使用 application/x-www-form-urlencoded 请求 token_endpoint，redirect_uri 必须与登录时完全相同。
5. 使用成熟 OIDC/JWT 库校验 id_token：算法只能是 RS256；通过 Discovery 获取 jwks_uri 并按 kid 验签；验证 iss、aud、exp、nbf 和 nonce。禁止只 decode JWT，禁止关闭签名校验。
6. 使用 issuer + sub 作为外部账号稳定唯一键，创建或关联本地用户。显示名、邮箱、用户名都不能作为唯一身份键。主站 roles/permissions 不自动授予本系统管理权限。
7. 建立本地会话并实现 GET /api/me。登录成功后清理 URL 中的 code/state，跳转到安全的站内地址；回跳地址只允许相对路径或白名单地址，避免开放重定向。
8. 实现 Access Token 到期前刷新。刷新请求使用 grant_type=refresh_token；认证中心会轮换 Refresh Token，必须在一次事务中替换整组 token，旧 Refresh Token 不得复用。并发刷新要加锁或做单飞控制。
9. 实现 POST /auth/logout：删除本地会话、OAuth token 和登录事务。当前提供方没有 revoke/end-session 端点，不要调用不存在的接口，也不要宣称会退出主站登录态。
10. 错误日志只记录 error code、HTTP 状态和 correlation id，不记录 authorization code、access_token、id_token、refresh_token、client_secret 或完整用户隐私数据。
11. 所有密钥和地址使用环境变量；提交 .env.example，但不要提交真实 secret。提供 CLIENT_ID、CLIENT_SECRET、OAUTH_ISSUER、OAUTH_REDIRECT_URI、SESSION_SECRET 示例。
12. 添加关键测试：state 不匹配、事务重放、PKCE、错误 issuer/audience/nonce、过期 token、未知 kid 刷新 JWKS、redirect_uri 精确匹配、Refresh Token 轮换和并发刷新。
13. 最后给出：修改文件列表、环境变量、认证中心后台登记值、启动命令、测试结果，以及从登录到退出的人工验收步骤。不要只写方案，完成实际代码。

认证中心的已知行为：
- Authorization Code 有效期 5 分钟且只能使用一次。
- Access Token 和 ID Token 有效期 1 小时。
- Refresh Token 有效期 7 天，刷新后立即轮换。
- Token Endpoint 的客户端认证方式为 none 或 client_secret_post。
- 用户唯一标识字段为 sub。
- 可调用 UserInfo Endpoint 获取资料；Authorization 使用 Bearer Access Token。
```

## 14. 上线验收清单

- 后台登记的 `redirect_uri` 与程序实际发送值完全一致。
- 公开客户端没有 Secret，且每次登录都生成新的 PKCE、`state` 和 `nonce`。
- 机密客户端的 Secret 只存在服务端环境变量或密钥系统中。
- 回调能拒绝错误 `state`、错误 `nonce`、过期或重复使用的授权码。
- ID Token 已通过 RS256、Issuer、Audience、时间和 Nonce 校验。
- 数据库用 `issuer + sub` 关联用户，业务权限由本系统管理。
- 刷新后原子保存新的 Refresh Token，多请求不会并发重复刷新。
- 日志、监控、错误页面和浏览器地址中没有泄露 Token 或 Secret。
- 退出后本地 Cookie 和服务端 Token 已清除，并明确当前不会退出主站会话。
- 生产环境全程 HTTPS，Cookie 标记为 `HttpOnly`、`Secure`、`SameSite=Lax`。
