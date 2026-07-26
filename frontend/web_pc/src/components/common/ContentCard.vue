<template>
  <component :is="as" class="content-card">
    <header v-if="title || description || $slots.actions" class="content-card__header">
      <div>
        <h2 v-if="title">{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="page-actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="content-card__body">
      <slot />
    </div>
  </component>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    as?: 'article' | 'section' | 'div'
    title?: string
    description?: string
  }>(),
  {
    as: 'section',
    title: '',
    description: '',
  },
)
</script>

<style scoped>
.content-card {
  overflow: hidden;
  padding: 0;
}

.content-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6) var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
}

.content-card__header h2,
.content-card__header p {
  margin: 0;
}

.content-card__header h2 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.content-card__header p {
  margin-top: var(--space-1);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.content-card__body {
  min-width: 0;
  padding: var(--space-6);
}
</style>
