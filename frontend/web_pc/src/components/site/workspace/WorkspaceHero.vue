<template>
  <section class="workspace-hero">
    <div class="container workspace-hero__inner">
      <div class="workspace-hero__copy site-reveal">
        <span>{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p v-if="description">{{ description }}</p>

        <div v-if="$slots.actions" class="workspace-hero__actions">
          <slot name="actions" />
        </div>
      </div>

      <div class="workspace-hero__console site-reveal">
        <div class="workspace-hero__console-head">
          <small>Workspace Console</small>
          <strong>{{ primaryMetric?.value ?? '--' }}</strong>
          <span>{{ primaryMetric?.label || '暂无数据' }}</span>
        </div>

        <div class="workspace-hero__metric-grid">
          <article v-for="item in metrics" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small v-if="item.note">{{ item.note }}</small>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type WorkspaceMetric = {
  label: string
  value: string | number
  note?: string
}

const props = defineProps<{
  eyebrow: string
  title: string
  description?: string
  metrics: WorkspaceMetric[]
}>()

const primaryMetric = computed(() => props.metrics[0])
</script>

<style scoped>
.workspace-hero {
  position: relative;
  overflow: hidden;
  color: #ffffff;
  background:
    radial-gradient(circle at 15% 16%, rgba(34, 211, 238, 0.22), transparent 28%),
    radial-gradient(circle at 84% 8%, rgba(34, 197, 94, 0.14), transparent 26%),
    linear-gradient(135deg, #0a0b0f 0%, #111827 46%, #05070b 100%);
}

.workspace-hero::before,
.workspace-hero::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.workspace-hero::before {
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.94), transparent);
}

.workspace-hero::after {
  right: -120px;
  bottom: -180px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  filter: blur(18px);
}

.workspace-hero__inner {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(280px, 0.82fr) minmax(420px, 1.18fr);
  gap: clamp(28px, 5vw, 72px);
  align-items: center;
  min-height: clamp(360px, 42vh, 460px);
  padding: clamp(42px, 7vw, 76px) 0;
}

.workspace-hero__copy span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.05);
  font-size: 13px;
  backdrop-filter: blur(10px);
}

.workspace-hero__copy h1 {
  margin: 18px 0 16px;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(34px, 4vw, 52px);
  font-weight: 600;
  line-height: 1.02;
}

.workspace-hero__copy p {
  max-width: 460px;
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: clamp(16px, 1.8vw, 19px);
  line-height: 1.8;
}

.workspace-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.workspace-hero__console {
  position: relative;
  display: grid;
  gap: 18px;
  padding: clamp(18px, 3vw, 24px);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 24px 80px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
}

.workspace-hero__console-head {
  display: grid;
  gap: 6px;
  padding: 4px 4px 0;
}

.workspace-hero__console-head small {
  color: rgba(255, 255, 255, 0.54);
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.workspace-hero__console-head strong {
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(34px, 4vw, 46px);
  font-weight: 500;
  line-height: 1;
}

.workspace-hero__console-head span {
  color: rgba(255, 255, 255, 0.76);
  font-size: 14px;
}

.workspace-hero__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.workspace-hero__metric-grid article {
  display: grid;
  gap: 8px;
  min-height: 106px;
  align-content: center;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.055);
}

.workspace-hero__metric-grid span,
.workspace-hero__metric-grid small {
  color: rgba(255, 255, 255, 0.62);
}

.workspace-hero__metric-grid span {
  font-size: 13px;
}

.workspace-hero__metric-grid strong {
  color: #ffffff;
  font-family:
    'SF Pro Display',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
  font-size: clamp(22px, 2vw, 28px);
  font-weight: 500;
  line-height: 1;
}

.workspace-hero__metric-grid small {
  font-size: 12px;
}

:global(.workspace-hero .el-button:not(.is-plain)) {
  border-color: #ffffff;
  color: #111827;
  background: #ffffff;
}

:global(.workspace-hero .el-button:not(.is-plain):hover) {
  border-color: #ffffff;
  color: #05070b;
  background: rgba(255, 255, 255, 0.92);
}

:global(.workspace-hero .el-button.is-plain) {
  border-color: rgba(255, 255, 255, 0.24);
  color: #ffffff;
  background: rgba(255, 255, 255, 0.06);
}

:global(.workspace-hero .el-button.is-plain:hover) {
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

@media (max-width: 980px) {
  .workspace-hero__inner {
    grid-template-columns: 1fr;
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .workspace-hero__inner {
    padding: 36px 0 42px;
  }

  .workspace-hero__metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
