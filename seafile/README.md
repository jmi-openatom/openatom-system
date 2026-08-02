# Seafile Community Edition

本目录提供 `cloud.jmi-openatom.cn` 的完整生产部署：

- Seafile CE `13.0.25`（当前官方已发布的最新稳定版）
- MariaDB 10.11
- Redis 7（仅作为缓存，不对宿主机暴露端口）
- OpenAtom 现有 OAuth/OIDC 登录
- 宿主机 Nginx + Let's Encrypt HTTPS
- 数据库与 Seafile 数据目录每日备份
- GitHub Actions 在 `main` 分支相关文件变更后自动部署

## 生产拓扑

只有 Seafile 的 HTTP 端口绑定到 `127.0.0.1:18083`。MariaDB 和 Redis 只存在于 Docker 私有网络中。宿主机 Nginx 终止 TLS，并把 `cloud.jmi-openatom.cn` 转发到该回环地址。

持久化目录默认如下：

| 内容 | 宿主机路径 |
| --- | --- |
| Seafile 配置、日志和文件数据 | `/www/wwwroot/openatom-seafile/data` |
| MariaDB 数据 | `/www/wwwroot/openatom-seafile/mysql` |
| 每日备份 | `/www/backup/openatom-seafile` |
| OAuth 共享密钥 | `/www/wwwroot/.openatom-secrets/seafile-oauth-client-secret` |

以上路径都可通过 GitHub Environment `SERVER` 的 Variables 修改。

## 首次部署前

1. 将 `cloud.jmi-openatom.cn` 的 A/AAAA 记录指向部署服务器。
2. 确认公网 80/443 已放行，服务器已安装 Docker Compose、Nginx 和 Certbot。
3. GitHub Environment `SERVER` 复用现有部署凭据：`SERVER_HOST`、`SERVER_USER`、`SERVER_PASSWORD`，以及可选的 `SERVER_PORT`。
4. 推送到 `main`。`OpenAtom Seafile CI/CD` 会上传配置、生成持久化随机密钥、启动服务、签发证书、安装 Nginx 配置并做健康检查。

可选 Variables：

| Variable | 默认值 |
| --- | --- |
| `SEAFILE_DEPLOY_PATH` | `/www/wwwroot/openatom-seafile` |
| `SEAFILE_BACKUP_PATH` | `/www/backup/openatom-seafile` |
| `SEAFILE_NGINX_CONF_DIR` | `/www/server/panel/vhost/nginx` |
| `OPENATOM_SHARED_SECRET_DIR` | `/www/wwwroot/.openatom-secrets` |

首次部署生成的 Seafile 应急管理员密码保存在服务器的 `/www/wwwroot/openatom-seafile/.env` 中，不会写入仓库或 Actions 日志：

```bash
grep '^INIT_SEAFILE_ADMIN_' /www/wwwroot/openatom-seafile/.env
```

## OAuth 接入

身份中心会自动维护机密客户端 `openatom-seafile`：

- Authorization endpoint: `https://oauth.jmi-openatom.cn/api/v1/oauth/authorize`
- Token endpoint: `https://oauth.jmi-openatom.cn/api/v1/oauth/token`
- UserInfo endpoint: `https://oauth.jmi-openatom.cn/api/v1/oauth/userinfo`
- Redirect URI: `https://cloud.jmi-openatom.cn/oauth/callback/`
- Scopes: `openid profile email`

后端启动器从 `APP_SEAFILE_OAUTH_CLIENT_SECRET` 读取密钥并以 BCrypt 保存；Seafile 使用同一个服务器端持久化密钥。默认保留本地管理员登录作为灾难恢复入口。确认 OAuth 用户已被提升为 Seafile 管理员后，可将 `.env` 中的 `SEAFILE_DISABLE_PASSWORD_LOGIN` 改为 `true` 并重新部署。

## 备份

`backup` 服务启动后会立即执行一次备份，之后每 86400 秒执行一次。每个快照先导出 `ccnet_db`、`seafile_db`、`seahub_db`，再归档 `data/seafile` 下的 `conf`、`seafile-data` 和 `seahub-data`，最后生成 SHA-256 校验文件。默认保留 14 天。

备份目录示例：

```text
/www/backup/openatom-seafile/20260802T030000Z/
├── ccnet_db.sql.gz
├── seafile_db.sql.gz
├── seahub_db.sql.gz
├── seafile-data.tar.gz
└── SHA256SUMS
```

手工触发一次独立备份：

```bash
cd /www/wwwroot/openatom-seafile
docker compose --env-file .env exec backup /scripts/backup.sh
```

检查最近一次备份：

```bash
cd /www/backup/openatom-seafile/$(ls -1 /www/backup/openatom-seafile | sort | tail -n 1)
sha256sum -c SHA256SUMS
```

生产环境还应把该目录同步到异机或对象存储；同机备份不能防范整机磁盘故障。恢复时应停止 Seafile，恢复三个数据库和 `seafile` 数据目录，再运行 `seaf-fsck` 检查一致性。

## 手工运维

校验并启动：

```bash
cd /www/wwwroot/openatom-seafile
docker compose --env-file .env config --quiet
./deploy/deploy.sh "$PWD/.env"
```

查看状态和日志：

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs --tail=200 seafile
docker compose --env-file .env logs --tail=200 backup
curl -i http://127.0.0.1:18083/api2/ping/
curl -i https://cloud.jmi-openatom.cn/api2/ping/
```

升级小版本前先确认最近备份可校验，再修改 `SEAFILE_IMAGE`；跨大版本必须按 Seafile 官方升级顺序逐版升级。
