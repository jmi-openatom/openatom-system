# OpenAtom System — UI 设计体系总结（AI 参考用）

> 本文件汇总本仓库所有前端项目的 UI 方案、UI 库选型与设计规范，供编写其他项目时参考。

---

## 1. 项目全景

| 项目 | 路径 | 技术栈 | UI 库 | 用途 |
|---|---|---|---|---|
| 主站 + 管理后台 | `frontend/web_pc` | Vue 3.5 + Vite 6 + TypeScript + TailwindCSS 3.4 | **Element Plus 2.9** + 自定义组件 | 官网站点（SiteLayout）+ 后台管理（AdminLayout） |
| 移动端小程序 | `frontend/uni_app` | uni-app + Vue 3 | **Vant Weapp 1.11** + **tmui-uni** + @dcloudio/uni-ui | 微信小程序 |
| 实验室前台 | `lab-ui-web` | Vue 3.5 + Vite 6 | **无 UI 库**（纯手写 CSS + lucide-vue-next 图标） | 轻量前台页 |
| 邮件系统 | `mail-system/mail-web` | Vue 3.5 + Vite 6 | **无 UI 库**（纯手写 CSS + lucide-vue-next 图标） | 轻量邮件 Web |
| 文档站 | `docs-site` | VuePress 2 | VuePress 默认主题 | 开发文档 |

---

## 2. UI 库选型

### 2.1 主站 / 管理后台（web_pc）

- **组件库**：`element-plus ^2.9.3` + `@element-plus/icons-vue`，按需引入（`unplugin-vue-components`），深色模式开启 `element-plus/theme-chalk/dark/css-vars.css`
- **样式方案**：TailwindCSS 3.4（`@tailwind base/components/utilities`）+ **CSS Design Tokens**（CSS 变量）+ 少量组件级 scoped CSS
- **图标**：`lucide-vue-next`（新版）+ `@element-plus/icons-vue`（Element 组件内使用）
- **动画**：`gsap`、`motion-v`、`vue-use-spring`、`lenis`（平滑滚动）、`cobe`（3D 地球）、`three`（3D）、`mapbox-gl`（地图）
- **图表**：`echarts ^6`
- **工具类**：`class-variance-authority` + `clsx` + `tailwind-merge`（组件变体管理）
- **Markdown 渲染**：`markdown-it` + `mermaid`（图表）

### 2.2 移动端（uni_app）

- `@vant/weapp`（Vant 小程序版，1.11）、`tmui-uni`（3.2）、`@dcloudio/uni-ui`、`pinia`、`sass`
- 注意：`postinstall` 脚本 `patch-vant-use.js` + `sync-vant-weapp.js` 用于适配 uni-app

### 2.3 轻量项目（lab-ui-web / mail-web）

- 刻意**不引入 UI 组件库**，只依赖 `lucide-vue-next` 图标 + 手写 CSS，适用于小体量界面

---

## 3. 设计体系（web_pc，核心参考）

### 3.1 核心设计理念

- **Apple 风格克制设计**：中性色为主、黑白即主色（`--color-primary: #1d1d1f` 近黑），状态色（绿/橙/红）仅用于状态表达，绝不做装饰用途
- **4px 栅格**：间距 `--space-1..16` = 4px 递增（4/8/12/16/20/24/28/32/40/48/64）
- **玻璃拟态（Glassmorphism）**：导航栏、工具栏、浮层使用毛玻璃（`backdrop-filter: blur(28px) saturate(1.8)`）
- **圆角**：xs 4px / sm 6px / md 8px / lg 12px / xl 16px / round 9999px
- **阴影**：极轻，xs/sm/md/lg 四档，注重边界线（`border: 1px solid rgba(0,0,0,0.1)`）而非阴影

### 3.2 Design Tokens 文件结构（`src/styles/`）

| 文件 | 作用 |
|---|---|
| `reset.css` | 样式重置 |
| `tokens.css` | **核心设计变量**（颜色/字体/间距/圆角/阴影/动效/层级），含 `[data-theme='dark']` 深色覆盖 |
| `theme.css` | 将 tokens 桥接到 **Element Plus 变量**（`--el-*`），实现组件库主题统一 |
| `global.css` | Tailwind 指令 + 全局类（container、page-title、panel、admin 布局、Element Plus 深度定制 `!important` 覆盖） |
| `components.css` | 应用结构类（page-container、toolbar、content-card、state-panel、status-badge、表格/对话框定制） |

> **重要约定**：Element Plus 的样式通过 `--el-*` 变量桥接（见 `theme.css`），而不是直接改组件 SCSS。

### 3.3 关键 Token 摘录

```css
/* 主色即近黑（亮色），近白（暗色） */
--color-primary: #1d1d1f;
--color-bg-page: #f5f5f7;
--color-text-primary: #1d1d1f;
--color-text-secondary: #6e6e73;
--color-border: rgba(0, 0, 0, 0.1);

/* 字体：系统字体栈（SF Pro + PingFang SC） */
--font-family-sans: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text',
  'Helvetica Neue', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;

/* 字号阶梯：12 / 13 / 14 / 15 / 17 / 20 / 24 / 28 / 32 */
--font-size-xs: 12px; ... --font-size-display: 32px;

/* 控件高度：30 / 36 / 42 */
--control-height-sm: 30px; --control-height: 36px; --control-height-lg: 42px;

/* 玻璃拟态 */
--glass-bg: rgba(255, 255, 255, 0.68);
--glass-filter: blur(28px) saturate(1.8);

/* 兼容别名 --oa-*：旧代码大量使用，新代码建议直接用 --color-*/-- 基础变量 */
```

### 3.4 深色模式

- 通过 `html.dark` / `[data-theme='dark']` 切换（composable：`src/composables/useTheme`，入口 `initTheme()`）
- 所有 tokens 提供暗色值；Element Plus 深色变量在 `theme.css` 中同步覆盖
- 组件内一律使用语义变量，禁止写死颜色，保证双主题自动生效

---

## 4. 布局体系

### 4.1 SiteLayout（官网）

- 毛玻璃吸顶 header（`--oa-header-bg`）+ 内容容器
- 页面骨架类：`.site-page`（页动画 `oaPageEnter`）、`.container`（`min(1180px, 100% - 48px)`）、`.toolbar`（页头标题区，min-height 220px）、`.page-header__eyebrow`（小号大写标签）

### 4.2 AdminLayout（管理后台）

- Element Plus `el-container` 结构：`el-aside`（可折叠侧边栏 + el-menu 分组菜单）+ `el-header`（毛玻璃、面包屑 + 页面标题 + 主题切换 + 用户下拉）+ `el-main`
- 路由切换动画：`mode="out-in"` + gsap 过渡（`routeTransition`）
- 移动端：抽屉式菜单 + 折叠按钮

### 4.3 页面通用模式

```html
<div class="admin-page page-container">
  <div class="toolbar">
    <div class="toolbar__heading">
      <h2>页面标题</h2>
      <p>页面描述</p>
    </div>
    <div class="toolbar__actions">
      <!-- 操作按钮 -->
    </div>
  </div>
  <div class="filter-bar"><!-- 筛选栏 --></div>
  <div class="content-card"><!-- 内容卡片 --></div>
</div>
```

---

## 5. 通用业务组件（`src/components/common/`）

| 组件 | 说明 |
|---|---|
| `PageHeader.vue` | 页头（标题 + 描述 + 操作区） |
| `ContentCard.vue` | 内容卡片容器 |
| `FilterBar.vue` | 筛选栏 |
| `ViewPage.vue` / `ViewToolbar.vue` | 列表页脚手架 |
| `ConfirmDialog.vue` | 确认对话框 |
| `StatusBadge.vue` | 状态徽章（success/warning/danger/info） |
| `StatePanel.vue` | 空状态 / 加载失败面板 |
| `AppStatusBar.vue` | 全局网络状态提示条 |
| `SearchInput.vue` / `UserAvatar.vue` / `ThemeToggle.vue` / `MarkdownContent.vue` | 通用小件 |

## 6. 特效组件（`src/components/ui/`）

Apple 风格展示组件（多用于官网首页）：

- `apple-card-carousel` 苹果风卡片轮播
- `aurora-background` 极光背景
- `flip-card` 翻转卡片
- `globe`（cobe 3D 地球）
- `interactive-grid-pattern` 交互网格
- `liquid-logo` 液态 Logo
- `marquee` 跑马灯
- `morphing-text` 变形文字
- `radiant-text` 光晕文字
- `smooth-cursor` 自定义光标
- `tetris` 俄罗斯方块装饰
- `encrypted-text`、`link-preview`、`pattern-background`

---

## 7. Element Plus 定制规范

- 按钮：圆角 `--radius-md`，按下 `scale(0.98)`，focus-visible 用 2px 描边而非阴影
- 输入框：`box-shadow: 0 0 0 1px var(--color-border-strong) inset` 代替默认边框；聚焦态 = 1px 主色描边 + 3px `--oa-focus-ring`
- 对话框/抽屉：圆角 xl、标题/主体/底部三段式分隔线、内容区 `max-height + overflow-y: auto`
- 表格：表头 `--color-bg-subtle` + semibold，行 hover 浅灰，圆角 lg
- 弹层遮罩：半透明黑 + `backdrop-filter: blur(5px) saturate(0.72)`
- 分页、Tabs、Switch、Checkbox、Upload、DatePicker 均有统一 `--oa-*` 变量定制（见 `global.css` 后半部分）
- **移动端适配**：`@media (max-width: 767px)` 下输入控件 min-height 提至 44px、表单纵向排列、表格横向滚动

---

## 8. 动效约定

- 时长：fast 120ms / base 200ms / slow 320ms；缓动 `cubic-bezier(0.25, 0.1, 0.25, 1)`（standard）、`cubic-bezier(0.16, 1, 0.3, 1)`（out）
- 页面入场动画：`oaPageEnter`（fade + translateY 18px，0.46s）——注意 admin 页面禁用 transform 动画以防 fixed 弹窗错位
- 滚动入场：`.site-motion-pending` / `.site-motion-visible`（IntersectionObserver + transform）
- 完整支持 `prefers-reduced-motion: reduce`（全局关闭动画）
- 使用 `lenis` 做平滑滚动；复杂动效用 gsap / motion-v / vue-use-spring

---

## 9. 移动端（uni_app）规范

- 基于 Vant Weapp 组件（`<van-button>` 等），样式用 scss
- 状态管理 pinia；路由为 uni-app pages.json 方式
- 与 Web 端共享同一套业务语义（状态色 success/warning/danger）

---

## 10. 编写新项目时可直接复用的约定

1. **选型**：管理/中后台 → Vue 3 + Element Plus + Tailwind；纯展示/轻量 → 无组件库 + lucide 图标；小程序 → Vant Weapp
2. **样式必须基于 CSS 变量**，禁止硬编码颜色；深色模式自动生效
3. **4px 间距栅格**，控件高度 30/36/42，圆角 8px 起
4. **黑白为主色**，状态色（绿/橙/红）只表达状态
5. 页面结构统一为 `toolbar（标题+操作）→ filter-bar（筛选）→ content-card（内容）`
6. 弹窗/抽屉/浮层用毛玻璃 + 三段式布局；遮罩加 blur
7. 动画遵循 120/200/320ms 时长体系，并处理 `prefers-reduced-motion`
8. 管理端路由切换用 out-in 过渡动画，注意 `fixed` 弹窗不要被路由 transform 容器影响
