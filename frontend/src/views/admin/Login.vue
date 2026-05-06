<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-copy">
        <span class="login-logo">OA</span>
        <h1>OpenAtom 管理后台</h1>
        <p>登录后可统一维护社团、成员、审批、面试和权限等业务数据。</p>
      </div>
      <el-card shadow="never" class="login-card">
        <div class="register-switch">
          <el-switch
            v-model="registerEnabled"
            :disabled="!canManageRegister"
            inline-prompt
            active-text="注册开"
            inactive-text="注册关"
            @change="handleRegisterSwitch"
          />
          <span>{{ canManageRegister ? '允许新用户自助注册' : `当前注册：${registerEnabled ? '开启' : '关闭'}` }}</span>
        </div>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="账号登录" name="login">
            <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" @keyup.enter="handleLogin" />
              </el-form-item>
              <div class="login-options">
                <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
              </div>
              <el-button type="primary" class="full-btn" :loading="loading" @click="handleLogin">登录</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane v-if="registerEnabled" label="注册账号" name="register">
            <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top">
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
                <el-input v-model="registerForm.password" type="password" show-password />
              </el-form-item>
              <el-button type="primary" class="full-btn" :loading="loading" @click="handleRegister">注册并登录</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { authApi, siteApi } from '../../api'
import { clearRememberedLogin, getRememberedLogin, setRememberedLogin, setSession } from '../../utils/auth'
import { hasAdminAccess, hasRole } from '../../utils/permission'

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
        password: ''
      },
      registerForm: {
        username: '',
        password: '',
        realName: '',
        phone: '',
        email: ''
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      registerRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  computed: {
    canManageRegister() {
      return hasRole('super_admin')
    }
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
    async handleRegisterSwitch(value) {
      if (!this.canManageRegister) {
        await this.fetchRegisterEnabled()
        return
      }
      try {
        await authApi.updateRegisterEnabled(value)
        ElMessage.success(value ? '已开启注册' : '已关闭注册')
      } catch (error) {
        await this.fetchRegisterEnabled()
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
              remember: true
            })
          } else {
            clearRememberedLogin()
          }
          setSession(result)
          ElMessage.success('登录成功')
          this.$router.replace(this.$route.query.redirect || (hasAdminAccess() ? '/admin/dashboard' : '/progress'))
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
            password: this.registerForm.password
          })
          if (this.rememberPassword) {
            setRememberedLogin({
              username: this.registerForm.username,
              password: this.registerForm.password,
              remember: true
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
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at left top, rgba(147, 197, 253, 0.34), transparent 34%),
    radial-gradient(circle at right center, rgba(191, 219, 254, 0.56), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #edf4ff 100%);
}

.login-panel {
  display: grid;
  width: min(980px, 100%);
  grid-template-columns: 1fr 390px;
  gap: 28px;
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
  color: #fff;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 16px;
  font-weight: 800;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.18);
}

.login-copy h1 {
  margin: 18px 0 12px;
  font-size: 42px;
  letter-spacing: -0.03em;
}

.login-copy p {
  max-width: 520px;
  color: #475569;
  line-height: 1.8;
}

.login-card {
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 24px 60px rgba(37, 99, 235, 0.12);
  backdrop-filter: blur(16px);
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
  color: #64748b;
  font-size: 13px;
}

.login-card :deep(.el-card__body) {
  padding: 24px 24px 18px;
}

.login-card :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(219, 230, 245, 0.9);
}

.login-card :deep(.el-tabs__item.is-active) {
  color: var(--oa-primary-dark);
}

@media (max-width: 860px) {
  .login-panel {
    grid-template-columns: 1fr;
  }
}
</style>
