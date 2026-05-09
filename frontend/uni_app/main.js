import App from './App'

import './style/global.css'

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
// #ifdef H5
import 'vant/lib/index.css'
// #endif

export function createApp() {
  const app = createSSRApp(App)
  return {
    app
  }
}
// #endif