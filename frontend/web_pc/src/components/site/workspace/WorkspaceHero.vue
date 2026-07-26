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

      <div class="workspace-hero__console site-reveal" aria-label="工作台概览">
        <div class="workspace-hero__console-head">
          <small>WORKSPACE INDEX</small>
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
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  border-bottom: 1px solid var(--oa-border);
}

.workspace-hero__inner {
  display: grid;
  min-height: clamp(340px, 38vh, 420px);
  grid-template-columns: minmax(280px, 0.9fr) minmax(380px, 1.1fr);
  align-items: stretch;
  gap: 0;
}

.workspace-hero__copy {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  flex-direction: column;
  padding: clamp(48px, 7vw, 80px) clamp(36px, 5vw, 64px) clamp(48px, 7vw, 80px) 0;
}

.workspace-hero__copy > span {
  color: var(--oa-muted-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.workspace-hero__copy h1 {
  margin: 12px 0 14px;
  color: var(--oa-text);
  font-size: clamp(34px, 4vw, 50px);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: 0;
}

.workspace-hero__copy p {
  max-width: 520px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 16px;
  line-height: 1.72;
}

.workspace-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.workspace-hero__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.workspace-hero__console {
  display: grid;
  grid-template-columns: minmax(150px, 0.72fr) minmax(0, 1.28fr);
  gap: 0;
  border-left: 1px solid var(--oa-border);
}

.workspace-hero__console-head {
  display: grid;
  align-content: end;
  gap: 8px;
  padding: 28px;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
}

.workspace-hero__console-head small {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.workspace-hero__console-head strong {
  font-size: clamp(38px, 5vw, 58px);
  font-weight: 650;
  line-height: 1;
}

.workspace-hero__console-head span {
  font-size: 13px;
  opacity: 0.72;
}

.workspace-hero__metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.workspace-hero__metric-grid article {
  display: grid;
  min-height: 120px;
  align-content: center;
  gap: 7px;
  padding: 20px;
  border-left: 1px solid var(--oa-border);
  border-bottom: 1px solid var(--oa-border);
}

.workspace-hero__metric-grid article:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.workspace-hero__metric-grid span,
.workspace-hero__metric-grid small {
  color: var(--oa-muted);
  font-size: 12px;
}

.workspace-hero__metric-grid strong {
  color: var(--oa-text);
  font-size: 24px;
  font-weight: 650;
}

@media (max-width: 940px) {
  .workspace-hero__inner {
    grid-template-columns: 1fr;
  }

  .workspace-hero__copy {
    padding-right: 0;
  }

  .workspace-hero__console {
    border-top: 1px solid var(--oa-border);
    border-left: 0;
  }
}

@media (max-width: 640px) {
  .workspace-hero__console {
    grid-template-columns: 1fr;
  }

  .workspace-hero__console-head {
    min-height: 180px;
  }

  .workspace-hero__metric-grid article:nth-last-child(-n + 2) {
    border-bottom: 1px solid var(--oa-border);
  }

  .workspace-hero__metric-grid article:nth-child(even) {
    border-right: 0;
  }
}
</style>
