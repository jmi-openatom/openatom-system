import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './styles/global.css'
import App from '@/App.vue'
import router from '@/router'
import { initTheme } from '@/composables/useTheme'

initTheme()
const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as Parameters<typeof app.component>[1])
}

app.use(ElementPlus)
app.use(router)
app.mount('#app')
