import { createApp } from 'vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/global.css'
import App from '@/App.vue'
import router from '@/router'
import { initTheme } from '@/composables/useTheme'
import { installElementPlus } from '@/plugins/element-plus'

initTheme()
const app = createApp(App)

installElementPlus(app)
app.use(router)
app.mount('#app')
