<template>
  <ViewPage class="login-page">
    <ThemeToggle class="login-theme-toggle" />
    <div class="login-panel">
      <div class="login-copy">
        <span class="login-logo">OA</span>
        <h1>OpenAtom 统一登录</h1>
        <p>登录后可访问官网、管理后台和已接入的开放应用。</p>
      </div>
      <el-card class="login-card" shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="账号登录" name="login">
            <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  placeholder="请输入密码"
                  show-password
                  type="password"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <div class="login-options">
                <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
              </div>
              <el-button :loading="loading" class="full-btn" type="primary" @click="handleLogin"
                >登录
              </el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane v-if="registerEnabled" label="注册账号" name="register">
            <el-form
              ref="registerRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="registerForm.realName" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="registerForm.phone" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="registerForm.email" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" show-password type="password" />
              </el-form-item>
              <el-button :loading="loading" class="full-btn" type="primary" @click="handleRegister">
                注册并登录
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { ElMessage } from 'element-plus'
import { authApi, siteApi } from '@/api'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  clearRememberedLogin,
  getRememberedLogin,
  setRememberedLogin,
  setSession,
  appendTokenQuery,
  shouldUseFullPageAuthRedirect,
} from '@/utils/auth.ts'
import { hasAdminAccess, hasRole } from '@/utils/permission.ts'

const activeTab = ref('login')

const registerEnabled = ref(false)

const loading = ref(false)

const rememberPassword = ref(false)

const loginForm = ref({
  username: '',
  password: '',
})

const registerForm = ref({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
})

const loginRules = ref({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const registerRules = ref({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const loginRef = ref<any>()

const registerRef = ref<any>()

const router = useRouter()

const route = useRoute()

const canManageRegister = computed(() => {
  return hasRole('super_admin')
})

function loginRedirectTarget() {
  const redirect = route.query.redirect
  const value = Array.isArray(redirect) ? redirect[0] : redirect
  return value || (hasAdminAccess() ? '/admin/dashboard' : '/progress')
}

function finishLoginRedirect() {
  const target = loginRedirectTarget()
  if (shouldUseFullPageAuthRedirect(target)) {
    window.location.assign(appendTokenQuery(target))
    return
  }
  router.replace(target)
}

async function fetchRegisterEnabled() {
  registerEnabled.value = Boolean(await siteApi.registerEnabled())
  if (!registerEnabled.value && activeTab.value === 'register') {
    activeTab.value = 'login'
  }
}

function handleLogin() {
  loginRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const result = await authApi.login(loginForm.value)
      if (rememberPassword.value) {
        setRememberedLogin({
          username: loginForm.value.username,
          password: loginForm.value.password,
          remember: true,
        })
      } else {
        clearRememberedLogin()
      }
	      setSession(result)
	      ElMessage.success('登录成功')
	      finishLoginRedirect()
    } finally {
      loading.value = false
    }
  })
}

function handleRegister() {
  if (!registerEnabled.value) {
    ElMessage.warning('当前已关闭注册，请联系管理员开通')
    activeTab.value = 'login'
    return
  }
  registerRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authApi.register(registerForm.value)
      const result = await authApi.login({
        username: registerForm.value.username,
        password: registerForm.value.password,
      })
      if (rememberPassword.value) {
        setRememberedLogin({
          username: registerForm.value.username,
          password: registerForm.value.password,
          remember: true,
        })
      } else {
        clearRememberedLogin()
      }
	      setSession(result)
	      ElMessage.success('注册成功')
	      finishLoginRedirect()
    } finally {
      loading.value = false
    }
  })
}

onMounted(async () => {
  const rememberedLogin = getRememberedLogin()
  loginForm.value.username = rememberedLogin.username || ''
  loginForm.value.password = rememberedLogin.password || ''
  rememberPassword.value = Boolean(rememberedLogin.remember)
  await fetchRegisterEnabled()
})
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: var(--oa-page-bg);
}

.login-theme-toggle {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 2;
}

.login-panel {
  display: grid;
  width: min(1040px, 100%);
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 48px;
  align-items: center;
}

.login-copy {
  color: var(--oa-text);
}

.login-logo {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border-radius: 8px;
  font-weight: 600;
  box-shadow: none;
}

.login-copy h1 {
  margin: 18px 0 12px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 56px;
  font-weight: 600;
  line-height: 1.07;
  letter-spacing: 0;
}

.login-copy p {
  max-width: 520px;
  color: var(--oa-muted);
  font-size: 24px;
  font-weight: 300;
  line-height: 1.5;
}

.login-card {
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-elevated-bg);
  box-shadow: none;
  backdrop-filter: none;
}

.full-btn {
  width: 100%;
}

.login-options {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 16px;
}

.register-switch {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 10px;
  color: var(--oa-muted);
  font-size: 13px;
}

.login-card :deep(.el-card__body) {
  padding: 24px 24px 18px;
}

.login-card :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--oa-border);
}

.login-card :deep(.el-tabs__item.is-active) {
  color: var(--oa-primary-dark);
}

@media (max-width: 860px) {
  .login-panel {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .login-copy {
    text-align: center;
  }

  .login-logo {
    margin: 0 auto;
  }

  .login-copy p {
    margin-right: auto;
    margin-left: auto;
  }
}
</style>
