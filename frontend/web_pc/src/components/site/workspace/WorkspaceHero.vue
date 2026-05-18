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
  --workspace-hero-text: #0f172a;
  --workspace-hero-muted: rgba(15, 23, 42, 0.68);
  --workspace-hero-soft: rgba(15, 23, 42, 0.54);
  --workspace-hero-grid: rgba(15, 23, 42, 0.05);
  --workspace-hero-orb: rgba(56, 189, 248, 0.14);
  --workspace-hero-chip-border: rgba(15, 23, 42, 0.12);
  --workspace-hero-chip-bg: rgba(255, 255, 255, 0.72);
  --workspace-hero-panel-border: rgba(15, 23, 42, 0.1);
  --workspace-hero-panel-bg: rgba(255, 255, 255, 0.7);
  --workspace-hero-tile-border: rgba(15, 23, 42, 0.08);
  --workspace-hero-tile-bg: rgba(255, 255, 255, 0.62);
  --workspace-hero-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 24px 80px rgba(15, 23, 42, 0.12);
  --workspace-hero-solid-button-border: #0f172a;
  --workspace-hero-solid-button-bg: #0f172a;
  --workspace-hero-solid-button-text: #ffffff;
  --workspace-hero-solid-button-hover-border: #020617;
  --workspace-hero-solid-button-hover-bg: #020617;
  --workspace-hero-solid-button-hover-text: #ffffff;
  --workspace-hero-plain-button-border: rgba(15, 23, 42, 0.14);
  --workspace-hero-plain-button-bg: rgba(255, 255, 255, 0.54);
  --workspace-hero-plain-button-text: #0f172a;
  --workspace-hero-plain-button-hover-border: rgba(15, 23, 42, 0.26);
  --workspace-hero-plain-button-hover-bg: rgba(255, 255, 255, 0.86);
  --workspace-hero-plain-button-hover-text: #020617;
  position: relative;
  overflow: hidden;
  color: var(--workspace-hero-text);
  background:
    radial-gradient(circle at 15% 16%, rgba(14, 165, 233, 0.18), transparent 28%),
    radial-gradient(circle at 84% 8%, rgba(34, 197, 94, 0.12), transparent 26%),
    linear-gradient(135deg, #f8fbff 0%, #edf5ff 48%, #f7fbff 100%);
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
    linear-gradient(var(--workspace-hero-grid) 1px, transparent 1px),
    linear-gradient(90deg, var(--workspace-hero-grid) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.94), transparent);
}

.workspace-hero::after {
  right: -120px;
  bottom: -180px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: var(--workspace-hero-orb);
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
  border: 1px solid var(--workspace-hero-chip-border);
  border-radius: 999px;
  color: var(--workspace-hero-muted);
  background: var(--workspace-hero-chip-bg);
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
  color: var(--workspace-hero-muted);
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
  border: 1px solid var(--workspace-hero-panel-border);
  border-radius: 30px;
  background: var(--workspace-hero-panel-bg);
  box-shadow: var(--workspace-hero-shadow);
  backdrop-filter: blur(18px);
}

.workspace-hero__console-head {
  display: grid;
  gap: 6px;
  padding: 4px 4px 0;
}

.workspace-hero__console-head small {
  color: var(--workspace-hero-soft);
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
  color: var(--workspace-hero-muted);
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
  border: 1px solid var(--workspace-hero-tile-border);
  border-radius: 18px;
  background: var(--workspace-hero-tile-bg);
}

.workspace-hero__metric-grid span,
.workspace-hero__metric-grid small {
  color: var(--workspace-hero-soft);
}

.workspace-hero__metric-grid span {
  font-size: 13px;
}

.workspace-hero__metric-grid strong {
  color: var(--workspace-hero-text);
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
  border-color: var(--workspace-hero-solid-button-border);
  color: var(--workspace-hero-solid-button-text);
  background: var(--workspace-hero-solid-button-bg);
}

:global(.workspace-hero .el-button:not(.is-plain):hover) {
  border-color: var(--workspace-hero-solid-button-hover-border);
  color: var(--workspace-hero-solid-button-hover-text);
  background: var(--workspace-hero-solid-button-hover-bg);
}

:global(.workspace-hero .el-button.is-plain) {
  border-color: var(--workspace-hero-plain-button-border);
  color: var(--workspace-hero-plain-button-text);
  background: var(--workspace-hero-plain-button-bg);
}

:global(.workspace-hero .el-button.is-plain:hover) {
  border-color: var(--workspace-hero-plain-button-hover-border);
  color: var(--workspace-hero-plain-button-hover-text);
  background: var(--workspace-hero-plain-button-hover-bg);
}

:global(html.dark) .workspace-hero {
  --workspace-hero-text: #ffffff;
  --workspace-hero-muted: rgba(255, 255, 255, 0.72);
  --workspace-hero-soft: rgba(255, 255, 255, 0.62);
  --workspace-hero-grid: rgba(255, 255, 255, 0.035);
  --workspace-hero-orb: rgba(255, 255, 255, 0.08);
  --workspace-hero-chip-border: rgba(255, 255, 255, 0.18);
  --workspace-hero-chip-bg: rgba(255, 255, 255, 0.05);
  --workspace-hero-panel-border: rgba(255, 255, 255, 0.16);
  --workspace-hero-panel-bg: rgba(255, 255, 255, 0.08);
  --workspace-hero-tile-border: rgba(255, 255, 255, 0.12);
  --workspace-hero-tile-bg: rgba(255, 255, 255, 0.055);
  --workspace-hero-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 24px 80px rgba(0, 0, 0, 0.28);
  --workspace-hero-solid-button-border: #ffffff;
  --workspace-hero-solid-button-bg: #ffffff;
  --workspace-hero-solid-button-text: #111827;
  --workspace-hero-solid-button-hover-border: #ffffff;
  --workspace-hero-solid-button-hover-bg: rgba(255, 255, 255, 0.92);
  --workspace-hero-solid-button-hover-text: #05070b;
  --workspace-hero-plain-button-border: rgba(255, 255, 255, 0.24);
  --workspace-hero-plain-button-bg: rgba(255, 255, 255, 0.06);
  --workspace-hero-plain-button-text: #ffffff;
  --workspace-hero-plain-button-hover-border: rgba(255, 255, 255, 0.4);
  --workspace-hero-plain-button-hover-bg: rgba(255, 255, 255, 0.12);
  --workspace-hero-plain-button-hover-text: #ffffff;
  background:
    radial-gradient(circle at 15% 16%, rgba(34, 211, 238, 0.22), transparent 28%),
    radial-gradient(circle at 84% 8%, rgba(34, 197, 94, 0.14), transparent 26%),
    linear-gradient(135deg, #0a0b0f 0%, #111827 46%, #05070b 100%);
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
