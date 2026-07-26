<template>
  <section :aria-busy="kind === 'loading'" :role="role" class="state-panel">
    <slot name="eyebrow" />
    <div aria-hidden="true" class="state-panel__icon">
      <el-icon :class="{ 'is-loading': kind === 'loading' }">
        <Loading v-if="kind === 'loading'" />
        <WarningFilled v-else-if="kind === 'error'" />
        <Box v-else />
      </el-icon>
    </div>
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.actions" class="page-actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Box, Loading, WarningFilled } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    kind?: 'empty' | 'loading' | 'error'
    title: string
    description?: string
  }>(),
  {
    kind: 'empty',
    description: '',
  },
)

const role = computed(() => (props.kind === 'error' ? 'alert' : 'status'))
</script>
