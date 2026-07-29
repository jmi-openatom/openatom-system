# OpenAtom Mail 上线验收记录

> 本文件是待填写的运维证据模板，不代表公网验收已经完成。执行人应附上时间、环境、脱敏日志或截图以及复核人。

## 基本信息

- 环境：
- Git Commit：
- 邮件域与公网 IP：
- 执行时间（含时区）：
- 执行人 / 复核人：

## 自动检查

```sh
cd /absolute/path/openatom-system
EXPECTED_PUBLIC_IP=203.0.113.10 \
  MAIL_DOMAIN=jmi-openatom.cn \
  MAIL_HOSTNAME=mx1.jmi-openatom.cn \
  MAIL_WEB_HOST=mail.jmi-openatom.cn \
  MAIL_OAUTH_ISSUER=https://oauth.jmi-openatom.cn/api/v1 \
  DKIM_SELECTOR=stalwart \
  mail-system/deploy/acceptance/public-preflight.sh \
  | tee mail-public-preflight.txt
```

- 结果文件：
- 失败项及处理记录：

### 2026-07-30 本地代码级复验

本次复验针对当前工作树执行，结论如下。真实 Stalwart 容器、DNS、SMTP 和恢复演练仍须在 CI 或目标服务器完成，不能由这些结果替代。

| 检查 | 结果 | 证据摘要 |
| --- | --- | --- |
| `mail-api` 全量 Maven Verify | 通过 | 27 tests，0 failures，0 errors，含 ClamAV INSTREAM、上传/下载 fail-closed、Stalwart Account JSON、拼音与幂等开通 |
| 主站 OAuth / OIDC / Outbox 定向测试 | 通过 | 20 tests，0 failures，0 errors |
| `mail-web` TypeScript 与生产构建 | 通过 | `vue-tsc --noEmit`、Vite 生产构建成功 |
| Compose 模型 | 通过 | `docker compose ... config --quiet` 成功 |
| 部署脚本与工作流静态检查 | 通过 | `sh -n`、Ruby YAML 解析、`git diff --check` 成功 |
| 真实 Stalwart 自动引导 | 等待 CI | 本机 Docker daemon 不可用；工作流会实际创建域、生成两枚 Key、二次运行验证不轮换，并执行最小权限账号 API smoke test |

### 仓库 CI 已覆盖的门禁

以下项目由 `.github/workflows/mail-system.yml` 在每次相关变更时重新执行；它们是代码级证据，不能代替下方真实公网与恢复演练：

| 门禁 | 自动证据 |
| --- | --- |
| OAuth RS256/JWKS、State、PKCE、禁用用户 | 主站定向单元测试 |
| Outbox 幂等、重试、退避和陈旧任务恢复 | `MailboxProvisioningClientTest`、`MailboxOutboxSchedulerTest` |
| 拼音、稳定重名、纠音别名、暂停恢复、首次登录前预建 | `MailboxProvisioningServiceIntegrationTest`、`LocalPartGeneratorTest` |
| JMAP 账号隔离、发件人防伪、收件人数/频率、附件限制与 fail-closed 扫描 | `JmapBffControllerTest`、`ClamAvMalwareScannerTest` |
| 真实 Stalwart 管理对象结构、最小权限账号 Key 与二次幂等 | 临时 Stalwart 容器 + 两次 `bootstrap-automation.sh` + `stalwart-account-api-smoke.sh` |
| Stalwart 声明式域/OIDC/MTA/限额/指标配置 | CLI Dry Run 和实际 Apply |
| UI TypeScript、生产构建、Compose 镜像构建 | `mail-web-build`、`container-images` |
| Prometheus 规则、Alertmanager 路由和队列指标名称 | `promtool`、`amtool`、真实 `/metrics/prometheus` 检查 |
| 管理及监控端口只绑定回环地址 | Compose 模型与部署后端口断言 |

## 业务与可靠性实测

| 检查项 | 结果 | 证据 / 备注 |
| --- | --- | --- |
| 新用户创建后 60 秒内邮箱变为 `ACTIVE` | 待测 | |
| 用户首次登录前可从外域投递 | 待测 | |
| 同名、重复事件、重复登录不产生重复邮箱 | 待测 | |
| 多音字地址纠正后新旧地址均可收信 | 待测 | |
| 禁用用户不能登录或发信，恢复后地址不变 | 待测 | |
| 内部双向收发、回复和搜索 | 待测 | |
| Gmail / Outlook 双向收发，SPF、DKIM、DMARC 对齐 | 待测 | |
| 临时失败重试，永久失败产生可理解 DSN | 待测 | |
| Stalwart 重启后邮件和队列不丢失 | 待测 | |
| 超配额、超 25 MiB、超收件人数返回明确错误 | 待测 | |
| 日志抽检不含 Token、密码、正文、附件 | 待测 | |

## UI/UX 实测

| 检查项 | 结果 | 证据 / 备注 |
| --- | --- | --- |
| 桌面端与主站颜色、字号、圆角、间距 Token 一致 | 待测 | |
| 移动端收件箱、阅读、写信无横向溢出 | 待测 | |
| 深色主题对比度和状态色正确 | 待测 | |
| 键盘焦点顺序、可见焦点和 44 px 点击区 | 待测 | |
| `prefers-reduced-motion` 下减少动画 | 待测 | |
| 加载、空、错误、会话过期状态明确 | 待测 | |

## 备份恢复演练

- 备份路径 / 校验和：
- 隔离空环境：
- 恢复用时：
- 恢复后的邮箱数 / 别名数 / 抽样邮件数：
- DKIM、队列和登录复核：
- 结论：

## 最终结论

- [ ] 所有阻断项通过
- [ ] 风险接受经过负责人签字
- [ ] 已批准切换 MX / 开放公网发信
