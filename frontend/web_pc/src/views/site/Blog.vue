<template>
  <ViewPage class="blog-page" :loading="loading">
    <section class="blog-hero home-interactive-section">
      <div class="container blog-hero__inner">
        <div class="section-heading reveal-block">
          <span>OpenAtom Blog</span>
          <h1>技术博客</h1>
          <p>沉淀社团成员的工程实践、竞赛复盘和开源笔记，让经验像代码一样被复用。</p>
        </div>

        <div class="hero__actions blog-hero__actions">
          <el-button v-if="isLoggedIn" size="large" @click="$router.push('/blog/my')">
            我的博客
          </el-button>
          <el-button
            v-if="canWrite"
            size="large"
            type="primary"
            @click="$router.push('/blog/write')"
          >
            写文章
          </el-button>
        </div>

        <div class="command-panel blog-command-panel">
          <div class="blog-command-panel__fields">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="搜索标题、摘要、正文"
              @keyup.enter="reload"
              @clear="reload"
            />
            <el-select v-model="query.category" clearable placeholder="文章分类" @change="reload">
              <el-option
                v-for="category in categories"
                :key="category"
                :label="category"
                :value="category"
              />
            </el-select>
            <el-input
              v-model="query.tag"
              clearable
              placeholder="标签"
              @keyup.enter="reload"
              @clear="reload"
            />
          </div>
          <el-button type="primary" @click="reload">筛选</el-button>
        </div>
      </div>
    </section>

    <section class="blog-stream-section home-interactive-section">
      <div class="container blog-stream">
        <div class="section-heading reveal-block">
          <span>文章流</span>
          <h2>最新发布</h2>
        </div>

        <div class="blog-content">
          <article
            v-for="article in rows"
            :key="article.id"
            class="blog-row reveal-card"
            @click="$router.push(`/blog/${article.id}`)"
          >
            <div class="blog-row__body">
              <div class="blog-row__meta">
                <el-tag v-if="article.featured" type="warning" effect="plain">推荐</el-tag>
                <span>{{ article.category || '未分类' }}</span>
                <span>{{ article.authorName || '匿名作者' }}</span>
                <span>{{ formatDateTime(article.publishedAt || article.createdAt) }}</span>
              </div>
              <h2>{{ article.title }}</h2>
              <p>{{ article.summary || '作者暂未填写摘要' }}</p>
              <div class="blog-row__tags">
                <el-tag v-for="tag in article.tags || []" :key="tag" size="small" effect="plain">
                  {{ tag }}
                </el-tag>
              </div>
              <footer>
                <span>{{ article.viewCount || 0 }} 阅读</span>
                <span>{{ article.likeCount || 0 }} 点赞</span>
                <span>{{ article.favoriteCount || 0 }} 收藏</span>
                <span>{{ article.shareCount || 0 }} 分享</span>
                <span>{{ article.commentCount || 0 }} 评论</span>
              </footer>
            </div>
            <div class="blog-row__cover" :class="{ 'is-empty': !article.coverUrl }">
              <img v-if="article.coverUrl" :alt="article.title" :src="article.coverUrl" />
              <span v-else>{{ coverInitial(article.title) }}</span>
            </div>
          </article>

          <el-empty v-if="!loading && !rows.length" description="暂无已发布文章" />

          <el-pagination
            v-if="total > query.pageSize"
            class="pager"
            background
            layout="prev, pager, next"
            :current-page="query.page"
            :page-size="query.pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { getToken } from '@/utils/auth.ts'
import { hasRole } from '@/utils/permission.ts'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)
const rows = ref<any[]>([])
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
const canWrite = computed(() => isLoggedIn.value && hasRole('formal_member'))

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

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function coverInitial(title: string) {
  return String(title || 'B').slice(0, 1).toUpperCase()
}

onMounted(() => {
  fetchCategories()
  fetchList()
})
</script>

<style scoped>
.blog-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.blog-hero,
.blog-stream-section {
  position: relative;
  overflow: hidden;
}

.blog-hero {
  min-height: min(82vh, 780px);
  padding: clamp(92px, 10vw, 132px) 0 clamp(56px, 7vw, 88px);
  background: var(--oa-elevated-bg);
  border-bottom: 1px solid var(--oa-border);
}

.blog-hero__inner {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 28px;
}

.blog-hero .section-heading {
  max-width: 880px;
}

.blog-hero .section-heading h1 {
  margin: 12px 0 16px;
  color: var(--oa-text);
  font-size: clamp(46px, 7vw, 92px);
  font-weight: 650;
  line-height: 0.98;
  letter-spacing: 0;
}

.blog-hero .section-heading p {
  max-width: 760px;
  color: var(--oa-muted);
  font-size: clamp(18px, 2.1vw, 24px);
  line-height: 1.7;
}

.blog-hero__actions {
  justify-content: flex-start;
}

.blog-command-panel {
  width: min(920px, 100%);
  padding: 22px;
}

.blog-command-panel__fields {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) minmax(180px, 0.8fr) minmax(160px, 0.7fr);
  gap: 12px;
  margin-bottom: 14px;
}

.blog-stream-section {
  padding: clamp(54px, 7vw, 88px) 0;
}

.blog-stream {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 28px;
}

.blog-content {
  display: grid;
  gap: 16px;
}

.blog-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 22px;
  min-height: 190px;
  padding: 22px;
  cursor: pointer;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 24px;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease;
}

.blog-row:hover {
  border-color: var(--oa-primary);
  transform: translateY(-3px);
}

.blog-row__body {
  min-width: 0;
}

.blog-row__meta,
.blog-row footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-row h2 {
  margin: 12px 0 8px;
  color: var(--oa-text);
  font-size: clamp(22px, 2.2vw, 30px);
  line-height: 1.3;
}

.blog-row p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--oa-muted);
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.blog-row__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 24px;
  margin: 14px 0;
}

.blog-row__cover {
  display: grid;
  min-height: 146px;
  overflow: hidden;
  place-items: center;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 18px;
}

.blog-row__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-row__cover span {
  color: var(--oa-muted);
  font-size: 48px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .blog-command-panel__fields,
  .blog-row {
    grid-template-columns: 1fr;
  }

  .blog-row__cover {
    min-height: 190px;
  }
}
</style>
