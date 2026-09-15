<template>
  <main class="dev-login-page">
    <section class="content-card">
      <p class="eyebrow">LOCAL QA ONLY</p>
      <h1>本地验收入口</h1>
      <p>此页面只在 Vite 开发模式中注册，生产构建无法访问。</p>
      <div><el-button v-for="role in roles" :key="role.value" :loading="loading === role.value" @click="login(role.value)">{{ role.label }}</el-button></div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { http, getErrorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
const router = useRouter(), auth = useAuthStore(), loading = ref('')
const roles = [{label:'以成员身份进入',value:'MEMBER'},{label:'以导师身份进入',value:'MENTOR'},{label:'以项目负责人身份进入',value:'PROJECT_OWNER'},{label:'以管理员身份进入',value:'ADMIN'}]
async function login(role:string){loading.value=role;try{await http.post('/dev/login',undefined,{params:{role}});await auth.resolve(true);await router.push(role==='ADMIN'?'/admin':role==='PROJECT_OWNER'?'/task-management':'/dashboard')}catch(reason){ElMessage.error(getErrorMessage(reason,'本地登录失败'))}finally{loading.value=''}}
</script>

<style scoped>
.dev-login-page{min-height:100vh;display:grid;place-items:center;padding:24px;background:var(--color-bg-page)}
.content-card{width:min(520px,100%);min-height:300px;display:grid;align-content:center}
h1{margin:0;font-size:36px;letter-spacing:-.04em}
p:not(.eyebrow){color:var(--color-text-secondary);line-height:1.7}
.content-card>div{margin-top:20px;display:flex;flex-wrap:wrap;gap:12px}
</style>
