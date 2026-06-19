<template>
  <ViewPage class="site-system-page regulations-page" :loading="loading">
    <template v-if="!isDetailRoute">
      <SitePageHero
        eyebrow="社团治理"
        title="规章制度"
        description="公开社团章程、管理办法与日常协作规范，让制度清晰、可查、可复用。"
      >
        <template #actions>
          <div class="regulation-actions">
            <el-select
              v-if="clubs.length > 1"
              v-model="selectedClubId"
              filterable
              placeholder="选择社团"
              style="width: 240px"
              @change="reload"
            >
              <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
            </el-select>
            <el-input
              v-model="keyword"
              clearable
              placeholder="搜索制度标题或内容"
              :prefix-icon="Search"
              @clear="reload"
              @keyup.enter="reload"
            />
            <el-button type="primary" @click="reload">查询</el-button>
          </div>
        </template>
      </SitePageHero>

      <section class="site-system-section">
        <div class="container regulations-shell">
          <SiteSectionHeading
            eyebrow="制度目录"
            title="已发布制度"
            description="点击制度卡片查看完整正文、流程图与最近发布时间。"
          />

          <div v-if="rows.length" class="regulation-grid">
            <article
              v-for="(item, index) in rows"
              :key="item.id"
              class="regulation-card site-system-surface site-reveal"
              role="button"
              tabindex="0"
              @click="$router.push(`/regulations/${item.id}`)"
              @keydown.enter="$router.push(`/regulations/${item.id}`)"
              @keydown.space.prevent="$router.push(`/regulations/${item.id}`)"
            >
              <div class="regulation-card__index">
                {{ String(index + 1).padStart(2, '0') }}
              </div>
              <div class="regulation-card__body">
                <span>{{ item.clubName || '开放原子开源社团' }}</span>
                <h2>{{ item.title }}</h2>
                <p>{{ item.summary || '点击查看制度完整内容。' }}</p>
                <footer>
                  <time>{{ formatDateTime(item.publishedAt || item.updatedAt) }}</time>
                  <strong
                    >查看全文 <el-icon><ArrowRight /></el-icon
                  ></strong>
                </footer>
              </div>
            </article>
          </div>
          <el-empty v-else-if="!loading" class="site-system-empty" description="暂无已发布制度" />
        </div>
      </section>
    </template>

    <template v-else>
      <SitePageHero
        eyebrow="规章制度"
        :title="detail?.title || '制度详情'"
        :description="detail?.summary || '查看制度完整内容。'"
        compact
      >
        <template #actions>
          <el-button :icon="ArrowLeft" @click="$router.push('/regulations')">返回目录</el-button>
        </template>
      </SitePageHero>

      <section class="site-system-section">
        <div v-if="detail" class="container regulation-detail-layout">
          <article ref="regulationDocumentRef" class="regulation-document site-system-surface">
            <MarkdownContent :content="detail.contentMarkdown" />
          </article>
          <aside class="regulation-sidebar">
            <nav
              v-if="tableOfContents.length"
              class="regulation-toc site-system-surface"
              aria-label="规章制度目录"
            >
              <span class="regulation-toc__eyebrow">内容目录</span>
              <button class="regulation-toc__title" type="button" @click="scrollToDocumentTop">
                {{ detail.title }}
              </button>
              <div class="regulation-toc__list">
                <button
                  v-for="heading in tableOfContents"
                  :key="heading.id"
                  type="button"
                  class="regulation-toc__item"
                  :class="[
                    `is-level-${Math.min(heading.level, 4)}`,
                    { 'is-active': activeHeadingId === heading.id },
                  ]"
                  :aria-current="activeHeadingId === heading.id ? 'location' : undefined"
                  @click="scrollToHeading(heading.id)"
                >
                  {{ heading.text }}
                </button>
              </div>
            </nav>

            <section class="regulation-meta site-system-surface" aria-label="制度信息">
              <div>
                <span>所属社团</span>
                <strong>{{ detail.clubName || '开放原子开源社团' }}</strong>
              </div>
              <div>
                <span>发布状态</span>
                <strong>正式发布</strong>
              </div>
              <div>
                <span>发布时间</span>
                <strong>{{ formatDateTime(detail.publishedAt || detail.createdAt) }}</strong>
              </div>
              <div>
                <span>最近更新</span>
                <strong>{{ formatDateTime(detail.updatedAt || detail.publishedAt) }}</strong>
              </div>
              <p>正文支持 Markdown 排版与 Mermaid 流程图。</p>
            </section>
          </aside>
        </div>
        <el-empty
          v-else-if="!loading"
          class="site-system-empty"
          description="制度不存在或尚未发布"
        />
      </section>
    </template>
  </ViewPage>
</template>

<script setup lang="ts">
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import SiteSectionHeading from '@/components/site/shell/SiteSectionHeading.vue'
import { siteApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { extractMarkdownHeadings } from '@/utils/markdown.ts'
import { ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const loading = ref(false)
const clubs = ref<any[]>([])
const rows = ref<any[]>([])
const detail = ref<any>(null)
const selectedClubId = ref<number | string>('')
const keyword = ref('')
const regulationDocumentRef = ref<HTMLElement>()
const activeHeadingId = ref('')
let headingFrame = 0

const isDetailRoute = computed(() => Boolean(route.params.id))
const documentHeadings = computed(() =>
  extractMarkdownHeadings(detail.value?.contentMarkdown || ''),
)
const tableOfContents = computed(() => {
  const title = String(detail.value?.title || '').trim()
  return documentHeadings.value.filter(
    (heading, index) =>
      !(
        index === 0 &&
        heading.level === 1 &&
        heading.text.trim().toLocaleLowerCase() === title.toLocaleLowerCase()
      ),
  )
})

async function loadClubs() {
  const result = await siteApi.clubs()
  clubs.value = result?.list || result || []
  if (!selectedClubId.value && clubs.value.length) selectedClubId.value = clubs.value[0].id
}

async function fetchList() {
  loading.value = true
  try {
    rows.value =
      (await siteApi.regulations({
        clubId: selectedClubId.value || undefined,
        keyword: keyword.value.trim() || undefined,
      })) || []
  } finally {
    loading.value = false
  }
}

async function fetchDetail() {
  if (!route.params.id) return
  loading.value = true
  detail.value = null
  activeHeadingId.value = ''
  try {
    detail.value = await siteApi.regulationDetail(route.params.id as string)
    await nextTick()
    window.requestAnimationFrame(() => {
      const hash = decodeURIComponent(window.location.hash.slice(1))
      if (hash) document.getElementById(hash)?.scrollIntoView({ block: 'start' })
      queueHeadingUpdate()
    })
  } finally {
    loading.value = false
  }
}

function updateActiveHeading() {
  headingFrame = 0
  const root = regulationDocumentRef.value
  if (!root) return
  const elements = documentHeadings.value
    .map((heading) => root.querySelector<HTMLElement>(`#${CSS.escape(heading.id)}`))
    .filter((element): element is HTMLElement => Boolean(element))
  if (!elements.length) return

  const offset = 128
  let active = elements[0]
  for (const element of elements) {
    if (element.getBoundingClientRect().top <= offset) active = element
    else break
  }
  activeHeadingId.value = active.id
}

function queueHeadingUpdate() {
  if (headingFrame) return
  headingFrame = window.requestAnimationFrame(updateActiveHeading)
}

function scrollToHeading(id: string) {
  const target = regulationDocumentRef.value?.querySelector<HTMLElement>(`#${CSS.escape(id)}`)
  if (!target) return
  activeHeadingId.value = id
  target.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    block: 'start',
  })
  window.history.replaceState(null, '', `#${encodeURIComponent(id)}`)
}

function scrollToDocumentTop() {
  regulationDocumentRef.value?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    block: 'start',
  })
  window.history.replaceState(null, '', window.location.pathname + window.location.search)
}

function reload() {
  fetchList()
}

async function loadByRoute() {
  if (isDetailRoute.value) {
    await fetchDetail()
    return
  }
  if (!clubs.value.length) await loadClubs()
  await fetchList()
}

watch(
  () => route.fullPath,
  () => loadByRoute(),
)

onMounted(() => {
  window.addEventListener('scroll', queueHeadingUpdate, { passive: true })
  window.addEventListener('resize', queueHeadingUpdate, { passive: true })
  loadByRoute()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', queueHeadingUpdate)
  window.removeEventListener('resize', queueHeadingUpdate)
  if (headingFrame) window.cancelAnimationFrame(headingFrame)
})
</script>

<style scoped>
:global(html:has(.regulations-page)),
:global(body:has(.regulations-page)) {
  overflow-x: clip;
}

.regulations-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.regulation-actions {
  display: flex;
  width: min(760px, 100%);
  gap: 12px;
}

.regulation-actions :deep(.el-input) {
  flex: 1;
  min-width: 220px;
}

.regulations-shell {
  display: grid;
  gap: 28px;
}

.regulation-grid {
  display: grid;
  gap: 16px;
}

.regulation-card {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 24px;
  padding: 28px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
}

.regulation-card:hover {
  transform: translateY(-2px);
  border-color: var(--oa-text);
}

.regulation-card:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--oa-primary) 22%, transparent);
  outline-offset: 3px;
}

.regulation-card__index {
  display: grid;
  width: 64px;
  height: 64px;
  place-items: center;
  border-radius: 18px;
  color: #ffffff;
  background: var(--oa-text);
  font-size: 20px;
  font-weight: 700;
}

.regulation-card__body > span {
  color: var(--oa-muted);
  font-size: 13px;
}

.regulation-card h2 {
  margin: 8px 0 10px;
  color: var(--oa-text);
  font-size: clamp(22px, 3vw, 30px);
}

.regulation-card p {
  max-width: 780px;
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.75;
}

.regulation-card footer {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-top: 20px;
  color: var(--oa-muted);
  font-size: 13px;
}

.regulation-card footer strong {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--oa-text);
}

.regulation-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 24px;
  align-items: start;
}

.regulation-document {
  min-height: 560px;
  padding: clamp(26px, 4vw, 48px);
}

.regulation-sidebar {
  position: sticky;
  top: calc(var(--oa-site-header-height) + 20px);
  display: block;
  min-width: 0;
  max-height: calc(100vh - var(--oa-site-header-height) - 40px);
  align-self: start;
  padding-right: 4px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.regulation-toc {
  --regulation-toc-accent: #0a5cff;

  padding: 24px 18px 20px;
}

.regulation-toc__eyebrow {
  display: block;
  margin: 0 6px 8px;
  color: var(--oa-muted);
  font-size: 12px;
}

.regulation-toc__title,
.regulation-toc__item {
  display: block;
  width: 100%;
  border: 0;
  color: var(--oa-muted);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
}

.regulation-toc__title {
  padding: 8px 6px 14px;
  border-bottom: 1px solid var(--oa-border);
  color: var(--oa-text);
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
}

.regulation-toc__list {
  max-height: min(42vh, 430px);
  margin-top: 10px;
  padding-right: 4px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.regulation-toc__item {
  position: relative;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.45;
}

.regulation-toc__item:hover,
.regulation-toc__item.is-active {
  color: var(--regulation-toc-accent);
  background: color-mix(in srgb, var(--regulation-toc-accent) 8%, transparent);
}

.regulation-toc__title:focus-visible,
.regulation-toc__item:focus-visible {
  outline: 2px solid var(--regulation-toc-accent);
  outline-offset: 2px;
}

.regulation-toc__item.is-active::before {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 0;
  width: 2px;
  border-radius: 99px;
  background: var(--regulation-toc-accent);
  content: '';
}

.regulation-toc__item.is-level-2 {
  font-size: 15px;
  font-weight: 600;
}

.regulation-toc__item.is-level-3 {
  padding-left: 26px;
}

.regulation-toc__item.is-level-4 {
  padding-left: 42px;
  font-size: 13px;
}

.regulation-meta {
  display: grid;
  gap: 20px;
  margin-top: 16px;
  padding: 24px;
}

.regulation-meta div {
  display: grid;
  gap: 6px;
}

.regulation-meta span {
  color: var(--oa-muted);
  font-size: 12px;
}

.regulation-meta strong {
  color: var(--oa-text);
  line-height: 1.5;
}

.regulation-meta p {
  margin: 0;
  padding-top: 18px;
  border-top: 1px solid var(--oa-border);
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 820px) {
  .regulation-actions,
  .regulation-card footer {
    align-items: stretch;
    flex-direction: column;
  }

  .regulation-detail-layout {
    grid-template-columns: 1fr;
  }

  .regulation-meta {
    position: static;
  }

  .regulation-sidebar {
    position: static;
    order: -1;
    max-height: none;
    padding-right: 0;
    overflow: visible;
  }

  .regulation-toc__list {
    max-height: 360px;
  }
}

@media (max-width: 560px) {
  .regulation-card {
    grid-template-columns: 1fr;
    padding: 22px;
  }
}
</style>
