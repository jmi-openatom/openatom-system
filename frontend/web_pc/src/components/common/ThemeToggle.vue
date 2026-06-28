<template>
  <el-button
      :aria-label="tooltip"
      class="theme-toggle"
      @click="toggleTheme"
      style="border-radius: 50%;"
  >
    <el-icon :size="10">
      <component :is="themeIcon" />
    </el-icon>
  </el-button>
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
}
</style>
