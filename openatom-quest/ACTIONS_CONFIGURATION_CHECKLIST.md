# OpenAtom Quest Actions 配置清单

本文对应 [`.github/workflows/openatom-quest.yml`](../.github/workflows/openatom-quest.yml)。配置完成后，提交到 `main` 会自动测试并部署；也可以在 GitHub Actions 页面手动运行 `OpenAtom Quest CI/CD`。

## 1. GitHub Environment

在仓库进入 `Settings → Environments`，创建环境：

- 环境名称：`SERVER`
- 如需上线前人工确认，可为该环境启用 `Required reviewers`

## 2. GitHub Actions Secrets

在 `Settings → Environments → SERVER → Environment secrets` 配置：

| Secret | 必填 | 示例/说明 |
| --- | :---: | --- |
| `SERVER_HOST` | 是 | 部署服务器 IP 或 SSH 域名 |
| `SERVER_USER` | 是 | 有权运行 Docker 的 SSH 用户 |
| `SERVER_PASSWORD` | 是 | SSH 登录密码 |
| `SERVER_PORT` | 否 | SSH 端口，未配置时使用 `22` |
| `QUEST_DB_PASSWORD` | 是 | Quest 数据库用户密码，至少 16 位 |
| `QUEST_DB_ROOT_PASSWORD` | 是 | MySQL Root 密码，至少 16 位且与上一项不同 |
| `QUEST_OAUTH_CLIENT_SECRET` | 否 | 公共客户端不配置；机密客户端填写 OAuth Secret |
| `QUEST_OAUTH_BOOTSTRAP_ADMIN_SUBJECTS` | 首次上线必填 | 首位管理员的 OpenAtom 稳定 `sub`；多人用英文逗号分隔 |

当前工作流沿用仓库其他部署任务的 `SERVER_*` 配置。如果这些 Secrets 已存在，无需重复创建。Quest 密码建议使用 `openssl rand -hex 32` 生成；为确保自动生成 `.env` 安全，Secret 只使用字母、数字和 `._~+=/-`。

## 3. GitHub Actions Variables

在 `Settings → Environments → SERVER → Environment variables` 配置：

| Variable | 必填 | 默认值 |
| --- | :---: | --- |
| `QUEST_DEPLOY_PATH` | 否 | `/www/wwwroot/openatom-quest` |
| `QUEST_OAUTH_CLIENT_ID` | 否 | `openatom-quest` |
| `QUEST_HTTP_PORT` | 否 | `18084` |

## 4. 服务器准备

服务器需要安装：

- Docker Engine
- Docker Compose v2，或兼容的 `docker-compose`
- `curl`
- 可用的 HTTPS 入口网关，例如宝塔 Nginx、宿主机 Nginx 或 Caddy

首次部署前只需创建部署目录：

```bash
mkdir -p /www/wwwroot/openatom-quest
```

Action 会读取 GitHub Secrets/Variables，在服务器自动生成权限为 `600` 的 `.env`。

## 5. GitHub 配置与服务器 `.env` 对应关系

Action 自动生成以下内容，无需登录服务器填写：

```dotenv
QUEST_DB_PASSWORD=${GitHub Secret: QUEST_DB_PASSWORD}
QUEST_DB_ROOT_PASSWORD=${GitHub Secret: QUEST_DB_ROOT_PASSWORD}
QUEST_OAUTH_CLIENT_ID=${GitHub Variable: QUEST_OAUTH_CLIENT_ID}
QUEST_OAUTH_CLIENT_SECRET=${GitHub Secret: QUEST_OAUTH_CLIENT_SECRET}
QUEST_OAUTH_BOOTSTRAP_ADMIN_SUBJECTS=${GitHub Secret: QUEST_OAUTH_BOOTSTRAP_ADMIN_SUBJECTS}
QUEST_HTTP_PORT=${GitHub Variable: QUEST_HTTP_PORT}
```

MySQL 数据库名固定为 `quest`，数据库用户固定为 `quest`，由 Compose 在首次启动时创建。

说明：

- OAuth 公共客户端不创建 `QUEST_OAUTH_CLIENT_SECRET`；若后台配置了 Secret，再添加该 Secret。
- `QUEST_OAUTH_BOOTSTRAP_ADMIN_SUBJECTS` 只按 OAuth 返回的稳定 `sub` 精确匹配，绝不按邮箱提权。匹配用户下次 OAuth 登录时会获得管理员角色并写入审计日志；完成首位管理员初始化后可以删除该 Secret。
- 数据库密码只在数据库首次初始化时生效。已有 `quest_mysql_data` 数据卷时，不要只修改 GitHub Secret；应先在 MySQL 内修改账号密码，再同步修改 Secret。
- Action 每次部署都会覆盖服务器 `.env`，其来源始终是 GitHub Environment `SERVER`。

可用以下命令生成随机密码：

```bash
openssl rand -base64 32
```

## 6. OpenAtom OAuth 后台

按以下内容手动创建或修改应用：

| 字段 | 值 |
| --- | --- |
| 应用名称 | `OpenAtom Quest` |
| Client ID | `openatom-quest` |
| Client Secret | 公共客户端留空；机密客户端自行生成并配置为 GitHub Environment Secret `QUEST_OAUTH_CLIENT_SECRET` |
| 回调地址 | `https://quest.jmi-openatom.cn/api/auth/callback` |
| Scopes | `openid profile email` |
| Grant Types | `authorization_code refresh_token` |
| 状态 | 启用 |

## 7. 域名与 HTTPS 反向代理

1. 将 `quest.jmi-openatom.cn` 的 DNS 记录指向部署服务器。
2. 为 `quest.jmi-openatom.cn` 申请并启用有效 HTTPS 证书。
3. 将 HTTPS 请求反向代理到 `http://127.0.0.1:18084`，端口需与 `QUEST_HTTP_PORT` 一致。
4. 保留 `Host`、`X-Real-IP`、`X-Forwarded-For` 和 `X-Forwarded-Proto` 请求头。

Nginx 示例：

```nginx
location / {
    proxy_pass http://127.0.0.1:18084;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 8. 首次运行与验收

1. 在 GitHub 打开 `Actions → OpenAtom Quest CI/CD → Run workflow`。
2. 确认 `backend-check`、`frontend-check`、`deployment-config-check` 和 `deploy` 全部通过。
3. 访问 `https://quest.jmi-openatom.cn/api/system/health`，应返回成功状态。
4. 访问 `https://quest.jmi-openatom.cn`，点击“使用 OpenAtom 账号登录”。
5. 验证授权后返回 Quest，首次用户进入资料完善页。
6. 计划作为首位管理员的用户登录后，访问 `https://quest.jmi-openatom.cn/api/auth/identity`，复制返回的 `subject`。
7. 将该值保存为 GitHub Environment Secret `QUEST_OAUTH_BOOTSTRAP_ADMIN_SUBJECTS`，重新运行 Action，再退出并重新登录。
8. 确认该用户可以进入管理后台；随后可以删除 `QUEST_OAUTH_BOOTSTRAP_ADMIN_SUBJECTS`，后续角色由后台分配。
9. 验证退出后原会话失效。

## 9. 常见失败定位

| 现象 | 检查项 |
| --- | --- |
| 数据库密码格式校验失败 | 检查两个数据库 Secrets 是否至少 16 位并仅使用允许字符 |
| SSH/SCP 失败 | `SERVER_HOST`、`SERVER_USER`、`SERVER_PASSWORD`、`SERVER_PORT` |
| 数据库认证失败 | `.env` 密码是否与已有 MySQL 数据卷内账号一致 |
| OAuth 返回 `invalid_client` | OAuth 后台与 GitHub 中的 Client ID、Secret、启用状态 |
| OAuth 返回回调地址错误 | 回调必须精确为 `https://quest.jmi-openatom.cn/api/auth/callback` |
| 健康检查失败 | 查看 `docker compose logs --tail=200 backend frontend` |
| 浏览器无法保持登录 | HTTPS、反代 `X-Forwarded-Proto`、浏览器 Secure Cookie |
