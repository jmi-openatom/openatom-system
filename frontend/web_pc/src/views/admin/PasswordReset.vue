<template>
  <div class="reset-page">
    <!-- 左侧地图区 -->
    <aside class="reset-aside">
      <HomeMapSection background static class="reset-aside__map" />
      <div class="reset-aside__content">
        <div class="reset-aside__brand">
          <div class="reset-aside__logo">
            <img src="/logo.png" alt="OpenAtom" />
          </div>
          <span class="reset-aside__badge">JMI · OPENATOM</span>
        </div>

        <div class="reset-aside__hero">
          <p class="reset-aside__eyebrow">统一身份认证平台</p>
          <h1 class="reset-aside__title">找回密码</h1>
          <p class="reset-aside__name">邮箱验证码安全重置</p>
          <p class="reset-aside__tagline">
            通过绑定邮箱接收验证码，即可安全重置账号密码，全程无需管理员介入。
          </p>
        </div>

        <div class="reset-aside__steps">
          <div v-for="(step, index) in steps" :key="step" class="reset-aside__step">
            <span
              class="reset-aside__step-dot"
              :class="{ active: currentStep === index + 1, done: currentStep > index + 1 }"
            ></span>
            <span>{{ step }}</span>
          </div>
        </div>

        <div class="reset-aside__footer">
          <span>© 2025-2027 JMI-OPENATOM All rights reserved.</span>
        </div>
      </div>
    </aside>

    <!-- 右侧表单区 -->
    <main class="reset-main">
      <div class="reset-form-wrapper">
        <router-link class="reset-back-link" to="/login">
          <ArrowLeft :size="18" aria-hidden="true" />
          <span>返回登录</span>
        </router-link>

        <div class="reset-form-brand">
          <div class="reset-form-brand__logo">
            <img src="/logo.png" alt="OpenAtom" />
          </div>
          <div class="reset-form-brand__copy">
            <strong>JMI-OPENATOM</strong>
            <small>找回密码</small>
          </div>
        </div>

        <!-- 步骤 1：输入账号 -->
        <div v-if="currentStep === 1" class="reset-panel">
          <div class="reset-form-header">
            <h2>输入账号</h2>
            <p>请输入注册时的用户名、学号或绑定邮箱，完成滑块验证后，我们将向绑定邮箱发送验证码。</p>
          </div>
          <form class="reset-form" @submit.prevent="handleSendCode">
            <div class="reset-field">
              <label class="reset-label" for="reset-account">账号</label>
              <div class="reset-control">
                <UserRound :size="18" aria-hidden="true" class="reset-control__icon" />
                <input
                  id="reset-account"
                  v-model="form.account"
                  type="text"
                  placeholder="请输入用户名、学号或邮箱"
                  class="reset-input"
                  autocomplete="username"
                />
              </div>
            </div>

            <div class="reset-field">
              <label class="reset-label">滑块验证</label>
              <div
                v-if="captcha"
                class="reset-captcha"
                :class="{ 'is-loading': captchaLoading }"
              >
                <div class="reset-captcha__stage">
                  <img
                    ref="imageRef"
                    :src="captcha.backgroundBase64"
                    class="reset-captcha__bg"
                    alt="滑块验证码背景"
                    @load="measureScale"
                  />
                  <img
                    :src="captcha.pieceBase64"
                    class="reset-captcha__piece"
                    :style="pieceStyle"
                    alt=""
                  />
                  <button
                    type="button"
                    class="reset-captcha__refresh"
                    aria-label="刷新验证码"
                    title="刷新验证码"
                    @click="loadCaptcha"
                  >
                    <RefreshCw :size="15" aria-hidden="true" />
                  </button>
                </div>
                <div
                  ref="sliderRef"
                  class="reset-captcha__slider"
                  :class="{ 'is-dragging': dragging }"
                  @pointerdown="onSliderPointerDown"
                  @pointermove="onSliderPointerMove"
                  @pointerup="onSliderPointerUp"
                  @pointercancel="onSliderPointerUp"
                >
                  <span class="reset-captcha__slider-hint">向右滑动完成验证</span>
                  <span class="reset-captcha__slider-knob" :style="{ left: pieceX + 'px' }">
                    <ChevronsRight :size="18" aria-hidden="true" />
                  </span>
                </div>
              </div>
              <div v-else class="reset-captcha__empty">
                <button type="button" class="reset-captcha__empty-btn" @click="loadCaptcha">
                  <RefreshCw :size="16" aria-hidden="true" />
                  <span>验证码加载失败，点击重试</span>
                </button>
              </div>
            </div>

            <button type="submit" class="reset-submit" :disabled="sending || !captcha">
              <template v-if="!sending">
                <Send :size="18" aria-hidden="true" />
                <span>发送验证码</span>
              </template>
              <span v-else class="reset-spinner"></span>
            </button>
          </form>
        </div>

        <!-- 步骤 2：输入验证码与新密码 -->
        <div v-else-if="currentStep === 2" class="reset-panel">
          <div class="reset-form-header">
            <h2>重置密码</h2>
            <p>
              验证码已发送至 <strong>{{ form.account }}</strong>
              绑定的邮箱，请在 5 分钟内完成验证。
            </p>
          </div>
          <form class="reset-form" @submit.prevent="handleReset">
            <div class="reset-field">
              <label class="reset-label" for="reset-code">邮箱验证码</label>
              <div class="reset-control">
                <KeyRound :size="18" aria-hidden="true" class="reset-control__icon" />
                <input
                  id="reset-code"
                  v-model="form.code"
                  type="text"
                  inputmode="numeric"
                  maxlength="6"
                  placeholder="6 位数字验证码"
                  class="reset-input reset-input--code"
                  autocomplete="one-time-code"
                />
                <button
                  type="button"
                  class="reset-resend"
                  :disabled="countdown > 0 || sending"
                  @click="handleSendCode"
                >
                  <span v-if="countdown > 0">{{ countdown }}s 后重发</span>
                  <span v-else>重新发送</span>
                </button>
              </div>
            </div>
            <div class="reset-field">
              <label class="reset-label" for="reset-password">新密码</label>
              <div class="reset-control">
                <LockKeyhole :size="18" aria-hidden="true" class="reset-control__icon" />
                <input
                  id="reset-password"
                  v-model="form.newPassword"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="8-72 位，请设置新登录密码"
                  class="reset-input reset-input--password"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="reset-field__toggle"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  :title="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  <EyeOff v-if="showPassword" :size="18" aria-hidden="true" />
                  <Eye v-else :size="18" aria-hidden="true" />
                </button>
              </div>
            </div>
            <div class="reset-field">
              <label class="reset-label" for="reset-confirm">确认新密码</label>
              <div class="reset-control">
                <LockKeyhole :size="18" aria-hidden="true" class="reset-control__icon" />
                <input
                  id="reset-confirm"
                  v-model="form.confirmPassword"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请再次输入新密码"
                  class="reset-input reset-input--password"
                  autocomplete="new-password"
                />
              </div>
            </div>
            <button type="submit" class="reset-submit" :disabled="submitting">
              <template v-if="!submitting">
                <KeyRound :size="18" aria-hidden="true" />
                <span>确认重置</span>
              </template>
              <span v-else class="reset-spinner"></span>
            </button>
          </form>
        </div>

        <!-- 步骤 3：重置成功 -->
        <div v-else class="reset-panel">
          <div class="reset-success">
            <div class="reset-success__icon">
              <CheckCircle2 :size="34" aria-hidden="true" />
            </div>
            <div class="reset-form-header">
              <h2>重置成功</h2>
              <p>密码已更新，请使用新密码重新登录。所有旧会话已安全退出。</p>
            </div>
            <button type="button" class="reset-submit" @click="goToLogin">
              <LogIn :size="18" aria-hidden="true" />
              <span>返回登录</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus/es/components/message/index'
import { authApi } from '@/api'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import HomeMapSection from '@/components/site/home/HomeMapSection.vue'
import {
  ArrowLeft,
  CheckCircle2,
  ChevronsRight,
  Eye,
  EyeOff,
  KeyRound,
  LockKeyhole,
  LogIn,
  RefreshCw,
  Send,
  UserRound,
} from 'lucide-vue-next'

const CAPTCHA_NATURAL_WIDTH = 280
const CAPTCHA_PIECE_NATURAL_WIDTH = 55
const CAPTCHA_PIECE_NATURAL_HEIGHT = 44

interface CaptchaData {
  captchaId: string
  backgroundBase64: string
  pieceBase64: string
  pieceY: number
}

const steps = ['验证邮箱', '设置新密码', '完成']

const router = useRouter()
const currentStep = ref(1)
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
const showPassword = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  account: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const captcha = ref<CaptchaData | null>(null)
const captchaLoading = ref(false)
const pieceX = ref(0)
const dragging = ref(false)
const imageScale = ref(1)
const imageRef = ref<HTMLImageElement>()
const sliderRef = ref<HTMLDivElement>()

const trackMax = computed(
  () => Math.max(0, CAPTCHA_NATURAL_WIDTH - CAPTCHA_PIECE_NATURAL_WIDTH) * imageScale.value,
)

const pieceStyle = computed(() => {
  const scale = imageScale.value
  return {
    left: `${pieceX.value}px`,
    top: `${(captcha.value ? captcha.value.pieceY : 0) * scale}px`,
    width: `${CAPTCHA_PIECE_NATURAL_WIDTH * scale}px`,
    height: `${CAPTCHA_PIECE_NATURAL_HEIGHT * scale}px`,
  }
})

function measureScale() {
  if (imageRef.value && imageRef.value.clientWidth > 0) {
    imageScale.value = imageRef.value.clientWidth / CAPTCHA_NATURAL_WIDTH
  }
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    captcha.value = await authApi.captcha()
    pieceX.value = 0
    await nextTick()
    measureScale()
  } catch {
    captcha.value = null
  } finally {
    captchaLoading.value = false
  }
}

function onSliderPointerDown(event: PointerEvent) {
  if (!sliderRef.value || !captcha.value) return
  dragging.value = true
  sliderRef.value.setPointerCapture(event.pointerId)
  updatePieceFromPointer(event)
}

function updatePieceFromPointer(event: PointerEvent) {
  const slider = sliderRef.value
  if (!slider || !captcha.value) return
  const rect = slider.getBoundingClientRect()
  pieceX.value = Math.min(trackMax.value, Math.max(0, event.clientX - rect.left))
}

function onSliderPointerMove(event: PointerEvent) {
  if (!dragging.value) return
  updatePieceFromPointer(event)
}

function onSliderPointerUp(event: PointerEvent) {
  if (!dragging.value) return
  dragging.value = false
  if (sliderRef.value) sliderRef.value.releasePointerCapture?.(event.pointerId)
  if (pieceX.value > 0) handleSendCode()
}

async function handleSendCode() {
  if (!form.account.trim()) {
    ElMessage.warning('请输入账号')
    pieceX.value = 0
    return
  }
  if (!captcha.value) {
    ElMessage.warning('请先完成滑块验证')
    await loadCaptcha()
    return
  }
  if (countdown.value > 0) return
  sending.value = true
  try {
    await authApi.sendPasswordResetCode({
      account: form.account.trim(),
      captchaId: captcha.value.captchaId,
      captchaValue: Math.round(pieceX.value / imageScale.value),
    })
    ElMessage.success('验证码已发送，请查收邮件')
    startCountdown()
    if (currentStep.value === 1) currentStep.value = 2
  } catch {
    await loadCaptcha()
  } finally {
    sending.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleReset() {
  if (!form.code.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!/^\d{6}$/.test(form.code.trim())) {
    ElMessage.warning('请输入 6 位数字验证码')
    return
  }
  if (!form.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 72) {
    ElMessage.warning('密码长度必须在 8 到 72 个字符之间')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await authApi.resetPassword({
      account: form.account.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword,
    })
    ElMessage.success('密码重置成功')
    currentStep.value = 3
  } catch {
    // 错误已由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push('/login')
}

onMounted(loadCaptcha)

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.reset-page {
  --reset-control-radius: 14px;
  --reset-pill-radius: 999px;
  position: relative;
  display: flex;
  min-height: 100svh;
  overflow: hidden;
  background: var(--oa-page-bg);
  color: var(--oa-text);
}

.reset-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 52%;
  min-width: 0;
  overflow: hidden;
  color: #fff;
  background: #111113;
}

.reset-aside__map {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.reset-aside::after {
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

.reset-aside__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
  padding: 48px 56px;
}

.reset-aside__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: auto;
}

.reset-aside__logo {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 8px;
  backdrop-filter: blur(6px);
}

.reset-aside__logo img {
  width: 30px;
  height: 30px;
  border-radius: 6px;
}

.reset-aside__badge {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.86);
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.38);
}

.reset-aside__hero {
  width: min(560px, 100%);
  margin: 0;
  padding-bottom: 0;
}

.reset-aside__eyebrow {
  margin: 0 0 14px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 600;
}

.reset-aside__title {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: 48px;
  font-weight: 700;
  line-height: 1.12;
  color: #fff;
  text-shadow: 0 10px 32px rgba(0, 0, 0, 0.42);
}

.reset-aside__name {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 30px;
  font-weight: 600;
  line-height: 1.2;
}

.reset-aside__tagline {
  margin: 18px 0 0;
  max-width: 360px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 16px;
  line-height: 1.7;
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.36);
}

.reset-aside__steps {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 32px 0 0;
}

.reset-aside__step {
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

.reset-aside__step-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
}

.reset-aside__step-dot.active {
  background: #f7f7f8;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.14);
}

.reset-aside__step-dot.done {
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.18);
}

.reset-aside__footer {
  margin-top: auto;
  padding-top: 24px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.3);
}

.reset-main {
  position: relative;
  z-index: 2;
  display: flex;
  width: 48%;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  background: var(--oa-page-bg);
}

.reset-main::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 1px;
  background: var(--oa-divider);
  opacity: 0.74;
}

.reset-form-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
}

.reset-back-link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  margin-bottom: 18px;
  color: var(--oa-muted);
  border-radius: 10px;
  text-decoration: none;
}

.reset-back-link:hover,
.reset-back-link:focus-visible {
  color: var(--oa-text);
  outline: 2px solid var(--oa-border-strong);
  outline-offset: 4px;
}

.reset-form-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.reset-form-brand__logo {
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

.reset-form-brand__logo img {
  width: 28px;
  height: 28px;
  border-radius: 7px;
}

.reset-form-brand__copy {
  display: grid;
  gap: 2px;
}

.reset-form-brand__copy strong {
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.reset-form-brand__copy small {
  color: var(--oa-muted);
  font-size: 12px;
}

.reset-form-header {
  margin-bottom: 26px;
}

.reset-form-header h2 {
  margin: 0;
  font-family: var(--font-family-display);
  font-size: 32px;
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: 0.02em;
  color: var(--oa-text);
}

.reset-form-header > p {
  margin: 9px 0 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--oa-muted);
}

.reset-form-header > p strong {
  color: var(--oa-text-soft);
  font-weight: 600;
}

.reset-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reset-captcha {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reset-captcha__stage {
  position: relative;
  width: 100%;
  max-width: 280px;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 12px;
}

.reset-captcha__bg {
  display: block;
  width: 100%;
  height: auto;
}

.reset-captcha__piece {
  position: absolute;
  pointer-events: none;
  user-select: none;
  -webkit-user-drag: none;
}

.reset-captcha__refresh {
  position: absolute;
  top: 8px;
  right: 8px;
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--oa-muted);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.reset-captcha__refresh:hover {
  color: var(--oa-text);
}

.reset-captcha__slider {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 280px;
  height: 44px;
  border: 1px solid var(--oa-border);
  border-radius: var(--reset-control-radius);
  background: var(--oa-button-bg);
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.reset-captcha__slider.is-dragging {
  cursor: grabbing;
}

.reset-captcha__slider-hint {
  position: absolute;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 13px;
  color: var(--oa-faint);
  pointer-events: none;
}

.reset-captcha__slider-knob {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  display: inline-flex;
  width: 44px;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  background: var(--oa-text);
  color: var(--oa-active-text);
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
}

.reset-captcha__empty {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 280px;
  min-height: 44px;
}

.reset-captcha__empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px dashed var(--oa-border-strong);
  border-radius: 12px;
  background: transparent;
  color: var(--oa-muted);
  font-size: 13px;
  cursor: pointer;
}

.reset-captcha__empty-btn:hover {
  color: var(--oa-text);
  border-color: var(--oa-text);
}

.reset-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reset-label {
  color: var(--oa-text-soft);
  font-size: 14px;
  font-weight: 600;
}

.reset-control {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.reset-control__icon {
  position: absolute;
  left: 14px;
  z-index: 1;
  color: var(--oa-faint);
  pointer-events: none;
}

.reset-input {
  width: 100%;
  min-height: 48px;
  padding: 0 16px 0 44px;
  border: 1px solid var(--oa-border);
  border-radius: var(--reset-control-radius);
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

.reset-input--code {
  padding-right: 108px;
}

.reset-input--password {
  padding-right: 48px;
}

.reset-input::placeholder {
  color: var(--oa-faint);
}

.reset-input:focus {
  border-color: var(--oa-text);
  background: var(--oa-elevated-bg);
  box-shadow: 0 0 0 3px var(--oa-focus-ring);
}

.reset-input:hover:not(:focus) {
  border-color: var(--oa-border-strong);
}

.reset-field__toggle {
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

.reset-field__toggle:hover {
  color: var(--oa-text);
  background: var(--oa-button-subtle-bg);
}

.reset-resend {
  position: absolute;
  right: 8px;
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  padding: 0 10px;
  border: none;
  border-radius: 9px;
  background: var(--oa-button-subtle-bg);
  color: var(--oa-text-soft);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease,
    opacity 0.18s ease;
}

.reset-resend:hover:not(:disabled) {
  color: var(--oa-text);
  background: var(--oa-border);
}

.reset-resend:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.reset-submit {
  display: inline-flex;
  width: 100%;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border: none;
  border-radius: var(--reset-pill-radius);
  background: var(--oa-text);
  color: var(--oa-active-text);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    opacity 0.18s ease;
}

.reset-submit:hover:not(:disabled) {
  box-shadow: 0 6px 20px var(--oa-focus-ring);
}

.reset-submit:focus-visible {
  outline: 2px solid var(--oa-text);
  outline-offset: 3px;
}

.reset-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reset-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--oa-active-text);
  border-top-color: transparent;
  border-radius: 50%;
  animation: reset-spin 0.7s linear infinite;
}

@keyframes reset-spin {
  to {
    transform: rotate(360deg);
  }
}

.reset-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 12px 0;
  text-align: center;
}

.reset-success__icon {
  display: grid;
  width: 68px;
  height: 68px;
  place-items: center;
  border-radius: 50%;
  background: rgba(52, 211, 153, 0.12);
  color: #34d399;
}

.reset-success .reset-form-header {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .reset-aside {
    display: none;
  }

  .reset-main {
    width: 100%;
    padding: 32px 24px;
  }
}
</style>