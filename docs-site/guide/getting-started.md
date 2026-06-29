# 快速开始

## 环境要求

### 必需环境

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 21 | 后端运行时 |
| Node.js | 20+ | 前端构建 |
| pnpm | 9+ | 前端包管理器 |
| MySQL | 8.0 | 数据库 |
| Maven | 3.9+ | 后端构建（或使用项目自带的 mvnw） |

### 可选环境

| 工具 | 用途 |
|------|------|
| Redis | 缓存（本地开发可不用，但功能受限） |
| Docker | 容器化部署 |
| 微信开发者工具 | UniApp 小程序开发 |

## 后端启动

### 1. 初始化数据库

本地开发使用独立 MySQL，执行初始化脚本：

```bash
mysql -u root -p < backend/db/create_table.sql
```

或手动创建数据库：

```sql
CREATE DATABASE `openatom_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

::: warning 注意
开发环境 Flyway 已禁用（`enabled: false`），需要手动执行 SQL。
生产环境 Flyway 已启用，会自动执行迁移脚本。
:::

### 2. 配置数据库连接

编辑 `backend/src/main/resources/application-dev.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/openatom_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 你的密码
```

### 3. 运行后端

```bash
cd backend
./mvnw spring-boot:run
```

或使用 IDE（IntelliJ IDEA）运行 `OpenatomSystemApplication` 主类。

后端启动后监听 `http://127.0.0.1:8921/api/v1`。

### 4. 验证后端

```bash
curl http://127.0.0.1:8921/api/v1/site/register-enabled
```

返回类似以下内容表示启动成功：

```json
{"code": 0, "message": "success", "data": true}
```

## 前端启动（PC 端）

### 1. 安装依赖

```bash
cd frontend/web_pc
pnpm install
```

### 2. 配置环境变量

创建 `.env.local` 文件（可选，默认使用 `.env` 中的配置）：

```bash
# API 基础地址（本地开发指向后端）
VITE_API_BASE_URL=http://127.0.0.1:8921/api/v1

# OIDC 认证地址（本地开发可指向后端）
VITE_OIDC_AUTHORITY=http://127.0.0.1:8921/api/v1
VITE_OIDC_CLIENT_ID=openatom-web
```

### 3. 启动开发服务器

```bash
pnpm run dev
```

前端开发服务器监听 `http://localhost:5173`。

### 4. 构建生产版本

```bash
pnpm run build
```

构建产物输出到 `frontend/web_pc/dist/`。

## 前端启动（微信小程序）

```bash
cd frontend/uni_app
pnpm install
pnpm run dev:mp-weixin
```

编译产物输出到 `frontend/uni_app/unpackage/dist/`，使用微信开发者工具打开该目录预览。

## Redis 配置（可选）

本地开发如需 Redis，可使用 Docker 快速启动：

```bash
docker run -d --name openatom-redis -p 6379:6379 redis:7-alpine
```

在 `application-dev.yaml` 中配置：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password:
```

## 默认管理员账号

系统首次启动时会自动初始化默认管理员账号（详见 `SystemUserInitializer`）。如需修改默认账号信息，请查看 `bootstrap` 包下的初始化器。

## 常见问题

### 后端启动报 `Unknown column` SQL 错误

实体类添加了新字段但未执行对应的数据库迁移。开发环境需要手动执行 `backend/src/main/resources/db/migration/` 下对应的 SQL 文件。

### 前端 API 请求 401

确认后端已启动，且 `VITE_API_BASE_URL` 配置正确。本地开发时前端默认通过 Vite 代理或直连后端。

### pnpm install 失败

确保 Node.js 版本 >= 20，pnpm 版本 >= 9：

```bash
node -v
pnpm -v
```

如需安装 pnpm：

```bash
npm install -g pnpm@9
```

## 下一步

- [项目介绍](./introduction.md) - 了解系统整体架构
- [后端架构](../backend/architecture.md) - 深入后端设计
- [Docker 部署](../deploy/docker.md) - 生产环境部署
