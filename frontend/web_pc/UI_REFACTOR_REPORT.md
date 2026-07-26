# Web PC UI/UX 规范化报告

## 结论

本次审查覆盖 `src` 中 185 个 Vue/TypeScript/CSS 文件、80 个视图、56 个组件与 93 个路由路径定义。除用户明确不要求验收的 `/next` 外，所有页面均通过根布局、Element Plus 主题映射、共享页面骨架和全局状态规范纳入同一套黑白灰设计系统；`/next` 保留现有路由和业务行为，并已兼容新的中性色 Token，但不作为最终视觉验收页面。

没有修改 API 地址、请求参数、路由路径、权限判断或数据结构。

## 原始问题审查

- 样式来源分散在超长全局样式、页面 scoped 样式与 Element Plus 按需样式中，加载顺序会使组件库默认蓝色重新出现。
- 公共站点、登录页、激活流程、成员档案与后台页面使用过多套色板，紫色、蓝色、橙色及彩色渐变缺乏统一语义。
- 页面内容宽度、左右留白、顶部工具栏、卡片圆角、控件高度和表格密度不一致。
- 许多页面分别实现标题、筛选、状态、弹窗和搜索结构，空态/加载态/错误态缺乏统一语义。
- 顶部栏缺少稳定的毛玻璃层级；部分普通卡片则存在不必要的装饰和阴影。
- 部分图标按钮缺少可访问名称，个别表单缺少显式 label 关联；焦点状态会退回 Element Plus 蓝色。
- 基线扫描记录到 933 次十六进制颜色、256 次 `!important`、181 个行内 style 属性。大量数值属于历史页面和第三方视觉实现，存在重复与维护成本。
- 管理后台需要登录态，无法在未提供凭据的环境中逐页运行时访问；因此后台采用路由、模板、组件与构建产物的代码级全量审查，并对公共路径做浏览器视觉验证。

## Design System

### 配色与主题

- 主视觉只使用黑、白与 `gray-50` 至 `gray-950`。
- Primary 使用 `#1d1d1f`，Hover 使用纯黑；深色主题反转为近白 Primary。
- 成功、警告、危险和信息仅使用低饱和语义 Token，不作为装饰色。
- Element Plus 的 Primary、Fill、Border、Text、Size、Radius、Mask 与 Shadow 全部映射到项目 Token。
- 使用 `html:root` 提高变量覆盖稳定性，避免按需加载的 Element Plus 样式重新注入默认蓝色。

### 字体与层级

- 系统字体栈：Apple 系统字体、SF Pro、Helvetica Neue、苹方及常见中文系统字体。
- 字号范围 12/13/14/15/17/20/24/28/32px。
- 页面标题 24px、600；卡片标题 17px、600；正文 14px；辅助信息 12–13px。
- 正文行高 1.65，标题行高 1.35，避免全局重字重。

### 间距、圆角、阴影与布局

- 4px 基础间距网格，页面主间距 24px。
- 控件高度 30/36/42px；移动端交互控件最低 44px。
- 卡片 12px、弹窗 16px、按钮和输入框 8px；标签仅在确需胶囊语义时使用全圆角。
- 普通卡片使用边框、不默认悬浮；阴影只用于导航、下拉、弹窗等层级。
- 公共内容最大宽度 1280px；后台内容最大宽度 1360px。
- 页面 gutter：桌面 32px、中等屏幕 24px、小屏幕 16px。

### 毛玻璃

- 按最新要求增强为 `blur(28px) saturate(180%)`。
- 仅用于顶部导航、后台顶栏、浮动工具栏、筛选工具面、弹窗/抽屉头部和下拉菜单。
- 所有玻璃表面先声明纯色背景作为降级，再使用 68%/80% 透明层与克制边框。
- 普通内容卡片、表格和工作区面板保持实色，避免整站过度毛玻璃。

### 交互与无障碍

- Focus Visible 统一为黑/白高对比轮廓，移除默认蓝色焦点环。
- 支持 `prefers-reduced-motion`，关闭装饰性连续动画并缩短过渡。
- 图标按钮补充 `aria-label`，表单 label 与控件 id 关联；404 使用统一状态语义。
- Hover/Active/Disabled、表单 Focus、表格 Hover、弹窗遮罩和分页状态统一。

## 公共组件

- `ViewPage.vue`：现有 PageContainer 基础，负责 loading 与页面根语义。
- `ViewToolbar.vue`：重构为统一 PageHeader/Toolbar，自动关联标题的可访问 id。
- `PageHeader.vue`：标准标题、说明、眉题、面包屑和操作区。
- `ContentCard.vue`：统一内容容器与可选头部/操作区。
- `FilterBar.vue`：响应式筛选与操作工具面。
- `SearchInput.vue`：统一搜索输入框、清除和搜索事件。
- `StatusBadge.vue`：默认/成功/警告/危险状态标签。
- `StatePanel.vue`：统一 Empty、Loading 与 Error 三类状态。
- `ConfirmDialog.vue`：统一取消/确认顺序与危险操作语义。
- `ThemeToggle.vue`：统一尺寸、圆角和可访问名称。

## 页面覆盖清单

### 根级与认证/错误（4）

- `AuthCallback.vue`
- `GithubAuthCallback.vue`
- `GiteeAuthCallback.vue`
- `NotFound.vue`

### 管理后台（41）

- `ActivationSettings.vue`
- `Activities.vue`
- `AiActivityAutomation.vue`
- `AlumniGroups.vue`
- `AlumniManagers.vue`
- `Applications.vue`
- `Awards.vue`
- `Blogs.vue`
- `BotGroups.vue`
- `CheckIns.vue`
- `Clubs.vue`
- `Dashboard.vue`
- `DataOpen.vue`
- `Departments.vue`
- `FileMigration.vue`
- `FormSubmissions.vue`
- `GroupEditor.vue`
- `Groups.vue`
- `Images.vue`
- `Interviews.vue`
- `Leaves.vue`
- `Login.vue`
- `Logs.vue`
- `Lotteries.vue`
- `MemberProfileComments.vue`
- `Memberships.vue`
- `Notifications.vue`
- `OauthClients.vue`
- `OfficeDocuments.vue`
- `PartnerClubs.vue`
- `Points.vue`
- `Positions.vue`
- `QrCenter.vue`
- `RecruitmentCampaigns.vue`
- `Regulations.vue`
- `Roles.vue`
- `SchoolCalendar.vue`
- `ShowcaseApps.vue`
- `SiteForms.vue`
- `Users.vue`
- `Votes.vue`

### 公共站点（35）

- `About.vue`
- `Activation.vue`
- `Activities.vue`
- `ActivityDetail.vue`
- `AlumniManagers.vue`
- `AppDetail.vue`
- `ApplicationForm.vue`
- `ApplicationProgress.vue`
- `Apps.vue`
- `Blog.vue`
- `BlogDetail.vue`
- `CheckInScan.vue`
- `Clubs.vue`
- `EveningStudy.vue`
- `Home.vue`
- `ImageHosting.vue`
- `Leaves.vue`
- `LotteryScreen.vue`
- `MemberProfile.vue`
- `Members.vue`
- `MyBlog.vue`
- `NextPage.vue`（保留兼容；按用户要求不纳入最终视觉验收）
- `Notifications.vue`
- `OpenPlatform.vue`
- `Partners.vue`
- `Points.vue`
- `Profile.vue`
- `ProfileEditor.vue`
- `QrScreen.vue`
- `Recruitment.vue`
- `Regulations.vue`
- `SchoolCalendar.vue`
- `SiteForm.vue`
- `Votes.vue`
- `Workspace.vue`

其中 72 个视图复用 `ViewPage`，37 个视图复用 `ViewToolbar`；其余沉浸式公共页通过 `SiteLayout`、公共 Token 与全局 Element Plus 主题纳入统一规范。

## 文件清单

### 新增

- `src/styles/tokens.css`
- `src/styles/theme.css`
- `src/styles/reset.css`
- `src/styles/components.css`
- `src/components/common/PageHeader.vue`
- `src/components/common/ContentCard.vue`
- `src/components/common/FilterBar.vue`
- `src/components/common/SearchInput.vue`
- `src/components/common/StatusBadge.vue`
- `src/components/common/StatePanel.vue`
- `src/components/common/ConfirmDialog.vue`

### 本轮直接修改

- `src/main.ts`
- `src/styles/global.css`
- `src/layouts/SiteLayout.vue`
- `src/layouts/AdminLayout.vue`
- `src/components/common/ThemeToggle.vue`
- `src/components/common/ViewToolbar.vue`
- `src/components/site/home/Test.vue`
- `src/components/site/member/MemberBannerCard.vue`
- `src/components/site/member/MemberProfileRenderer.vue`
- `src/components/site/workspace/WorkspacePanel.vue`
- `src/views/NotFound.vue`
- `src/views/admin/Lotteries.vue`
- `src/views/site/About.vue`
- `src/views/site/Activation.vue`
- `src/views/site/MemberProfile.vue`
- `src/views/site/Members.vue`
- `src/views/site/NextPage.vue`
- `src/views/site/OpenPlatform.vue`
- `src/views/site/activation-redesign.css`

### 删除

- 无。

工作树中原本已有的首页、地图、人物墙和 MorphingText 修改均被保留，没有回滚或覆盖其业务逻辑。

## 量化结果

- 十六进制颜色出现次数：933 → 521，减少约 44%。
- 高饱和硬编码颜色：收敛至 31 次；剩余主要为成功/警告/危险、第三方登录品牌和深色中性色。
- 新增样式层未新增 `any`；仅在 `prefers-reduced-motion` 的全局安全降级中使用 4 次必要的 `!important`，未用于业务组件抢占样式。
- Element Plus 默认蓝色变量已在运行时确认变为 `#1d1d1f`。
- 顶栏运行时计算值：`blur(28px) saturate(1.8)`；浅色背景 `rgba(255,255,255,.8)`，深色背景 `rgba(28,28,30,.8)`。

## 验证结果

- TypeScript：`vue-tsc --noEmit` 通过。
- 生产构建：`vite build` 通过，5266 个模块完成转换。
- 本轮修改和新增的 30 个代码/样式文件：`oxfmt --check` 通过。
- 全量 `src` 格式检查：仍有 69 个未触及的历史文件不符合 oxfmt；没有为制造超大纯格式 diff 而改写这些文件。
- ESLint/Oxlint：项目未安装 oxlint 可执行文件，也没有 lint script。
- Stylelint：未配置。
- 单元测试：未配置 test script。
- 浏览器：公共首页、活动页、登录页完成视觉检查；活动页在 1280×720、1366×768、1440×900、1920×1080、2560×1440 下无横向溢出。
- 主题：浅色/深色均完成截图验证；主题切换与焦点状态通过。
- 管理后台：未提供登录态，无法逐页运行时进入；路由、页面模板、共享组件、响应式 CSS、类型检查和生产构建均已覆盖。

## 构建警告与遗留限制

- Rollup 会移除 `@vueuse/core` 中位置不合法的 `#__PURE__` 注释；来自第三方依赖。
- Mapbox 与 Mermaid/ELK 相关 chunk 超过 900kB。功能构建成功，但属于后续性能拆包议题，不是本轮 UI 规范化引入。
- 未认证访问 `/activation` 会按现有守卫跳转登录并记录激活信息请求失败；这是现有认证/API 状态，不是视觉回归。
