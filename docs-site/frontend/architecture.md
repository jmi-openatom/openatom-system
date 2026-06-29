# 前端架构概览

## 整体架构

PC 端前端基于 Vue 3 + Vite + TypeScript 构建，采用组合式 API（Composition API）风格：

```
┌─────────────────────────────────────────────────────────┐
│                      用户浏览器                           │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    Vue Router                            │
│    ┌─────────────┐         ┌──────────────┐            │
│    │  Site 路由   │         │  Admin 路由   │            │
│    │  (前台展示)  │         │  (管理后台)   │            │
│    └──────┬──────┘         └──────┬───────┘            │
│           │                       │                      │
│    ┌──────┴──────┐         ┌──────┴───────┐            │
│    │ SiteLayout  │         │ AdminLayout  │            │
│    └──────┬──────┘         └──────┬───────┘            │
│           │                       │                      │
│           ▼                       ▼                      │
│    ┌──────────────────────────────────────┐             │
│    │         页面组件 (Views)              │             │
│    │   Home / Profile / Blog / Clubs ...   │             │
│    └──────────────────┬───────────────────┘             │
│                       │                                  │
│    ┌──────────────────┴───────────────────┐             │
│    │       公共组件 (Components)            │             │
│    │   common / site / admin / ui          │             │
│    └──────────────────┬───────────────────┘             │
│                       │                                  │
│    ┌──────────────────┴───────────────────┐             │
│    │       组合式函数 (Composables)         │             │
│    │   useAppStatus / useTheme / ...       │             │
│    └──────────────────┬───────────────────┘             │
│                       │                                  │
│    ┌──────────────────┴───────────────────┐             │
│    │         API 请求层 (api/)              │             │
│    │   Axios 实例 + 请求/响应拦截器          │             │
│    └──────────────────┬───────────────────┘             │
│                       │                                  │
│    ┌──────────────────┴───────────────────┐             │
│    │      工具函数 (utils/)                  │             │
│    │   auth / oidc / permission / format   │             │
│    └──────────────────────────────────────┘             │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              后端 API (api.jmi-openatom.cn)               │
└─────────────────────────────────────────────────────────┘
```

## 双布局模式

前端采用双布局模式，前台展示与管理后台分离：

### Site 布局（前台展示）

- 路由前缀：`/`（非 `/admin`）
- 布局组件：`SiteLayout.vue`
- 页面：首页、社团展示、招新报名、活动展示、博客、个人中心等
- 设计风格：Apple 风格设计系统（参考 `DESIGN.md`）
- UI 特性：GSAP 动画、全屏 Hero、卡片轮播

### Admin 布局（管理后台）

- 路由前缀：`/admin/*`
- 布局组件：`AdminLayout.vue`
- 页面：仪表盘、用户管理、社团管理、成员管理、活动管理等
- UI 特性：Element Plus 表格、表单、对话框
- 权限控制：路由守卫 + 按钮级权限

## 技术栈

| 技术 | 用途 |
|------|------|
| Vue 3 | 核心框架（组合式 API） |
| Vite 6 | 构建工具 |
| TypeScript | 类型安全 |
| Element Plus | 管理后台 UI 组件库 |
| TailwindCSS | 原子化 CSS（前台样式） |
| Vue Router 4 | 路由管理 |
| Axios | HTTP 请求 |
| GSAP + motion-v | 动画引擎 |
| Tailwind Merge | Tailwind 类名合并 |
| Mapbox GL | 地图组件 |

## 前台设计系统

前台采用 Apple 风格设计系统，核心原则：

- **单一蓝色**：`#0066cc` 作为唯一交互色
- **摄影优先**：UI 退让，让内容说话
- **交替瓦片**：浅色/深色全幅瓦片交替作为视觉分隔
- **药丸按钮**：主 CTA 使用全圆角药丸形
- **负字距标题**：标题使用负字距营造紧凑感
- **单一阴影**：仅产品图片使用阴影，UI 不使用

详见 `DESIGN.md` 文件。

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | 后端 API 地址 | `https://api.jmi-openatom.cn/api/v1` |
| `VITE_OIDC_AUTHORITY` | OIDC 认证地址 | `https://oauth.jmi-openatom.cn/api/v1` |
| `VITE_OIDC_CLIENT_ID` | OIDC 客户端 ID | `openatom-web` |
| `VITE_APP_VERSION` | 应用版本号 | 自动从 git 获取 |

## 构建优化

### 代码分割

Vite 配置了 `manualChunks` 手动分包：

```ts
manualChunks(id) {
    if (!id.includes('node_modules')) return undefined
    if (id.includes('mapbox-gl')) return 'mapbox-gl'
    if (id.includes('markdown-it')) return 'markdown'
    if (id.includes('gsap') || id.includes('motion-v')) return 'vendor-motion'
    if (id.includes('/vue/') || id.includes('vue-router')) return 'vendor-vue'
    return undefined
}
```

### 自动导入

使用 `unplugin-vue-components` 自动导入 Element Plus 组件：

```ts
Components({
    dts: false,
    resolvers: [ElementPlusResolver({ directives: false, importStyle: 'css' })],
})
```

### 版本号

构建时自动注入版本号，格式为 `v{版本}.{构建号}-{commitHash}`：

```ts
let appVersion = `v${pkg.version}`
const commitCount = execSync('git rev-list --count HEAD').toString().trim()
const shortHash = execSync('git rev-parse --short HEAD').toString().trim()
appVersion = `v${pkg.version}.${commitCount}-${shortHash}`
```
