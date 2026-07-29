# Stalwart v0.16 首次引导清单

> 管理面只监听宿主机 `127.0.0.1:18081`。请通过 SSH 隧道访问，不要把 `/admin` 暴露到公网。

运行 Actions 的 `deployment_mode=auto` 后，`deploy/stalwart/plan.ndjson.template` 会自动创建邮件域与 OIDC Directory，并配置默认主机名、认证目录、RCPT 防中继、25 MiB 邮件限制、出站直投和 Prometheus。自动引导脚本还会创建专用自动化账号、两枚最小权限 API Key、查询 Domain ID，将三者写入服务器 `0600` 环境文件，然后删除临时账号密码并关闭恢复模式。

自动创建的账号自动化 API Key 权限仅包含：

   - `sysAccountGet`
   - `sysAccountQuery`
   - `sysAccountCreate`
   - `sysAccountUpdate`

配置自动化 Key 的权限清单见 [`environment-variables.md`](environment-variables.md)。两个 Key 不合并，均不授予销毁、日志或邮件正文读取权限。

仍需人工完成的只有外部基础设施项目：

1. 从域对象的 `dnsZoneFile` 复制真实 DKIM 公钥，替换 DNS 模板占位符；发布模板中其余记录，并在 IP 服务商设置 PTR。
2. 建立并由运维真实接收：`postmaster@jmi-openatom.cn`、`abuse@jmi-openatom.cn`、`dmarc@jmi-openatom.cn`、`tlsrpt@jmi-openatom.cn`。
3. 如需交互式管理，按恢复流程临时启用 `STALWART_RECOVERY_ADMIN`，经 SSH 隧道访问 `http://127.0.0.1:18081/admin`；操作完成立即清空并恢复 `STALWART_RECOVERY_MODE=0`。

`mail-api` 预建的 Stalwart 技术账户名是不可变 OAuth `sub`，姓名拼音地址是用户可见、可收发的启用别名。这个区分保证改名不会破坏 OAuth 映射；重名时按稳定规则增加后缀。不要手工把技术账户名改成拼音。

验证 OIDC 时，Access Token 的 `aud` 必须包含 `stalwart`，签名为 RS256；JWKS 只能出现 RSA 公钥参数，不能出现 `k`、`d` 或客户端密钥。
