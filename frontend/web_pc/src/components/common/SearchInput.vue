<template>
  <el-input
    v-model="model"
    :aria-label="ariaLabel"
    :clearable="clearable"
    :placeholder="placeholder"
    class="search-input"
  >
    <template #prefix>
      <el-icon aria-hidden="true"><Search /></el-icon>
    </template>
  </el-input>
</template>

<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    placeholder?: string
    ariaLabel?: string
    clearable?: boolean
  }>(),
  {
    placeholder: '搜索',
    ariaLabel: '搜索',
    clearable: true,
  },
)

const model = defineModel<string>({ default: '' })
</script>

<style scoped>
.search-input {
  width: min(280px, 100%);
}
</style>
