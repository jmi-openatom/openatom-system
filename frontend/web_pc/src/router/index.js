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
    '/admin/site-forms',
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

// 检查是否有权访问具体的管理路径
function canAccessAdminPath(to) {
    const permissions = to.meta?.permissions || []
    // 如果没有配置 permissions，默认允许（如 dashboard）
    if (permissions.length === 0) return true
    return hasAnyPermission(permissions)
}

// 判断是否是管理端路径
function requiresAdminAuth(to) {
    return to.path.startsWith('/admin') && to.path !== '/admin/login'
}

// 判断是否是前台需要登录的路径
function requiresSiteLogin(to) {
    return to.matched.some((record) => record.meta?.requiresSiteLogin)
}

// 自动寻找当前用户第一个有权限进入的后台页面
function firstAccessibleAdminPath() {
    const accessible = adminFallbackRoutes.find((path) => {
        const route = router.getRoutes().find((item) => item.path === path)
        return route ? canAccessAdminPath(route) : false
    })
    return accessible || '/admin/dashboard'
}

const routes = [
    {
        path: '/',
        component: SiteLayout,
        children: [
            { path: '', name: 'site-home', component: () => import('../views/site/Home.vue') },
            { path: 'activities', name: 'site-activities', component: () => import('../views/site/Activities.vue') },
            {
                path: 'activities/:id',
                name: 'site-activity-detail',
                component: () => import('../views/site/ActivityDetail.vue')
            },
            { path: 'apply', name: 'site-apply', component: () => import('../views/site/Recruitment.vue') },
            {
                path: 'apply/:id',
                name: 'site-apply-detail',
                component: () => import('../views/site/ApplicationForm.vue')
            },
            { path: 'forms/:id', name: 'site-form-detail', component: () => import('../views/site/SiteForm.vue') },
            {
                path: 'progress',
                name: 'site-progress',
                meta: { requiresSiteLogin: true },
                component: () => import('../views/site/ApplicationProgress.vue')
            },
            {
                path: 'profile',
                name: 'site-profile',
                meta: { requiresSiteLogin: true },
                component: () => import('../views/site/Profile.vue')
            }
        ]
    },
    { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/Login.vue') },
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
                component: () => import('../views/admin/Dashboard.vue')
            },
            {
                path: 'users',
                name: 'admin-users',
                meta: { permissions: ['user:list'] },
                component: () => import('../views/admin/Users.vue')
            },
            {
                path: 'clubs',
                name: 'admin-clubs',
                meta: { permissions: ['club:list'] },
                component: () => import('../views/admin/Clubs.vue')
            },
            {
                path: 'positions',
                name: 'admin-positions',
                meta: { permissions: ['position:list'] },
                component: () => import('../views/admin/Positions.vue')
            },
            {
                path: 'recruitment-campaigns',
                name: 'admin-recruitment-campaigns',
                meta: { permissions: ['recruitment-campaign:list'] },
                component: () => import('../views/admin/RecruitmentCampaigns.vue')
            },
            {
                path: 'site-forms',
                name: 'admin-site-forms',
                meta: { permissions: ['site-form:list'] },
                component: () => import('../views/admin/SiteForms.vue')
            },
            {
                path: 'form-submissions',
                name: 'admin-form-submissions',
                meta: { permissions: ['site-form:detail'] },
                component: () => import('../views/admin/FormSubmissions.vue')
            },
            {
                path: 'office-documents',
                name: 'admin-office-documents',
                meta: { permissions: ['document:list'] },
                component: () => import('../views/admin/OfficeDocuments.vue')
            },
            {
                path: 'activities',
                name: 'admin-activities',
                meta: { permissions: ['activity:list'] },
                component: () => import('../views/admin/Activities.vue')
            },
            {
                path: 'awards',
                name: 'admin-awards',
                meta: { permissions: ['award:list'] },
                component: () => import('../views/admin/Awards.vue')
            },
            {
                path: 'applications',
                name: 'admin-applications',
                meta: { permissions: ['application:list'] },
                component: () => import('../views/admin/Applications.vue')
            },
            {
                path: 'interviews',
                name: 'admin-interviews',
                meta: { permissions: ['interview:list'] },
                component: () => import('../views/admin/Interviews.vue')
            },
            {
                path: 'memberships',
                name: 'admin-memberships',
                meta: { permissions: ['membership:list'] },
                component: () => import('../views/admin/Memberships.vue')
            },
            {
                path: 'roles',
                name: 'admin-roles',
                meta: { permissions: ['role:list', 'permission:list'] },
                component: () => import('../views/admin/Roles.vue')
            },
            {
                path: 'logs',
                name: 'admin-logs',
                meta: { permissions: ['log:operation:list', 'log:login:list'] },
                component: () => import('../views/admin/Logs.vue')
            }
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

// 路由拦截器
router.beforeEach((to) => {
    const isLogin = !!getToken()

    // 1. 未登录处理：访问管理端或前台受限页面
    if ((requiresAdminAuth(to) || requiresSiteLogin(to)) && !isLogin) {
        return {
            path: '/admin/login',
            query: { redirect: to.fullPath } // 关键点：记录来源地址
        }
    }

    // 2. 已登录状态下访问登录页
    if (to.path === '/admin/login' && isLogin) {
        // 如果是管理员，去后台；如果是普通用户，去进度页
        return hasAdminAccess() ? firstAccessibleAdminPath() : '/progress'
    }

    // 3. 管理后台权限检查
    if (requiresAdminAuth(to) && isLogin) {
        // 没有后台访问特权
        if (!hasAdminAccess()) {
            return '/progress'
        }
        // 有特权但没有具体页面的权限
        if (!canAccessAdminPath(to)) {
            return firstAccessibleAdminPath()
        }
    }

    return true
})

export default router