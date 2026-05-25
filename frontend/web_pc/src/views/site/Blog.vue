<template>
  <ViewPage class="blog-page" :loading="loading">
    <section class="blog-shell">
      <div class="container blog-shell__inner">
        <aside class="blog-sidebar">
          <div class="blog-sidebar__title">
            <span>OpenAtom Blog</span>
            <h1>技术博客</h1>
          </div>

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

          <div class="blog-sidebar__actions">
            <el-button type="primary" @click="reload">筛选</el-button>
            <el-button v-if="isLoggedIn" @click="$router.push('/blog/my')">我的博客</el-button>
            <el-button v-if="canWrite" type="success" @click="$router.push('/blog/write')">
              写文章
            </el-button>
          </div>
        </aside>

        <main class="blog-content">
          <article
            v-for="article in rows"
            :key="article.id"
            class="blog-row"
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
        </main>
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

.blog-shell {
  padding: 92px 0 56px;
}

.blog-shell__inner {
  display: grid;
  grid-template-columns: minmax(220px, 300px) minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.blog-sidebar {
  position: sticky;
  top: calc(var(--oa-site-header-height) + 24px);
  display: grid;
  gap: 14px;
  padding: 22px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

.blog-sidebar__title span {
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-sidebar__title h1 {
  margin: 6px 0 0;
  color: var(--oa-text);
  font-size: 28px;
  line-height: 1.2;
}

.blog-sidebar__actions {
  display: grid;
  gap: 10px;
}

.blog-content {
  display: grid;
  gap: 14px;
}

.blog-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 20px;
  min-height: 176px;
  padding: 22px;
  cursor: pointer;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
  transition:
    border-color 0.2s ease,
    transform 0.2s ease;
}

.blog-row:hover {
  border-color: var(--oa-primary);
  transform: translateY(-2px);
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
  font-size: 22px;
  line-height: 1.35;
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
  min-height: 132px;
  overflow: hidden;
  place-items: center;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

.blog-row__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-row__cover span {
  color: var(--oa-muted);
  font-size: 42px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .blog-shell {
    padding-top: 76px;
  }

  .blog-shell__inner,
  .blog-row {
    grid-template-columns: 1fr;
  }

  .blog-sidebar {
    position: static;
  }

  .blog-row__cover {
    min-height: 180px;
  }
}
</style>
