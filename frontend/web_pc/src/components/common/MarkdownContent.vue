<template>
  <article class="markdown-content" v-html="html"></article>
</template>

<script setup lang="ts">
import { renderMarkdown } from '@/utils/markdown.ts'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    content?: string
  }>(),
  {
    content: '',
  },
)

const html = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.markdown-content {
  min-width: 0;
  color: var(--oa-muted);
  overflow-wrap: anywhere;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.35em 0 0.65em;
  color: var(--oa-text);
  line-height: 1.35;
}

.markdown-content :deep(h1:first-child),
.markdown-content :deep(h2:first-child),
.markdown-content :deep(h3:first-child),
.markdown-content :deep(h4:first-child),
.markdown-content :deep(h5:first-child),
.markdown-content :deep(h6:first-child),
.markdown-content :deep(p:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p),
.markdown-content :deep(li) {
  line-height: 1.85;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 1.5em;
}

.markdown-content :deep(blockquote) {
  margin: 1em 0;
  padding: 10px 16px;
  border-left: 3px solid var(--oa-primary);
  border-radius: 0 10px 10px 0;
  background: var(--oa-page-soft-bg);
}

.markdown-content :deep(blockquote p) {
  margin: 0;
}

.markdown-content :deep(code) {
  padding: 2px 6px;
  border-radius: 5px;
  color: var(--oa-text);
  background: var(--oa-button-subtle-bg);
}

.markdown-content :deep(pre) {
  max-width: 100%;
  padding: 16px;
  border-radius: 12px;
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  overflow-x: auto;
}

.markdown-content :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-content :deep(a) {
  color: var(--oa-primary);
  text-underline-offset: 3px;
}

.markdown-content :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 18px auto;
  border-radius: 14px;
}

.markdown-content :deep(table) {
  display: block;
  width: 100%;
  border-collapse: collapse;
  overflow-x: auto;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 10px 12px;
  border: 1px solid var(--oa-border);
  text-align: left;
}

.markdown-content :deep(hr) {
  margin: 24px 0;
  border: 0;
  border-top: 1px solid var(--oa-border);
}
</style>
