<template>
  <section class="workspace-panel" :class="{ 'workspace-panel--compact': compact }">
    <header
      v-if="eyebrow || title || description || $slots.actions"
      class="workspace-panel__header"
    >
      <div>
        <span v-if="eyebrow">{{ eyebrow }}</span>
        <h2 v-if="title">{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="workspace-panel__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="workspace-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title?: string
    description?: string
    compact?: boolean
  }>(),
  {
    eyebrow: '',
    title: '',
    description: '',
    compact: false,
  },
)
</script>

<style scoped>
.workspace-panel {
  position: relative;
  overflow: hidden;
  min-width: 0;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-elevated-bg);
}

.workspace-panel::before {
  display: none;
}

.workspace-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-5);
  padding: var(--space-6) var(--space-6) 0;
}

.workspace-panel__header span {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--oa-muted);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.workspace-panel__header h2 {
  margin: 0;
  color: var(--oa-text);
  font-family: var(--font-family-display);
  font-size: clamp(20px, 2vw, 26px);
  font-weight: 600;
}

.workspace-panel__header p {
  max-width: 560px;
  margin: 10px 0 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.workspace-panel__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}

.workspace-panel__body {
  padding: var(--space-6);
}

.workspace-panel--compact .workspace-panel__body {
  padding-top: 18px;
}

@media (max-width: 720px) {
  .workspace-panel__header {
    flex-direction: column;
  }

  .workspace-panel__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
