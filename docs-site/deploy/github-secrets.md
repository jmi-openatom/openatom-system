# GitHub Secrets 配置指南

本文档说明如何在 GitHub Actions 中配置敏感环境变量，确保 CI/CD 流水线安全部署。

## 什么是 GitHub Secrets？

GitHub Secrets 是加密的环境变量，用于存储敏感信息（如密码、API Key、Token 等）。它们不会在日志中显示，只能通过 `${{ secrets.SECRET_NAME }}` 语法在 workflows 中引用。

## 配置步骤

### 1. 进入 Settings → Secrets and variables → Actions

1. 打开你的 GitHub 仓库
2. 点击 **Settings** 标签
3. 左侧菜单找到 **Secrets and variables** → **Actions**
4. 点击 **New repository secret** 按钮

### 2. 添加以下 Secrets

####  必需配置（必须填写）

| Secret 名称 | 说明 | 示例值 |
|------------|------|--------|
| `SERVER_HOST` | 生产服务器 IP 或域名 | `your-server-ip.com` |
| `SERVER_USER` | SSH 登录用户名 | `root` 或 `ubuntu` |
| `SERVER_PASSWORD` | SSH 登录密码 | `your_ssh_password` |
| `REDIS_PASSWORD` | Redis 密码 | `strong_redis_password_123!` |
| `SA_TOKEN_JWT_SECRET_KEY` | JWT 签名密钥（至少 32 位随机字符串） | `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6` |
| `SPRING_DATASOURCE_URL` | 数据库连接 URL | `jdbc:mysql://127.0.0.1:3306/openatom-db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `openatom-db` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `your_db_password` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | `sk-your-deepseek-api-key` |
| `APP_BOT_CALLBACK_TOKEN` | QQ 机器人回调 Token | `your_bot_callback_token` |
| `APP_NAPCAT_ACCESS_TOKEN` | NapCat 访问 Token | `your_napcat_access_token` |
| `APP_MINIAPP_APP_ID` | 微信小程序 AppID | `wx8c6b52ab95da0938` |
| `APP_MINIAPP_APP_SECRET` | 微信小程序 AppSecret | `d15542dcaa23b92191962235dacef7b9` |

#### 🟡 可选配置（有默认值）

| Secret 名称 | 默认值 | 说明 |
|------------|--------|------|
| `SERVER_PORT` | `22` | SSH 端口号 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_DATABASE` | `0` | Redis 数据库编号 |
| `SA_TOKEN_TIMEOUT` | `2592000` | Token 有效期（秒，默认 30 天） |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek API 地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | DeepSeek 模型名称 |
| `APP_NAPCAT_BASE_URL` | `http://127.0.0.1:3000` | NapCat API 地址 |
| `APP_BOT_LEAVE_REVIEW_CALLBACK_URL` | `http://127.0.0.1:6198/openatom/leave-review` | 请假审批回调 URL |
| `VITE_OIDC_AUTHORITY` | `https://oauth.jmi-openatom.cn/api/v1` | OIDC 认证地址 |
| `VITE_OIDC_CLIENT_ID` | `openatom-web` | OIDC 客户端 ID |
| `APP_OIDC_ISSUER` | `https://oauth.jmi-openatom.cn/api/v1` | OIDC 签发者地址 |
| `APP_AVATAR_STORAGE_DIR` | `/app/uploads/avatars` | 头像存储目录 |
| `APP_IMAGE_HOSTING_STORAGE_DIR` | `/app/uploads/images` | 图床存储目录 |
| `APP_LEAVE_ATTACHMENT_STORAGE_DIR` | `/app/uploads/leave-attachments` | 请假附件存储目录 |
| `APP_DOCUMENT_TEMPLATE_STORAGE_DIR` | `/app/uploads/document-templates` | 文书模板存储目录 |
| `APP_GENERATED_DOCUMENT_STORAGE_DIR` | `/app/uploads/generated-documents` | 生成文书存储目录 |

### 3. 验证配置

添加完所有 Secrets 后，手动触发一次工作流进行测试：

1. 进入 **Actions** 标签
2. 选择 **CI/CD Pipeline** 工作流
3. 点击 **Run workflow** → **Run workflow** 按钮
4. 查看执行日志，确认没有错误

## 安全最佳实践

### ✅ 应该做的

1. **使用强密码和密钥**
   - JWT 密钥至少 32 位随机字符
   - 数据库密码包含大小写字母、数字、特殊字符
   - 使用密码管理器生成随机字符串

2. **定期轮换密钥**
   - 每 3-6 个月更换一次 JWT 密钥
   - API Key 泄露后立即更换

3. **最小权限原则**
   - 数据库用户只授予必要的权限
   - Redis 设置访问密码并限制 IP

4. **不要提交到代码库**
   - `.env` 文件已在 `.gitignore` 中排除
   - 永远不要在代码中硬编码密钥

### ❌ 不应该做的

1. **不要在代码中硬编码密钥**
   ```java
   // ❌ 错误做法
   private static final String JWT_SECRET = "my-secret-key";
   
   // ✅ 正确做法
   @Value("${sa-token.jwt-secret-key}")
   private String jwtSecret;
   ```

2. **不要在日志中打印密钥**
   ```java
   // ❌ 错误做法
   log.info("JWT Secret: {}", jwtSecret);
   
   // ✅ 正确做法
   log.debug("JWT configured successfully");
   ```

3. **不要将 Secrets 传递给 PR**
   - GitHub Actions 默认不会在 PR 中暴露 Secrets
   - 只在 `main` 分支或受保护的分支上使用 Secrets

## 故障排查

### 问题 1：部署失败，提示缺少环境变量

**症状**：后端启动失败，日志显示 `Could not resolve placeholder 'SA_TOKEN_JWT_SECRET_KEY'`

**解决方案**：
1. 检查 GitHub Secrets 中是否已添加该变量
2. 确认变量名拼写正确（区分大小写）
3. 重新触发工作流

### 问题 2：SSH 连接失败

**症状**：`Host key verification failed` 或 `Permission denied`

**解决方案**：
1. 确认 `SERVER_HOST`、`SERVER_USER`、`SERVER_PASSWORD` 配置正确
2. 测试本地 SSH 连接：`ssh user@host`
3. 检查服务器防火墙是否允许 SSH 访问

### 问题 3：数据库连接失败

**症状**：`Communications link failure` 或 `Access denied`

**解决方案**：
1. 确认 `SPRING_DATASOURCE_URL`、`USERNAME`、`PASSWORD` 配置正确
2. 检查数据库服务是否运行：`systemctl status mysql`
3. 确认数据库用户权限：`SHOW GRANTS FOR 'username'@'localhost';`

### 问题 4：Redis 连接失败

**症状**：`NOAUTH Authentication required`

**解决方案**：
1. 确认 `REDIS_PASSWORD` 配置正确
2. 测试 Redis 连接：`redis-cli -h 127.0.0.1 -p 6379 -a your_password ping`
3. 检查 Redis 配置文件中的 `requirepass` 设置

## 本地开发 vs 生产环境

### 本地开发（application-dev.yaml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/openatom_db
    username: root
    password: root
sa-token:
  jwt-secret-key: dev-jwt-secret-key-for-local-only
```

### 生产环境（application-prod.yaml + GitHub Secrets）

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
sa-token:
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET_KEY}
```

通过 GitHub Secrets 注入：
```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/openatom-db"
export SPRING_DATASOURCE_USERNAME="openatom-db"
export SPRING_DATASOURCE_PASSWORD="production_password"
export SA_TOKEN_JWT_SECRET_KEY="long-random-secret-key"
docker-compose up -d --build
```

## 相关文档

- [GitHub Actions Secrets 官方文档](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Docker Compose 环境变量](https://docs.docker.com/compose/environment-variables/)
