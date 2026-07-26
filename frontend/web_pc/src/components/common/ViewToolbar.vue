<template>
  <header :aria-labelledby="title ? titleId : undefined" class="toolbar page-header">
    <div v-if="title || description" class="toolbar__heading">
      <h2 v-if="title" :id="titleId">{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <slot />
  </header>
</template>

<script setup lang="ts">
import { useId } from 'vue'

const titleId = useId()

withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '',
    description: '',
  },
)
</script>
