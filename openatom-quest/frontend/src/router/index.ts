import { createRouter, createWebHistory } from 'vue-router'
import QuestLayout from '@/layouts/QuestLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores/pinia'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: QuestLayout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'profile/setup', name: 'profile-setup', component: () => import('@/views/ProfileSetupView.vue') },
        { path: 'onboarding', name: 'onboarding', component: () => import('@/views/OnboardingView.vue') },
        { path: 'routes', name: 'routes', component: () => import('@/views/GrowthRoutesView.vue') },
        { path: 'routes/:id', name: 'route-detail', component: () => import('@/views/GrowthRouteDetailView.vue') },
        { path: 'tasks', name: 'tasks', component: () => import('@/views/TaskCenterView.vue') },
        { path: 'tasks/:id', name: 'task-detail', component: () => import('@/views/TaskDetailView.vue') },
        { path: 'assignments', name: 'assignments', component: () => import('@/views/MyAssignmentsView.vue') },
        { path: 'assignments/:id', name: 'assignment-detail', component: () => import('@/views/AssignmentDetailView.vue') },
        { path: 'notifications', name: 'notifications', component: () => import('@/views/NotificationsView.vue') },
        { path: 'points', name: 'points', component: () => import('@/views/PointsView.vue') },
        { path: 'reviews', name: 'reviews', component: () => import('@/views/ReviewQueueView.vue'), meta: { permission: 'submission:review' } },
        { path: 'task-management', name: 'task-management', component: () => import('@/views/TaskManagementView.vue'), meta: { permission: 'task:manage' } },
        { path: 'admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { permission: 'stats:global' } },
      ],
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

if (import.meta.env.DEV) {
  router.addRoute({ path: '/dev-login', name: 'dev-login', component: () => import('@/views/DevLoginView.vue'), meta: { public: true } })
}

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia)
  const member = await auth.resolve()
  if (to.meta.public) {
    if (to.name === 'login' && member) {
      return member.profileCompleted ? (member.onboardingCompleted ? '/dashboard' : '/onboarding') : '/profile/setup'
    }
    return true
  }
  if (!member) return { name: 'login', query: { returnTo: to.fullPath } }
  if (!member.profileCompleted && to.name !== 'profile-setup') return { name: 'profile-setup' }
  if (member.profileCompleted && !member.onboardingCompleted && to.name !== 'onboarding') return { name: 'onboarding' }
  if (to.meta.permission && !member.permissions.includes(String(to.meta.permission))) return { name: 'dashboard' }
  return true
})

export default router
