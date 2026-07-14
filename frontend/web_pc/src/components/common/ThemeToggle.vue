<template>
  <el-button
    :aria-label="tooltip"
    :icon="themeIcon"
    :title="tooltip"
    circle
    class="theme-toggle"
    @click="toggleTheme"
  />
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'

const { isDark, toggleTheme } = useTheme()

const tooltip = computed(() => (isDark.value ? '切换到浅色模式' : '切换到暗黑模式'))
const themeIcon = computed(() => (isDark.value ? Sunny : Moon))
</script>

<style scoped>
.theme-toggle {
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  min-height: 40px;
  aspect-ratio: 1;
  padding: 0;
}
</style>
