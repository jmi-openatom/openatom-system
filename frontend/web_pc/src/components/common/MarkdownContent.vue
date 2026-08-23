<template>
  <MarkdownRender
    class="markdown-content"
    :content="content"
    :mode="mode"
    :final="true"
    :smooth-streaming="false"
    :fade="false"
    :typewriter="false"
    :is-dark="isDark"
  />
</template>

<script setup lang="ts">
import { MarkdownRender } from 'markstream-vue'
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'

const props = withDefaults(
  defineProps<{
    content?: string
    mode?: 'docs' | 'chat' | 'minimal'
  }>(),
  {
    content: '',
    mode: 'docs',
  },
)

const { resolvedTheme } = useTheme()
const isDark = computed(() => resolvedTheme.value === 'dark')

defineExpose({
  content: () => props.content,
})
</script>

<style scoped>
.markdown-content {
  min-width: 0;
  color: var(--oa-muted);
  overflow-wrap: anywhere;
}

.markdown-content :deep(.markdown-renderer > :first-child) {
  margin-top: 0;
}

.markdown-content :deep(.link-node),
.markdown-content :deep(a) {
  color: var(--oa-primary);
  text-underline-offset: 3px;
}

.markdown-content :deep(img) {
  border-radius: 14px;
}
</style>