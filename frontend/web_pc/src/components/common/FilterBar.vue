<template>
  <section :aria-label="label" class="filter-bar" role="search">
    <div class="toolbar__filters">
      <slot />
    </div>
    <div v-if="$slots.actions" class="toolbar__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
  }>(),
  {
    label: '筛选条件',
  },
)
</script>
