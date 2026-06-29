# 路由与权限

## 路由架构

前端使用 Vue Router 4，采用 `createWebHistory` 模式，路由按前台展示和管理后台两大区域组织。

## 路由守卫

路由守卫实现了多层次的访问控制：

```typescript
// router/index.ts
router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized) => {
    startNavigation()

    // 1. 检查是否需要登录
    if (to.meta.requiresAuth !== false) {
        const token = getToken()
        if (!token) {
            // 未登录 → OIDC 重定向或跳转登录页
            if (shouldUseFullPageAuthRedirect()) {
                window.location.href = buildOidcAuthorizeUrl(to.fullPath)
                return false
            }
            return appendTokenQuery({ path: '/login', query: { redirect: to.fullPath } })
        }
    }

    // 2. 检查管理后台权限
    if (to.path.startsWith('/admin')) {
        if (!hasAdminAccess()) {
            return { path: '/', query: { msg: 'no_permission' } }
        }
    }

    // 3. 检查页面级权限
    if (to.meta.permissions) {
        if (!hasAnyPermission(to.meta.permissions)) {
            return { path: '/admin/dashboard', query: { msg: 'no_permission' } }
        }
    }

    finishNavigation()
})
```

## 前台路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | `Home.vue` | 首页 |
| `/clubs` | `Clubs.vue` | 社团展示 |
| `/recruitment` | `Recruitment.vue` | 招新信息 |
| `/application/form` | `ApplicationForm.vue` | 报名表单 |
| `/application/:id` | `AppDetail.vue` | 申请详情 |
| `/application/progress/:id` | `ApplicationProgress.vue` | 审批进度 |
| `/activities` | `Activities.vue` | 活动列表 |
| `/activities/:id` | `ActivityDetail.vue` | 活动详情 |
| `/blog` | `Blog.vue` | 博客列表 |
| `/blog/:id` | `BlogDetail.vue` | 博客详情 |
| `/my-blog` | `MyBlog.vue` | 我的博客 |
| `/profile` | `Profile.vue` | 个人中心 |
| `/notifications` | `Notifications.vue` | 通知列表 |
| `/leaves` | `Leaves.vue` | 请假申请 |
| `/points` | `Points.vue` | 积分商城 |
| `/votes` | `Votes.vue` | 投票参与 |
| `/lottery/:id` | `LotteryScreen.vue` | 抽奖大屏 |
| `/check-in/scan` | `CheckInScan.vue` | 扫码签到 |
| `/qr/:id` | `QrScreen.vue` | 二维码展示 |
| `/school-calendar` | `SchoolCalendar.vue` | 校历查看 |
| `/regulations` | `Regulations.vue` | 规章制度 |
| `/evening-study` | `EveningStudy.vue` | 晚自习签到 |
| `/images` | `ImageHosting.vue` | 图床服务 |
| `/open-platform` | `OpenPlatform.vue` | 数据开放 |
| `/site-form/:id` | `SiteForm.vue` | 表单填写 |
| `/alumni` | `AlumniManagers.vue` | 校友展示 |

## 管理后台路由

| 路径 | 页面 | 说明 | 权限 |
|------|------|------|------|
| `/admin/dashboard` | `Dashboard.vue` | 仪表盘 | 登录 |
| `/admin/users` | `Users.vue` | 用户管理 | `user:list` |
| `/admin/clubs` | `Clubs.vue` | 社团管理 | `club:list` |
| `/admin/positions` | `Positions.vue` | 岗位管理 | `position:list` |
| `/admin/memberships` | `Memberships.vue` | 成员管理 | `membership:list` |
| `/admin/recruitment-campaigns` | `RecruitmentCampaigns.vue` | 招新计划 | `recruitment-campaign:list` |
| `/admin/applications` | `Applications.vue` | 入会申请 | `application:list` |
| `/admin/interviews` | `Interviews.vue` | 面试管理 | `interview:list` |
| `/admin/activities` | `Activities.vue` | 活动管理 | `activity:list` |
| `/admin/check-ins` | `CheckIns.vue` | 签到管理 | `check-in:list` |
| `/admin/leaves` | `Leaves.vue` | 请假管理 | `leave-application:list` |
| `/admin/awards` | `Awards.vue` | 获奖管理 | `award:list` |
| `/admin/notifications` | `Notifications.vue` | 通知管理 | `notification:list` |
| `/admin/office-documents` | `OfficeDocuments.vue` | 文书管理 | `document:list` |
| `/admin/school-calendar` | `SchoolCalendar.vue` | 校历管理 | `school-calendar:manage` |
| `/admin/regulations` | `Regulations.vue` | 规章管理 | `regulation:list` |
| `/admin/roles` | `Roles.vue` | 角色管理 | `role:list` |
| `/admin/site-forms` | `SiteForms.vue` | 表单管理 | `site-form:list` |
| `/admin/form-submissions` | `FormSubmissions.vue` | 表单提交 | `site-form:list` |
| `/admin/lotteries` | `Lotteries.vue` | 抽奖管理 | `lottery:list` |
| `/admin/votes` | `Votes.vue` | 投票管理 | `vote:list` |
| `/admin/points` | `Points.vue` | 积分管理 | `point:account:list` |
| `/admin/blogs` | `Blogs.vue` | 博客管理 | `blog:list` |
| `/admin/images` | `Images.vue` | 图床管理 | `image:list` |
| `/admin/showcase-apps` | `ShowcaseApps.vue` | 应用展示 | `showcase-app:list` |
| `/admin/data-open` | `DataOpen.vue` | 数据开放 | `data-open:list` |
| `/admin/oauth-clients` | `OauthClients.vue` | OAuth 管理 | `oauth-client:list` |
| `/admin/bot-groups` | `BotGroups.vue` | QQ 群管理 | `bot-management:list` |
| `/admin/alumni-groups` | `AlumniGroups.vue` | 校友分组 | - |
| `/admin/alumni-managers` | `AlumniManagers.vue` | 往届管理 | - |
| `/admin/ai-activity-automation` | `AiActivityAutomation.vue` | AI 活动 | - |
| `/admin/logs` | `Logs.vue` | 日志查看 | `log:operation:list` |
| `/admin/qr-center` | `QrCenter.vue` | 二维码 | 登录 |

## 路由懒加载

所有页面组件使用动态导入（懒加载），并实现了重试机制：

```typescript
function resilientView(loader: () => Promise<any>) {
    return async () => {
        let lastError: unknown
        for (let attempt = 0; attempt <= ROUTE_LOAD_RETRIES; attempt++) {
            try {
                return await loader()
            } catch (error) {
                lastError = error
                if (!isRecoverableRouteLoadError(error) || attempt === ROUTE_LOAD_RETRIES) break
                await waitForOnline()
                await sleep(450 * 2 ** attempt)
            }
        }
        throw lastError
    }
}

// 使用示例
{ path: '/admin/users', component: resilientView(() => import('@/views/admin/Users.vue')) }
```

当动态导入失败（网络问题或部署路径变化）时，自动重试 2 次，并等待网络恢复。

## 权限工具函数

### `hasAdminAccess()`

检查当前用户是否有管理后台访问权限（拥有任意 admin 权限或超管角色）。

### `hasAnyPermission(permissions)`

检查当前用户是否拥有指定权限列表中的任意一个。

### `getToken()`

获取当前存储的认证 Token。

### `clearSession()`

清除登录会话（Token、用户信息等）。

## OIDC 认证流程

```
用户访问受保护页面
        │
        ▼
  检查 Token ──── 无 Token ──── 重定向到 OIDC 授权页
        │                              │
     有 Token                          │
        │                              ▼
        ▼                    OAuth 授权 → 回调 AuthCallback.vue
  验证权限                                │
        │                                  │
        ▼                                  ▼
  允许访问 ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ 存储 Token
```

`AuthCallback.vue` 页面处理 OIDC 回调，解析授权码并交换 Token。
