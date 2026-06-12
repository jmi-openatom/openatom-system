import { createRouter, createWebHistory } from 'vue-router'
import { isAdmin, session } from '@/store/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/auth/callback', name: 'auth-callback', component: () => import('@/views/AuthCallback.vue') },
    { path: '/', name: 'dashboard', component: () => import('@/views/index/DashboardView.vue') },
    { path: '/problem', name: 'problem', component: () => import('@/views/index/problem/ProblemView.vue') },
    { path: '/checkin', name: 'checkin', component: () => import('@/views/index/checkin/CheckinView.vue') },
    { path: '/admin', name: 'admin', component: () => import('@/views/admin/AdminView.vue') },
  ],
})

router.beforeEach((to) => {
  if (to.path !== '/login' && to.path !== '/auth/callback' && !session.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path.startsWith('/admin') && !isAdmin()) {
    return '/'
  }
  return true
})

export default router
