<template>
  <article ref="contentRef" class="markdown-content" v-html="html"></article>
</template>

<script setup lang="ts">
import { renderMarkdown } from '@/utils/markdown.ts'
import { computed, nextTick, onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    content?: string
  }>(),
  {
    content: '',
  },
)

const html = computed(() => renderMarkdown(props.content))
const contentRef = ref<HTMLElement>()
let renderSequence = 0

async function renderMermaid() {
  await nextTick()
  const root = contentRef.value
  if (!root) return
  const codeBlocks = Array.from(root.querySelectorAll<HTMLElement>('pre > code.language-mermaid'))
  if (!codeBlocks.length) return

  const { default: mermaid } = await import('mermaid')
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    theme: 'base',
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
    themeVariables: {
      fontFamily:
        '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
      textColor: '#1d1d1f',
      primaryTextColor: '#1d1d1f',
      secondaryTextColor: '#1d1d1f',
      tertiaryTextColor: '#1d1d1f',
      primaryColor: '#eef3ff',
      primaryBorderColor: '#6687ff',
      lineColor: '#6b7280',
    },
    flowchart: {
      htmlLabels: false,
      useMaxWidth: true,
    },
  })

  for (const codeBlock of codeBlocks) {
    const pre = codeBlock.parentElement
    if (!pre) continue
    const diagram = document.createElement('div')
    diagram.className = 'mermaid-diagram'
    pre.replaceWith(diagram)
    try {
      renderSequence += 1
      const result = await mermaid.render(
        `oa-mermaid-${renderSequence}`,
        codeBlock.textContent || '',
      )
      diagram.innerHTML = result.svg
      result.bindFunctions?.(diagram)
    } catch (_error) {
      diagram.classList.add('is-error')
      diagram.textContent = 'Mermaid 图表语法有误，请检查后重试。'
    }
  }
}

watch(
  () => props.content,
  () => renderMermaid(),
  { flush: 'post' },
)

onMounted(renderMermaid)
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
  scroll-margin-top: calc(var(--oa-site-header-height) + 24px);
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

.markdown-content :deep(.mermaid-diagram) {
  margin: 24px 0;
  padding: 20px;
  border: 1px solid var(--oa-border);
  border-radius: 16px;
  background: #ffffff;
  overflow-x: auto;
  text-align: center;
}

.markdown-content :deep(.mermaid-diagram svg) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
}

.markdown-content :deep(.mermaid-diagram text),
.markdown-content :deep(.mermaid-diagram .label text),
.markdown-content :deep(.mermaid-diagram .nodeLabel) {
  fill: #1d1d1f !important;
  color: #1d1d1f !important;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif !important;
  font-size: 14px !important;
  opacity: 1 !important;
}

.markdown-content :deep(.mermaid-diagram foreignObject div),
.markdown-content :deep(.mermaid-diagram foreignObject span) {
  color: #1d1d1f !important;
  -webkit-text-fill-color: #1d1d1f !important;
  opacity: 1 !important;
}

.markdown-content :deep(.mermaid-diagram.is-error) {
  color: var(--el-color-danger);
  background: var(--oa-page-soft-bg);
  text-align: left;
}
</style>
