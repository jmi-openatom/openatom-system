import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router'
import { initializeAppStatus } from '@/appStatus'
import '@/styles/global.css'

initializeAppStatus()
const app = createApp(App)
app.use(router)
router
  .isReady()
  .catch((error) => console.error('[router bootstrap]', error))
  .finally(() => app.mount('#app'))
