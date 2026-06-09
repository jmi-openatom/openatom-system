import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { hasAdminAccess, hasAnyPermission } from '@/utils/permission.ts'
import SiteLayout from '@/layouts/SiteLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { appendTokenQuery, getToken } from '@/utils/auth.ts'
import { buildOidcAuthorizeUrl } from '@/utils/oidc.ts'

const adminFallbackRoutes = [
  '/admin/dashboard',
  '/admin/oauth-clients',
  '/admin/users',
  '/admin/clubs',
  '/admin/positions',
  '/admin/recruitment-campaigns',
  '/admin/site-forms',
  '/admin/form-submissions',
  '/admin/lotteries',
  '/admin/votes',
  '/admin/points',
  '/admin/office-documents',
  '/admin/images',
  '/admin/showcase-apps',
  '/admin/data-open',
  '/admin/leaves',
  '/admin/school-calendar',
  '/admin/activities',
  '/admin/blogs',
  '/admin/check-ins',
  '/admin/bot-groups',
  '/admin/awards',
  '/admin/applications',
  '/admin/interviews',
  '/admin/memberships',
  '/admin/roles',
  '/admin/logs',
  '/admin/notifications',
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
    component: SiteLayout,
    children: [
      { path: '', name: 'site-home', component: () => import('../views/site/Home.vue') },
      {
        path: 'activities',
        name: 'site-activities',
        component: () => import('../views/site/Activities.vue'),
      },
      {
        path: 'activities/:id',
        name: 'site-activity-detail',
        component: () => import('../views/site/ActivityDetail.vue'),
      },
      {
        path: 'apply',
        name: 'site-apply',
        component: () => import('../views/site/Recruitment.vue'),
      },
      {
        path: 'apply/:id',
        name: 'site-apply-detail',
        component: () => import('../views/site/ApplicationForm.vue'),
      },
      {
        path: 'forms/:id',
        name: 'site-form-detail',
        component: () => import('../views/site/SiteForm.vue'),
      },
      {
        path: 'votes',
        name: 'site-votes',
        component: () => import('../views/site/Votes.vue'),
      },
      {
        path: 'votes/:id',
        name: 'site-vote-detail',
        component: () => import('../views/site/Votes.vue'),
      },
      {
        path: 'blog',
        name: 'site-blog',
        component: () => import('../views/site/Blog.vue'),
      },
      {
        path: 'images',
        name: 'site-images',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/ImageHosting.vue'),
      },
      {
        path: 'open-platform',
        name: 'site-open-platform',
        component: () => import('../views/site/OpenPlatform.vue'),
      },
      {
        path: 'apps',
        name: 'site-apps',
        component: () => import('../views/site/Apps.vue'),
      },
      {
        path: 'apps/:id',
        name: 'site-app-detail',
        component: () => import('../views/site/AppDetail.vue'),
      },
      {
        path: 'points',
        name: 'site-points',
        component: () => import('../views/site/Points.vue'),
      },
      {
        path: 'blog/my',
        name: 'site-blog-my',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/MyBlog.vue'),
      },
      {
        path: 'blog/write',
        name: 'site-blog-write',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/MyBlog.vue'),
      },
      {
        path: 'blog/:id',
        name: 'site-blog-detail',
        component: () => import('../views/site/BlogDetail.vue'),
      },
      {
        path: 'progress',
        name: 'site-progress',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/ApplicationProgress.vue'),
      },
      {
        path: 'profile',
        name: 'site-profile',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/Profile.vue'),
      },
      {
        path: 'notifications',
        name: 'site-notifications',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/Notifications.vue'),
      },
      {
        path: 'leaves',
        name: 'site-leaves',
        meta: { requiresSiteLogin: true },
        component: () => import('../views/site/Leaves.vue'),
      },
      {
        path: 'calendar',
        name: 'site-calendar',
        component: () => import('../views/site/SchoolCalendar.vue'),
      },
    ],
  },
  {
    path: '/check-in/scan',
    name: 'site-check-in-scan',
    component: () => import('../views/site/CheckInScan.vue'),
  },
  {
    path: '/lottery/:id/screen',
    name: 'lottery-screen',
    component: () => import('../views/site/LotteryScreen.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/admin/Login.vue'),
  },
  {
    path: '/admin/login',
    redirect: (to) => ({ path: '/login', query: to.query }),
  },
  {
    path: '/auth/callback',
    name: 'auth-callback',
    component: () => import('../views/AuthCallback.vue'),
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
	      {
	        path: 'dashboard',
	        name: 'admin-dashboard',
	        meta: { permissions: [] },
	        component: () => import('../views/admin/Dashboard.vue'),
	      },
	      {
	        path: 'oauth-clients',
	        name: 'admin-oauth-clients',
	        meta: { permissions: ['oauth-client:list'] },
	        component: () => import('../views/admin/OauthClients.vue'),
	      },
      {
        path: 'users',
        name: 'admin-users',
        meta: { permissions: ['user:list'] },
        component: () => import('../views/admin/Users.vue'),
      },
      {
        path: 'clubs',
        name: 'admin-clubs',
        meta: { permissions: ['club:list'] },
        component: () => import('../views/admin/Clubs.vue'),
      },
      {
        path: 'positions',
        name: 'admin-positions',
        meta: { permissions: ['position:list'] },
        component: () => import('../views/admin/Positions.vue'),
      },
      {
        path: 'recruitment-campaigns',
        name: 'admin-recruitment-campaigns',
        meta: { permissions: ['recruitment-campaign:list'] },
        component: () => import('../views/admin/RecruitmentCampaigns.vue'),
      },
      {
        path: 'site-forms',
        name: 'admin-site-forms',
        meta: { permissions: ['site-form:list'] },
        component: () => import('../views/admin/SiteForms.vue'),
      },
      {
        path: 'form-submissions',
        name: 'admin-form-submissions',
        meta: { permissions: ['site-form:detail'] },
        component: () => import('../views/admin/FormSubmissions.vue'),
      },
      {
        path: 'lotteries',
        name: 'admin-lotteries',
        meta: { permissions: ['lottery:list'] },
        component: () => import('../views/admin/Lotteries.vue'),
      },
      {
        path: 'votes',
        name: 'admin-votes',
        meta: { permissions: ['vote:list'] },
        component: () => import('../views/admin/Votes.vue'),
      },
      {
        path: 'points',
        name: 'admin-points',
        meta: { permissions: ['point:account:list', 'point:item:list', 'point:redemption:list'] },
        component: () => import('../views/admin/Points.vue'),
      },
      {
        path: 'office-documents',
        name: 'admin-office-documents',
        meta: { permissions: ['document:list'] },
        component: () => import('../views/admin/OfficeDocuments.vue'),
      },
      {
        path: 'images',
        name: 'admin-images',
        meta: { permissions: ['image:list'] },
        component: () => import('../views/admin/Images.vue'),
      },
      {
        path: 'showcase-apps',
        name: 'admin-showcase-apps',
        meta: { permissions: ['showcase-app:list'] },
        component: () => import('../views/admin/ShowcaseApps.vue'),
      },
      {
        path: 'data-open',
        name: 'admin-data-open',
        meta: { permissions: ['data-open:list'] },
        component: () => import('../views/admin/DataOpen.vue'),
      },
      {
        path: 'leaves',
        name: 'admin-leaves',
        meta: { permissions: ['leave-application:list'] },
        component: () => import('../views/admin/Leaves.vue'),
      },
      {
        path: 'school-calendar',
        name: 'admin-school-calendar',
        meta: { permissions: ['school-calendar:manage'] },
        component: () => import('../views/admin/SchoolCalendar.vue'),
      },
      {
        path: 'activities',
        name: 'admin-activities',
        meta: { permissions: ['activity:list'] },
        component: () => import('../views/admin/Activities.vue'),
      },
      {
        path: 'blogs',
        name: 'admin-blogs',
        meta: { permissions: ['blog:list'] },
        component: () => import('../views/admin/Blogs.vue'),
      },
      {
        path: 'check-ins',
        name: 'admin-check-ins',
        meta: { permissions: ['check-in:list'] },
        component: () => import('../views/admin/CheckIns.vue'),
      },
      {
        path: 'bot-groups',
        name: 'admin-bot-groups',
        meta: { permissions: ['bot-management:list'] },
        component: () => import('../views/admin/BotGroups.vue'),
      },
      {
        path: 'awards',
        name: 'admin-awards',
        meta: { permissions: ['award:list'] },
        component: () => import('../views/admin/Awards.vue'),
      },
      {
        path: 'applications',
        name: 'admin-applications',
        meta: { permissions: ['application:list'] },
        component: () => import('../views/admin/Applications.vue'),
      },
      {
        path: 'interviews',
        name: 'admin-interviews',
        meta: { permissions: ['interview:list'] },
        component: () => import('../views/admin/Interviews.vue'),
      },
      {
        path: 'memberships',
        name: 'admin-memberships',
        meta: { permissions: ['membership:list'] },
        component: () => import('../views/admin/Memberships.vue'),
      },
      {
        path: 'roles',
        name: 'admin-roles',
        meta: { permissions: ['role:list', 'permission:list'] },
        component: () => import('../views/admin/Roles.vue'),
      },
      {
        path: 'logs',
        name: 'admin-logs',
        meta: { permissions: ['log:operation:list', 'log:login:list'] },
        component: () => import('../views/admin/Logs.vue'),
      },
      {
        path: 'notifications',
        name: 'admin-notifications',
        meta: { permissions: ['notification:list'] },
        component: () => import('../views/admin/Notifications.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if ((requiresAdminAuth(to) || requiresSiteLogin(to)) && !getToken()) {
    window.location.assign(buildOidcAuthorizeUrl(to.fullPath))
    return false
  }

  // 管理后台路径权限检查
  if (requiresAdminAuth(to)) {
    if (!hasAdminAccess()) {
      return '/progress'
    }
    if (!canAccessAdminPath(to)) {
      return firstAccessibleAdminPath()
    }
  }

	  // 已登录用户访问登录页的处理
  if (to.path === '/login' && getToken()) {
	    const redirect = Array.isArray(to.query.redirect) ? to.query.redirect[0] : to.query.redirect
	    if (redirect && (redirect.startsWith('/api/') || redirect.startsWith('/oauth/'))) {
	      window.location.assign(appendTokenQuery(redirect))
	      return false
	    }
	    return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
	  }

  return true
})

export default router
