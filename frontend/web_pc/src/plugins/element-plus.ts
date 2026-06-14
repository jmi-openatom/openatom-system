import type { App } from 'vue'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { ElLoading } from 'element-plus/es/components/loading/index'

export function installElementPlus(app: App) {
  app.use(ElLoading)
}
