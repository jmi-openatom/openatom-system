<template>
  <ViewPage :loading="loading" class="blog-page">
    <section class="blog-hero" aria-labelledby="blog-title">
      <div class="container blog-hero__inner">
        <div class="blog-hero__copy">
          <span>OPENATOM BLOG</span>
          <h1 id="blog-title">技术博客</h1>
          <p>沉淀社团成员的工程实践、竞赛复盘和开源笔记，让经验像代码一样被复用。</p>
          <div class="blog-hero__actions">
            <el-button
              class="blog-write-button"
              size="large"
              type="primary"
              @click="$router.push('/blog/write')"
            >
              写文章
            </el-button>
            <el-button v-if="isLoggedIn" size="large" @click="$router.push('/blog/my')">
              我的博客
            </el-button>
          </div>
          <p class="blog-hero__meta">共 {{ total }} 篇文章 · 来自社团成员的真实记录</p>
        </div>
      </div>
    </section>

    <section class="blog-index-section" aria-labelledby="latest-articles-title">
      <div class="container blog-index">
        <aside class="blog-filter-panel" aria-label="文章筛选">
          <form role="search" @submit.prevent="reload">
            <el-input
              v-model="query.keyword"
              aria-label="搜索文章"
              clearable
              placeholder="搜索文章、标签、作者"
              size="large"
              @clear="reload"
              @keyup.enter="reload"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </form>

          <div class="blog-filter-group">
            <p>文章分类</p>
            <button
              :class="{ 'is-active': !query.category }"
              type="button"
              @click="selectCategory('')"
            >
              <span>全部分类</span>
              <strong>{{ total }}</strong>
            </button>
            <button
              v-for="category in categories"
              :key="category"
              :class="{ 'is-active': query.category === category }"
              type="button"
              @click="selectCategory(category)"
            >
              <span>{{ category }}</span>
            </button>
          </div>

          <div class="blog-filter-group">
            <p>标签</p>
            <el-input
              v-model="query.tag"
              aria-label="按标签筛选"
              clearable
              placeholder="输入标签"
              @clear="reload"
              @keyup.enter="reload"
            />
          </div>

          <button
            v-if="hasActiveFilters"
            class="blog-filter-reset"
            type="button"
            @click="resetFilters"
          >
            清空筛选
          </button>
        </aside>

        <main class="blog-feed">
          <div class="blog-feed__heading">
            <div>
              <span>EDITOR'S PICK</span>
              <h2 id="latest-articles-title">最新发布</h2>
            </div>
            <p>{{ total }} 篇文章</p>
          </div>

          <router-link
            v-if="featuredArticle"
            :to="`/blog/${featuredArticle.id}`"
            class="blog-featured"
          >
            <div class="blog-featured__body">
              <span class="blog-featured__label">精选推荐</span>
              <h3>{{ featuredArticle.title }}</h3>
              <p>{{ featuredArticle.summary || '作者暂未填写摘要' }}</p>
              <div class="blog-story-meta">
                <span>{{ featuredArticle.authorName || '匿名作者' }}</span>
                <span>{{ featuredArticle.category || '未分类' }}</span>
                <time>{{
                  formatDateTime(featuredArticle.publishedAt || featuredArticle.createdAt)
                }}</time>
                <span>{{ featuredArticle.viewCount || 0 }} 阅读</span>
              </div>
            </div>
            <div :class="{ 'is-empty': !featuredArticle.coverUrl }" class="blog-featured__media">
              <img
                v-if="featuredArticle.coverUrl"
                :alt="featuredArticle.title"
                :src="featuredArticle.coverUrl"
              />
              <span v-else>{{ coverInitial(featuredArticle.title) }}</span>
            </div>
          </router-link>

          <div v-if="listRows.length" class="blog-story-list">
            <router-link
              v-for="article in listRows"
              :key="article.id"
              :to="`/blog/${article.id}`"
              class="blog-story-row"
            >
              <div class="blog-story-row__body">
                <div class="blog-story-row__eyebrow">
                  <span>{{ article.category || '未分类' }}</span>
                  <span>{{ article.authorName || '匿名作者' }}</span>
                  <time>{{ formatDateTime(article.publishedAt || article.createdAt) }}</time>
                </div>
                <h3>{{ article.title }}</h3>
                <p>{{ article.summary || '作者暂未填写摘要' }}</p>
                <div class="blog-story-meta">
                  <span v-for="tag in article.tags || []" :key="tag">{{ tag }}</span>
                  <span>{{ article.viewCount || 0 }} 阅读</span>
                  <span>{{ article.likeCount || 0 }} 点赞</span>
                </div>
              </div>
              <div :class="{ 'is-empty': !article.coverUrl }" class="blog-story-row__media">
                <img
                  v-if="article.coverUrl"
                  :alt="article.title"
                  :src="article.coverUrl"
                  loading="lazy"
                />
                <span v-else>{{ coverInitial(article.title) }}</span>
              </div>
            </router-link>
          </div>

          <el-empty v-if="!loading && !rows.length" description="暂无已发布文章" />

          <el-pagination
            v-if="total > query.pageSize"
            :current-page="query.page"
            :page-size="query.pageSize"
            :total="total"
            background
            class="blog-pagination"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </main>
      </div>
    </section>
  </ViewPage>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'

interface BlogArticle {
  id: number
  title: string
  summary?: string
  category?: string
  authorName?: string
  publishedAt?: string
  createdAt?: string
  coverUrl?: string
  tags?: string[]
  viewCount?: number
  likeCount?: number
}

const loading = ref(false)
const rows = ref<BlogArticle[]>([])
const total = ref(0)
const categories = ref<string[]>([])
const query = ref({
  keyword: '',
  category: '',
  tag: '',
  page: 1,
  pageSize: 10,
})

const isLoggedIn = computed(() => Boolean(getToken()))
const hasActiveFilters = computed(() => {
  return Boolean(query.value.keyword || query.value.category || query.value.tag)
})
const featuredArticle = computed(() => rows.value[0])
const listRows = computed(() => rows.value.slice(1))

async function fetchCategories() {
  categories.value = (await siteApi.blogCategories()) || []
}

async function fetchList() {
  loading.value = true
  try {
    const data = await siteApi.blogArticles({
      ...query.value,
      keyword: query.value.keyword || undefined,
      category: query.value.category || undefined,
      tag: query.value.tag || undefined,
    })
    rows.value = data?.list || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  fetchList()
}

function selectCategory(category: string) {
  query.value.category = category
  reload()
}

function resetFilters() {
  query.value.keyword = ''
  query.value.category = ''
  query.value.tag = ''
  reload()
}

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function coverInitial(title: string) {
  return String(title || 'B')
    .slice(0, 1)
    .toUpperCase()
}

onMounted(() => {
  fetchCategories()
  fetchList()
})
</script>

<style scoped>
.blog-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-bg);
}

.blog-hero {
  position: relative;
  min-height: clamp(440px, 38vw, 560px);
  overflow: hidden;
  background: var(--oa-page-soft-bg);
  border-bottom: 1px solid var(--oa-border);
}

.blog-hero::before {
  position: absolute;
  inset: 0;
  background: #faf9f7 url('/blog-hero-wide-light.png?v=3') right center / auto 100% no-repeat;
  content: '';
  pointer-events: none;
}

.blog-hero__inner {
  position: relative;
  z-index: 1;
  display: grid;
  min-height: clamp(440px, 38vw, 560px);
  align-content: center;
  padding-top: clamp(48px, 7vw, 84px);
  padding-bottom: clamp(48px, 7vw, 84px);
}

.blog-hero__copy {
  position: relative;
  z-index: 1;
  width: min(47%, 520px);
  text-align: left;
}

.blog-hero__copy > span,
.blog-feed__heading span {
  color: var(--oa-muted-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.blog-hero h1 {
  margin: 12px 0 14px;
  color: var(--oa-text);
  font-size: clamp(42px, 4.8vw, 64px);
  font-weight: 720;
  line-height: 1;
  letter-spacing: 0;
}

.blog-hero__copy > p:not(.blog-hero__meta) {
  max-width: 480px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.72;
}

.blog-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.blog-hero__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.blog-hero__actions :deep(.el-button) {
  min-height: 44px;
  padding-inline: 22px;
  border-radius: var(--radius-md);
}

.blog-hero__actions :deep(.blog-write-button) {
  min-width: 116px;
  font-weight: 700;
}

.blog-hero__meta {
  margin: 18px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-index-section {
  padding: clamp(36px, 5vw, 64px) 0 clamp(72px, 8vw, 112px);
}

.blog-index {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  align-items: start;
  gap: 20px;
}

.blog-filter-panel {
  position: sticky;
  top: calc(var(--oa-site-header-height) + 20px);
  display: grid;
  gap: 24px;
  padding: 16px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 96%, transparent);
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  backdrop-filter: saturate(150%) blur(22px);
  -webkit-backdrop-filter: saturate(150%) blur(22px);
}

.blog-filter-panel :deep(.el-input__wrapper) {
  min-height: 44px;
  background: var(--oa-page-bg);
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--oa-button-border) inset !important;
}

.blog-filter-group {
  display: grid;
  gap: 6px;
}

.blog-filter-group + .blog-filter-group {
  padding-top: 20px;
  border-top: 1px solid var(--oa-border);
}

.blog-filter-group > p {
  margin: 0 0 6px;
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 700;
}

.blog-filter-group > button {
  display: flex;
  min-height: 38px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 10px;
  color: var(--oa-muted-strong);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  text-align: left;
}

.blog-filter-group > button:hover {
  color: var(--oa-text);
  background: var(--oa-button-hover-bg);
}

.blog-filter-group > button.is-active {
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
}

.blog-filter-group > button strong {
  font-size: 12px;
  font-weight: 600;
}

.blog-filter-reset {
  min-height: 40px;
  color: var(--oa-muted);
  background: transparent;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font: inherit;
}

.blog-filter-reset:hover {
  color: var(--oa-text);
  border-color: var(--oa-button-hover-border);
}

.blog-feed {
  min-width: 0;
  background: transparent;
}

.blog-feed__heading {
  display: flex;
  min-height: 80px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 0 0 20px;
  border-bottom: 1px solid var(--oa-border);
}

.blog-feed__heading h2 {
  margin: 6px 0 0;
  color: var(--oa-text);
  font-size: 26px;
  line-height: 1.1;
}

.blog-feed__heading > p {
  margin: 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-featured {
  display: grid;
  min-height: 260px;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 43%);
  color: inherit;
  border-bottom: 1px solid var(--oa-border);
}

.blog-featured__body {
  padding: 28px 28px 24px;
}

.blog-featured__label {
  display: inline-flex;
  padding: 4px 7px;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border-radius: var(--radius-xs);
  font-size: 11px;
  font-weight: 700;
}

.blog-featured h3 {
  margin: 16px 0 10px;
  color: var(--oa-text);
  font-size: clamp(24px, 2.4vw, 34px);
  line-height: 1.28;
}

.blog-featured__body > p,
.blog-story-row__body > p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--oa-muted);
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.blog-story-meta,
.blog-story-row__eyebrow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
  color: var(--oa-muted);
  font-size: 12px;
}

.blog-featured .blog-story-meta {
  margin-top: 20px;
}

.blog-featured__media,
.blog-story-row__media {
  display: grid;
  overflow: hidden;
  place-items: center;
  background: var(--oa-page-soft-bg);
  border-left: 1px solid var(--oa-border);
}

.blog-featured__media img,
.blog-story-row__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.24s ease;
}

.blog-featured__media > span,
.blog-story-row__media > span {
  color: var(--oa-muted);
  font-size: 42px;
  font-weight: 700;
}

.blog-featured:hover .blog-featured__media img,
.blog-story-row:hover .blog-story-row__media img {
  transform: scale(1.02);
}

.blog-story-list {
  display: grid;
}

.blog-story-row {
  display: grid;
  min-height: 150px;
  grid-template-columns: minmax(0, 1fr) 190px;
  color: inherit;
  border-bottom: 1px solid var(--oa-border);
  transition: background-color 0.18s ease;
}

.blog-story-row:last-child {
  border-bottom: 0;
}

.blog-story-row:hover {
  background: var(--oa-button-subtle-bg);
}

.blog-story-row__body {
  min-width: 0;
  padding: 20px 24px;
}

.blog-story-row h3 {
  margin: 8px 0 6px;
  color: var(--oa-text);
  font-size: 19px;
  line-height: 1.35;
}

.blog-story-row .blog-story-meta {
  margin-top: 12px;
}

.blog-pagination {
  display: flex;
  justify-content: center;
  padding: 24px;
  border-top: 1px solid var(--oa-border);
}

.blog-featured:focus-visible,
.blog-story-row:focus-visible,
.blog-filter-group > button:focus-visible,
.blog-filter-reset:focus-visible {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: -3px;
}

@media (max-width: 980px) {
  .blog-index {
    grid-template-columns: 1fr;
  }

  .blog-filter-panel {
    position: static;
    grid-template-columns: minmax(220px, 1fr) 1fr 1fr;
    align-items: start;
  }

  .blog-filter-group + .blog-filter-group {
    padding-top: 0;
    border-top: 0;
  }

  .blog-filter-reset {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .blog-hero {
    min-height: 650px;
  }

  .blog-hero__inner {
    min-height: 650px;
    align-content: start;
    padding-top: 42px;
    padding-bottom: 330px;
  }

  .blog-hero::before {
    background-position: 68% bottom;
    background-size: auto 310px;
  }

  .blog-hero__copy {
    width: 100%;
  }

  .blog-filter-panel {
    grid-template-columns: 1fr;
  }

  .blog-filter-reset {
    grid-column: auto;
  }

  .blog-featured,
  .blog-story-row {
    grid-template-columns: 1fr;
  }

  .blog-featured__media {
    min-height: 220px;
    grid-row: 1;
    border-left: 0;
    border-bottom: 1px solid var(--oa-border);
  }

  .blog-story-row__media {
    display: none;
  }
}

@media (max-width: 520px) {
  .blog-hero h1 {
    font-size: 40px;
  }

  .blog-feed__heading,
  .blog-featured__body,
  .blog-story-row__body {
    padding-inline: 18px;
  }
}

:global(html.dark .blog-hero::before) {
  background-color: #0d0d0f;
  background-image: url('/blog-hero-wide-dark.png?v=3');
}
</style>
