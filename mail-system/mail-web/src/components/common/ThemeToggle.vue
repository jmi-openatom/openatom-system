<template>
  <button class="icon-button" type="button" :aria-label="theme === 'light' ? '切换深色模式' : '切换浅色模式'" @click="toggleTheme">
    <Moon v-if="theme === 'light'" :size="18" />
    <Sun v-else :size="18" />
  </button>
</template>

<script lang="ts" setup>
import { Moon, Sun } from 'lucide-vue-next'
import { useUiStore } from '../../stores/ui'

const { theme, toggleTheme } = useUiStore()
</script>
