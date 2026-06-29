# UniApp 微信小程序

## 概述

除 PC 端 Web 应用外，系统还包含一个基于 UniApp 的微信小程序端，位于 `frontend/uni_app/` 目录。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| UniApp | - | 跨端框架，编译到微信小程序 |
| Vue 3 | 3.5+ | 核心框架 |
| Vant Weapp | 1.11+ | 微信小程序 UI 组件库 |
| Pinia | 2.1+ | 状态管理 |
| TypeScript | 6.0+ | 类型安全 |
| Sass | 1.99+ | 样式预处理 |

## 目录结构

```
frontend/uni_app/
├── api/                    # API 请求封装
├── components/             # 公共组件
├── config/                 # 配置文件
├── i18n/                   # 国际化
├── pages/                  # 页面
├── scripts/                # 构建脚本
├── static/                 # 静态资源
├── store/                  # Pinia 状态管理
├── style/                  # 全局样式
├── utils/                  # 工具函数
├── wxcomponents/vant/      # Vant Weapp 组件
├── App.vue                 # 根组件
├── main.js                 # 应用入口
├── manifest.json           # UniApp 应用配置
├── pages.json              # 页面路由配置
└── package.json
```

## 开发命令

```bash
# 安装依赖
pnpm install

# 编译到微信小程序
pnpm run dev:mp-weixin

# 打开微信开发者工具
pnpm run open:mp-weixin

# 预览模式（编译并打开）
pnpm run preview:mp-weixin

# 类型检查
pnpm run typecheck
```

## 微信小程序配置

### AppID

小程序 AppID 在 `manifest.json` 中配置：

```json
{
    "mp-weixin": {
        "appid": "wx8c6b52ab95da0938",
        "setting": {
            "urlCheck": false
        }
    }
}
```

### API 配置

小程序 API 地址在 `config/` 目录中配置，需指向后端 API 地址。

### Vant Weapp 同步

项目使用自定义脚本同步 Vant Weapp 组件：

```bash
# 同步 Vant Weapp 组件
pnpm run sync:vant-weapp
```

### PostInstall 脚本

安装依赖后自动执行：

```json
{
    "postinstall": "node scripts/patch-vant-use.js && node scripts/sync-vant-weapp.js"
}
```

## 功能说明

小程序端主要提供以下功能：

- 用户登录（微信授权登录）
- 招新报名
- 活动查看与报名
- 签到打卡
- 通知查看
- 个人信息管理

## 与 PC 端的关系

小程序端与 PC 端共享同一套后端 API，通过 Sa-Token 进行认证。小程序登录流程：

1. 用户点击"微信登录"
2. 调用 `wx.login()` 获取 `code`
3. 发送 `code` 到后端 `POST /auth/miniapp-login`
4. 后端调用微信 API 获取 `openid`
5. 返回 Sa-Token JWT
6. 后续请求携带 Token

## CI/CD

GitHub Actions 中包含小程序的类型检查：

```yaml
- name: UniApp Type Check
  run: |
    cd frontend/uni_app
    pnpm install --frozen-lockfile
    pnpm run typecheck
```
