# 环境变量

## 概述

系统通过环境变量管理敏感配置和多环境差异。所有环境变量均可在 `docker-compose.yml` 或 `.env` 文件中设置。

## 环境变量清单

### Redis

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `REDIS_PASSWORD` | `openatom123` | Redis 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_DATABASE` | `0` | Redis 数据库编号 |
| `REDIS_TIMEOUT` | `5000ms` | Redis 超时时间 |
| `REDIS_POOL_MAX_ACTIVE` | `20` | 连接池最大活跃数 |
| `REDIS_POOL_MAX_IDLE` | `10` | 连接池最大空闲数 |
| `REDIS_POOL_MIN_IDLE` | `5` | 连接池最小空闲数 |
| `REDIS_POOL_MAX_WAIT` | `3000ms` | 连接池最大等待时间 |

### Sa-Token / JWT

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SA_TOKEN_JWT_SECRET_KEY` | 开发环境默认密钥 | JWT 签名密钥（**生产环境必须修改**） |
| `SA_TOKEN_TIMEOUT` | `2592000` | Token 有效期（秒，默认 30 天） |
| `SA_TOKEN_LOG_ENABLED` | `false` | 是否开启 Sa-Token 日志 |

### 数据库连接池

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_POOL_NAME` | `openatom-{env}-hikari` | 连接池名称 |
| `DB_POOL_MAX_SIZE` | dev: 10 / prod: 20 | 连接池最大大小 |
| `DB_POOL_MIN_IDLE` | dev: 2 / prod: 5 | 连接池最小空闲 |
| `DB_CONNECTION_TIMEOUT_MS` | `30000` | 连接超时时间 |
| `DB_IDLE_TIMEOUT_MS` | `600000` | 空闲超时时间 |
| `DB_MAX_LIFETIME_MS` | `1800000` | 连接最大生命周期 |

### 前端构建参数

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `VITE_APP_VERSION` | 自动从 git 获取 | 应用版本号 |
| `VITE_API_BASE_URL` | `https://api.jmi-openatom.cn/api/v1` | 后端 API 地址 |
| `VITE_OIDC_AUTHORITY` | `https://oauth.jmi-openatom.cn/api/v1` | OIDC 认证地址 |
| `VITE_OIDC_CLIENT_ID` | `openatom-web` | OIDC 客户端 ID |
| `FRONTEND_PORT` | `18080` | 前端映射端口 |
| `DOCS_PORT` | `16000` | 文档站映射端口 |

### 微信小程序

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `APP_MINIAPP_APP_ID` | `wx8c6b52ab95da0938` | 微信小程序 AppID |
| `APP_MINIAPP_APP_SECRET` | `d15542dcaa23b92191962235dacef7b9` | 微信小程序 AppSecret |

### AI（DeepSeek）

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek API 地址 |
| `DEEPSEEK_API_KEY` | （空） | DeepSeek API Key |
| `DEEPSEEK_MODEL` | `deepseek-chat` | DeepSeek 模型名称 |
| `DEEPSEEK_TIMEOUT_SECONDS` | `30` | DeepSeek 请求超时 |

### QQ 机器人

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `APP_BOT_CALLBACK_TOKEN` | （空） | 机器人回调 Token |
| `APP_BOT_LEAVE_REVIEW_CALLBACK_URL` | `http://127.0.0.1:6198/openatom/leave-review` | 请假审批回调 URL |
| `APP_NAPCAT_BASE_URL` | `http://127.0.0.1:3000` | NapCat API 地址 |
| `APP_NAPCAT_ACCESS_TOKEN` | `sRtnBQJy-lqPTWjj` | NapCat 访问 Token |
| `ASTRBOT_PORT` | `6185` | AstrBot 面板端口 |
| `NAPCAT_PORT` | `6099` | NapCat WebUI 端口 |
| `NAPCAT_UID` | `1000` | NapCat 运行用户 UID |
| `NAPCAT_GID` | `1000` | NapCat 运行用户 GID |

### OIDC

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `APP_OIDC_ISSUER` | `https://oauth.jmi-openatom.cn/api/v1` | OIDC 签发者地址 |

### 文件存储

| 变量名 | Docker 默认值 | 说明 |
|--------|-------------|------|
| `APP_AVATAR_STORAGE_DIR` | `/app/uploads/avatars` | 头像存储目录 |
| `APP_IMAGE_HOSTING_STORAGE_DIR` | `/app/uploads/images` | 图床存储目录 |
| `APP_LEAVE_ATTACHMENT_STORAGE_DIR` | `/app/uploads/leave-attachments` | 请假附件存储目录 |
| `APP_DOCUMENT_TEMPLATE_STORAGE_DIR` | `/app/uploads/document-templates` | 文书模板存储目录 |
| `APP_GENERATED_DOCUMENT_STORAGE_DIR` | `/app/uploads/generated-documents` | 生成文书存储目录 |

### 限流

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `APP_APPLICATION_SUBMIT_MAX_PER_SECOND` | `5` | 报名提交限流（次/秒） |

### 日志

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `LOG_LEVEL_ROOT` | `INFO` | 根日志级别 |
| `LOG_LEVEL_APP` | `INFO` | 应用日志级别 |
| `LOG_FILE` | `./logs/openatom-system.log` | 日志文件路径 |
| `TOMCAT_ACCESS_LOG_ENABLED` | `true` | Tomcat 访问日志开关 |
| `TOMCAT_ACCESS_LOG_DIR` | `logs` | 访问日志目录 |
| `TOMCAT_BASEDIR` | `/tmp/openatom-system` | Tomcat 基础目录 |

### Redis 缓存

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `APP_REDIS_CACHE_ENABLED` | `true` | Redis 缓存开关 |
| `APP_REDIS_CACHE_KEY_PREFIX` | `openatom:cache` | 缓存键前缀 |
| `APP_REDIS_CACHE_DEFAULT_TTL` | `10m` | 默认缓存过期时间 |
| `APP_REDIS_CACHE_SCAN_BATCH_SIZE` | `500` | 缓存扫描批量大小 |

### CORS

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | `*` | CORS 允许的来源 |

### 服务端口

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SERVER_PORT` | `8921` | 后端服务端口 |
| `MULTIPART_MAX_FILE_SIZE` | `20MB` | 上传文件最大大小 |
| `MULTIPART_MAX_REQUEST_SIZE` | `20MB` | 请求体最大大小 |

## .env 文件示例

在项目根目录创建 `.env` 文件：

```bash
# Redis
REDIS_PASSWORD=your_strong_redis_password

# JWT
SA_TOKEN_JWT_SECRET_KEY=your_very_long_and_random_jwt_secret_key

# AI
DEEPSEEK_API_KEY=sk-your-deepseek-api-key

# Bot
APP_BOT_CALLBACK_TOKEN=your_bot_callback_token
APP_NAPCAT_ACCESS_TOKEN=your_napcat_access_token

# Frontend
VITE_API_BASE_URL=https://api.jmi-openatom.cn/api/v1
VITE_OIDC_AUTHORITY=https://oauth.jmi-openatom.cn/api/v1
VITE_OIDC_CLIENT_ID=openatom-web
```

::: danger 安全提示
- `.env` 文件**不应**提交到 Git 仓库（已在 `.gitignore` 中排除）
- 生产环境的 JWT 密钥应使用足够长的随机字符串
- 不要在代码中硬编码密码或密钥
:::
