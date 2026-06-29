# 认证与权限

## Sa-Token 集成

系统使用 [Sa-Token](https://sa-token.cc) 作为认证与权限框架，版本 1.37.0，采用 JWT 模式。

### 配置

```yaml
sa-token:
  token-name: jmiopenatom              # Token 名称（请求头名称）
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET_KEY:...}  # JWT 密钥
  timeout: 2592000                      # Token 有效期（30 天，单位秒）
  active-timeout: -1                    # 临时 Token 有效期（-1 不限）
  is-concurrent: true                   # 允许同账号多端登录
  is-share: false                       # 不共享 Token
  token-style: uuid                     # Token 风格（UUID）
  is-log: true                          # 日志输出
```

### 登录流程

```
前端                           后端                           Redis
  │                              │                              │
  │  POST /api/v1/auth/login     │                              │
  │  { username, password }      │                              │
  │─────────────────────────────>│                              │
  │                              │  PasswordService.verify()    │
  │                              │  StpUtil.login(userId)       │
  │                              │  生成 JWT Token               │
  │                              │─────────────────────────────>│
  │                              │  存储 Session                │
  │                              │<─────────────────────────────│
  │  Result.success(tokenInfo)   │                              │
  │<─────────────────────────────│                              │
  │                              │                              │
  │  后续请求: Header jmiopenatom = {token}                      │
  │─────────────────────────────>│                              │
  │                              │  StpUtil.checkLogin()        │
  │                              │  StpUtil.checkPermission()   │
  │                              │<─────────────────────────────│
  │  响应数据                     │                              │
  │<─────────────────────────────│                              │
```

### Token 传递方式

前端通过请求头传递 Token：

```
jmiopenatom: {token}
Authorization: Bearer {token}
```

前端 Axios 拦截器自动添加两种格式以兼容不同场景。

## RBAC 权限模型

系统采用 RBAC（Role-Based Access Control）模型：

```
用户 (User) ──关联──> 角色 (Role) ──关联──> 权限 (Permission)
```

### 权限点定义

权限点在 `SystemPermission` 枚举中定义，包含 100+ 个权限点，每个权限点包含：

| 属性 | 说明 | 示例 |
|------|------|------|
| `displayName` | 权限名称 | "创建社团" |
| `code` | 权限编码 | "club:create" |
| `type` | 权限类型 | "api" |
| `path` | API 路径 | "/clubs" |
| `method` | HTTP 方法 | "POST" |

### 接口级权限控制

Controller 方法使用 Sa-Token 注解进行权限校验：

```java
@RestController
@RequestMapping("/clubs")
public class ClubController {

    @SaCheckPermission("club:create")
    @PostMapping
    public Result<Club> create(@Valid @RequestBody RequestCreateClubDTO dto) {
        return Result.success(clubService.create(dto));
    }

    @SaCheckPermission("club:list")
    @GetMapping
    public Result<Page<Club>> list(PageRequest request) {
        return Result.success(clubService.list(request));
    }
}
```

### 权限接口实现

`SaPermissionInterfaceImpl` 实现了 Sa-Token 的 `StpInterface` 接口，从数据库加载用户权限列表：

```java
@Component
public class SaPermissionInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 查询用户角色 → 查询角色权限 → 返回权限编码列表
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 查询用户角色列表
    }
}
```

## OIDC 认证中心

系统内置 OAuth2/OIDC 认证中心，支持第三方应用接入：

### 端点

| 端点 | 路径 | 说明 |
|------|------|------|
| 授权端点 | `/api/v1/oauth/authorize` | 授权码获取 |
| Token 端点 | `/api/v1/oauth/token` | Token 交换 |
| UserInfo 端点 | `/api/v1/oauth/userinfo` | 用户信息 |
| 发现文档 | `/api/v1/.well-known/openid-configuration` | OIDC Discovery |

### 客户端管理

通过 `OauthClientController` 管理OAuth 客户端：

- 创建客户端（client_id / client_secret）
- 配置回调地址
- 配置授权范围（scope）
- 启用/禁用客户端

### 相关实体

- `OauthClient` — 客户端配置
- `OauthAuthorizationCode` — 授权码

## 密码服务

`PasswordService` 封装密码加密与校验：

- 使用 `spring-security-crypto` 的 `BCryptPasswordEncoder`
- 密码哈希存储，不可逆
- 注册 / 修改密码时自动加密

## 多端登录

系统支持多种登录方式：

| 登录方式 | 接口 | 说明 |
|----------|------|------|
| 账号密码 | `POST /auth/login` | 管理后台登录 |
| 公开登录 | `POST /auth/public-login` | 前台用户登录 |
| 小程序登录 | `POST /auth/miniapp-login` | 微信小程序登录 |
| OIDC 登录 | `GET /oauth/authorize` | 第三方应用 OIDC 登录 |
| QQ 绑定 | `POST /auth/confirm-qq-bind` | QQ 账号绑定 |

## 限流

`ApplicationSubmitRateLimitInterceptor` 基于 Redis 实现报名提交限流：

- 默认限制：5 次/秒
- 可配置：`app.rate-limit.application-submit.max-per-second`
- 超限抛出 `RateLimitExceededException`
