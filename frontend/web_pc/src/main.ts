import { createApp } from 'vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@fontsource/orbitron/500.css'
import '@fontsource/orbitron/700.css'
import '@fontsource/orbitron/900.css'
import '@fontsource/oxanium/700.css'
import '@fontsource/space-grotesk/400.css'
import '@fontsource/space-grotesk/500.css'
import '@fontsource/space-grotesk/600.css'
import '@fontsource/space-grotesk/700.css'
import '@fontsource/noto-sans-sc/400.css'
import '@fontsource/noto-sans-sc/500.css'
import '@fontsource/noto-sans-sc/700.css'
import './styles/global.css'
import './styles/tokens.css'
import './styles/theme.css'
import './styles/reset.css'
import './styles/components.css'
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
