<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-copy">
        <span class="login-logo">OA</span>
        <h1>OpenAtom 登录</h1>
        <p>登录后可统一维护社团、成员、审批、面试和权限等业务数据。</p>
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
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { authApi, siteApi } from '@/api'
import {
  clearRememberedLogin,
  getRememberedLogin,
  setRememberedLogin,
  setSession,
} from '@/utils/auth.ts'
import { hasAdminAccess, hasRole } from '@/utils/permission.ts'

export default {
  name: 'AdminLogin',
  data() {
    return {
      activeTab: 'login',
      registerEnabled: false,
      loading: false,
      rememberPassword: false,
      loginForm: {
        username: '',
        password: '',
      },
      registerForm: {
        username: '',
        password: '',
        realName: '',
        phone: '',
        email: '',
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      },
      registerRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      },
    }
  },
  computed: {
    canManageRegister() {
      return hasRole('super_admin')
    },
  },
  async created() {
    const rememberedLogin = getRememberedLogin()
    this.loginForm.username = rememberedLogin.username || ''
    this.loginForm.password = rememberedLogin.password || ''
    this.rememberPassword = Boolean(rememberedLogin.remember)
    await this.fetchRegisterEnabled()
  },
  methods: {
    async fetchRegisterEnabled() {
      this.registerEnabled = Boolean(await siteApi.registerEnabled())
      if (!this.registerEnabled && this.activeTab === 'register') {
        this.activeTab = 'login'
      }
    },

    handleLogin() {
      this.$refs.loginRef.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const result = await authApi.login(this.loginForm)
          if (this.rememberPassword) {
            setRememberedLogin({
              username: this.loginForm.username,
              password: this.loginForm.password,
              remember: true,
            })
          } else {
            clearRememberedLogin()
          }
          setSession(result)
          ElMessage.success('登录成功')
          this.$router.replace(
            this.$route.query.redirect || (hasAdminAccess() ? '/admin/dashboard' : '/progress'),
          )
        } finally {
          this.loading = false
        }
      })
    },
    handleRegister() {
      if (!this.registerEnabled) {
        ElMessage.warning('当前已关闭注册，请联系管理员开通')
        this.activeTab = 'login'
        return
      }
      this.$refs.registerRef.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          await authApi.register(this.registerForm)
          const result = await authApi.login({
            username: this.registerForm.username,
            password: this.registerForm.password,
          })
          if (this.rememberPassword) {
            setRememberedLogin({
              username: this.registerForm.username,
              password: this.registerForm.password,
              remember: true,
            })
          } else {
            clearRememberedLogin()
          }
          setSession(result)
          ElMessage.success('注册成功')
          this.$router.replace(hasAdminAccess() ? '/admin/dashboard' : '/progress')
        } finally {
          this.loading = false
        }
      })
    },
  },
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: #ffffff;
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
  color: #ffffff;
  background: #1d1d1f;
  border-radius: 8px;
  font-weight: 600;
  box-shadow: none;
}

.login-copy h1 {
  margin: 18px 0 12px;
  font-family: 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 56px;
  font-weight: 600;
  line-height: 1.07;
  letter-spacing: 0;
}

.login-copy p {
  max-width: 520px;
  color: #7a7a7a;
  font-size: 24px;
  font-weight: 300;
  line-height: 1.5;
}

.login-card {
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #ffffff;
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
  color: #7a7a7a;
  font-size: 13px;
}

.login-card :deep(.el-card__body) {
  padding: 24px 24px 18px;
}

.login-card :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(224, 224, 224, 0.9);
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
