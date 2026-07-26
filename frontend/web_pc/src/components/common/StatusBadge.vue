<template>
  <span :class="`status-badge--${tone}`" class="status-badge">
    <span v-if="dot" aria-hidden="true" class="status-badge__dot" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    tone?: 'neutral' | 'success' | 'warning' | 'danger'
    dot?: boolean
  }>(),
  {
    label: '',
    tone: 'neutral',
    dot: false,
  },
)
</script>

<style scoped>
.status-badge__dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  background: currentColor;
  border-radius: 50%;
}
</style>
