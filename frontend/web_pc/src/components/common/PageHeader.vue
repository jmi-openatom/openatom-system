<template>
  <header :aria-labelledby="titleId" class="page-header">
    <div class="page-header__copy">
      <slot name="breadcrumb" />
      <span v-if="eyebrow" class="page-header__eyebrow">{{ eyebrow }}</span>
      <h1 :id="titleId" class="page-header__title">{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { useId } from 'vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    eyebrow?: string
  }>(),
  {
    description: '',
    eyebrow: '',
  },
)

const titleId = useId()
</script>
