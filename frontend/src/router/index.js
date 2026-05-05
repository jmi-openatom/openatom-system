import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'
import { hasAdminAccess, hasAnyPermission } from '../utils/permission'
import SiteLayout from '../layouts/SiteLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'

const adminFallbackRoutes = [
  '/admin/dashboard',
  '/admin/users',
  '/admin/clubs',
  '/admin/positions',
  '/admin/recruitment-campaigns',
  '/admin/form-submissions',
  '/admin/office-documents',
  '/admin/activities',
  '/admin/awards',
  '/admin/applications',
  '/admin/interviews',
  '/admin/memberships',
  '/admin/roles',
  '/admin/logs'
]

function canAccessAdminPath(to) {
  const permissions = to.meta?.permissions || []
  return hasAnyPermission(permissions)
}

function requiresAdminAuth(to) {
  return to.path.startsWith('/admin') && to.path !== '/admin/login'
}

function requiresSiteLogin(to) {
  return to.matched.some((record) => record.meta?.requiresSiteLogin)
}

function firstAccessibleAdminPath() {
  return (
    adminFallbackRoutes.find((path) => {
      const route = router.getRoutes().find((item) => item.path === path)
      return route ? canAccessAdminPath(route) : false
    }) || '/admin/dashboard'
  )
}

const routes = [
  {
    path: '/',
    component: SiteLayout,
    children: [
      { path: '', name: 'site-home', component: () => import('../views/site/Home.vue') },
      { path: 'activities', name: 'site-activities', component: () => import('../views/site/Activities.vue') },
      { path: 'activities/:id', name: 'site-activity-detail', component: () => import('../views/site/ActivityDetail.vue') },
      { path: 'forms', name: 'site-forms', component: () => import('../views/site/Recruitment.vue') },
      { path: 'forms/:id', name: 'site-form-detail', component: () => import('../views/site/ApplicationForm.vue') },
      { path: 'apply', redirect: '/forms' },
      { path: 'progress', name: 'site-progress', meta: { requiresSiteLogin: true }, component: () => import('../views/site/ApplicationProgress.vue') },
      { path: 'profile', name: 'site-profile', meta: { requiresSiteLogin: true }, component: () => import('../views/site/Profile.vue') }
    ]
  },
  { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/Login.vue') },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', meta: { permissions: [] }, component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', name: 'admin-users', meta: { permissions: ['user:list'] }, component: () => import('../views/admin/Users.vue') },
      { path: 'clubs', name: 'admin-clubs', meta: { permissions: ['club:list'] }, component: () => import('../views/admin/Clubs.vue') },
      { path: 'positions', name: 'admin-positions', meta: { permissions: ['position:list'] }, component: () => import('../views/admin/Positions.vue') },
      { path: 'forms', redirect: '/admin/recruitment-campaigns' },
      { path: 'recruitment-campaigns', name: 'admin-recruitment-campaigns', meta: { permissions: ['recruitment-campaign:list'] }, component: () => import('../views/admin/RecruitmentCampaigns.vue') },
      { path: 'form-submissions', name: 'admin-form-submissions', meta: { permissions: ['recruitment-campaign:detail'] }, component: () => import('../views/admin/FormSubmissions.vue') },
      { path: 'office-documents', name: 'admin-office-documents', meta: { permissions: ['document:list'] }, component: () => import('../views/admin/OfficeDocuments.vue') },
      { path: 'activities', name: 'admin-activities', meta: { permissions: ['activity:list'] }, component: () => import('../views/admin/Activities.vue') },
      { path: 'awards', name: 'admin-awards', meta: { permissions: ['award:list'] }, component: () => import('../views/admin/Awards.vue') },
      { path: 'applications', name: 'admin-applications', meta: { permissions: ['application:list'] }, component: () => import('../views/admin/Applications.vue') },
      { path: 'interviews', name: 'admin-interviews', meta: { permissions: ['interview:list'] }, component: () => import('../views/admin/Interviews.vue') },
      { path: 'memberships', name: 'admin-memberships', meta: { permissions: ['membership:list'] }, component: () => import('../views/admin/Memberships.vue') },
      { path: 'roles', name: 'admin-roles', meta: { permissions: ['role:list', 'permission:list'] }, component: () => import('../views/admin/Roles.vue') },
      { path: 'logs', name: 'admin-logs', meta: { permissions: ['log:operation:list', 'log:login:list'] }, component: () => import('../views/admin/Logs.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if ((requiresAdminAuth(to) || requiresSiteLogin(to)) && !getToken()) {
    return { path: '/admin/login', query: { redirect: to.fullPath } }
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
  if (to.path === '/admin/login' && getToken()) {
    return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
  }

  return true
})

export default router
