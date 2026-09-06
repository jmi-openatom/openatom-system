<template>
  <section :class="['reading-outline', { 'reading-outline--collapsible': collapsible }]">
    <button
      v-if="collapsible"
      :aria-controls="outlineId"
      :aria-expanded="expanded"
      class="reading-outline__toggle"
      type="button"
      @click="expanded = !expanded"
    >
      <span><ListTree :size="17" aria-hidden="true" /> 本文目录</span>
      <span class="reading-outline__toggle-meta">
        {{ headings.length }} 个章节
        <ChevronDown :class="{ 'is-open': expanded }" :size="16" aria-hidden="true" />
      </span>
    </button>
    <div v-else class="reading-outline__heading">
      <span><ListTree :size="17" aria-hidden="true" /> 本文目录</span>
      <span>{{ headings.length }} 节</span>
    </div>

    <nav
      v-show="!collapsible || expanded"
      :id="outlineId"
      aria-label="本文目录"
      class="reading-outline__nav"
      data-lenis-prevent
    >
      <ol>
        <li v-for="heading in headings" :key="heading.id">
          <a
            :aria-current="activeId === heading.id ? 'location' : undefined"
            :class="{ 'is-active': activeId === heading.id }"
            :href="`#${encodeURIComponent(heading.id)}`"
            :style="{ '--outline-depth': Math.min(heading.level - baseLevel, 3) }"
            @click.prevent="selectHeading(heading.id)"
          >
            {{ heading.text }}
          </a>
        </li>
      </ol>
    </nav>

    <div class="reading-outline__progress">
      <div>
        <span>阅读进度</span><span>{{ progress }}%</span>
      </div>
      <progress :value="progress" aria-label="文章阅读进度" max="100">{{ progress }}%</progress>
    </div>
  </section>
</template>

<script lang="ts" setup>
import { ChevronDown, ListTree } from 'lucide-vue-next'
import { computed, ref, useId } from 'vue'
import type { MarkdownHeading } from '@/utils/markdown.ts'

const props = withDefaults(
  defineProps<{
    headings: MarkdownHeading[]
    activeId: string
    progress: number
    collapsible?: boolean
  }>(),
  { collapsible: false },
)
const emit = defineEmits<{ select: [id: string] }>()
const expanded = ref(false)
const outlineId = useId()
const baseLevel = computed(() => Math.min(...props.headings.map((heading) => heading.level), 6))

function selectHeading(id: string) {
  expanded.value = false
  emit('select', id)
}
</script>

<style scoped>
.reading-outline {
  display: grid;
  gap: 18px;
}

.reading-outline__heading,
.reading-outline__toggle,
.reading-outline__heading > span:first-child,
.reading-outline__toggle > span,
.reading-outline__progress > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.reading-outline__heading,
.reading-outline__toggle {
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 650;
}

.reading-outline__heading > span:last-child,
.reading-outline__toggle-meta {
  color: var(--oa-muted);
  font-size: 12px;
  font-weight: 400;
  white-space: nowrap;
}

.reading-outline__toggle {
  width: 100%;
  min-height: 44px;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.reading-outline__toggle svg {
  transition: transform 160ms ease;
}

.reading-outline__toggle .is-open {
  transform: rotate(180deg);
}

.reading-outline__nav {
  max-height: min(45vh, 440px);
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--oa-border) transparent;
}

.reading-outline__nav ol {
  display: grid;
  gap: 3px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.reading-outline__nav a {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 8px 10px 8px calc(12px + var(--outline-depth) * 12px);
  border-left: 2px solid transparent;
  border-radius: 0 7px 7px 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.6;
  text-decoration: none;
  overflow-wrap: anywhere;
  transition:
    color 160ms ease,
    background 160ms ease;
}

.reading-outline__nav a:hover {
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
}

.reading-outline__nav a.is-active {
  border-left-color: var(--oa-text);
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  font-weight: 600;
}

.reading-outline__nav a:focus-visible,
.reading-outline__toggle:focus-visible {
  outline: 2px solid var(--oa-text);
  outline-offset: 3px;
}

.reading-outline__progress {
  display: grid;
  gap: 9px;
  padding-top: 15px;
  border-top: 1px solid var(--oa-border);
}

.reading-outline__progress > div {
  color: var(--oa-muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.reading-outline__progress progress {
  display: block;
  width: 100%;
  height: 3px;
  overflow: hidden;
  border: 0;
  border-radius: 999px;
  background: var(--oa-border);
  color: var(--oa-text);
  accent-color: var(--oa-text);
}

.reading-outline__progress progress::-webkit-progress-bar {
  background: var(--oa-border);
}

.reading-outline__progress progress::-webkit-progress-value {
  background: var(--oa-text);
  border-radius: 999px;
}

.reading-outline__progress progress::-moz-progress-bar {
  background: var(--oa-text);
  border-radius: 999px;
}

@media (prefers-reduced-motion: reduce) {
  .reading-outline__toggle svg,
  .reading-outline__nav a {
    transition: none;
  }
}
</style>
