<template>
  <MarkdownRender
    class="markdown-content"
    :class="{ 'markdown-content--compact': mode === 'minimal' }"
    :content="content"
    :mode="mode"
    :final="true"
    :smooth-streaming="false"
    :fade="false"
    :typewriter="false"
    :is-dark="isDark"
    :custom-markdown-it="customMarkdownIt"
    :batch-rendering="false"
    :defer-nodes-until-visible="false"
    :viewport-priority="false"
    :max-live-nodes="0"
    :node-virtual="false"
    :code-block-options="codeBlockOptions"
    :code-block-props="codeBlockProps"
    code-block-dark-theme="github-dark"
    code-block-light-theme="github-light"
    html-policy="safe"
  />
</template>

<script lang="ts">
import { setDefaultI18nMap } from 'markstream-vue'

setDefaultI18nMap({
  'common.copy': '复制',
  'common.copied': '已复制',
  'common.expand': '展开',
  'common.collapse': '收起',
  'common.preview': '预览',
  'common.source': '源码',
  'common.export': '导出',
  'common.open': '打开',
  'common.minimize': '退出全屏',
  'common.zoomIn': '放大',
  'common.zoomOut': '缩小',
  'common.resetZoom': '重置缩放',
  'image.loadError': '图片加载失败',
  'image.loading': '图片加载中…',
})
</script>

<script setup lang="ts">
import { MarkdownRender, type CodeBlockOptions } from 'markstream-vue'
import type MarkdownIt from 'markdown-it'
import { computed, useId } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { withMarkdownHeadingIds } from '@/utils/markdown'
import 'katex/dist/katex.min.css'

const props = withDefaults(
  defineProps<{
    content?: string
    mode?: 'docs' | 'chat' | 'minimal'
    headingIdPrefix?: string
  }>(),
  {
    content: '',
    mode: 'docs',
  },
)

const { resolvedTheme } = useTheme()
const isDark = computed(() => resolvedTheme.value === 'dark')
const instanceId = `markdown-${useId()}`
const customMarkdownIt = computed(() => {
  const prefix = props.headingIdPrefix ?? instanceId
  return (md: MarkdownIt) => withMarkdownHeadingIds(md, prefix)
})
const codeBlockOptions: CodeBlockOptions = {
  fontSize: 13,
  lineHeight: 24,
  fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
  padding: 16,
  tabSize: 2,
  maxHeight: 560,
  overflow: 'scroll',
}
const codeBlockProps = {
  showCopyButton: true,
  showExpandButton: false,
  showFontSizeButtons: false,
  showPreviewButton: false,
  showCollapseButton: false,
}

defineExpose({
  content: () => props.content,
})
</script>

<style scoped>
.markdown-content {
  --reader-font-size: 16px;
  min-width: 0;
  max-width: 100%;
  color: var(--color-text-regular);
  font-family:
    'Noto Sans SC',
    -apple-system,
    BlinkMacSystemFont,
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
  font-size: var(--reader-font-size);
  line-height: 1.9;
  overflow-wrap: anywhere;
  text-align: start;
}

.markdown-content--compact {
  --reader-font-size: 14px;
  line-height: 1.8;
}

.markdown-content :deep(.heading-node) {
  color: var(--color-text-primary);
  font-family: inherit;
  font-weight: 700;
  line-height: 1.45;
  letter-spacing: -0.025em;
  text-wrap: pretty;
  scroll-margin-top: calc(var(--oa-site-header-height, 80px) + 28px);
}

.markdown-content :deep(h1.heading-node) {
  margin: 1.65em 0 0.8em;
  font-size: 2em;
}

.markdown-content :deep(h2.heading-node) {
  margin: 2em 0 0.9em;
  padding-bottom: 0.5em;
  border-bottom: 1px solid var(--color-border-light);
  font-size: 1.55em;
}

.markdown-content :deep(h3.heading-node) {
  margin: 1.7em 0 0.75em;
  font-size: 1.25em;
}

.markdown-content :deep(h4.heading-node),
.markdown-content :deep(h5.heading-node),
.markdown-content :deep(h6.heading-node) {
  margin: 1.5em 0 0.65em;
  font-size: 1.05em;
}

.markdown-content :deep(.paragraph-node) {
  margin: 0 0 1.15em;
  color: inherit;
  font-size: inherit;
  line-height: inherit;
}

.markdown-content :deep(.strong-node) {
  color: var(--color-text-primary);
  font-weight: 700;
}

.markdown-content :deep(.emphasis-node) {
  font-style: italic;
}

.markdown-content :deep(.link-node) {
  color: var(--color-text-primary);
  text-decoration: underline;
  text-decoration-color: var(--color-border-strong);
  text-decoration-thickness: 1px;
  text-underline-offset: 4px;
  transition: text-decoration-color 150ms ease;
}

.markdown-content :deep(.link-node:hover) {
  text-decoration-color: currentColor;
}

.markdown-content :deep(a:focus-visible),
.markdown-content :deep(button:focus-visible),
.markdown-content :deep(.heading-node:focus-visible) {
  outline: 2px solid var(--color-text-primary);
  outline-offset: 4px;
}

.markdown-content :deep(.inline-code) {
  padding: 0.15em 0.4em;
  border: 1px solid var(--color-border-light);
  border-radius: 5px;
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
  font-family: var(--font-family-mono);
  font-size: 0.88em;
  white-space: break-spaces;
  box-decoration-break: clone;
  -webkit-box-decoration-break: clone;
}

.markdown-content :deep(.list-node) {
  margin: 0.75em 0 1.25em;
  padding-inline-start: 1.6em;
}

.markdown-content :deep(ul.list-node) {
  list-style-type: disc;
}

.markdown-content :deep(ol.list-node) {
  list-style-type: decimal;
}

.markdown-content :deep(.list-item) {
  margin: 0.35em 0;
  padding-inline-start: 0.2em;
  color: inherit;
  font-size: inherit;
  line-height: inherit;
}

.markdown-content :deep(.list-item::marker) {
  color: var(--color-text-secondary);
}

.markdown-content :deep(.list-item > .paragraph-node) {
  margin-bottom: 0.35em;
}

.markdown-content :deep(.list-item .list-node) {
  margin-block: 0.3em 0.65em;
}

.markdown-content :deep(.blockquote-node) {
  margin: 1.5em 0;
  padding: 16px 20px;
  border: 0;
  border-inline-start: 3px solid var(--color-border-strong);
  border-radius: 0 10px 10px 0;
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
}

.markdown-content :deep(.blockquote-node .paragraph-node:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(.code-block-container),
.markdown-content :deep(.mermaid-block-container) {
  min-width: 0;
  max-width: 100%;
  margin: 1.5em 0;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
}

.markdown-content :deep(.code-block-header),
.markdown-content :deep(.mermaid-block-header) {
  min-height: 46px;
  padding: 8px 14px;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 12px;
}

.markdown-content :deep(.code-header-title) {
  color: var(--color-text-regular);
  font-family: var(--font-family-mono);
  font-weight: 500;
}

.markdown-content :deep(.code-header-caption) {
  display: none;
}

.markdown-content :deep(.code-block-header button) {
  min-width: 32px;
  min-height: 32px;
}

.markdown-content :deep(.table-node-wrapper) {
  max-width: 100%;
  margin: 1.5em 0;
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.markdown-content :deep(.table-node) {
  display: table;
  width: 100%;
  margin: 0;
  border-collapse: collapse;
  font-size: 0.9em;
  line-height: 1.7;
}

.markdown-content :deep(.table-node th),
.markdown-content :deep(.table-node td) {
  min-width: 100px;
  padding: 12px 16px;
  border: 0;
  border-bottom: 1px solid var(--color-border-light);
  vertical-align: top;
}

.markdown-content :deep(.table-node th) {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
  font-weight: 600;
}

.markdown-content :deep(.table-node tbody tr:nth-child(even)) {
  background: var(--color-bg-subtle);
}

.markdown-content :deep(.table-node tbody tr:last-child td) {
  border-bottom: 0;
}

.markdown-content :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin-inline: auto;
  border-radius: 10px;
}

.markdown-content :deep(.image-node-container) {
  margin-block: 1.5em;
}

.markdown-content :deep(.hr-node) {
  margin: 2.5em 0;
  border: 0;
  border-top: 1px solid var(--color-border);
}

.markdown-content :deep(.math-block) {
  max-width: 100%;
  overflow-x: auto;
  padding-block: 12px;
}

.markdown-content :deep(.admonition) {
  margin-block: 1.5em;
  border-radius: 10px;
}

.markdown-content > :deep(.node-slot[data-node-index='0'] > .node-content > :first-child) {
  margin-top: 0;
}

.markdown-content > :deep(.node-slot:last-child > .node-content > :last-child) {
  margin-bottom: 0;
}

.markdown-content--compact :deep(:is(h1, h2, h3, h4, h5, h6).heading-node) {
  margin-top: 1.2em;
  font-size: 1.15em;
}

@media (max-width: 640px) {
  .markdown-content :deep(h1.heading-node) {
    font-size: 1.7em;
  }

  .markdown-content :deep(h2.heading-node) {
    font-size: 1.4em;
  }

  .markdown-content :deep(.blockquote-node) {
    padding: 12px 16px;
  }

  .markdown-content :deep(.code-block-header button) {
    min-width: 44px;
    min-height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .markdown-content :deep(*) {
    animation: none !important;
    transition: none !important;
    scroll-behavior: auto !important;
  }
}
</style>
