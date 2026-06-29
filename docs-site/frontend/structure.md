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
│   │   ├── AuthCallback.vue        #   OIDC 认证回调
│   │   ├── admin/                  #   管理后台页面（35 个）
│   │   └── site/                   #   前台展示页面（30 个）
│   ├── components/                 # 公共组件
│   │   ├── admin/                  #   管理后台组件
│   │   ├── common/                 #   通用组件
│   │   ├── site/                   #   前台组件
│   │   │   ├── home/               #   首页组件
│   │   │   ├── shell/              #   布局壳组件
│   │   │   ├── workspace/          #   工作台组件
│   │   │   └── blog/               #   博客组件
│   │   └── ui/                     #   动画 UI 组件
│   ├── composables/                # 组合式函数
│   ├── constants/                  # 常量定义
│   ├── plugins/                    # 插件注册
│   ├── styles/                     # 全局样式
│   ├── utils/                      # 工具函数
│   ├── lib/                        # 第三方库封装
│   └── vendor/                     # 第三方代码
├── public/                         # 静态资源
├── package.json
├── vite.config.ts                  # Vite 构建配置
├── tsconfig.json                   # TypeScript 配置
└── Dockerfile                      # 前端容器化
```

## 管理后台页面（admin/）

| 页面 | 文件 | 说明 |
|------|------|------|
| 仪表盘 | `Dashboard.vue` | 数据概览 |
| 用户管理 | `Users.vue` | 用户 CRUD、角色分配 |
| 社团管理 | `Clubs.vue` | 社团 CRUD |
| 岗位管理 | `Positions.vue` | 岗位 CRUD |
| 成员管理 | `Memberships.vue` | 成员关系管理 |
| 招新计划 | `RecruitmentCampaigns.vue` | 招新计划管理 |
| 入会申请 | `Applications.vue` | 申请审批 |
| 面试管理 | `Interviews.vue` | 面试安排 |
| 活动管理 | `Activities.vue` | 活动 CRUD |
| 签到管理 | `CheckIns.vue` | 签到会话管理 |
| 请假管理 | `Leaves.vue` | 请假审批 |
| 获奖管理 | `Awards.vue` | 获奖记录 |
| 通知管理 | `Notifications.vue` | 通知推送 |
| 文书管理 | `OfficeDocuments.vue` | 文书生成与导出 |
| 校历管理 | `SchoolCalendar.vue` | 校历设置 |
| 规章制度 | `Regulations.vue` | 规章制度管理 |
| 角色管理 | `Roles.vue` | 角色与权限分配 |
| 表单管理 | `SiteForms.vue` | 信息收集表单 |
| 表单提交 | `FormSubmissions.vue` | 表单数据查看 |
| 抽奖管理 | `Lotteries.vue` | 抽奖活动 |
| 投票管理 | `Votes.vue` | 投票活动 |
| 积分管理 | `Points.vue` | 积分系统 |
| 博客管理 | `Blogs.vue` | 博客审核 |
| 图床管理 | `Images.vue` | 图床资源 |
| 应用展示 | `ShowcaseApps.vue` | 展示应用管理 |
| 数据开放 | `DataOpen.vue` | 数据开放申请 |
| OAuth 客户端 | `OauthClients.vue` | OAuth 应用管理 |
| QQ 群管理 | `BotGroups.vue` | 机器人群管理 |
| 校友分组 | `AlumniGroups.vue` | 往届成员分组 |
| 往届管理 | `AlumniManagers.vue` | 往届管理人员 |
| AI 活动自动化 | `AiActivityAutomation.vue` | AI 活动管理 |
| 日志查看 | `Logs.vue` | 操作/登录日志 |
| 二维码中心 | `QrCenter.vue` | 二维码生成 |
| 登录页 | `Login.vue` | 管理后台登录 |

## 前台展示页面（site/）

| 页面 | 文件 | 说明 |
|------|------|------|
| 首页 | `Home.vue` | 社团首页展示 |
| 社团展示 | `Clubs.vue` | 社团列表 |
| 招新 | `Recruitment.vue` | 招新信息展示 |
| 报名表单 | `ApplicationForm.vue` | 入会申请填写 |
| 申请详情 | `AppDetail.vue` | 申请查看 |
| 申请进度 | `ApplicationProgress.vue` | 审批进度 |
| 活动 | `Activities.vue` | 活动列表 |
| 活动详情 | `ActivityDetail.vue` | 活动详情 |
| 博客 | `Blog.vue` | 博客列表 |
| 博客详情 | `BlogDetail.vue` | 博客文章 |
| 我的博客 | `MyBlog.vue` | 个人博客管理 |
| 个人中心 | `Profile.vue` | 个人信息 |
| 通知 | `Notifications.vue` | 通知列表 |
| 请假 | `Leaves.vue` | 请假申请 |
| 积分 | `Points.vue` | 积分商城 |
| 投票 | `Votes.vue` | 投票参与 |
| 抽奖 | `LotteryScreen.vue` | 抽奖大屏 |
| 签到 | `CheckInScan.vue` | 扫码签到 |
| 二维码 | `QrScreen.vue` | 二维码展示 |
| 校历 | `SchoolCalendar.vue` | 校历查看 |
| 规章制度 | `Regulations.vue` | 规章查看 |
| 晚自习 | `EveningStudy.vue` | 晚自习签到 |
| 图床 | `ImageHosting.vue` | 图床服务 |
| 开放平台 | `OpenPlatform.vue` | 数据开放 |
| 信息收集 | `SiteForm.vue` | 表单填写 |
| 校友管理 | `AlumniManagers.vue` | 往届展示 |

## 动画 UI 组件（components/ui/）

前台使用了一系列自定义动画组件，参考 Apple 风格设计：

| 组件 | 说明 |
|------|------|
| `AuroraBackground` | 极光背景动画 |
| `AppleCardCarousel` | Apple 风格卡片轮播 |
| `FlipCard` | 翻转卡片 |
| `Globe` | 3D 地球组件 |
| `Marquee` | 跑马灯滚动 |
| `SmoothCursor` | 平滑光标跟随 |
| `PatternBackground` | 图案背景 |
| `InteractiveGridPattern` | 交互网格 |
| `RadiantText` | 发光文字 |
| `LiquidLogo` | 液态 Logo 动画 |
| `Tetris` | 俄罗斯方块装饰 |
| `EncryptedText` | 加密文字效果 |

## 工具函数（utils/）

| 文件 | 说明 |
|------|------|
| `auth.ts` | Token 管理（存储、清除、获取） |
| `oidc.ts` | OIDC 认证 URL 构建 |
| `permission.ts` | 权限判断（`hasAnyPermission`、`hasAdminAccess`） |
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
