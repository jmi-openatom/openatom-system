import App from './App'

import './style/global.css'
import 'tmui-uni/css/tmui.css'
import 'tmui-uni/css/remixicon.css'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
App.mpType = 'app'
const app = new Vue({
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import tmui from 'tmui-uni'
// #ifdef H5
import 'vant/lib/index.css'
// #endif

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.use(tmui)
  return {
    app
  }
}
// #endif
