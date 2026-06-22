import { createApp } from 'vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/global.css'
import App from '@/App.vue'
import router from '@/router'
import { initializeAppStatus } from '@/composables/useAppStatus'
import { initTheme } from '@/composables/useTheme'
import { installElementPlus } from '@/plugins/element-plus'

initTheme()
initializeAppStatus()
const app = createApp(App)

installElementPlus(app)
app.use(router)

router
  .isReady()
  .catch((error) => {
    console.error('[router bootstrap]', error)
  })
  .finally(() => {
    app.mount('#app')
  })
