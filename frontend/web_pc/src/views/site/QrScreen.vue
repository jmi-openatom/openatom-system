<template>
  <main class="qr-screen">
    <section class="qr-panel" aria-live="polite">
      <div class="qr-copy">
        <p>{{ subtitleText }}</p>
        <h1>{{ titleText }}</h1>
      </div>

      <div class="qr-frame">
        <img v-if="targetUrl" :src="qrUrl" alt="二维码" class="qr-image" />
        <div v-else class="qr-empty">等待 URL</div>
      </div>

      <p class="qr-url">{{ targetUrl || '请在地址栏追加 ?url=' }}</p>
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

function normalizeQueryValue(value: unknown) {
  if (Array.isArray(value)) return String(value[0] || '').trim()
  return String(value || '').trim()
}
</script>

<style scoped>
.qr-screen {
  display: grid;
  min-height: 100vh;
  min-height: 100svh;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 18%, rgba(0, 102, 204, 0.14), transparent 30%),
    linear-gradient(135deg, #f8fafc 0%, #eef2f7 44%, #ffffff 100%);
  color: #0f172a;
}

.qr-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 42vw);
  gap: clamp(32px, 6vw, 88px);
  align-items: center;
  width: min(1180px, calc(100vw - 56px));
}

.qr-copy {
  min-width: 0;
}

.qr-copy p {
  margin: 0 0 18px;
  color: #2563eb;
  font-size: clamp(20px, 2vw, 34px);
  font-weight: 760;
  letter-spacing: 0;
}

.qr-copy h1 {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: clamp(56px, 8vw, 132px);
  font-weight: 860;
  letter-spacing: 0;
  line-height: 0.98;
}

.qr-frame {
  display: grid;
  aspect-ratio: 1;
  place-items: center;
  width: min(42vw, 520px);
  min-width: 320px;
  padding: clamp(18px, 2.8vw, 34px);
  border: 1px solid rgba(148, 163, 184, 0.48);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.16);
}

.qr-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-empty {
  color: #64748b;
  font-size: 28px;
  font-weight: 700;
}

.qr-url {
  grid-column: 1 / -1;
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  color: #475569;
  font-size: clamp(18px, 1.7vw, 28px);
  font-weight: 650;
  letter-spacing: 0;
  line-height: 1.25;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 860px) {
  .qr-screen {
    padding: 28px 0;
    overflow: auto;
  }

  .qr-panel {
    grid-template-columns: 1fr;
    justify-items: center;
    width: min(100%, calc(100vw - 32px));
    text-align: center;
  }

  .qr-frame {
    width: min(100%, 460px);
    min-width: 0;
  }
}
</style>
