<template>
  <div class="login-page">
    <aside class="login-aside">
      <HomeMapSection background static class="login-aside__map" />
      <div class="login-aside__content">
        <div class="login-aside__brand">
          <div class="login-aside__logo"><img src="/logo-light.svg?v=20261005" alt="OpenAtom" /></div>
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
            <span class="login-aside__feature-dot"></span>社团官网与活动信息
          </div>
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>管理后台与成员系统
          </div>
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>开放应用生态授权
          </div>
          <div class="login-aside__feature">
            <span class="login-aside__feature-dot"></span>OPENATOM - DESK
          </div>
        </div>
        <div class="login-aside__footer">
          © 2025-2027 JMI-OPENATOM &amp;
          <a href="http://www.ariven.cn/">Ariven(软件技术252301 何治皓).</a> All rights reserved.
        </div>
      </div>
    </aside>

    <main class="login-main">
      <div class="login-theme-toggle"><ThemeToggle /></div>
      <div class="login-form-wrapper">
        <router-link class="login-back-link" to="/">
          <ArrowLeft :size="16" aria-hidden="true" /> 返回官网
        </router-link>
        <div class="login-form-header">
          <h2>确认授权登录</h2>
          <p>请确认以下应用和资料，再决定是否继续。</p>
        </div>

        <p v-if="loading" class="consent-status" role="status">正在读取授权请求…</p>
        <div v-else-if="errorMessage" class="consent-error" role="alert">
          <p>{{ errorMessage }}</p>
          <router-link to="/">返回官网</router-link>
        </div>
        <template v-else-if="consent">
          <div class="consent-app">
            <div class="consent-app__icon"><ShieldCheck :size="24" aria-hidden="true" /></div>
            <div>
              <strong>{{ consent.client_name || consent.client_id }}</strong>
              <span>{{ applicationHost }}</span>
            </div>
          </div>
          <p class="consent-account">
            将以 <strong>{{ consent.account_name || consent.username }}</strong>
            <span v-if="consent.username && consent.account_name !== consent.username">
              （{{ consent.username }}）
            </span>
            的 OpenAtom 账号登录
          </p>
          <section class="consent-permissions" aria-labelledby="consent-permissions-title">
            <h3 id="consent-permissions-title">应用将可获取</h3>
            <ul>
              <li><Check :size="16" aria-hidden="true" />账号标识、姓名与头像</li>
              <li><Check :size="16" aria-hidden="true" />邮箱、手机号与学籍资料</li>
              <li><Check :size="16" aria-hidden="true" />社团成员身份、角色与权限</li>
            </ul>
          </section>
          <p class="consent-scopes">本次申请范围：{{ scopeSummary }}</p>
          <p class="consent-note">
            确认后，OpenAtom 会将上述账号资料提供给该应用。你可以选择拒绝此次授权。
          </p>
          <div class="consent-actions">
            <button class="login-submit" type="button" :disabled="submitting" @click="decide(true)">
              {{ submitting ? '正在处理…' : '同意并继续' }}
            </button>
            <button
              class="consent-reject"
              type="button"
              :disabled="submitting"
              @click="decide(false)"
            >
              拒绝授权
            </button>
          </div>
          <p v-if="actionError" class="consent-action-error" role="alert">{{ actionError }}</p>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Check, ShieldCheck } from 'lucide-vue-next'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import HomeMapSection from '@/components/site/home/HomeMapSection.vue'
import { clearSession, getToken } from '@/utils/auth.ts'
import { getOidcAuthority } from '@/utils/oidc.ts'

interface ConsentRequest {
  client_name: string
  client_id: string
  redirect_uri: string
  scope: string
  account_name: string
  username: string
}

const route = useRoute()
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')
const actionError = ref('')
const consent = ref<ConsentRequest | null>(null)
const requestId = computed(() => {
  const value = route.query.request_id
  return typeof value === 'string' && /^[a-f0-9]{64}$/.test(value) ? value : ''
})
const applicationHost = computed(() => {
  try {
    return consent.value ? new URL(consent.value.redirect_uri).host : ''
  } catch {
    return ''
  }
})
const scopeSummary = computed(() => {
  const labels: Record<string, string> = {
    openid: '账号身份',
    profile: '个人资料',
    email: '邮箱',
    mail: '邮箱服务',
    roles: '社团角色',
    permissions: '系统权限',
  }
  return (consent.value?.scope || '')
    .split(/\s+/)
    .filter(Boolean)
    .map((scope) => labels[scope] || scope)
    .join('、')
})

function loginAgain() {
  clearSession()
  window.location.replace(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
}

function authHeaders(token: string) {
  return { jmiopenatom: token }
}

onMounted(async () => {
  if (!requestId.value) {
    errorMessage.value = '授权请求无效，请返回应用后重新发起登录。'
    loading.value = false
    return
  }
  const token = getToken()
  if (!token) {
    loginAgain()
    return
  }
  try {
    const response = await axios.get<ConsentRequest>(
      `${getOidcAuthority()}/oauth/consent/requests/${requestId.value}`,
      { headers: authHeaders(token), withCredentials: false },
    )
    consent.value = response.data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      loginAgain()
      return
    }
    if (axios.isAxiosError(error) && error.response?.status === 403) {
      errorMessage.value = '当前登录账号与发起授权的账号不一致，请返回应用后重新发起登录。'
      return
    }
    errorMessage.value = '授权请求已失效，请返回应用后重新发起登录。'
  } finally {
    loading.value = false
  }
})

async function decide(approved: boolean) {
  if (!consent.value || submitting.value) return
  const token = getToken()
  if (!token) {
    loginAgain()
    return
  }
  submitting.value = true
  actionError.value = ''
  try {
    const response = await axios.post<{ redirect_url: string }>(
      `${getOidcAuthority()}/oauth/consent/requests/${requestId.value}/decision`,
      { approved },
      { headers: authHeaders(token), withCredentials: false },
    )
    const target = new URL(response.data.redirect_url)
    if (target.protocol !== 'https:' && target.protocol !== 'http:')
      throw new Error('Invalid redirect')
    if (target.origin !== new URL(consent.value.redirect_uri).origin)
      throw new Error('Invalid redirect')
    window.location.assign(target.toString())
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      loginAgain()
      return
    }
    actionError.value = '授权处理失败或请求已过期，请返回应用后重试。'
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
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
    linear-gradient(90deg, rgba(8, 8, 10, 0.72), rgba(8, 8, 10, 0.42) 46%, rgba(8, 8, 10, 0.16)),
    linear-gradient(180deg, rgba(8, 8, 10, 0.5), rgba(8, 8, 10, 0.14) 38%, rgba(8, 8, 10, 0.48));
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
  color: rgba(255, 255, 255, 0.86);
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.38);
}
.login-aside__hero {
  width: min(560px, 100%);
  margin: 0;
}
.login-aside__eyebrow {
  margin: 0 0 14px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 600;
}
.login-aside__title {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: 48px;
  font-weight: 700;
  line-height: 1.12;
  color: #fff;
  text-shadow: 0 10px 32px rgba(0, 0, 0, 0.42);
}
.login-aside__name {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 30px;
  font-weight: 600;
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
.login-aside__footer a {
  color: inherit;
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
  animation: fade-up 0.32s ease-out;
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
.login-form-header {
  margin-bottom: 26px;
}
.login-form-header h2 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: 32px;
  font-weight: 800;
  line-height: 1.12;
  color: var(--oa-text);
}
.login-form-header p {
  margin: 9px 0 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--oa-muted);
}
.consent-status,
.consent-error {
  color: var(--oa-muted);
  line-height: 1.6;
}
.consent-error a {
  color: var(--oa-text);
}
.consent-app {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--oa-border);
  border-radius: 14px;
  background: var(--oa-elevated-bg);
}
.consent-app__icon {
  display: grid;
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 12px;
  background: var(--oa-button-subtle-bg);
  color: var(--oa-text);
}
.consent-app strong,
.consent-app span {
  display: block;
  overflow-wrap: anywhere;
}
.consent-app strong {
  font-size: 16px;
  color: var(--oa-text);
}
.consent-app span {
  margin-top: 3px;
  color: var(--oa-muted);
  font-size: 13px;
}
.consent-account {
  margin: 20px 0;
  color: var(--oa-text-soft);
  font-size: 14px;
  line-height: 1.6;
}
.consent-account strong {
  color: var(--oa-text);
}
.consent-permissions {
  padding: 20px;
  border: 1px solid var(--oa-border);
  border-radius: 14px;
  background: var(--oa-elevated-bg);
}
.consent-permissions h3 {
  margin: 0 0 16px;
  color: var(--oa-text);
  font-size: 15px;
}
.consent-permissions ul {
  display: grid;
  gap: 13px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.consent-permissions li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: var(--oa-text-soft);
  font-size: 14px;
  line-height: 1.4;
}
.consent-permissions li svg {
  flex: 0 0 16px;
  color: var(--oa-text);
}
.consent-scopes {
  margin: 14px 0 0;
  color: var(--oa-text-soft);
  font-size: 13px;
  line-height: 1.6;
}
.consent-note {
  margin: 16px 0 22px;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.6;
}
.consent-actions {
  display: grid;
  gap: 10px;
}
.login-submit,
.consent-reject {
  display: inline-flex;
  width: 100%;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.login-submit {
  border: 0;
  background: var(--oa-text);
  color: var(--oa-active-text);
}
.login-submit:hover:not(:disabled) {
  background: var(--oa-active-hover-bg);
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(29, 29, 31, 0.18);
}
.consent-reject {
  border: 1px solid var(--oa-border);
  background: var(--oa-button-bg);
  color: var(--oa-text);
}
.consent-reject:hover:not(:disabled) {
  border-color: var(--oa-border-strong);
  background: var(--oa-elevated-bg);
}
.login-submit:disabled,
.consent-reject:disabled {
  opacity: 0.6;
  cursor: wait;
}
.login-submit:focus-visible,
.consent-reject:focus-visible {
  outline: 2px solid var(--oa-text);
  outline-offset: 3px;
}
.consent-action-error {
  margin: 14px 0 0;
  color: #b42318;
  font-size: 13px;
}
.login-theme-toggle {
  position: fixed;
  top: 10px;
  right: 10px;
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
:global(html.dark) .login-aside::after {
  background:
    linear-gradient(90deg, rgba(8, 8, 10, 0.76), rgba(8, 8, 10, 0.48) 46%, rgba(8, 8, 10, 0.2)),
    linear-gradient(180deg, rgba(8, 8, 10, 0.54), rgba(8, 8, 10, 0.18) 38%, rgba(8, 8, 10, 0.54));
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
  .login-aside__title {
    font-size: 36px;
  }
  .login-aside__name {
    font-size: 22px;
  }
}
@media (max-width: 720px) {
  .login-page {
    flex-direction: column;
    overflow-y: auto;
  }
  .login-aside {
    display: none;
  }
  .login-main {
    width: 100%;
    min-height: 100svh;
    padding: 40px 24px;
  }
  .login-form-header h2 {
    font-size: 26px;
  }
}
@media (max-width: 480px) {
  .login-main {
    padding: 24px 14px;
  }
  .login-form-header {
    margin-bottom: 22px;
  }
}
</style>
