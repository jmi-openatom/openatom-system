import '@fontsource/space-grotesk/400.css'
import '@fontsource/space-grotesk/600.css'
import 'element-plus/es/components/base/style/css'
import 'element-plus/es/components/alert/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/checkbox/style/css'
import 'element-plus/es/components/divider/style/css'
import 'element-plus/es/components/dropdown/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/input-number/style/css'
import 'element-plus/es/components/option/style/css'
import 'element-plus/es/components/progress/style/css'
import 'element-plus/es/components/radio/style/css'
import 'element-plus/es/components/result/style/css'
import 'element-plus/es/components/segmented/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/skeleton/style/css'
import 'element-plus/es/components/steps/style/css'
import 'element-plus/es/components/switch/style/css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/styles/tokens.css'
import '@/styles/global.css'

import {
  ElAlert,
  ElButton,
  ElCheckbox,
  ElDivider,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElOption,
  ElProgress,
  ElRadioButton,
  ElRadioGroup,
  ElResult,
  ElSegmented,
  ElSelect,
  ElSkeleton,
  ElStep,
  ElSteps,
  ElSwitch,
} from 'element-plus'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores/pinia'

const app = createApp(App)
const components = [
  ElAlert, ElButton, ElCheckbox, ElDivider, ElDropdown, ElDropdownItem, ElDropdownMenu,
  ElForm, ElFormItem, ElInput, ElInputNumber, ElOption, ElProgress, ElRadioButton,
  ElRadioGroup, ElResult, ElSegmented, ElSelect, ElSkeleton, ElStep, ElSteps, ElSwitch,
]
components.forEach((component) => app.component(component.name!, component))
app.use(pinia)
app.use(router)
app.mount('#app')
