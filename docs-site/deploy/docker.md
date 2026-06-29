# Docker 部署

## 概述

项目已完整支持 Docker 化部署，通过 `docker-compose.yml` 编排全栈服务。

## 一键启动

在项目根目录下执行：

```bash
docker-compose up -d --build
```

该命令将构建并启动以下服务：

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| Redis | openatom-redis | 6379（仅本地） | Redis 缓存 |
| Backend | openatom-backend | 8921 | Spring Boot 后端（host 网络模式） |
| Frontend | openatom-frontend | 18080→80 | Vue 前端 + Nginx |
| AstrBot | openatom-astrbot | 6185, 6198 | QQ 机器人核心 |
| NapCat | openatom-napcat | 6099, 3000 | QQ 协议端 |
| Docs | openatom-docs | 16000→80 | VuePress 开发文档站 + Nginx |

## 前置准备

### 1. 创建数据库

在宿主机 MySQL 中创建数据库：

```sql
CREATE DATABASE `openatom-db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置生产环境

编辑 `backend/src/main/resources/application-prod.yaml`，填写数据库地址、用户名和密码。

::: tip 宝塔 MySQL
生产 Backend 使用 host 网络模式，可以直接连接只监听 `127.0.0.1:3306` 的宝塔 MySQL，不需要开放 MySQL 到 Docker 网段或公网。
:::

### 3. 配置环境变量

创建 `.env` 文件或通过环境变量配置：

```bash
# Redis 密码
REDIS_PASSWORD=your_redis_password

# JWT 密钥
SA_TOKEN_JWT_SECRET_KEY=your_jwt_secret

# 前端 API 地址
VITE_API_BASE_URL=https://api.jmi-openatom.cn/api/v1

# OIDC 配置
VITE_OIDC_AUTHORITY=https://oauth.jmi-openatom.cn/api/v1
VITE_OIDC_CLIENT_ID=openatom-web

# AI 配置
DEEPSEEK_API_KEY=your_deepseek_api_key

# 机器人配置
APP_BOT_CALLBACK_TOKEN=your_bot_callback_token
APP_NAPCAT_ACCESS_TOKEN=your_napcat_token
```

## 网络模式

### Backend（host 网络）

Backend 容器使用 `network_mode: host`，直接使用宿主机网络：

- 可直接连接 `127.0.0.1:3306` 的 MySQL
- 可直接连接 `127.0.0.1:6379` 的 Redis
- 不需要端口映射

### Frontend（桥接网络）

Frontend 容器使用桥接网络，通过端口映射访问：

- 宿主机 `18080` → 容器 `80`
- 配置 `extra_hosts: backend:host-gateway` 以访问 Backend

## 数据持久化

### Docker 卷

| 卷名 | 容器路径 | 说明 |
|------|----------|------|
| `redis_data` | `/data` | Redis 持久化数据 |
| `avatar_data` | `/app/uploads` | 后端上传文件（头像、图片、附件等） |

### AstrBot 数据

AstrBot 数据目录通过 bind mount 挂载：

```yaml
volumes:
  - ./astrbot/data:/AstrBot/data    # AstrBot 数据
  - ./astrbot/napcat/config:/app/napcat/config  # NapCat 配置
  - ./astrbot/ntqq:/app/.config/QQ   # QQ 数据
```

## 健康检查

### Redis

```yaml
healthcheck:
    test: ["CMD-SHELL", "redis-cli -a \"$REDIS_PASSWORD\" ping | grep -q PONG"]
    interval: 10s
    timeout: 5s
    retries: 12
```

### Backend

```yaml
healthcheck:
    test: ["CMD-SHELL", "bash -c 'exec 3<>/dev/tcp/127.0.0.1/8921'"]
    interval: 10s
    timeout: 5s
    retries: 12
    start_period: 60s
```

## 文档站独立部署

文档站（VuePress）已容器化，可独立部署或随全栈一起启动。

### 随全栈启动

文档站已集成到 `docker-compose.yml` 中，执行 `docker-compose up -d --build` 即可自动启动。

### 单独构建与运行

```bash
# 构建镜像
docker build -t openatom-docs:latest -f docs-site/Dockerfile docs-site/

# 运行容器
docker run -d --name openatom-docs -p 16000:80 openatom-docs:latest
```

访问 `http://localhost:16000` 即可查看文档。

### 镜像结构

文档站镜像采用多阶段构建：

1. **Build 阶段**：`node:22-alpine` + pnpm 安装依赖并执行 `vuepress build` 生成静态文件
2. **Serve 阶段**：`nginx:alpine` 托管 `.vuepress/dist` 中的静态产物

### Nginx 配置

Nginx 配置文件位于 `docs-site/nginx.conf`，特性包括：

- gzip 压缩（js/css/svg/json/woff2 等）
- VuePress SPA 路由兜底（`try_files $uri $uri/ $uri.html /index.html`）
- 静态资源 30 天长缓存（`immutable`）
- HTML 文件不缓存（确保更新及时）
- 安全响应头（X-Frame-Options、X-Content-Type-Options、X-XSS-Protection）

### 更新文档内容

修改 `docs-site/` 下的 Markdown 文件后，重新构建镜像即可：

```bash
docker build -t openatom-docs:latest -f docs-site/Dockerfile docs-site/
docker rm -f openatom-docs
docker run -d --name openatom-docs -p 16000:80 openatom-docs:latest
```

## 常用操作

### 重新构建并启动

```bash
docker compose down --remove-orphans
docker compose up -d --build
```

### 查看日志

```bash
# 查看所有服务日志
docker compose logs --tail=200

# 查看后端日志
docker compose logs --tail=200 backend

# 实时跟踪日志
docker compose logs -f backend
```

### 检查服务状态

```bash
docker compose ps -a
```

### 验证后端

```bash
curl -i http://127.0.0.1:8921/api/v1/site/register-enabled
```

### 进入容器

```bash
docker compose exec backend bash
docker compose exec frontend sh
```

## Flyway 自动迁移

后端启动时会通过 Flyway 自动执行 `backend/src/main/resources/db/migration/` 下的版本化迁移脚本。

首次接入已有生产库时：
1. Baseline 到版本 `0`
2. 执行幂等的 `V1__init_schema.sql` 补齐基础表
3. 后续按 `V2__...sql`、`V3__...sql` 持续演进

## 头像持久化

头像上传后保存到容器内 `/app/uploads/avatars`，由 `avatar_data` 持久卷承载。容器重建后头像仍然保留。

::: warning 注意
如果旧版本曾把头像直接写在容器临时文件系统中，容器重建后原始图片文件会丢失，数据库中的头像 URL 可能仍然存在但图片不可访问，需要用户重新上传。
:::
