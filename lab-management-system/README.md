# Lab Management System

独立实验室管理系统，和 CMS 的边界如下：

- 身份：通过 CMS OAuth 授权码登录，不读取 CMS 数据库。
- 准入：`/oauth/userinfo` 必须返回 `is_lab_member=true`，否则 LMS 拒绝发本地 JWT。
- 积分：LMS 签到成功后向 RabbitMQ 发布社团加分事件，CMS 侧消费后调用自己的积分逻辑。
- 数据：LMS 使用独立库 `db_lab_management`，本地表均以 `lab_` 前缀为主。

## 后端

```bash
cd lab-management-system
docker compose up -d lab-mysql lab-rabbitmq
LAB_DB_URL='jdbc:mysql://127.0.0.1:3307/db_lab_management?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false' \
LAB_RABBITMQ_PORT=5673 \
LAB_DEV_LOGIN_ENABLED=true \
../backend/mvnw -pl lab-admin -am spring-boot:run
```

## CMS OAuth 配置

CMS 需要创建 OAuth 客户端：

- `client_id`: `lab-lms`
- `redirect_uri`: `http://127.0.0.1:5174/auth/callback`
- `scope`: `openid profile`
- `userinfo` 字段：至少包含 `club_user_id` 或 `id`，以及 `is_lab_member`

## RabbitMQ 事件

LMS 发布到：

- exchange: `openatom.lab.events`
- routing key: `club.score.add`

消息体：

```json
{
  "eventType": "LAB_CHECKIN_SCORE_ADD",
  "checkinLogId": 1,
  "labUserId": 1,
  "clubUserId": 10001,
  "checkinDate": "2026-06-12",
  "scoreDelta": 3,
  "source": "oj_ac",
  "idempotencyKey": "lab-checkin-1"
}
```

CMS 消费者应按 `idempotencyKey` 幂等处理。

## 签到迁移范围

签到模块按 CMS 现有能力迁移到 LMS：

- 签到分组和成员
- 签到场次、目标名单、记录状态
- CMS 同款 30 秒轮换二维码 payload
- 晚自习计划、自动生成场次、迟到和旷课结算
- 刷题 AC 自动签到
- LMS 本地信誉分扣减和 RabbitMQ 社团加分事件

## 生产部署

LMS 是独立部署单元，生产部署文件在：

- GitHub Actions: `.github/workflows/lab-lms.yml`
- 后端镜像: `lab-management-system/Dockerfile`
- 前端镜像: `lab-ui-web/Dockerfile`
- 生产编排: `lab-management-system/docker-compose.prod.yml`
- 生产环境模板: `lab-management-system/.env.production.example`

流水线行为：

- Pull Request: 构建后端和前端，只做校验。
- main 分支 push 或手动触发: 上传 LMS 源码到服务器，然后在服务器本地 `docker compose build` 和部署。
- 前端生产镜像使用 Nginx，同源代理 `/api` 到 `lab-backend:8091`，前端构建变量固定为 `VITE_API_BASE_URL=/api`。

GitHub Environment 使用现有 `SERVER`。必须配置这些 Secrets：

```text
SERVER_HOST
SERVER_USER
SERVER_PASSWORD
```

部署脚本会优先读取服务器上的已有配置，不需要把 LMS 业务密钥都手工填到 GitHub Environment：

- 先读 `LAB_DEPLOY_PATH/.env`，默认 `/www/wwwroot/openatom-lms/.env`
- 再读 `CMS_DEPLOY_PATH/.env`，默认 `/www/wwwroot/openatom-system/.env`
- `LAB_JWT_SECRET` 缺失时会自动生成并写回 LMS `.env`
- 数据库和 RabbitMQ 账号密码可以复用 CMS 的 `DB_USERNAME/DB_PASSWORD`、`MYSQL_USERNAME/MYSQL_PASSWORD` 或对应的 `LAB_*` 变量

可选 Secrets/Variables：

```text
SERVER_PORT
LAB_MYSQL_ROOT_PASSWORD
LAB_DB_USERNAME
LAB_DB_PASSWORD
LAB_RABBITMQ_USERNAME
LAB_RABBITMQ_PASSWORD
LAB_JWT_SECRET
CMS_OAUTH_CLIENT_SECRET
LAB_AI_API_KEY
```

如果 LMS 和 CMS 共用同一套 MySQL 账号密码，可以不单独配置 `LAB_MYSQL_ROOT_PASSWORD`。workflow 会按顺序回退：

```text
LAB_DB_USERNAME <- CMS_DB_USERNAME <- DB_USERNAME <- MYSQL_USERNAME
LAB_DB_PASSWORD <- CMS_DB_PASSWORD <- DB_PASSWORD <- MYSQL_PASSWORD
LAB_MYSQL_ROOT_PASSWORD <- LAB_DB_PASSWORD <- CMS_DB_PASSWORD <- DB_PASSWORD <- MYSQL_PASSWORD
LAB_RABBITMQ_USERNAME <- LAB_DB_USERNAME <- CMS_DB_USERNAME <- DB_USERNAME <- MYSQL_USERNAME
LAB_RABBITMQ_PASSWORD <- LAB_DB_PASSWORD <- CMS_DB_PASSWORD <- DB_PASSWORD <- MYSQL_PASSWORD
```

`CMS_OAUTH_CLIENT_SECRET` 如果 CMS OAuth 客户端是公开客户端，可以留空；如果是保密客户端，必须和 CMS 后台 OAuth 客户端里的 Secret 一致。

必须按实际域名检查这些 Variables：

```text
LAB_DEPLOY_PATH=/www/wwwroot/openatom-lms
LAB_FRONTEND_PORT=18091
LAB_OAUTH_REDIRECT_URI=https://lab.jmi-openatom.cn/auth/callback
CMS_OAUTH_AUTHORIZE_URL=https://oauth.jmi-openatom.cn/api/v1/oauth/authorize
CMS_OAUTH_TOKEN_URL=https://oauth.jmi-openatom.cn/api/v1/oauth/token
CMS_OAUTH_USERINFO_URL=https://oauth.jmi-openatom.cn/api/v1/oauth/userinfo
CMS_OAUTH_CLIENT_ID=lab-lms
LAB_RABBITMQ_PUBLIC_BIND=127.0.0.1
LAB_RABBITMQ_PUBLIC_PORT=5673
```

其他生产变量可从 `.env.production.example` 复制到 GitHub Environment Variables 或由 workflow 默认值提供。真实 `.env` 由 workflow 在服务器写入 `LAB_DEPLOY_PATH/.env`，权限为 `600`，不要提交到仓库。

部署后服务端口：

- `LAB_FRONTEND_PORT`: 对外给 Nginx/宝塔/反向代理转发，默认 `18091`
- 后端仅在 Docker 网络内暴露 `8091`
- MySQL 默认不对宿主机暴露端口
- RabbitMQ 默认只绑定宿主机本地 `127.0.0.1:5673`，供同机 CMS 消费 `openatom.lab.events`；不要直接公网开放 5672/15672

本地验证生产编排：

```bash
docker compose --env-file lab-management-system/.env.production.example -f lab-management-system/docker-compose.prod.yml config
```
