<template>
  <div class="login-page">
    <!-- 左侧地图区 -->
    <aside class="login-aside">
      <HomeMapSection background static class="login-aside__map" />
      <div class="login-aside__content">
        <div class="login-aside__brand">
          <div class="login-aside__logo">
            <img src="/logo.png" alt="OpenAtom" />
          </div>
          <span class="login-aside__badge">JMI · OPENATOM</span>
        </div>

        <div class="login-aside__hero">
          <p class="login-aside__eyebrow">统一身份认证平台</p>
          <h1 class="login-aside__title">开放原子开源社团</h1>
          <p class="login-aside__name">JMI-OPENATOM</p>
          <p class="login-aside__tagline">一个账号访问官网、管理后台与开放应用生态。</p>
        </div>

        <div class="login-aside__features">
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>
            <span>社团官网与活动信息</span>
          </div>
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>
            <span>管理后台与成员系统</span>
          </div>
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>
            <span>开放应用生态授权</span>
          </div>
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>
            <span>OPENATOM - DESK</span>
          </div>
        </div>

        <div class="login-aside__footer">
          <span
            >© 2025-2027 JMI-OPENATOM &
            <a href="http://www.ariven.cn/">Ariven(软件技术252301 何治皓).</a> All rights
            reserved.</span
          >
        </div>
      </div>
    </aside>

    <!-- 右侧表单区 -->
    <main class="login-main">
      <div class="login-theme-toggle">
        <ThemeToggle />
      </div>
      <div class="login-form-wrapper">
        <router-link class="login-back-link" to="/">
          <ArrowLeft :size="16" aria-hidden="true" />
          返回官网
        </router-link>
        <div class="login-form-header">
          <h2>{{ activeTab === 'register' ? '创建账号' : '欢迎回来' }}</h2>
          <p>
            {{
              activeTab === 'register'
                ? '加入 JMI-OPENATOM，开启你的开源旅程。'
                : '一个账号，通行所有应用。'
            }}
          </p>
        </div>

        <div v-if="redirectHint" class="login-redirect-hint" role="status">
          登录后继续：<strong>{{ redirectHint }}</strong>
        </div>

        <div v-if="registerEnabled" class="login-tab-switch">
          <button
            type="button"
            :class="['login-tab-btn', { active: activeTab === 'login' }]"
            @click="activeTab = 'login'"
          >
            账号登录
          </button>
          <button
            type="button"
            :class="['login-tab-btn', { active: activeTab === 'register' }]"
            @click="activeTab = 'register'"
          >
            新账号注册
          </button>
          <span class="login-tab-indicator" :style="indicatorStyle"></span>
        </div>

        <!-- 登录表单 -->
        <form v-if="activeTab === 'login'" class="login-form" @submit.prevent="handleLogin">
          <div class="login-field">
            <label class="login-label" for="login-username">账号</label>
            <div class="login-control">
              <UserRound :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="login-username"
                ref="usernameInputRef"
                v-model="loginForm.username"
                type="text"
                placeholder="请输入用户名或学号"
                class="login-input"
                autocomplete="username"
              />
            </div>
          </div>
          <div class="login-field">
            <label class="login-label" for="login-password">密码</label>
            <div class="login-control">
              <LockKeyhole :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="login-password"
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="login-input login-input--password"
                autocomplete="current-password"
                @keyup.enter="handleLogin"
              />
              <button
                type="button"
                class="login-field__toggle"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                :title="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" :size="18" aria-hidden="true" />
                <Eye v-else :size="18" aria-hidden="true" />
              </button>
            </div>
          </div>
          <div class="login-form-row">
            <label class="login-checkbox">
              <input v-model="rememberPassword" type="checkbox" />
              <span>记住密码</span>
            </label>
          </div>
          <button type="submit" class="login-submit" :disabled="loading">
            <template v-if="!loading">
              <LogIn :size="18" aria-hidden="true" />
              <span>登录</span>
            </template>
            <span v-else class="login-spinner"></span>
          </button>
          <div class="login-social">
            <div class="login-social__heading">
              <span>其他登录方式</span>
              <small>仅限已绑定账号</small>
            </div>
            <div
              v-if="googleClientId"
              ref="googleButtonRef"
              class="login-google-button"
              aria-label="使用 Google 登录"
            ></div>
            <p v-if="googleUnavailable" class="login-social__error">Google 登录服务当前不可用</p>
            <div class="login-social__providers">
              <button
                type="button"
                class="login-provider-button"
                :disabled="Boolean(oauthLoading)"
                @click="handleGithubLogin"
              >
                <LoaderCircle
                  v-if="oauthLoading === 'github'"
                  :size="19"
                  aria-hidden="true"
                  class="login-provider-button__spinner"
                />
                <Github v-else :size="19" aria-hidden="true" />
                <span>GitHub</span>
              </button>
              <button
                type="button"
                class="login-provider-button login-provider-button--gitee"
                :disabled="Boolean(oauthLoading)"
                @click="handleGiteeLogin"
              >
                <LoaderCircle
                  v-if="oauthLoading === 'gitee'"
                  :size="19"
                  aria-hidden="true"
                  class="login-provider-button__spinner"
                />
                <svg
                  v-else
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                  class="login-provider-button__gitee"
                >
                  <path
                    d="M5 3h14a2 2 0 0 1 2 2v3H9v8h7v-3h-4V9h9v7a5 5 0 0 1-5 5H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"
                  />
                </svg>
                <span>Gitee</span>
              </button>
            </div>
          </div>
        </form>

        <!-- 注册表单 -->
        <form v-else class="login-form" @submit.prevent="handleRegister">
          <div class="login-field">
            <label class="login-label" for="register-username">账号</label>
            <div class="login-control">
              <UserRound :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="register-username"
                v-model="registerForm.username"
                type="text"
                placeholder="请输入用户名"
                class="login-input"
                autocomplete="username"
              />
            </div>
          </div>
          <div class="login-field">
            <label class="login-label" for="register-realname">姓名</label>
            <div class="login-control">
              <UserRound :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="register-realname"
                v-model="registerForm.realName"
                type="text"
                placeholder="请输入真实姓名"
                class="login-input"
                autocomplete="name"
              />
            </div>
          </div>
          <div class="login-field">
            <label class="login-label" for="register-phone">手机号</label>
            <div class="login-control">
              <Phone :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="register-phone"
                v-model="registerForm.phone"
                type="tel"
                placeholder="选填"
                class="login-input"
                autocomplete="tel"
              />
            </div>
          </div>
          <div class="login-field">
            <label class="login-label" for="register-email">邮箱</label>
            <div class="login-control">
              <Mail :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="register-email"
                v-model="registerForm.email"
                type="email"
                placeholder="选填"
                class="login-input"
                autocomplete="email"
              />
            </div>
          </div>
          <div class="login-field">
            <label class="login-label" for="register-password">密码</label>
            <div class="login-control">
              <LockKeyhole :size="18" aria-hidden="true" class="login-control__icon" />
              <input
                id="register-password"
                v-model="registerForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请设置登录密码"
                class="login-input login-input--password"
                autocomplete="new-password"
              />
              <button
                type="button"
                class="login-field__toggle"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                :title="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" :size="18" aria-hidden="true" />
                <Eye v-else :size="18" aria-hidden="true" />
              </button>
            </div>
          </div>
          <button type="submit" class="login-submit" :disabled="loading">
            <template v-if="!loading">
              <UserPlus :size="18" aria-hidden="true" />
              <span>注册并登录</span>
            </template>
            <span v-else class="login-spinner"></span>
          </button>
        </form>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { authApi, siteApi } from '@/api'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import HomeMapSection from '@/components/site/home/HomeMapSection.vue'
import {
  ArrowLeft,
  Eye,
  EyeOff,
  Github,
  LoaderCircle,
  LockKeyhole,
  LogIn,
  Mail,
  Phone,
  UserPlus,
  UserRound,
} from 'lucide-vue-next'
import {
  clearRememberedLogin,
  getRememberedLogin,
  setRememberedLogin,
  setSession,
  appendTokenQuery,
  shouldUseFullPageAuthRedirect,
} from '@/utils/auth.ts'
import { hasAdminAccess } from '@/utils/permission.ts'
import {
  cancelGoogleIdentityPrompt,
  googleClientId,
  renderGoogleButton,
  type GoogleCredentialResponse,
} from '@/utils/googleIdentity.ts'

const activeTab = ref<'login' | 'register'>('login')
const registerEnabled = ref(false)
const loading = ref(false)
const rememberPassword = ref(false)
const showPassword = ref(false)
const usernameInputRef = ref<HTMLInputElement>()
const googleButtonRef = ref<HTMLDivElement>()
const googleUnavailable = ref(false)
const oauthLoading = ref<'github' | 'gitee' | null>(null)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', realName: '', phone: '', email: '' })

const route = useRoute()

const redirectHint = computed(() => {
  const redirect = route.query.redirect
  const target = Array.isArray(redirect) ? redirect[0] : redirect
  if (!target) return ''
  if (target.startsWith('/apply')) return '入会申请'
  if (target.startsWith('/activities/')) return '活动报名'
  if (target.startsWith('/progress')) return '申请进度'
  if (target.startsWith('/workspace')) return '个人工作台'
  if (target.startsWith('/admin')) return '管理后台'
  return '刚才访问的页面'
})

const indicatorStyle = computed(() => {
  const isLogin = activeTab.value === 'login'
  return {
    transform: `translateX(${isLogin ? '0%' : '100%'})`,
    width: registerEnabled.value ? '50%' : '100%',
  }
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
  window.location.assign(target)
}

async function fetchRegisterEnabled() {
  registerEnabled.value = Boolean(await siteApi.registerEnabled())
  if (!registerEnabled.value && activeTab.value === 'register') {
    activeTab.value = 'login'
  }
}

async function handleLogin() {
  if (!loginForm.value.username) {
    ElMessage.warning('请输入用户名')
    usernameInputRef.value?.focus()
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
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
    await nextTick()
    ElMessage.success('登录成功')
    await nextTick()
    finishLoginRedirect()
  } catch {
    // 错误已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function handleGoogleCredential(response: GoogleCredentialResponse) {
  if (!response.credential || loading.value) return
  loading.value = true
  try {
    const result = await authApi.googleLogin({ credential: response.credential })
    setSession(result)
    await nextTick()
    ElMessage.success('Google 登录成功')
    finishLoginRedirect()
  } catch {
    // 错误已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function handleGithubLogin() {
  if (oauthLoading.value) return
  try {
    oauthLoading.value = 'github'
    const result = await authApi.githubLoginUrl(loginRedirectTarget())
    window.location.assign(result.url)
  } catch {
    // 错误已由请求拦截器统一处理
    oauthLoading.value = null
  }
}

async function handleGiteeLogin() {
  if (oauthLoading.value) return
  try {
    oauthLoading.value = 'gitee'
    const result = await authApi.giteeLoginUrl(loginRedirectTarget())
    window.location.assign(result.url)
  } catch {
    // 错误已由请求拦截器统一处理
    oauthLoading.value = null
  }
}

async function initializeGoogleLogin() {
  if (!googleClientId) return
  try {
    await nextTick()
    if (!googleButtonRef.value) return
    await renderGoogleButton(googleButtonRef.value, handleGoogleCredential)
  } catch (error) {
    googleUnavailable.value = true
    console.warn(error)
  }
}

async function handleRegister() {
  if (!registerEnabled.value) {
    ElMessage.warning('当前已关闭注册，请联系管理员开通')
    activeTab.value = 'login'
    return
  }
  if (!registerForm.value.username) return ElMessage.warning('请输入用户名')
  if (!registerForm.value.realName) return ElMessage.warning('请输入姓名')
  if (!registerForm.value.password) return ElMessage.warning('请输入密码')
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
    await nextTick()
    ElMessage.success('注册成功')
    await nextTick()
    finishLoginRedirect()
  } catch {
    // 错误已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const rememberedLogin = getRememberedLogin()
  loginForm.value.username = rememberedLogin.username || ''
  loginForm.value.password = rememberedLogin.password || ''
  rememberPassword.value = Boolean(rememberedLogin.remember)
  await fetchRegisterEnabled()
  await initializeGoogleLogin()
})

onBeforeUnmount(cancelGoogleIdentityPrompt)
</script>

<style scoped>
.login-page {
  --login-control-radius: 14px;
  --login-pill-radius: 999px;
  position: relative;
  display: flex;
  min-height: 100svh;
  overflow: hidden;
  background: var(--oa-page-bg);
  color: var(--oa-text);
}

.login-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 52%;
  min-width: 0;
  overflow: hidden;
  color: #fff;
  background: #111113;
}

.login-aside__map {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.login-aside::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(
      90deg,
      rgba(8, 8, 10, 0.72) 0%,
      rgba(8, 8, 10, 0.42) 46%,
      rgba(8, 8, 10, 0.16) 100%
    ),
    linear-gradient(
      180deg,
      rgba(8, 8, 10, 0.5) 0%,
      rgba(8, 8, 10, 0.14) 38%,
      rgba(8, 8, 10, 0.48) 100%
    );
}

.login-aside__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
  padding: 48px 56px;
}

.login-aside__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: auto;
}

.login-aside__logo {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 8px;
  backdrop-filter: blur(6px);
}

.login-aside__logo img {
  width: 30px;
  height: 30px;
  border-radius: 6px;
}

.login-aside__badge {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
  color: rgba(255, 255, 255, 0.86);
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.38);
}

.login-aside__hero {
  width: min(560px, 100%);
  margin: 0;
  padding-bottom: 0;
}

.login-aside__eyebrow {
  margin: 0 0 14px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0;
}

.login-aside__title {
  margin: 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 48px;
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: 0;
  color: #fff;
  text-shadow: 0 10px 32px rgba(0, 0, 0, 0.42);
}

.login-aside__name {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 30px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0;
}

.login-aside__tagline {
  margin: 18px 0 0;
  max-width: 360px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 16px;
  line-height: 1.7;
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.36);
}

.login-aside__features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 32px 0 0;
}

.login-aside__feature {
  display: flex;
  align-items: center;
  min-height: 34px;
  gap: 9px;
  padding: 7px 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
  font-size: 14px;
  color: rgba(255, 255, 255, 0.84);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.28);
}

.login-aside__feature-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  border-radius: 50%;
  background: #f7f7f8;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.14);
}

.login-aside__footer {
  margin-top: auto;
  padding-top: 24px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.3);
}

.login-main {
  position: relative;
  z-index: 2;
  display: flex;
  width: 48%;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  background: var(--oa-page-bg);
}

.login-main::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 1px;
  background: var(--oa-divider);
  opacity: 0.74;
}

.login-form-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.login-back-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  margin-bottom: 18px;
  color: var(--oa-muted);
  border-radius: 10px;
  text-decoration: none;
}

.login-back-link:hover,
.login-back-link:focus-visible {
  color: var(--oa-text);
  outline: 2px solid var(--oa-border-strong);
  outline-offset: 4px;
}

.login-redirect-hint {
  margin: -4px 0 20px;
  padding: 11px 13px;
  color: var(--oa-text-soft);
  border: 1px solid var(--oa-border);
  border-radius: 12px;
  background: var(--oa-elevated-bg);
  font-size: 13px;
  line-height: 1.5;
}

.login-form-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.login-form-brand__logo {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  place-items: center;
  border: 1px solid var(--oa-border);
  border-radius: 13px;
  background: var(--oa-elevated-bg);
  box-shadow: 0 6px 18px rgba(29, 29, 31, 0.08);
}

.login-form-brand__logo img {
  width: 28px;
  height: 28px;
  border-radius: 7px;
}

.login-form-brand__copy {
  display: grid;
  gap: 2px;
}

.login-form-brand__copy strong {
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.login-form-brand__copy small {
  color: var(--oa-muted);
  font-size: 12px;
}

.login-form-header {
  margin-bottom: 26px;
}

.login-form-header h2 {
  margin: 0;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: 32px;
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: 0.02em;
  color: var(--oa-text);
}

.login-form-header > p:not(.login-form-eyebrow) {
  margin: 9px 0 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--oa-muted);
}

.login-tab-switch {
  position: relative;
  display: flex;
  gap: 0;
  padding: 3px;
  margin-bottom: 28px;
  overflow: hidden;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 12px;
}

.login-tab-btn {
  position: relative;
  z-index: 1;
  display: inline-flex;
  flex: 1;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: var(--oa-muted);
  cursor: pointer;
  transition: color 0.18s ease;
}

.login-tab-btn.active {
  color: var(--oa-text);
}

.login-tab-indicator {
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 3px;
  width: 50%;
  background: var(--oa-elevated-bg);
  border-radius: 9px;
  box-shadow: 0 4px 14px rgba(29, 29, 31, 0.08);
  transition:
    transform 0.22s ease,
    width 0.22s ease;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-label {
  color: var(--oa-text-soft);
  font-size: 14px;
  font-weight: 600;
}

.login-control {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.login-control__icon {
  position: absolute;
  left: 14px;
  z-index: 1;
  color: var(--oa-faint);
  pointer-events: none;
}

.login-input {
  width: 100%;
  min-height: 48px;
  padding: 0 16px 0 44px;
  border: 1px solid var(--oa-border);
  border-radius: var(--login-control-radius);
  background: var(--oa-button-bg);
  font-size: 15px;
  line-height: 48px;
  color: var(--oa-text);
  outline: none;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.login-input--password {
  padding-right: 48px;
}

.login-input::placeholder {
  color: var(--oa-faint);
}

.login-input:focus {
  border-color: var(--oa-text);
  background: var(--oa-elevated-bg);
  box-shadow: 0 0 0 3px var(--oa-focus-ring);
}

.login-input:hover:not(:focus) {
  border-color: var(--oa-border-strong);
}

.login-field__toggle {
  position: absolute;
  right: 7px;
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--oa-muted);
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.login-field__toggle:hover {
  color: var(--oa-text);
  background: var(--oa-button-subtle-bg);
}

.login-form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 24px;
  margin: -2px 0 2px;
}

.login-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  line-height: 1;
  color: var(--oa-muted);
  cursor: pointer;
  user-select: none;
}

.login-checkbox input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: var(--oa-text);
  cursor: pointer;
}

.login-checkbox:hover {
  color: var(--oa-text);
}

.login-submit {
  display: inline-flex;
  width: 100%;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border: none;
  border-radius: var(--login-pill-radius);
  background: var(--oa-text);
  color: var(--oa-active-text);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
  cursor: pointer;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    opacity 0.18s ease;
  margin-top: 2px;
}

.login-submit:hover:not(:disabled) {
  background: var(--oa-active-hover-bg);
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(29, 29, 31, 0.18);
}

.login-submit:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.login-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-social {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid var(--oa-divider);
}

.login-social__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--oa-text-soft);
  font-size: 13px;
  font-weight: 600;
}

.login-social__heading small {
  color: var(--oa-faint);
  font-size: 12px;
  font-weight: 400;
}

.login-google-button {
  display: flex;
  width: 100%;
  min-height: 44px;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--login-pill-radius);
}

.login-social__providers {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.login-provider-button {
  display: inline-flex;
  width: 100%;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border: 1px solid var(--oa-border);
  border-radius: var(--login-pill-radius);
  background: var(--oa-button-bg);
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.login-provider-button:hover:not(:disabled) {
  border-color: var(--oa-border-strong);
  background: var(--oa-elevated-bg);
  box-shadow: 0 8px 24px rgba(29, 29, 31, 0.08);
  transform: translateY(-1px);
}

.login-provider-button:active:not(:disabled) {
  box-shadow: none;
  transform: translateY(0);
}

.login-provider-button:focus-visible,
.login-submit:focus-visible,
.login-field__toggle:focus-visible,
.login-tab-btn:focus-visible {
  outline: 2px solid var(--oa-text);
  outline-offset: 3px;
}

.login-provider-button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.login-provider-button__gitee {
  width: 19px;
  height: 19px;
  fill: #c71d23;
}

.login-provider-button__spinner {
  animation: spin 0.75s linear infinite;
}

.login-social__error {
  margin: -6px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
  text-align: center;
}

.login-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

:global(html.dark) .login-aside::after {
  background:
    linear-gradient(
      90deg,
      rgba(8, 8, 10, 0.76) 0%,
      rgba(8, 8, 10, 0.48) 46%,
      rgba(8, 8, 10, 0.2) 100%
    ),
    linear-gradient(
      180deg,
      rgba(8, 8, 10, 0.54) 0%,
      rgba(8, 8, 10, 0.18) 38%,
      rgba(8, 8, 10, 0.54) 100%
    );
}

:global(html.dark) .login-form-wrapper {
  background: transparent;
  box-shadow: none;
}

:global(html.dark) .login-input {
  background: rgba(20, 20, 22, 0.92);
}

@media (max-width: 1080px) {
  .login-aside {
    width: 48%;
  }

  .login-main {
    width: 52%;
    padding: 40px 28px;
  }

  .login-aside__content {
    padding: 40px 36px;
  }

  .login-aside__brand {
    margin-bottom: auto;
  }

  .login-aside__title {
    font-size: 36px;
  }

  .login-aside__name {
    font-size: 22px;
  }

  .login-aside__features {
    gap: 8px;
  }

  .login-aside__feature {
    padding: 7px 10px;
    font-size: 13px;
  }
}

@media (max-width: 720px) {
  .login-page {
    flex-direction: column;
    min-height: 100svh;
    overflow-y: auto;
  }

  .login-aside {
    display: none;
  }

  .login-main {
    width: 100%;
    min-height: 100svh;
    padding: 40px 24px;
    margin-top: 0;
  }

  .login-main::before {
    inset: 0 0 auto;
    width: auto;
    height: 1px;
  }

  .login-form-wrapper {
    max-width: 400px;
  }

  .login-form-header h2 {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .login-main {
    padding: 20px 14px;
    margin-top: 0;
  }

  .login-form-wrapper {
    padding: 0;
  }

  .login-form-header {
    margin-bottom: 22px;
  }

  .login-form-brand {
    margin-bottom: 24px;
  }

  .login-tab-btn {
    min-height: 36px;
    padding: 0 8px;
    font-size: 13px;
  }
}

.login-form-wrapper {
  animation: fade-up 0.32s ease-out;
}

.login-aside__content {
  animation: fade-in 0.6s ease-out 0.1s both;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.login-theme-toggle {
  position: fixed;
  top: 10px;
  right: 10px;
}
</style>
