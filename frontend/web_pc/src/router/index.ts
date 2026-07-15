import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { authApi, siteApi } from '@/api'
import { failNavigation, finishNavigation, startNavigation } from '@/composables/useAppStatus'
import { hasAdminAccess, hasAnyPermission } from '@/utils/permission.ts'
import {
  appendTokenQuery,
  clearSession,
  getToken,
  isActivated,
  shouldUseFullPageAuthRedirect,
} from '@/utils/auth.ts'
import { buildOidcAuthorizeUrl } from '@/utils/oidc.ts'
import { updateRouteSeo } from '@/utils/seo.ts'

const ROUTE_LOAD_RETRIES = 2

function sleep(delay: number) {
  return new Promise((resolve) => window.setTimeout(resolve, delay))
}

function waitForOnline(maxWait = 6000) {
  if (navigator.onLine) return Promise.resolve()
  return new Promise<void>((resolve) => {
    const timeout = window.setTimeout(done, maxWait)
    window.addEventListener('online', done, { once: true })

    function done() {
      window.clearTimeout(timeout)
      window.removeEventListener('online', done)
      resolve()
    }
  })
}

function isRecoverableRouteLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error || '')
  return /fetch dynamically imported module|loading chunk|importing a module script|networkerror/i.test(
    message,
  )
}

function resilientView(loader: () => Promise<any>) {
  return async () => {
    let lastError: unknown
    for (let attempt = 0; attempt <= ROUTE_LOAD_RETRIES; attempt += 1) {
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

const adminFallbackRoutes = [
  '/admin/dashboard',
  '/admin/oauth-clients',
  '/admin/users',
  '/admin/clubs',
  '/admin/departments',
  '/admin/positions',
  '/admin/activation-settings',
  '/admin/recruitment-campaigns',
  '/admin/site-forms',
  '/admin/qr-center',
  '/admin/form-submissions',
  '/admin/lotteries',
  '/admin/votes',
  '/admin/points',
  '/admin/office-documents',
  '/admin/images',
  '/admin/showcase-apps',
  '/admin/partner-clubs',
  '/admin/data-open',
  '/admin/leaves',
  '/admin/school-calendar',
  '/admin/activities',
  '/admin/ai-activities',
  '/admin/blogs',
  '/admin/regulations',
  '/admin/check-ins',
  '/admin/bot-groups',
  '/admin/awards',
  '/admin/applications',
  '/admin/interviews',
  '/admin/memberships',
  '/admin/alumni-managers',
  '/admin/alumni-groups',
  '/admin/roles',
  '/admin/logs',
  '/admin/notifications',
  '/admin/file-migration',
]

function canAccessAdminPath(to: RouteLocationNormalized): boolean {
  const permissions: string[] = (to.meta?.permissions as string[]) || []
  return hasAnyPermission(permissions)
}

function requiresAdminAuth(to: RouteLocationNormalized): boolean {
  return to.path.startsWith('/admin') && to.path !== '/admin/login'
}

function requiresSiteLogin(to: RouteLocationNormalized): boolean {
  return to.matched.some((record) => record.meta?.requiresSiteLogin)
}

const ACTIVATION_BYPASS_PATHS = new Set([
  '/activation',
  '/login',
  '/admin/login',
  '/auth/callback',
  '/auth/github/callback',
  '/auth/gitee/callback',
])

function firstAccessibleAdminPath(): string {
  return (
    adminFallbackRoutes.find((path) => {
      const route = router.getRoutes().find((item) => item.path === path)
      return route ? canAccessAdminPath(route as unknown as RouteLocationNormalized) : false
    }) || '/admin/dashboard'
  )
}

const routes = [
  {
    path: '/',
    component: resilientView(() => import('@/layouts/SiteLayout.vue')),
    children: [
      {
        path: '',
        name: 'site-home',
        component: resilientView(() => import('../views/site/Home.vue')),
      },
      {
        path: 'activities',
        name: 'site-activities',
        component: resilientView(() => import('../views/site/Activities.vue')),
      },
      {
        path: 'activities/:id',
        name: 'site-activity-detail',
        component: resilientView(() => import('../views/site/ActivityDetail.vue')),
      },
      {
        path: 'apply',
        name: 'site-apply',
        component: resilientView(() => import('../views/site/Recruitment.vue')),
      },
      {
        path: 'apply/:id',
        name: 'site-apply-detail',
        component: resilientView(() => import('../views/site/ApplicationForm.vue')),
      },
      {
        path: 'forms/:id',
        name: 'site-form-detail',
        component: resilientView(() => import('../views/site/SiteForm.vue')),
      },
      {
        path: 'votes',
        name: 'site-votes',
        component: resilientView(() => import('../views/site/Votes.vue')),
      },
      {
        path: 'votes/:id',
        name: 'site-vote-detail',
        component: resilientView(() => import('../views/site/Votes.vue')),
      },
      {
        path: 'blog',
        name: 'site-blog',
        component: resilientView(() => import('../views/site/Blog.vue')),
      },
      {
        path: 'regulations',
        name: 'site-regulations',
        component: resilientView(() => import('../views/site/Regulations.vue')),
      },
      {
        path: 'regulations/:id',
        name: 'site-regulation-detail',
        component: resilientView(() => import('../views/site/Regulations.vue')),
      },
      {
        path: 'images',
        name: 'site-images',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/ImageHosting.vue')),
      },
      {
        path: 'open-platform',
        name: 'site-open-platform',
        component: resilientView(() => import('../views/site/OpenPlatform.vue')),
      },
      {
        path: 'apps',
        name: 'site-apps',
        component: resilientView(() => import('../views/site/Apps.vue')),
      },
      {
        path: 'partners',
        name: 'site-partners',
        component: resilientView(() => import('../views/site/Partners.vue')),
      },
      {
        path: 'apps/:id',
        name: 'site-app-detail',
        component: resilientView(() => import('../views/site/AppDetail.vue')),
      },
      {
        path: 'points',
        name: 'site-points',
        component: resilientView(() => import('../views/site/Points.vue')),
      },
      {
        path: 'blog/my',
        name: 'site-blog-my',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/MyBlog.vue')),
      },
      {
        path: 'blog/write',
        name: 'site-blog-write',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/MyBlog.vue')),
      },
      {
        path: 'blog/:id',
        name: 'site-blog-detail',
        component: resilientView(() => import('../views/site/BlogDetail.vue')),
      },
      {
        path: 'progress',
        name: 'site-progress',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/ApplicationProgress.vue')),
      },
      {
        path: 'members',
        name: 'site-members',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/Members.vue')),
      },
      {
        path: 'members/:slug',
        name: 'site-member-profile',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/MemberProfile.vue')),
      },
      {
        path: 'profile',
        name: 'site-profile',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/MemberProfile.vue')),
      },
      {
        path: 'profile/edit',
        name: 'site-profile-edit',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/ProfileEditor.vue')),
      },
      {
        path: 'settings/account',
        name: 'site-account-settings',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/Profile.vue')),
      },
      {
        path: 'notifications',
        name: 'site-notifications',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/Notifications.vue')),
      },
      {
        path: 'leaves',
        name: 'site-leaves',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/Leaves.vue')),
      },
      {
        path: 'evening-study',
        name: 'site-evening-study',
        meta: { requiresSiteLogin: true },
        component: resilientView(() => import('../views/site/EveningStudy.vue')),
      },
      {
        path: 'calendar',
        name: 'site-calendar',
        component: resilientView(() => import('../views/site/SchoolCalendar.vue')),
      },
      {
        path: 'alumni-managers',
        name: 'site-alumni-managers',
        component: resilientView(() => import('../views/site/AlumniManagers.vue')),
      },
    ],
  },
  {
    path: '/about',
    name: 'about',
    component: resilientView(() => import('../views/site/About.vue')),
  },
  {
    path: '/next',
    name: 'site-next',
    component: resilientView(() => import('../views/site/NextPage.vue')),
  },
  {
    path: '/check-in/scan',
    name: 'site-check-in-scan',
    component: resilientView(() => import('../views/site/CheckInScan.vue')),
  },
  {
    path: '/lottery/:id/screen',
    name: 'lottery-screen',
    component: resilientView(() => import('../views/site/LotteryScreen.vue')),
  },
  {
    path: '/qr-screen',
    name: 'qr-screen',
    component: resilientView(() => import('../views/site/QrScreen.vue')),
  },
  {
    path: '/login',
    name: 'login',
    component: resilientView(() => import('../views/admin/Login.vue')),
  },
  {
    path: '/activation',
    name: 'site-activation',
    component: resilientView(() => import('../views/site/Activation.vue')),
  },
  {
    path: '/admin/login',
    redirect: (to) => ({ path: '/login', query: to.query }),
  },
  {
    path: '/auth/callback',
    name: 'auth-callback',
    component: resilientView(() => import('../views/AuthCallback.vue')),
  },
  {
    path: '/auth/github/callback',
    name: 'github-auth-callback',
    component: resilientView(() => import('../views/GithubAuthCallback.vue')),
  },
  {
    path: '/auth/gitee/callback',
    name: 'gitee-auth-callback',
    component: resilientView(() => import('../views/GiteeAuthCallback.vue')),
  },
  {
    path: '/admin',
    component: resilientView(() => import('@/layouts/AdminLayout.vue')),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        meta: { permissions: [] },
        component: resilientView(() => import('../views/admin/Dashboard.vue')),
      },
      {
        path: 'oauth-clients',
        name: 'admin-oauth-clients',
        meta: { permissions: ['oauth-client:list'] },
        component: resilientView(() => import('../views/admin/OauthClients.vue')),
      },
      {
        path: 'users',
        name: 'admin-users',
        meta: { permissions: ['user:list'] },
        component: resilientView(() => import('../views/admin/Users.vue')),
      },
      {
        path: 'clubs',
        name: 'admin-clubs',
        meta: { permissions: ['club:list'] },
        component: resilientView(() => import('../views/admin/Clubs.vue')),
      },
      {
        path: 'positions',
        name: 'admin-positions',
        meta: { permissions: ['position:list'] },
        component: resilientView(() => import('../views/admin/Positions.vue')),
      },
      {
        path: 'departments',
        name: 'admin-departments',
        meta: { permissions: ['department:list'] },
        component: resilientView(() => import('../views/admin/Departments.vue')),
      },
      {
        path: 'activation-settings',
        name: 'admin-activation-settings',
        meta: { permissions: ['club:update', 'department:update'] },
        component: resilientView(() => import('../views/admin/ActivationSettings.vue')),
      },
      {
        path: 'recruitment-campaigns',
        name: 'admin-recruitment-campaigns',
        meta: { permissions: ['recruitment-campaign:list'] },
        component: resilientView(() => import('../views/admin/RecruitmentCampaigns.vue')),
      },
      {
        path: 'site-forms',
        name: 'admin-site-forms',
        meta: { permissions: ['site-form:list'] },
        component: resilientView(() => import('../views/admin/SiteForms.vue')),
      },
      {
        path: 'qr-center',
        name: 'admin-qr-center',
        meta: { permissions: [] },
        component: resilientView(() => import('../views/admin/QrCenter.vue')),
      },
      {
        path: 'form-submissions',
        name: 'admin-form-submissions',
        meta: { permissions: ['site-form:detail'] },
        component: resilientView(() => import('../views/admin/FormSubmissions.vue')),
      },
      {
        path: 'lotteries',
        name: 'admin-lotteries',
        meta: { permissions: ['lottery:list'] },
        component: resilientView(() => import('../views/admin/Lotteries.vue')),
      },
      {
        path: 'votes',
        name: 'admin-votes',
        meta: { permissions: ['vote:list'] },
        component: resilientView(() => import('../views/admin/Votes.vue')),
      },
      {
        path: 'points',
        name: 'admin-points',
        meta: { permissions: ['point:account:list', 'point:item:list', 'point:redemption:list'] },
        component: resilientView(() => import('../views/admin/Points.vue')),
      },
      {
        path: 'office-documents',
        name: 'admin-office-documents',
        meta: { permissions: ['document:list'] },
        component: resilientView(() => import('../views/admin/OfficeDocuments.vue')),
      },
      {
        path: 'images',
        name: 'admin-images',
        meta: { permissions: ['image:list'] },
        component: resilientView(() => import('../views/admin/Images.vue')),
      },
      {
        path: 'showcase-apps',
        name: 'admin-showcase-apps',
        meta: { permissions: ['showcase-app:list'] },
        component: resilientView(() => import('../views/admin/ShowcaseApps.vue')),
      },
      {
        path: 'partner-clubs',
        name: 'admin-partner-clubs',
        meta: { permissions: ['partner-club:list'] },
        component: resilientView(() => import('../views/admin/PartnerClubs.vue')),
      },
      {
        path: 'data-open',
        name: 'admin-data-open',
        meta: { permissions: ['data-open:list'] },
        component: resilientView(() => import('../views/admin/DataOpen.vue')),
      },
      {
        path: 'leaves',
        name: 'admin-leaves',
        meta: { permissions: ['leave-application:list'] },
        component: resilientView(() => import('../views/admin/Leaves.vue')),
      },
      {
        path: 'school-calendar',
        name: 'admin-school-calendar',
        meta: { permissions: ['school-calendar:manage'] },
        component: resilientView(() => import('../views/admin/SchoolCalendar.vue')),
      },
      {
        path: 'activities',
        name: 'admin-activities',
        meta: { permissions: ['activity:list'] },
        component: resilientView(() => import('../views/admin/Activities.vue')),
      },
      {
        path: 'ai-activities',
        name: 'admin-ai-activities',
        meta: { permissions: ['activity:create'] },
        component: resilientView(() => import('../views/admin/AiActivityAutomation.vue')),
      },
      {
        path: 'blogs',
        name: 'admin-blogs',
        meta: { permissions: ['blog:list'] },
        component: resilientView(() => import('../views/admin/Blogs.vue')),
      },
      {
        path: 'regulations',
        name: 'admin-regulations',
        meta: { permissions: ['regulation:list'] },
        component: resilientView(() => import('../views/admin/Regulations.vue')),
      },
      {
        path: 'check-ins',
        name: 'admin-check-ins',
        meta: { permissions: ['check-in:list'] },
        component: resilientView(() => import('../views/admin/CheckIns.vue')),
      },
      {
        path: 'bot-groups',
        name: 'admin-bot-groups',
        meta: { permissions: ['bot-management:list'] },
        component: resilientView(() => import('../views/admin/BotGroups.vue')),
      },
      {
        path: 'awards',
        name: 'admin-awards',
        meta: { permissions: ['award:list'] },
        component: resilientView(() => import('../views/admin/Awards.vue')),
      },
      {
        path: 'applications',
        name: 'admin-applications',
        meta: { permissions: ['application:list'] },
        component: resilientView(() => import('../views/admin/Applications.vue')),
      },
      {
        path: 'interviews',
        name: 'admin-interviews',
        meta: { permissions: ['interview:list'] },
        component: resilientView(() => import('../views/admin/Interviews.vue')),
      },
      {
        path: 'memberships',
        name: 'admin-memberships',
        meta: { permissions: ['membership:list'] },
        component: resilientView(() => import('../views/admin/Memberships.vue')),
      },
      {
        path: 'alumni-managers',
        name: 'admin-alumni-managers',
        meta: { permissions: ['membership:list'] },
        component: resilientView(() => import('../views/admin/AlumniManagers.vue')),
      },
      {
        path: 'alumni-groups',
        name: 'admin-alumni-groups',
        meta: { permissions: ['membership:list'] },
        component: resilientView(() => import('../views/admin/AlumniGroups.vue')),
      },
      {
        path: 'roles',
        name: 'admin-roles',
        meta: { permissions: ['role:list', 'permission:list'] },
        component: resilientView(() => import('../views/admin/Roles.vue')),
      },
      {
        path: 'logs',
        name: 'admin-logs',
        meta: { permissions: ['log:operation:list', 'log:login:list'] },
        component: resilientView(() => import('../views/admin/Logs.vue')),
      },
      {
        path: 'notifications',
        name: 'admin-notifications',
        meta: { permissions: ['notification:list'] },
        component: resilientView(() => import('../views/admin/Notifications.vue')),
      },
      {
        path: 'file-migration',
        name: 'admin-file-migration',
        meta: {
          permissions: ['file:migration:stats', 'file:migration:export', 'file:migration:import'],
        },
        component: resilientView(() => import('../views/admin/FileMigration.vue')),
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: resilientView(() => import('../views/NotFound.vue')),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to, from) => {
  if (to.fullPath !== from.fullPath) startNavigation(to.fullPath)
  return true
})

// 缓存激活页是否启用，避免每次路由跳转都请求后端
let _activationEnabledCache: { value: boolean; fetched: boolean } = { value: true, fetched: false }

async function isActivationPageEnabled(): Promise<boolean> {
  if (_activationEnabledCache.fetched) return _activationEnabledCache.value
  try {
    const enabled = Boolean(await siteApi.activationEnabled())
    _activationEnabledCache = { value: enabled, fetched: true }
    return enabled
  } catch {
    return true // 请求失败时默认启用激活页
  }
}

router.beforeEach(async (to) => {
  if ((requiresAdminAuth(to) || requiresSiteLogin(to)) && !getToken()) {
    window.location.assign(buildOidcAuthorizeUrl(to.fullPath))
    return false
  }

  // 未激活账号拦截：除激活页/登录页/auth 回调外，所有页面都强制跳转到激活页
  // 仅当激活页引导流程启用时才拦截
  if (!ACTIVATION_BYPASS_PATHS.has(to.path) && getToken() && !isActivated()) {
    const activationEnabled = await isActivationPageEnabled()
    if (activationEnabled) {
      return { path: '/activation', query: { redirect: to.fullPath } }
    }
  }
  // 已激活用户访问激活页：不在路由守卫层拦截，交给 Activation.vue 组件
  // 根据服务器返回的真实状态自行决定是否重定向，避免 localStorage 缓存过期导致无法进入页面

  // 管理后台路径权限检查
  if (requiresAdminAuth(to)) {
    if (!hasAdminAccess()) {
      return '/progress'
    }
    if (!canAccessAdminPath(to)) {
      return firstAccessibleAdminPath()
    }
  }

  // 登录页不能仅凭本地 token 判断登录状态。失效 token 若被继续带回 OAuth
  // authorize，会在认证中心与客户端登录页之间形成重定向循环。
  if (to.path === '/login' && getToken()) {
    if (!navigator.onLine) {
      return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
    }
    try {
      await authApi.me()
    } catch (error) {
      const status = Number(
        (error as { response?: { status?: number }; code?: number })?.response?.status,
      )
      const code = Number((error as { code?: number })?.code)
      if (status === 401 || code === 401 || code === 40100) {
        clearSession()
        return true
      }
      return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
    }

    const redirect = Array.isArray(to.query.redirect) ? to.query.redirect[0] : to.query.redirect
    if (shouldUseFullPageAuthRedirect(redirect)) {
      window.location.assign(appendTokenQuery(redirect))
      return false
    }
    return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
  }

  return true
})

router.afterEach((to) => {
  sessionStorage.removeItem(`openatom_route_recovery:${to.fullPath}`)
  updateRouteSeo(to)
  finishNavigation()
})

router.onError((error, to) => {
  failNavigation(error)
  if (!isRecoverableRouteLoadError(error)) return

  const target = to.fullPath
  const recoveryKey = `openatom_route_recovery:${target}`
  if (sessionStorage.getItem(recoveryKey) || !navigator.onLine) return
  sessionStorage.setItem(recoveryKey, '1')
  window.setTimeout(() => window.location.assign(target), 700)
})

export default router
