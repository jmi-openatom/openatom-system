# 路由与权限

## 路由架构

前端使用 Vue Router 4，采用 `createWebHistory` 模式，路由按前台展示、根级页面和管理后台三大区域组织。所有页面组件使用动态导入（懒加载）并带重试机制。

## 路由守卫

路由守卫实现了多层次的访问控制：

```typescript
// router/index.ts
router.beforeEach(async (to) => {
    startNavigation(to.fullPath)

    // 1. 登录检查：管理后台与需登录的站点页面
    if ((requiresAdminAuth(to) || requiresSiteLogin(to)) && !getToken()) {
        window.location.assign(await buildOidcAuthorizeUrl(to.fullPath))
        return false
    }

    // 2. 未激活账号拦截（激活页引导流程启用时）
    if (!ACTIVATION_BYPASS_PATHS.has(to.path) && getToken() && !isActivated()) {
        return { path: '/activation', query: { redirect: to.fullPath } }
    }

    // 3. 管理后台权限检查
    if (requiresAdminAuth(to)) {
        await refreshAuthSnapshot()
        if (!hasAdminAccess()) return '/progress'
        if (!canAccessAdminPath(to)) return firstAccessibleAdminPath()
    }

    // 4. 登录页处理：已登录用户重定向到工作台/后台
    if (to.path === '/login' && getToken()) {
        return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
    }
})
```

- **登录检查**：`requiresSiteLogin` 由路由 `meta.requiresSiteLogin` 标记；`requiresAdminAuth` 匹配 `/admin` 前缀（`/admin/login` 除外）。未登录统一重定向到 OIDC 授权页。
- **未激活拦截**：除激活页/登录页/auth 回调外，未激活用户强制跳转 `/activation`。
- **权限检查**：按 `meta.permissions` 判断，无权限回落到第一个可访问的后台路径。
- **OIDC 流程**：登录态统一由 `/auth/callback` 接收授权码并换取 Token，失效 Token 通过 `authApi.me()` 校验后清除会话，避免重定向循环。

## 前台路由（SiteLayout 子路由）

| 路径 | 页面 | 说明 | 需登录 |
|------|------|------|--------|
| `/` | `Home.vue` | 首页 | |
| `/about` | `About.vue` | 关于我们（根级路由） | |
| `/next` | `NextPage.vue` | 加入开放原子（根级路由） | |
| `/activities` | `Activities.vue` | 活动列表 | |
| `/activities/:id` | `ActivityDetail.vue` | 活动详情 | |
| `/apply` | `Recruitment.vue` | 招新信息 | |
| `/apply/:id` | `ApplicationForm.vue` | 报名表单填写 | |
| `/forms/:id` | `SiteForm.vue` | 在线表单填写 | |
| `/blog` | `Blog.vue` | 博客列表 | |
| `/blog/:id` | `BlogDetail.vue` | 博客详情 | |
| `/blog/my` | `MyBlog.vue` | 我的博客 | ✓ |
| `/blog/write` | `MyBlog.vue` | 写博客 | ✓ |
| `/regulations` | `Regulations.vue` | 规章制度列表 | |
| `/regulations/:id` | `Regulations.vue` | 制度详情 | |
| `/apps` | `Apps.vue` | 应用展示 | |
| `/apps/:id` | `AppDetail.vue` | 应用详情 | |
| `/partners` | `Partners.vue` | 合作社团 | |
| `/open-platform` | `OpenPlatform.vue` | 数据开放平台 | |
| `/votes` | `Votes.vue` | 社区投票 | |
| `/votes/:id` | `Votes.vue` | 投票详情 | |
| `/points` | `Points.vue` | 社区积分 | |
| `/calendar` | `SchoolCalendar.vue` | 校历查看 | |
| `/alumni-managers` | `AlumniManagers.vue` | 往届/校友展示 | |
| `/images` | `ImageHosting.vue` | 图床服务 | ✓ |
| `/workspace` | `Workspace.vue` | 个人工作台 | ✓ |
| `/progress` | `ApplicationProgress.vue` | 我的申请进度 | ✓ |
| `/members` | `Members.vue` | 社团成员 | ✓ |
| `/members/:slug` | `MemberProfile.vue` | 成员主页 | ✓ |
| `/profile` | `MemberProfile.vue` | 我的主页 | ✓ |
| `/profile/edit` | `ProfileEditor.vue` | 编辑主页 | ✓ |
| `/settings/account` | `Profile.vue` | 账号与安全 | ✓ |
| `/notifications` | `Notifications.vue` | 通知中心 | ✓ |
| `/leaves` | `Leaves.vue` | 我的请假 | ✓ |
| `/evening-study` | `EveningStudy.vue` | 晚自习签到 | ✓ |

## 根级路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/check-in/scan` | `CheckInScan.vue` | 扫码签到 |
| `/qr-screen` | `QrScreen.vue` | 二维码大屏 |
| `/lottery/:id/screen` | `LotteryScreen.vue` | 抽奖大屏 |
| `/doc-edit/:id` | `DocEditorPage.vue` | 文档编辑（需登录） |
| `/shared-edit/:id` | `SharedEditPage.vue` | 共享文档编辑（需登录） |
| `/login` | `Login.vue` | 登录页 |
| `/activation` | `Activation.vue` | 账号激活 |
| `/auth/callback` | `AuthCallback.vue` | OIDC 认证回调 |
| `/auth/github/callback` | `GithubAuthCallback.vue` | GitHub 登录回调 |
| `/auth/gitee/callback` | `GiteeAuthCallback.vue` | Gitee 登录回调 |
| `/:pathMatch(.*)*` | `NotFound.vue` | 404 |

## 管理后台路由

| 路径 | 页面 | 说明 | 权限 |
|------|------|------|------|
| `/admin/dashboard` | `Dashboard.vue` | 仪表盘 | 登录 |
| `/admin/users` | `Users.vue` | 用户管理 | `user:list` |
| `/admin/clubs` | `Clubs.vue` | 社团管理 | `club:list` |
| `/admin/departments` | `Departments.vue` | 部门管理 | `department:list` |
| `/admin/positions` | `Positions.vue` | 岗位管理 | `position:list` |
| `/admin/groups` | `Groups.vue` | 统一分组中心 | `group:list` |
| `/admin/groups/create` | `GroupEditor.vue` | 新建分组 | `group:create` |
| `/admin/groups/:groupId` | `GroupEditor.vue` | 分组管理 | `group:detail` |
| `/admin/bot-groups` | `BotGroups.vue` | 机器人群管理 | `bot-management:list` |
| `/admin/alumni-groups` | `AlumniGroups.vue` | 校友分组 | `membership:list` |
| `/admin/memberships` | `Memberships.vue` | 成员管理 | `membership:list` |
| `/admin/alumni-managers` | `AlumniManagers.vue` | 往届管理 | `membership:list` |
| `/admin/activation-settings` | `ActivationSettings.vue` | 激活设置 | `club:update` 等 |
| `/admin/recruitment-campaigns` | `RecruitmentCampaigns.vue` | 招新计划 | `recruitment-campaign:list` |
| `/admin/applications` | `Applications.vue` | 入会申请 | `application:list` |
| `/admin/interviews` | `Interviews.vue` | 面试管理 | `interview:list` |
| `/admin/activities` | `Activities.vue` | 活动管理 | `activity:list` |
| `/admin/ai-activities` | `AiActivityAutomation.vue` | AI 活动自动化 | `activity:create` |
| `/admin/check-ins` | `CheckIns.vue` | 签到管理 | `check-in:list` |
| `/admin/leaves` | `Leaves.vue` | 请假管理 | `leave-application:list` |
| `/admin/awards` | `Awards.vue` | 获奖管理 | `award:list` |
| `/admin/notifications` | `Notifications.vue` | 通知管理 | `notification:list` |
| `/admin/office-documents` | `OfficeDocuments.vue` | 文书管理 | `document:list` |
| `/admin/doc-center` | `DocCenter.vue` | 文档中心 | `document:list` |
| `/admin/school-calendar` | `SchoolCalendar.vue` | 校历管理 | `school-calendar:manage` |
| `/admin/regulations` | `Regulations.vue` | 规章管理 | `regulation:list` |
| `/admin/roles` | `Roles.vue` | 角色管理 | `role:list` 等 |
| `/admin/site-forms` | `SiteForms.vue` | 表单管理 | `site-form:list` |
| `/admin/form-submissions` | `FormSubmissions.vue` | 表单提交 | `site-form:detail` |
| `/admin/lotteries` | `Lotteries.vue` | 抽奖管理 | `lottery:list` |
| `/admin/votes` | `Votes.vue` | 投票管理 | `vote:list` |
| `/admin/points` | `Points.vue` | 积分管理 | `point:*:list` |
| `/admin/blogs` | `Blogs.vue` | 博客管理 | `blog:list` |
| `/admin/member-profile-comments` | `MemberProfileComments.vue` | 主页评论管理 | `member-profile-comment:list` |
| `/admin/images` | `Images.vue` | 图床管理 | `image:list` |
| `/admin/showcase-apps` | `ShowcaseApps.vue` | 应用展示 | `showcase-app:list` |
| `/admin/partner-clubs` | `PartnerClubs.vue` | 合作社团 | `partner-club:list` |
| `/admin/data-open` | `DataOpen.vue` | 数据开放 | `data-open:list` |
| `/admin/oauth-clients` | `OauthClients.vue` | OAuth 管理 | `oauth-client:list` |
| `/admin/qr-center` | `QrCenter.vue` | 二维码中心 | 登录 |
| `/admin/file-migration` | `FileMigration.vue` | 文件迁移 | `file:migration:*` |
| `/admin/logs` | `Logs.vue` | 日志查看 | `log:operation:list` 等 |

## 路由懒加载与重试

所有页面组件使用动态导入，并实现重试机制：

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
```

当动态导入失败（网络问题或部署路径变化）时，自动等待网络恢复并重试 2 次；恢复失败后 `router.onError` 会通过 `sessionStorage` 记录并在 700ms 后整页刷新恢复。

## 权限工具函数

| 函数 | 说明 |
|------|------|
| `hasAdminAccess()` | 是否可访问管理后台（拥有任意后台权限或超管角色） |
| `hasAnyPermission(permissions)` | 拥有指定权限列表中的任意一个 |
| `getToken()` / `setSession()` / `clearSession()` | 会话 Token 读写与清理 |
| `isActivated()` | 账号是否已完成激活 |
| `shouldUseFullPageAuthRedirect(redirect)` | 是否使用整页重定向完成 OIDC 流程 |
| `buildOidcAuthorizeUrl(target)` | 构建 OIDC 授权 URL |
| `appendTokenQuery(url)` | 追加 Token 查询参数 |