<template>
  <main class="qr-screen">
    <div class="qr-screen__chrome" aria-hidden="true">
      <span></span>
      <span></span>
      <span></span>
    </div>
    <section class="qr-panel" aria-live="polite">
      <div class="qr-copy">
        <span class="qr-kicker">OpenAtom QR System</span>
        <h1 :style="titleStyle">{{ titleText }}</h1>
        <p>{{ subtitleText }}</p>
        <div class="qr-status">
          <span>{{ targetUrl ? 'READY' : 'WAITING' }}</span>
          <strong>{{ targetUrl ? '二维码已生成' : '等待 URL 参数' }}</strong>
        </div>
      </div>

      <div class="qr-frame">
        <div class="qr-frame__inner">
          <img v-if="targetUrl" :src="qrUrl" alt="二维码" class="qr-image" />
          <div v-else class="qr-empty">
            <span>QR</span>
            <strong>请追加 ?url=</strong>
          </div>
        </div>
      </div>

      <p class="qr-url">{{ targetUrl || '请在地址栏追加 ?url=目标地址' }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { qrSvgDataUrl } from '@/utils/qr.ts'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const targetUrl = computed(() => normalizeQueryValue(route.query.url))
const titleText = computed(() => normalizeQueryValue(route.query.title) || '扫码填写表单')
const subtitleText = computed(() => normalizeQueryValue(route.query.subtitle) || 'OpenAtom 在线二维码')
const qrUrl = computed(() => (targetUrl.value ? qrSvgDataUrl(targetUrl.value) : ''))
const titleStyle = computed(() => {
  const length = titleText.value.length
  const size = Math.max(30, Math.min(76, 92 - length * 1.45))
  return {
    fontSize: `${size}px`,
  }
})

function normalizeQueryValue(value: unknown) {
  if (Array.isArray(value)) return String(value[0] || '').trim()
  return String(value || '').trim()
}
</script>

<style scoped>
.qr-screen {
  position: relative;
  display: grid;
  min-height: 100vh;
  min-height: 100svh;
  place-items: center;
  overflow: hidden;
  padding: 44px;
  background:
    linear-gradient(rgba(29, 29, 31, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(29, 29, 31, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, #fbfbfd 0%, #f5f5f7 52%, #ffffff 100%);
  background-size:
    44px 44px,
    44px 44px,
    auto;
  color: #1d1d1f;
}

.qr-screen::before {
  position: absolute;
  inset: 0;
  content: '';
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.92), transparent 36%, rgba(255, 255, 255, 0.76)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.44), rgba(245, 245, 247, 0.86));
  pointer-events: none;
}

.qr-screen__chrome {
  position: absolute;
  inset: 24px;
  display: flex;
  justify-content: space-between;
  border: 1px solid rgba(29, 29, 31, 0.1);
  pointer-events: none;
}

.qr-screen__chrome span {
  width: 7px;
  height: 7px;
  background: #1d1d1f;
}

.qr-panel {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr;
  justify-items: center;
  gap: 24px;
  width: min(1280px, 100%);
}

.qr-copy {
  display: grid;
  justify-items: center;
  gap: 14px;
  width: 100%;
  min-width: 0;
  text-align: center;
}

.qr-kicker,
.qr-copy p {
  margin: 0;
  color: #6e6e73;
  font-size: 22px;
  font-weight: 500;
  letter-spacing: 0;
}

.qr-kicker {
  display: inline-flex;
  width: fit-content;
  min-height: 36px;
  align-items: center;
  padding: 0 14px;
  color: #ffffff;
  background: #1d1d1f;
  border-radius: 999px;
  font-size: 13px;
  line-height: 1;
}

.qr-copy h1 {
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.05;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-status {
  display: flex;
  width: fit-content;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  padding: 6px 14px 6px 6px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(29, 29, 31, 0.1);
  border-radius: 999px;
}

.qr-status span {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  padding: 0 10px;
  color: #ffffff;
  background: #1d1d1f;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.qr-status strong {
  color: #333333;
  font-size: 16px;
  font-weight: 500;
}

.qr-frame {
  position: relative;
  display: grid;
  aspect-ratio: 1;
  place-items: center;
  width: min(44vh, 520px);
  min-width: 360px;
  padding: 20px;
  border: 1px solid rgba(29, 29, 31, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.72);
}

.qr-frame::before,
.qr-frame::after {
  position: absolute;
  inset: 18px;
  content: '';
  border: 1px solid rgba(29, 29, 31, 0.1);
  border-radius: 20px;
  pointer-events: none;
}

.qr-frame::after {
  inset: -10px;
  border-radius: 34px;
  opacity: 0.42;
}

.qr-frame__inner {
  display: grid;
  aspect-ratio: 1;
  width: 100%;
  place-items: center;
  padding: 38px;
  background: #ffffff;
  border-radius: 18px;
}

.qr-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-empty {
  display: grid;
  place-items: center;
  gap: 10px;
  color: #7a7a7a;
  font-size: 20px;
  font-weight: 500;
}

.qr-empty span {
  color: #1d1d1f;
  font-size: 88px;
  font-weight: 700;
  line-height: 1;
}

.qr-empty strong {
  color: #333333;
  font-size: 18px;
  font-weight: 600;
}

.qr-url {
  grid-column: 1 / -1;
  max-width: 100%;
  margin: 2px 0 0;
  overflow: hidden;
  padding: 14px 18px;
  color: #6e6e73;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(29, 29, 31, 0.1);
  border-radius: 999px;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0;
  line-height: 1.25;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 860px) {
  .qr-screen {
    padding: 28px 16px;
    overflow: auto;
  }

  .qr-panel {
    grid-template-columns: 1fr;
    justify-items: center;
    width: 100%;
    text-align: center;
  }

  .qr-copy h1 {
    white-space: normal;
  }

  .qr-frame {
    width: min(100%, 460px);
    min-width: 0;
  }

  .qr-screen__chrome {
    inset: 14px;
  }
}
</style>
