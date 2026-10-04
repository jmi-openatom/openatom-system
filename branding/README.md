# 社团应用 Logo

所有资源来自 2026-10-05 生成的 JMI-OPENATOM Logo 素材包，保持原图形、比例与配色。

- `logo.svg` / `logo.png`：蓝色纯图标，透明底。
- `logo-light.svg` / `logo-light.png`：白色纯图标，透明底，用于深色背景。
- `logo-lockup.svg`：横向中英文字版。
- `app-icon.png` / `app-icon-dark.png`：1024×1024 无透明通道应用图标，用于 iOS 和网页桌面图标。
- `favicon.ico`：16、32、48、64 像素浏览器图标。

网页使用各应用自己的静态资源，版本标记为 `20261005`。兼容原有 `/logo.png`、`/logo.svg` 和 `/oauth-logo.png` 地址；邮箱群发模板同样引用新 Logo。

| 应用 | 资源目录／接入方式 |
| --- | --- |
| 官网与管理后台 | `frontend/web_pc/public`、`public/brand` |
| OAuth 登录 | `backend/src/main/resources/static/oauth-logo.png` |
| Quest | `openatom-quest/frontend/public` |
| 邮箱 | `mail-system/mail-web/public` |
| 开发文档 | `docs-site/.vuepress/public`，浅色蓝标、深色白标；`docs-site/public` 保留兼容副本 |
| 实验室 | `lab-ui-web/public` |
| UniApp 小程序 | `frontend/uni_app/static/logo.png` |
| iOS | `frontend/ios_app/openatom/Assets.xcassets`，应用图标及登录、首页标识 |
| Seafile 网盘 | `seafile/branding`，部署时复制到持久化的 Seahub `custom` 目录 |
| HedgeDoc 文档协作 | `docs-system/branding`，通过只读挂载替换图标和页眉素材 |

小程序与 iOS 首页对 `JMI-OPENATOM` 使用内置新 Logo，其他社团保留其自己的图片。小程序平台管理后台的头像需要在微信后台另行上传 `app-icon.png`；它不由源码中的页面图片控制。

Seafile 接入依据[官方自定义指南](https://manual.seafile.com/13.0/config/seahub_customization/)。HedgeDoc 使用 [1.10.1 源码的静态资源目录](https://github.com/hedgedoc/hedgedoc/tree/1.10.1/public)及[官方 Docker 路径](https://docs.hedgedoc.org/setup/docker/)；需要重新部署容器后才会生效。ONLYOFFICE、AstrBot、NapCat 的产品标识未作为社团 Logo 替换。
