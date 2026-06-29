# 组件库

## 组件分类

前端组件按功能和用途分为四大类：

```
components/
├── admin/      # 管理后台专用组件
├── common/     # 通用组件（前后台共用）
├── site/       # 前台展示专用组件
│   ├── home/   #   首页组件
│   ├── shell/  #   布局壳组件
│   ├── workspace/ # 工作台组件
│   └── blog/   #   博客组件
└── ui/         # 动画 UI 组件（前台装饰）
```

## 通用组件（common/）

| 组件 | 文件 | 说明 |
|------|------|------|
| 页面容器 | `ViewPage.vue` | 统一的页面容器，提供标题、工具栏插槽 |
| 工具栏 | `ViewToolbar.vue` | 页面顶部工具栏（搜索、按钮等） |
| 用户头像 | `UserAvatar.vue` | 用户头像组件，支持自定义大小和默认图 |
| 状态栏 | `AppStatusBar.vue` | 全局状态栏（加载中、网络状态等） |
| Markdown 渲染 | `MarkdownContent.vue` | Markdown 内容渲染组件 |
| 主题切换 | `ThemeToggle.vue` | 明暗主题切换按钮 |
| 对话框 | `ElDialog.vue` | Element Plus Dialog 封装 |
| 抽屉 | `ElDrawer.vue` | Element Plus Drawer 封装 |

## 前台组件（site/）

### 首页组件（site/home/）

| 组件 | 说明 |
|------|------|
| `HomeHero.vue` | 首页 Hero 区域（全屏动画） |
| `HomeHeroField.vue` | Hero 粒子动画场 |
| `HomeFocusSection.vue` | 焦点内容区 |
| `HomeActivitiesSection.vue` | 活动展示区 |
| `HomeAwardsSection.vue` | 获奖展示区 |
| `HomePeopleSection.vue` | 人员展示区 |
| `HomeMapSection.vue` | 地图展示区（Mapbox） |
| `HomeFeaturedBlogsSection.vue` | 精选博客区 |
| `HomeOverviewPage.vue` | 概览页面 |
| `HomeInteractiveBackdrop.vue` | 交互背景动画 |

### 布局壳组件（site/shell/）

| 组件 | 说明 |
|------|------|
| `SitePageHero.vue` | 前台页面 Hero 区域 |
| `SiteSectionHeading.vue` | 区块标题 |
| `SiteInfoStrip.vue` | 信息条 |

### 工作台组件（site/workspace/）

| 组件 | 说明 |
|------|------|
| `WorkspacePanel.vue` | 工作台面板 |
| `WorkspaceHero.vue` | 工作台 Hero |

### 博客组件（site/blog/）

| 组件 | 说明 |
|------|------|
| `BlogCommentItem.vue` | 博客评论项 |

## 动画 UI 组件（ui/）

前台使用了一系列自定义动画组件，均采用 Vue 3 组合式 API + GSAP/TailwindCSS 实现：

### AuroraBackground（极光背景）

全屏渐变背景动画，用于首页 Hero 区域。

### AppleCardCarousel（Apple 卡片轮播）

Apple 风格的 3D 卡片轮播组件，支持：
- 中心卡片放大效果
- 模糊背景图片
- 点击切换

### FlipCard（翻转卡片）

3D 翻转卡片，正面显示标题，背面显示详情。

### Globe（3D 地球）

基于 `cobe` 库的 3D 地球组件，用于地图展示区。

### Marquee（跑马灯）

无限滚动列表，用于评论、活动等横向滚动展示。

### SmoothCursor（平滑光标）

自定义光标跟随效果，平滑插值移动。

### 其他动画组件

| 组件 | 说明 |
|------|------|
| `PatternBackground` | SVG 图案背景 |
| `InteractiveGridPattern` | 交互式网格背景 |
| `RadiantText` | 渐变发光文字 |
| `LiquidLogo` | WebGL 液态 Logo 动画 |
| `Tetris` | 俄罗斯方块装饰动画 |
| `EncryptedText` | 加密文字打字机效果 |

## 管理后台组件（admin/）

| 组件 | 文件 | 说明 |
|------|------|------|
| 菜单偏好 | `AdminMenuPreferences.vue` | 管理后台菜单收藏/排序 |

## Element Plus 集成

管理后台使用 Element Plus 作为 UI 组件库，通过 `unplugin-vue-components` 自动导入：

```ts
// vite.config.ts
Components({
    dts: false,
    resolvers: [ElementPlusResolver({ directives: false, importStyle: 'css' })],
})
```

### 注册的插件

```ts
// plugins/element-plus.ts
// 注册 Element Plus 需要手动引入的插件（如 ElMessage、ElLoading）
```

## 样式系统

### TailwindCSS

前台使用 TailwindCSS 3.4 作为主要样式方案，配置了 `tailwindcss-animate` 插件支持动画。

### 类名合并

使用 `tailwind-merge` + `clsx` 合并 Tailwind 类名，避免冲突：

```ts
// lib/utils.ts
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs))
}
```

### 设计令牌

前台设计遵循 Apple 风格设计系统，核心设计令牌参考 `DESIGN.md`：

- 主色：`#0066cc`
- 深色背景：`#1d1d1f`
- 浅色背景：`#f5f5f7`
