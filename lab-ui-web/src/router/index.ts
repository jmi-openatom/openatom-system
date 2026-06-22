import { createRouter, createWebHistory } from 'vue-router'
import { failNavigation, finishNavigation, startNavigation } from '@/appStatus'
import { isAdmin, session } from '@/store/session'

function isChunkLoadError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error || '')
  return /fetch dynamically imported module|loading chunk|importing a module script|networkerror/i.test(
    message,
  )
}

function resilientView(loader: () => Promise<any>) {
  return async () => {
    let lastError: unknown
    for (let attempt = 0; attempt < 3; attempt += 1) {
      try {
        return await loader()
      } catch (error) {
        lastError = error
        if (!isChunkLoadError(error) || attempt === 2) break
        await new Promise((resolve) => window.setTimeout(resolve, 450 * 2 ** attempt))
      }
    }
    throw lastError
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: resilientView(() => import('@/views/LoginView.vue')) },
    {
      path: '/auth/callback',
      name: 'auth-callback',
      component: resilientView(() => import('@/views/AuthCallback.vue')),
    },
    {
      path: '/',
      name: 'dashboard',
      component: resilientView(() => import('@/views/index/DashboardView.vue')),
    },
    {
      path: '/problem',
      name: 'problem',
      component: resilientView(() => import('@/views/index/problem/ProblemView.vue')),
    },
    {
      path: '/checkin',
      name: 'checkin',
      component: resilientView(() => import('@/views/index/checkin/CheckinView.vue')),
    },
    {
      path: '/admin',
      name: 'admin',
      component: resilientView(() => import('@/views/admin/AdminView.vue')),
    },
  ],
})

router.beforeEach((to, from) => {
  if (to.fullPath !== from.fullPath) startNavigation(to.fullPath)
  return true
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

router.afterEach((to) => {
  sessionStorage.removeItem(`lab_route_recovery:${to.fullPath}`)
  finishNavigation()
})

router.onError((error, to) => {
  failNavigation()
  if (!isChunkLoadError(error) || !navigator.onLine) return
  const key = `lab_route_recovery:${to.fullPath}`
  if (sessionStorage.getItem(key)) return
  sessionStorage.setItem(key, '1')
  window.setTimeout(() => window.location.assign(to.fullPath), 700)
})

export default router
