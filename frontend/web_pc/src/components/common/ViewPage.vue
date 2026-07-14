<template>
  <div v-loading="loading" :aria-busy="loading" class="view-page" v-bind="$attrs">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  inheritAttrs: false,
})

withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  {
    loading: false,
  },
)
</script>
