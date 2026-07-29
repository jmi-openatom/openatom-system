# OpenAtom Mail

这是独立部署的自建邮件网站实现。它由 Vue 3 邮件前端、Spring Boot OAuth BFF/邮箱控制面、Stalwart、MySQL、Redis、ClamAV 与自托管监控栈组成；主站只通过事务 Outbox 推送用户快照，因此邮件服务故障不会阻塞注册。

## 目录

- `mail-web/`：独立邮箱网站，UI Token 与主站 `frontend/web_pc/src/styles/tokens.css` 对齐。
- `mail-api/`：机密 OAuth 客户端、服务端会话、JMAP BFF、拼音地址分配和 Stalwart 账号自动化。
- `deploy/`：Nginx、DNS、MTA-STS、Stalwart、Prometheus 告警、备份恢复和 Open Relay 检查。
- `docker-compose.mail.yml`：邮件系统独立故障域的 Compose 编排。

## 上线顺序

1. 复制 `.env.example` 为受权限保护的部署环境文件，生成所有随机密钥；`MAIL_ADDRESS_SALT` 上线后不得修改。
2. 在身份中心注册机密客户端 `openatom-mail-web`，回调地址设为 `https://mail.jmi-openatom.cn/api/oauth/callback`，Scope 包含 `openid profile email mail offline_access`。
3. 配置主站 RS256 私钥和 `kid`，部署数据库迁移 `V52__add_mailbox_outbox.sql`；主站 `APP_MAIL_SERVICE_TOKEN` 必须与邮件站 `MAIL_INTERNAL_SERVICE_TOKEN` 相同。
4. DNS 生效后向 `main` 推送：Actions 自动使用 `deployment_mode=auto`，启动 Compose、创建域、生成两枚最小权限 Stalwart API Key、查询 Domain ID、签发证书并完整部署，不需要进入管理后台复制值。
5. `tls` 阶段会自动渲染并安装 `deploy/nginx/mail.jmi-openatom.cn.conf`、申请受信任证书并注册续期 Hook；防火墙只开放 Web 入口和邮件协议端口，Stalwart 管理端继续只绑定 `127.0.0.1`。
6. 发布 `deploy/dns/jmi-openatom.zone.template` 中的 A、MX、SPF、DKIM、DMARC、MTA-STS 与 TLS-RPT 记录，并由公网 IP 提供商配置 PTR。
7. 完整部署会自动启动 Prometheus、Alertmanager、Node Exporter 与 Blackbox Exporter，加载 `deploy/monitoring/` 规则，并通过本机 Stalwart 向 `postmaster@jmi-openatom.cn` 投递告警；四个管理端口均只绑定回环地址。

附件上传和下载都必须先通过同一 Compose 内的 ClamAV 扫描；扫描服务不可用时按 fail-closed 策略拒绝附件。ClamAV 官方说明建议为签名引擎准备至少 3 GiB、优选 4 GiB 内存，因此生产服务器应至少配置 4 GiB RAM，并为其他容器预留余量。

首次基础设施引导与后续完整部署均由脚本执行：

```sh
mail-system/deploy/deploy.sh /etc/openatom/mail.env bootstrap
mail-system/deploy/deploy.sh /etc/openatom/mail.env tls
mail-system/deploy/deploy.sh /etc/openatom/mail.env full
mail-system/deploy/deploy.sh /etc/openatom/mail.env auto
```

完整字段、GitHub Secrets/Variables 和 RSA 密钥生成方法见 [`deploy/environment-variables.md`](deploy/environment-variables.md)。

主站 Outbox 默认每 5 秒投递，失败采用指数退避；每天一次的幂等用户快照会修复历史用户或跨系统漂移。OAuth 首次登录还会执行一次 JIT 补建。

## 本地验证

```sh
cd backend && ./mvnw test
cd mail-system/mail-api && ../../backend/mvnw test
cd mail-system/mail-web && pnpm exec vue-tsc --noEmit && pnpm build
docker compose --env-file /secure/path/openatom-mail.env \
  -f mail-system/docker-compose.mail.yml config --quiet
```

## GitHub Actions

`.github/workflows/mail-system.yml` 会在相关代码变更时执行：

- 主站 OAuth、禁用用户和事务 Outbox 测试。
- `mail-api` 全量测试与打包。
- `mail-web` 锁定依赖安装、TypeScript 检查与生产构建。
- Compose、Shell 脚本和私钥泄漏检查。
- 临时 Stalwart v0.16 容器上的域配置、最小权限 API Key、账号创建/查询/更新和 Prometheus 指标冒烟。
- Prometheus 规则、Alertmanager 路由与四个监控容器配置校验。
- `mail-api`、`mail-web` 两个 Docker 镜像的真实 Buildx 构建。

相关代码推送到 `main` 后会自动进入受保护的 `MAIL_SERVER` Environment，并以 `auto` 模式部署；也可手动选择 `auto`、`bootstrap`、`tls` 或 `full`。部署通过 SSH 用户名和密码认证，并强制核验服务器主机指纹。需要配置以下 GitHub Secrets：

- `MAIL_SERVER_HOST`、`MAIL_SERVER_USER`、`MAIL_SERVER_PASSWORD`、`MAIL_SERVER_FINGERPRINT`；`MAIL_SERVER_PORT` 可省略
- MAIL_DB_PASSWORD、`MAIL_DB_ROOT_PASSWORD`、`MAIL_REDIS_PASSWORD`
- `MAIL_ADDRESS_SALT`、`MAIL_INTERNAL_SERVICE_TOKEN`、`MAIL_OAUTH_CLIENT_SECRET`

`STALWART_CONFIG_TOKEN`、`STALWART_API_TOKEN`、`STALWART_DOMAIN_ID` 和一次性恢复密码全部在服务器上自动生成或查询，并保存在 `0600` 的 `.env.production`；不要为它们创建 GitHub Secret。

GitHub Environment Variable `MAIL_ACME_EMAIL` 用于 Let’s Encrypt 通知。`tls` 阶段要求 SSH 用户能够直接运行 `certbot`、`nginx` 并写入 `/etc/nginx` 与 `/etc/letsencrypt`；使用普通部署用户时，应仅授予这些精确命令和目录所需的受限 sudo 权限。

主站部署还需要 `APP_OIDC_PRIVATE_KEY_BASE64`、`APP_OIDC_PUBLIC_KEY_BASE64`，以及与邮件站相同的 `MAIL_INTERNAL_SERVICE_TOKEN`。可通过 `MAIL_DEPLOY_PATH` Repository Variable 修改服务器部署目录。

## 上线验收

自动化测试覆盖 RS256/JWKS、OAuth 状态与 PKCE、用户禁用、Outbox 重试、拼音/重名、幂等开通、首次登录补建、地址纠正、附件/收件人数限制及前端构建。以下项目必须在具有真实 DNS、静态公网 IP 和开放 TCP 25 的预生产环境执行，不能用单元测试代替：

- 新用户 60 秒内开通，并在首次登录前从公网邮箱投递成功。
- Gmail、Outlook 等外域双向收发，检查 SPF、DKIM、DMARC 对齐与退信内容。
- 临时失败进入重试队列，永久失败生成 DSN，重启 Stalwart 后队列和邮件仍存在。
- 执行 `deploy/acceptance/open-relay-check.sh mx1.jmi-openatom.cn`，确认外部到外部的未认证 RCPT 被拒绝。
- 执行 `deploy/acceptance/public-preflight.sh`，保存 DNS、PTR、TLS、OIDC/JWKS、端口和 Open Relay 汇总结果到 `deploy/acceptance/acceptance-report.md` 的验收副本。
- 配置 `MAIL_BACKUP_AGE_RECIPIENT` 后执行 `deploy/backup/backup.sh /var/backups/openatom-mail /etc/openatom/mail.env` 生成只保留密文的备份；在隔离空环境用 `CONFIRM_MAIL_RESTORE=RESTORE restore.sh <备份目录> <mail.env> <离线 age 私钥>` 恢复，核对邮箱数、抽样邮件、别名和队列。
- 用桌面端、移动端和深色主题进行 UI 走查，验证键盘焦点、44 px 点击区、减少动效、空/错/加载状态与主站一致。

完整设计和逐项验收清单见 [`../docs/standalone-self-hosted-mail-system-design.md`](../docs/standalone-self-hosted-mail-system-design.md)。
