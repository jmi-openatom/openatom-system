# 前端项目结构

## 目录结构

前端 PC 端代码位于 `frontend/web_pc/` 目录下：

```
frontend/web_pc/
├── src/
│   ├── main.ts                     # 应用入口
│   ├── App.vue                     # 根组件
│   ├── api/                        # API 请求层
│   │   ├── request.ts              #   Axios 实例与拦截器
│   │   └── index.ts                #   API 方法定义
│   ├── router/
│   │   └── index.ts                #   路由配置（含权限守卫）
│   ├── layouts/                    # 布局组件
│   │   ├── AdminLayout.vue         #   管理后台布局
│   │   └── SiteLayout.vue          #   前台布局
│   ├── views/                      # 页面视图
│   │   ├── admin/                  #   管理后台页面（42 个）
│   │   ├── site/                   #   前台展示页面（37 个）
│   │   └── AuthCallback.vue 等     #   根级页面（4 个）
│   ├── components/                 # 公共组件
│   │   ├── admin/                  #   管理后台组件
│   │   ├── common/                 #   通用组件（13 个）
│   │   ├── site/                   #   前台组件
│   │   └── ui/                     #   动画 UI 组件
│   ├── composables/                # 组合式函数（4 个）
│   ├── constants/                  # 常量定义（colleges.ts）
│   ├── plugins/                    # 插件注册（element-plus.ts）
│   ├── styles/                     # 全局样式（6 个 css）
│   ├── utils/                      # 工具函数（8 个）
│   ├── lib/                        # 第三方库封装
│   └── vendor/                     # 第三方代码
├── public/                         # 静态资源
├── package.json                    # pnpm 管理（pnpm 9）
├── vite.config.ts                  # Vite 构建配置
├── tsconfig.json                   # TypeScript 配置
├── nginx.conf                      # 容器内 Nginx 配置
└── Dockerfile                      # 前端容器化
```

## 管理后台页面（admin/，42 个）

| 页面 | 文件 | 说明 |
|------|------|------|
| 仪表盘 | `Dashboard.vue` | 数据概览 |
| 用户管理 | `Users.vue` | 用户 CRUD、角色分配 |
| 社团管理 | `Clubs.vue` | 社团 CRUD |
| 部门管理 | `Departments.vue` | 部门架构管理 |
| 岗位管理 | `Positions.vue` | 岗位 CRUD |
| 统一分组 | `Groups.vue` | 分组中心（部门/外群/校友） |
| 分组编辑 | `GroupEditor.vue` | 分组创建与详情 |
| 激活设置 | `ActivationSettings.vue` | 账号激活引导设置 |
| 成员管理 | `Memberships.vue` | 成员关系管理 |
| 招新计划 | `RecruitmentCampaigns.vue` | 招新计划管理 |
| 入会申请 | `Applications.vue` | 申请审批 |
| 面试管理 | `Interviews.vue` | 面试安排 |
| 活动管理 | `Activities.vue` | 活动 CRUD |
| AI 活动自动化 | `AiActivityAutomation.vue` | AI 活动管理 |
| 签到管理 | `CheckIns.vue` | 签到会话管理 |
| 请假管理 | `Leaves.vue` | 请假审批 |
| 获奖管理 | `Awards.vue` | 获奖记录 |
| 通知管理 | `Notifications.vue` | 通知推送 |
| 文书管理 | `OfficeDocuments.vue` | 文书生成与导出 |
| 文档中心 | `DocCenter.vue` | ONLYOFFICE 文档中心 |
| 校历管理 | `SchoolCalendar.vue` | 校历设置 |
| 规章制度 | `Regulations.vue` | 规章制度管理 |
| 角色管理 | `Roles.vue` | 角色与权限分配 |
| 表单管理 | `SiteForms.vue` | 信息收集表单 |
| 表单提交 | `FormSubmissions.vue` | 表单数据查看 |
| 抽奖管理 | `Lotteries.vue` | 抽奖活动 |
| 投票管理 | `Votes.vue` | 投票活动 |
| 积分管理 | `Points.vue` | 积分系统 |
| 博客管理 | `Blogs.vue` | 博客审核 |
| 主页评论管理 | `MemberProfileComments.vue` | 成员主页评论 |
| 图床管理 | `Images.vue` | 图床资源 |
| 应用展示 | `ShowcaseApps.vue` | 展示应用管理 |
| 合作社团 | `PartnerClubs.vue` | 合作社团管理 |
| 数据开放 | `DataOpen.vue` | 数据开放申请 |
| OAuth 客户端 | `OauthClients.vue` | OAuth 应用管理 |
| QQ 群管理 | `BotGroups.vue` | 机器人群管理 |
| 校友分组 | `AlumniGroups.vue` | 往届成员分组 |
| 往届管理 | `AlumniManagers.vue` | 往届管理人员 |
| 二维码中心 | `QrCenter.vue` | 二维码生成 |
| 文件迁移 | `FileMigration.vue` | 存储迁移工具 |
| 日志查看 | `Logs.vue` | 操作/登录日志 |
| 登录页 | `Login.vue` | 管理后台登录 |

## 前台展示页面（site/，37 个）

| 页面 | 文件 | 说明 |
|------|------|------|
| 首页 | `Home.vue` | 社团首页展示 |
| 关于我们 | `About.vue` | 关于页（含影集轮播） |
| 加入我们 | `NextPage.vue` | 开放原子招新落地页 |
| 社团展示 | `Clubs.vue` | 社团列表 |
| 招新 | `Recruitment.vue` | 招新信息展示 |
| 报名表单 | `ApplicationForm.vue` | 入会申请填写 |
| 申请详情 | `AppDetail.vue` | 申请查看 |
| 申请进度 | `ApplicationProgress.vue` | 审批进度 |
| 活动 | `Activities.vue` | 活动列表 |
| 活动详情 | `ActivityDetail.vue` | 活动详情 |
| 博客 | `Blog.vue` | 博客列表 |
| 博客详情 | `BlogDetail.vue` | 博客文章 |
| 我的博客 | `MyBlog.vue` | 个人博客管理/写作 |
| 应用展示 | `Apps.vue` | 开源应用 |
| 应用详情 | `AppDetail.vue` | 应用详情 |
| 合作社团 | `Partners.vue` | 合作社团展示 |
| 规章制度 | `Regulations.vue` | 规章列表/详情 |
| 社区投票 | `Votes.vue` | 投票参与 |
| 社区积分 | `Points.vue` | 积分商城 |
| 校历 | `SchoolCalendar.vue` | 校历查看 |
| 校友管理 | `AlumniManagers.vue` | 往届展示 |
| 开放平台 | `OpenPlatform.vue` | 数据开放 |
| 信息收集 | `SiteForm.vue` | 表单填写 |
| 图床 | `ImageHosting.vue` | 图床服务 |
| 工作台 | `Workspace.vue` | 个人工作台 |
| 成员列表 | `Members.vue` | 社团成员 |
| 成员主页 | `MemberProfile.vue` | 成员主页/我的主页 |
| 编辑主页 | `ProfileEditor.vue` | 主页信息编辑 |
| 账号安全 | `Profile.vue` | 账号与安全设置 |
| 通知 | `Notifications.vue` | 通知列表 |
| 请假 | `Leaves.vue` | 请假申请 |
| 晚自习 | `EveningStudy.vue` | 晚自习签到 |
| 激活 | `Activation.vue` | 账号激活引导 |
| 扫码签到 | `CheckInScan.vue` | 扫码签到 |
| 二维码大屏 | `QrScreen.vue` | 二维码展示 |
| 抽奖大屏 | `LotteryScreen.vue` | 抽奖大屏 |
| 文档编辑 | `DocEditorPage.vue` | 在线文档编辑 |
| 共享编辑 | `SharedEditPage.vue` | 共享文档编辑 |

## 动画 UI 组件（components/ui/）

前台使用了一系列自定义动画组件，参考 Apple 风格设计：

| 组件 | 说明 |
|------|------|
| `AuroraBackground` | 极光背景动画 |
| `AppleCardCarousel` | Apple 风格卡片轮播 |
| `FlipCard` | 翻转卡片 |
| `Globe` | 3D 地球组件 |
| `Marquee` | 跑马灯滚动（含 ReviewCard） |
| `SmoothCursor` | 平滑光标跟随 |
| `PatternBackground` | 图案背景 |
| `InteractiveGridPattern` | 交互网格 |
| `RadiantText` | 发光文字 |
| `LiquidLogo` | 液态 Logo 动画（shader） |
| `Tetris` | 俄罗斯方块装饰 |
| `EncryptedText` | 加密文字效果 |
| `MorphingText` | 文字变形 |
| `LinkPreview` | 链接预览 |

## 工具函数（utils/）

| 文件 | 说明 |
|------|------|
| `auth.ts` | Token 管理（存储、清除、获取、激活状态） |
| `oidc.ts` | OIDC 认证 URL 构建 |
| `permission.ts` | 权限判断（`hasAnyPermission`、`hasAdminAccess`） |
| `seo.ts` | 页面标题/描述/结构化数据管理 |
| `googleIdentity.ts` | Google 身份集成 |
| `format.ts` | 日期、数字格式化 |
| `markdown.ts` | Markdown 渲染 |
| `qr.ts` | 二维码生成 |

## 组合式函数（composables/）

| 文件 | 说明 |
|------|------|
| `useAppStatus.ts` | 应用全局状态（请求计数、加载状态） |
| `useRouteTransition.ts` | 路由过渡动画 |
| `useTheme.ts` | 主题切换（明/暗） |
| `useSiteMotion.ts` | 前台动画控制 |

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | 核心框架（组合式 API） |
| Vite | 6.0+ | 构建工具 |
| Element Plus | 2.9+ | UI 组件库 |
| TailwindCSS | 3.4 | 原子化 CSS |
| TypeScript | 5.8 | 类型安全 |
| ECharts | 6 | 图表 |
| Three.js | 0.185 | 3D 场景 |
| Mapbox GL | - | 地图 |
| GSAP / Lenis / motion-v | - | 动效引擎 |
| markdown-it / mermaid | - | Markdown / 流程图渲染 |
| pnpm | 9 | 包管理 |