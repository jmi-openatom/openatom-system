# OpenAtom Mail 环境变量清单

生产模板位于 [`../.env.production.example`](../.env.production.example)。填充值时复制到服务器受保护路径，例如 `/etc/openatom/mail.env`，权限建议为 `0600`，不要提交到 Git。

## 邮件服务器必填项

| 字段 | 存放位置 | 用途 | 值的来源 |
| --- | --- | --- | --- |
| `MAIL_DB_PASSWORD` | GitHub Secret / 邮件环境文件 | 邮件控制面 MySQL 用户密码 | 生成不少于 32 位随机值 |
| `MAIL_DB_ROOT_PASSWORD` | GitHub Secret / 邮件环境文件 | 邮件 MySQL root 密码 | 生成另一组不少于 32 位随机值 |
| `MAIL_REDIS_PASSWORD` | GitHub Secret / 邮件环境文件 | BFF 服务端会话 Redis 密码 | 生成不少于 32 位随机值 |
| `MAIL_ADDRESS_SALT` | GitHub Secret / 邮件环境文件 | 重名邮箱稳定后缀 HMAC 盐 | 生成不少于 32 位随机值；邮箱投产后永不轮换 |
| `MAIL_INTERNAL_SERVICE_TOKEN` | GitHub Secret / 两侧环境文件 | 主站 Outbox 调用邮件站的 Bearer Token | 生成不少于 32 位随机值；与 `APP_MAIL_SERVICE_TOKEN` 完全相同 |
| `MAIL_OAUTH_CLIENT_SECRET` | GitHub Secret / 邮件环境文件 | `openatom-mail-web` 机密 OAuth 客户端 | 在 OpenAtom OAuth 客户端管理中生成 |
| `STALWART_CONFIG_TOKEN` | 仅服务器 `0600` 环境文件 | 幂等应用 Stalwart 域、OIDC、MTA 和监控配置 | 首次自动部署由 Stalwart 生成并写入，不建 GitHub Secret |
| `STALWART_API_TOKEN` | 仅服务器 `0600` 环境文件 | `mail-api` 自动管理 Stalwart 账号 | 首次自动部署由 Stalwart 生成并写入，不建 GitHub Secret |
| `STALWART_DOMAIN_ID` | 仅服务器 `0600` 环境文件 | 创建邮箱别名时引用的域对象 | 首次自动部署精确查询并写入，不建 GitHub Secret |
| `MAIL_RESEND_API_KEY` | GitHub Secret / 邮件环境文件 | Resend 出站发信 API Key（`re_...`），mail-api 经 Resend 投递外发邮件 | 在 Resend 控制台创建 API Key，域名 `mailer.jmi-openatom.cn` 验证通过后填写 |
| `MAIL_RESEND_SMTP_USERNAME` | GitHub Secret / 邮件环境文件 | Resend SMTP 中继用户名（固定 `resend`） | 与 `MAIL_RESEND_API_KEY` 一起在部署时由 deploy.sh 填充或手工填写 |
| `MAIL_RESEND_SMTP_PASSWORD` | GitHub Secret / 邮件环境文件 | Resend SMTP 中继密码（即 API Key） | 与 `MAIL_RESEND_API_KEY` 相同；用于 Alertmanager 告警邮件投递 |
| `MAIL_ALERT_RECIPIENT` | 邮件环境文件 | Alertmanager 告警邮件收件人；必须是真实可收信邮箱（Resend 已验证域上的地址若无 inbound 路由会退信） | 运维或管理员邮箱，如 `ops@example.com` |

`STALWART_RECOVERY_ADMIN` 也不需要 GitHub Secret。首次引导会在服务器本地生成一次性值；两枚 Key 验证成功后删除临时账号密码、清空该值并设置 `STALWART_RECOVERY_MODE=0`。只有明确的灾难恢复才人工设置它。

可用以下命令分别生成普通随机值：

```sh
openssl rand -base64 48
```

## 邮件服务器有默认值的字段

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `MAIL_DOMAIN` | `jmi-openatom.cn` | 自动分配邮箱的域名 |
| `MAIL_HOSTNAME` | `mx1.jmi-openatom.cn` | SMTP HELO、PTR 和证书使用的邮件主机名 |
| `MAIL_ACME_EMAIL` | 无 | Let’s Encrypt 到期与安全通知邮箱；GitHub Environment Variable，`tls` 阶段必填 |
| `STALWART_PUBLIC_URL` | `https://mail.jmi-openatom.cn` | JMAP/OAuth 发现文档对外地址 |
| `MAIL_DEFAULT_QUOTA_BYTES` | `2147483648` | 单邮箱默认 2 GiB 配额 |
| `MAIL_MAX_MESSAGE_BYTES` | `26214400` | SMTP 单封邮件硬限制 25 MiB |
| `MAIL_OAUTH_ISSUER` | `https://oauth.jmi-openatom.cn/api/v1` | OIDC Issuer，必须精确匹配 Token |
| `MAIL_OAUTH_AUDIENCE` | `stalwart` | Stalwart 验证的 Access Token Audience |
| `MAIL_OAUTH_AUTHORIZATION_URL` | `https://oauth.jmi-openatom.cn/api/v1/oauth/authorize` | OAuth 授权端点 |
| `MAIL_OAUTH_TOKEN_URL` | `https://oauth.jmi-openatom.cn/api/v1/oauth/token` | OAuth Token 端点 |
| `MAIL_OAUTH_JWKS_URL` | `https://oauth.jmi-openatom.cn/api/v1/oauth/jwks` | RS256 公钥集合 |
| `MAIL_OAUTH_CLIENT_ID` | `openatom-mail-web` | 邮件站 OAuth Client ID |
| `MAIL_OAUTH_REDIRECT_URI` | `https://mail.jmi-openatom.cn/api/oauth/callback` | 必须登记为精确回调地址 |
| `MAIL_API_INTERNAL_PORT` | `18090` | mail-api 宿主机回环端口 |
| `MAIL_WEB_INTERNAL_PORT` | `18082` | mail-web 宿主机回环端口 |
| `STALWART_SETUP_PORT` | `18081` | Stalwart 管理端回环端口 |
| `MAIL_PROMETHEUS_INTERNAL_PORT` | `19090` | Prometheus 回环管理端口，不对公网开放 |
| `MAIL_ALERTMANAGER_INTERNAL_PORT` | `19093` | Alertmanager 回环管理端口，不对公网开放 |
| `MAIL_NODE_EXPORTER_INTERNAL_PORT` | `19100` | Node Exporter 回环端口，不对公网开放 |
| `MAIL_BLACKBOX_INTERNAL_PORT` | `19115` | Blackbox Exporter 回环端口，不对公网开放 |
| `STALWART_RECOVERY_MODE` | `0` | 正常运行必须为 `0`；引导脚本会临时覆盖为 `1` |
| `STALWART_AUTOMATION_ACCOUNT` | `openatom-automation` | 自动生成两枚 Key 的专用账号；完成后不保留密码 |
| `STALWART_CLI_IMAGE` | `ghcr.io/stalwartlabs/cli:latest` | 应用声明式计划的官方 CLI 镜像；可固定到已验证 Digest |
| `STALWART_DOCKER_NETWORK` | `openatom-mail_mail-internal` | CLI 连接 Stalwart 的 Compose 内网名称 |
| `STALWART_TLS_HOST_DIR` | `./.runtime/stalwart-tls` | Certbot 证书复制目录，仅 Stalwart 容器只读挂载 |
| `NGINX_CONF_DIR` | `/etc/nginx/conf.d` | 自动安装邮件站 Nginx 配置的目录 |
| `MAIL_BACKUP_AGE_RECIPIENT` | 无 | `age1...` 形式的备份加密公钥；启用备份任务时必填，不是 Secret |
| `MAIL_RESEND_RELAY_DOMAIN` | `mailer.jmi-openatom.cn` | Resend 已验证的发件域名，Web 前端以此域名构造发件 Identity | Resend 控制台“Domains”中确认验证状态 |

## 主站必填项

这些字段填写到现有主站部署 `.env`，不是邮件 Compose 的环境文件：

| 字段 | GitHub 设置 | 说明 |
| --- | --- | --- |
| `APP_OIDC_PRIVATE_KEY_BASE64` | Secret | PKCS#8 DER 格式 RSA 私钥的单行 Base64 |
| `APP_OIDC_PUBLIC_KEY_BASE64` | Secret | X.509 DER 格式 RSA 公钥的单行 Base64 |
| `MAIL_INTERNAL_SERVICE_TOKEN` | Secret | 现有主站 Action 会把它写成 `APP_MAIL_SERVICE_TOKEN` |
| `APP_OIDC_KEY_ID` | Variable，可省略 | 当前 JWKS `kid`，默认 `openatom-oidc-rs256` |
| `APP_OIDC_RESOURCE_AUDIENCE` | Variable，可省略 | 默认 `stalwart` |
| `APP_MAIL_PROVISION_URL` | Variable，可省略 | 默认 `http://127.0.0.1:18090/internal/v1/mailboxes/provision` |
| `APP_OIDC_PREVIOUS_KEY_ID` | Variable，可省略 | 密钥轮换窗口内的旧 `kid` |
| `APP_OIDC_PREVIOUS_PUBLIC_KEY_BASE64` | Secret，可省略 | 密钥轮换窗口内的旧公钥 |

在安全的运维机器生成 RS256 密钥：

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out oidc-private.pem
openssl pkcs8 -topk8 -nocrypt -in oidc-private.pem -outform DER \
  | base64 | tr -d '\n'
openssl pkey -in oidc-private.pem -pubout -outform DER \
  | base64 | tr -d '\n'
```

第一条 Base64 输出填 `APP_OIDC_PRIVATE_KEY_BASE64`，第二条填 `APP_OIDC_PUBLIC_KEY_BASE64`。PEM 文件不得进入仓库。

## GitHub Actions 部署字段

在 Repository → Settings → Environments 新建受保护环境 `MAIL_SERVER`，并添加：

- SSH Secrets：`MAIL_SERVER_HOST`、`MAIL_SERVER_USER`、`MAIL_SERVER_PASSWORD`、`MAIL_SERVER_FINGERPRINT`；`MAIL_SERVER_PORT` 可省略，默认 `22`。
- 应用 Secrets：`MAIL_DB_PASSWORD`、`MAIL_DB_ROOT_PASSWORD`、`MAIL_REDIS_PASSWORD`、`MAIL_ADDRESS_SALT`、`MAIL_INTERNAL_SERVICE_TOKEN`、`MAIL_OAUTH_CLIENT_SECRET`。
- Variables：`MAIL_ACME_EMAIL` 必填；`MAIL_DEPLOY_PATH` 可选，默认 `/www/wwwroot/openatom-system`。

不要创建 `STALWART_CONFIG_TOKEN`、`STALWART_API_TOKEN`、`STALWART_DOMAIN_ID` 或 `STALWART_RECOVERY_ADMIN` GitHub Secret；它们是服务器端自动引导状态，Actions 每次更新其他字段时会原样保留。

### 每个 GitHub Secret 的示例

> 下表仅说明格式。所有含 `EXAMPLE_ONLY` 的值都不能直接用于生产；随机 Secret 应分别生成，不能复用。`MAIL_INTERNAL_SERVICE_TOKEN` 是唯一例外：邮件站与主站必须使用同一个随机值。

| GitHub Secret | 示例格式 | 真实值怎么取得 |
| --- | --- | --- |
| `MAIL_SERVER_HOST` | `203.0.113.10` 或 `mx1.jmi-openatom.cn` | 邮件服务器公网 IP 或可解析主机名 |
| `MAIL_SERVER_USER` | `openatom-deploy` | 服务器上允许登录并能运行 Docker 的 SSH 用户 |
| `MAIL_SERVER_PASSWORD` | `EXAMPLE_ONLY_SshLoginPassword_ChangeMe!` | 该 SSH 用户的真实登录密码；不要填写 GitHub 密码 |
| `MAIL_SERVER_FINGERPRINT` | `SHA256:EXAMPLE_ONLY_BASE64_HOST_KEY_FINGERPRINT` | 从服务器控制台可信核对 `ssh-keygen -lf` 输出的第二列 |
| `MAIL_SERVER_PORT` | `22` | SSHD 监听端口；使用 22 时可不创建此 Secret |
| `MAIL_DB_PASSWORD` | `EXAMPLE_ONLY_mail_db_48_random_chars` | 单独执行 `openssl rand -base64 48` |
| `MAIL_DB_ROOT_PASSWORD` | `EXAMPLE_ONLY_mail_root_different_48_random_chars` | 再单独执行一次 `openssl rand -base64 48` |
| `MAIL_REDIS_PASSWORD` | `EXAMPLE_ONLY_redis_48_random_chars` | 再单独执行一次 `openssl rand -base64 48` |
| `MAIL_ADDRESS_SALT` | `EXAMPLE_ONLY_address_hmac_salt_never_rotate` | 单独执行 `openssl rand -base64 48`；首个邮箱创建后永不更换 |
| `MAIL_INTERNAL_SERVICE_TOKEN` | `EXAMPLE_ONLY_shared_main_to_mail_service_token` | 单独执行 `openssl rand -base64 48`；主站同名 Secret 必须完全相同 |
| `MAIL_OAUTH_CLIENT_SECRET` | `EXAMPLE_ONLY_openatom_mail_web_oauth_secret` | 身份中心注册机密客户端 `openatom-mail-web` 后生成 |

服务器自动生成后的受限文件形态类似下面这样；仅用于理解，不要把这些字段复制进 GitHub：

```dotenv
STALWART_CONFIG_TOKEN=API_EXAMPLE_ONLY_server_generated_configuration_key
STALWART_API_TOKEN=API_EXAMPLE_ONLY_server_generated_account_key
STALWART_DOMAIN_ID=EXAMPLE_ONLY_server_resolved_domain_id
STALWART_RECOVERY_ADMIN=
STALWART_RECOVERY_MODE=0
```

主站现有部署还需要这些 GitHub Secrets：

| GitHub Secret | 示例格式 | 真实值怎么取得 |
| --- | --- | --- |
| `APP_OIDC_PRIVATE_KEY_BASE64` | `EXAMPLE_ONLY_PKCS8_DER_PRIVATE_KEY_BASE64` | 使用下文 OpenSSL 命令生成；实际值是很长的单行 Base64 |
| `APP_OIDC_PUBLIC_KEY_BASE64` | `EXAMPLE_ONLY_X509_DER_PUBLIC_KEY_BASE64` | 从同一 RSA 私钥导出公钥后生成单行 Base64 |
| `MAIL_INTERNAL_SERVICE_TOKEN` | `EXAMPLE_ONLY_shared_main_to_mail_service_token` | 必须复制邮件站的同名 Secret，不能重新生成 |
| `APP_OIDC_PREVIOUS_PUBLIC_KEY_BASE64` | `EXAMPLE_ONLY_PREVIOUS_X509_PUBLIC_KEY_BASE64` | 仅密钥轮换兼容窗口填写，否则不创建 |

`MAIL_SERVER_FINGERPRINT` 是 SSH 主机公钥指纹，不是登录密码。应从服务器控制台或云厂商可信通道核对后填写，例如 `SHA256:...`。在可信运维机可查看候选值：

```sh
ssh-keyscan -p 22 mail-server.example.com | ssh-keygen -lf -
```

向 `main` 推送相关代码后，`OpenAtom Mail CI/CD` 会在全部测试通过后自动执行 `deployment_mode=auto`：通过“用户名 + 密码”SSH 拉取 `main`，首次运行自动创建域、两枚最小权限 Key 和 Domain ID；没有证书时自动用 Certbot Webroot 为 `mail`、`mta-sts`、`mx1` 签发同一张证书；随后应用完整声明式配置、构建并健康检查。服务器环境文件中的自动生成字段会跨部署保留，Actions 日志不会打印 Key。SSH 用户必须有权运行 Docker、Nginx、Certbot，并写入 `/etc/nginx`、`/etc/letsencrypt`。首次运行前 DNS A 记录必须已生效，否则 ACME 会失败；修正 DNS 后重新运行失败的工作流即可。

自动引导是可重跑的：当三项服务器状态完整且恢复模式已关闭时会保持原 Key 不变；若第一次运行在清理恢复凭据前中断，则下次运行会借助仍在服务器本地的恢复凭据重新生成并验证 Key，最后撤销临时密码。CI 也会连续运行两次引导并比较环境文件摘要，防止正常重部署意外轮换 Token。

配置自动化 Key 只授予 `sysDirectoryGet/Query/Create/Update`、`sysDomainGet/Query/Create/Update`、`sysMtaInboundThrottleGet/Query/Create/Update`、`sysCertificateGet/Query/Create/Update`，以及 `sysAuthenticationUpdate`、`sysSystemSettingsUpdate`、`sysMtaStageAuthUpdate`、`sysMtaStageRcptUpdate`、`sysMtaInboundSessionUpdate`、`sysMtaStageDataUpdate`、`sysMtaOutboundStrategyUpdate`、`sysMetricsUpdate`、`sysJmapUpdate`、`sysEmailUpdate`。账号自动化 Key 只授予 `sysAccountGet/Query/Create/Update`。两个 Key 必须分开，均不授予销毁、邮件正文读取或日志读取权限。