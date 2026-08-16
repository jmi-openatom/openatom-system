# 社团在线文档协作系统

社团内部在线文档：**HedgeDoc**（Markdown 实时协作）+ **ONLYOFFICE**（Word/Excel/PPT 在线协同编辑），
均通过主站 OIDC 身份中心（`oauth.jmi-openatom.cn`）单点登录。

## 域名

默认域名：**`md.jmi-openatom.cn`**（HedgeDoc 文档站）。可换成任意域名/子域名，
只需在以下两处保持一致：

1. 本目录 `.env` 的 `HEDGEDOC_DOMAIN`
2. 主站前端构建变量 `VITE_DOCS_URL`（`frontend/web_pc/.env.production`）

ONLYOFFICE 的对外域名同理（README 示例用 `office.jmi-openatom.cn`，可自由替换）。

## 架构

```
<文档域名>   → nginx 反代 → hedgedoc:3000   （Markdown，OIDC 登录）
office 域名  → nginx 反代 → documentserver:80（Office 三件套，仅引擎）
```

## 1. 注册 OAuth 客户端（主站管理端）

登录主站管理后台 → **OAuth 客户端管理** → 新建：

| 客户端 | HedgeDoc |
|---|---|
| clientId | `openatom-hedgedoc` |
| clientSecret | 自定（保留明文，仅显示一次） |
| 名称 | 在线文档协作 |
| 回调地址 | `https://<文档域名>/auth/oauth2/callback`（**必须完全一致**） |
| scopes | `openid profile email` |
| grantTypes | `authorization_code refresh_token` |

> ONLYOFFICE 由主站文档管理模块（宿主应用）接入时，再注册一个客户端，回调指向主站。

## 2. DNS 与 nginx

- 添加 A 记录：`md.jmi-openatom.cn`、`office.jmi-openatom.cn` → 服务器 IP
- nginx（宝塔或其他）站点配置：

```nginx
# md.jmi-openatom.cn
server {
  listen 80;
  server_name md.jmi-openatom.cn;
  # ... SSL 证书（Let's Encrypt）...
  location / {
    proxy_pass http://127.0.0.1:18085;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

## 3. 部署

### 自动部署（推荐，GitHub Actions）

推送 `docs-system/` 相关代码到 `main` 后，`OpenAtom Docs Deployment` 工作流会自动：
上传 compose 文件 → 写 `.env` → 启动 HedgeDoc + 数据库 → 健康检查。

需要在 GitHub `SERVER` 环境（与 Seafile/邮件站共用）配置：

- Secrets：`SERVER_HOST`、`SERVER_USER`、`SERVER_PASSWORD`（`SERVER_PORT` 可选，默认 22）
- Secrets（生成后不回显，重新部署时若缺失需补）：
  - `HEDGEDOC_SESSION_SECRET`、`HEDGEDOC_DB_PASS`
  - `OAUTH2_CLIENT_SECRET`（与主站注册的客户端密钥一致）
  - `DOCUMENT_SERVER_JWT_SECRET`、`DOCUMENT_SERVER_DB_PASS`（ONLYOFFICE 用，可先随便填）
- Variables：`DOCS_DOMAIN`（默认 `md.jmi-openatom.cn`）、`DOCS_DEPLOY_PATH`（默认 `/www/wwwroot/openatom-docs`）

部署目录为 `DOCS_DEPLOY_PATH`（默认 `/www/wwwroot/openatom-docs`），服务只启动
HedgeDoc 与数据库；ONLYOFFICE 如需启用，在服务器上手动执行：

```sh
cd /www/wwwroot/openatom-docs
docker compose --env-file .env -f docker-compose.docs.yml --profile office up -d
```

### 手动部署

```sh
cp .env.example .env          # 填写密钥（openssl rand -base64 48）
docker compose --env-file .env -f docker-compose.docs.yml up -d hedgedoc hedgedoc-db
docker compose --env-file .env -f docker-compose.docs.yml ps   # 确认 healthy
```

端口说明：HedgeDoc 默认回环端口 `18085`、ONLYOFFICE `18086`
（`HEDGEDOC_INTERNAL_PORT` / `DOCUMENT_SERVER_INTERNAL_PORT` 可改）。
`18083` 已由 Seafile 占用，不要复用。

访问 `https://md.jmi-openatom.cn` → 点「登录」→ OpenAtom → 主站账号登录。

## 4. ONLYOFFICE（可选，较重）

```sh
docker compose --env-file .env -f docker-compose.docs.yml --profile office up -d
```

ONLYOFFICE Document Server 只提供渲染/协同引擎，需要宿主应用才能打开文档。
后续在 openatom-system 主站增加「文档中心」模块（上传 .docx/.xlsx/.pptx，用
Document Server API + `DOCUMENT_SERVER_JWT_SECRET` 签名打开），即与 HedgeDoc 一起
嵌入主站工作台。默认不启动（`--profile office`）。

## 备份

- `hedgedoc-db-data` 卷（PostgreSQL）：`docker compose ... exec hedgedoc-db pg_dump -U hedgedoc hedgedoc`
- ONLYOFFICE 文档存于 ds-postgres 卷与 documentserver 容器内 `/var/www/onlyoffice/Data`

## 常见问题

- 登录跳转后报 redirect 不匹配：确认回调地址与注册的完全一致（含结尾斜杠）。
- 登录后 401：确认 OAuth 客户端已启用、scope 含 `openid`。
- 忘记密码/无主站账号：先到主站注册/登录，再回文档站登录。
