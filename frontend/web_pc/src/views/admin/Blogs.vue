<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select v-model="query.status" clearable placeholder="文章状态" @change="reload">
          <el-option label="草稿" value="draft" />
          <el-option label="待审核" value="pending" />
          <el-option label="已发布" value="published" />
          <el-option label="已隐藏" value="hidden" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索文章"
          style="width: 220px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" :icon="Refresh" @click="reload">刷新</el-button>
      </div>
      <el-button :icon="Tickets" @click="openInteractions()">互动记录</el-button>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="title" label="文章" min-width="260">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted-line">{{ row.summary || '暂无摘要' }}</p>
          <p v-if="row.rejectReason" class="danger-line">原因：{{ row.rejectReason }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="130" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="featured" label="推荐" width="90">
        <template #default="{ row }">
          <el-tag :type="row.featured ? 'warning' : 'info'">{{ row.featured ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数据" width="210">
        <template #default="{ row }">
          {{ row.viewCount || 0 }} 阅读 · {{ row.likeCount || 0 }} 赞 · {{ row.favoriteCount || 0 }} 藏 ·
          {{ row.shareCount || 0 }} 分享
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReview(row)">审核</el-button>
          <el-button link type="success" @click="openComments(row)">评论</el-button>
          <el-button link type="primary" @click="openInteractions(row)">互动</el-button>
          <el-button
            v-if="row.status === 'published'"
            link
            type="success"
            @click="$router.push(`/blog/${row.id}`)"
          >
            查看
          </el-button>
          <el-popconfirm title="确定删除该文章？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

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

    <el-dialog
      v-model="reviewVisible"
      class="blog-review-dialog"
      title="文章审核"
      width="1180px"
      destroy-on-close
    >
      <div class="review-layout">
        <aside class="review-panel">
          <el-form label-width="92px">
            <el-form-item label="文章">
              <strong>{{ currentArticle.title }}</strong>
            </el-form-item>
            <el-form-item label="作者">
              <span>{{ currentArticle.authorName || '-' }}</span>
            </el-form-item>
            <el-form-item label="分类">
              <span>{{ currentArticle.category || '-' }}</span>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="reviewForm.status">
                <el-option label="草稿" value="draft" />
                <el-option label="待审核" value="pending" />
                <el-option label="审核通过" value="published" />
                <el-option label="隐藏" value="hidden" />
                <el-option label="驳回" value="rejected" />
              </el-select>
            </el-form-item>
            <el-form-item label="推荐">
              <el-switch v-model="reviewForm.featured" />
            </el-form-item>
            <el-form-item label="原因">
              <el-input
                v-model="reviewForm.reason"
                type="textarea"
                :rows="5"
                maxlength="500"
                show-word-limit
                placeholder="隐藏或驳回时填写，作者可见"
              />
            </el-form-item>
          </el-form>
        </aside>

        <article class="review-preview">
          <header class="review-preview__header">
            <div>
              <span>预览</span>
              <h2>{{ currentArticle.title || '未命名文章' }}</h2>
              <p>{{ currentArticle.summary || '暂无摘要' }}</p>
            </div>
            <el-tag :type="statusType(currentArticle.status)">{{ statusText(currentArticle.status) }}</el-tag>
          </header>
          <img
            v-if="currentArticle.coverUrl"
            class="review-preview__cover"
            :src="currentArticle.coverUrl"
            alt=""
          />
          <div class="review-preview__meta">
            <span>{{ currentArticle.authorName || '匿名作者' }}</span>
            <span>{{ formatDateTime(currentArticle.updatedAt || currentArticle.createdAt) }}</span>
          </div>
          <article
            v-if="reviewPreviewHtml"
            class="markdown-body review-preview__markdown"
            v-html="reviewPreviewHtml"
          ></article>
          <el-empty v-else description="暂无正文可预览" :image-size="80" />
        </article>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReview">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="commentsVisible" size="560px" title="评论管理">
      <div class="comment-list">
        <article v-for="comment in comments" :key="comment.id" class="comment-item">
          <UserAvatar :name="comment.userName || '匿名用户'" :size="38" :src="comment.userAvatar || ''" />
          <div>
            <header>
              <strong>{{ comment.userName || '匿名用户' }}</strong>
              <el-tag :type="comment.status === 'visible' ? 'success' : 'info'" size="small">
                {{ comment.status === 'visible' ? '显示中' : '已隐藏' }}
              </el-tag>
              <el-tag v-if="comment.parentId" type="info" size="small">回复 #{{ comment.parentId }}</el-tag>
            </header>
            <p>{{ comment.content }}</p>
            <small>{{ formatDateTime(comment.createdAt) }}</small>
          </div>
          <el-button
            size="small"
            :type="comment.status === 'visible' ? 'warning' : 'success'"
            @click="toggleComment(comment)"
          >
            {{ comment.status === 'visible' ? '隐藏' : '恢复' }}
          </el-button>
        </article>
        <el-empty v-if="!comments.length" description="暂无评论" />
      </div>
    </el-drawer>

    <el-drawer v-model="interactionsVisible" size="720px" title="互动记录">
      <div class="interaction-toolbar">
        <el-select v-model="interactionQuery.interactionType" clearable placeholder="互动类型" @change="reloadInteractions">
          <el-option label="点赞" value="like" />
          <el-option label="收藏" value="favorite" />
          <el-option label="分享" value="share" />
        </el-select>
        <el-button :icon="Refresh" @click="reloadInteractions">刷新</el-button>
      </div>
      <el-alert
        v-if="interactionQuery.articleId"
        class="interaction-scope"
        type="info"
        :closable="false"
        :title="`当前仅查看《${currentArticle.title}》`"
      />
      <el-table v-loading="interactionsLoading" :data="interactions" class="admin-table">
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="interactionTagType(row.interactionType)">
              {{ interactionText(row.interactionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="articleTitle" label="文章" min-width="180" show-overflow-tooltip />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="interaction-user">
              <UserAvatar :name="row.userName || '匿名用户'" :size="30" :src="row.userAvatar || ''" />
              <span>{{ row.userName || '匿名用户' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="channel" label="渠道" width="110" />
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="interactionTotal > interactionQuery.pageSize"
        class="pager"
        background
        layout="prev, pager, next"
        :current-page="interactionQuery.page"
        :page-size="interactionQuery.pageSize"
        :total="interactionTotal"
        @current-change="handleInteractionPageChange"
      />
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import UserAvatar from '@/components/common/UserAvatar.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { blogApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { renderMarkdown } from '@/utils/markdown.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh, Tickets } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)
const saving = ref(false)
const reviewVisible = ref(false)
const commentsVisible = ref(false)
const interactionsVisible = ref(false)
const interactionsLoading = ref(false)
const rows = ref<any[]>([])
const comments = ref<any[]>([])
const interactions = ref<any[]>([])
const total = ref(0)
const interactionTotal = ref(0)
const currentArticle = ref<Record<string, any>>({})
const reviewForm = ref({ status: 'published', featured: false, reason: '' })
const reviewPreviewHtml = computed(() =>
  renderMarkdown(currentArticle.value.contentMarkdown || currentArticle.value.summary || ''),
)
const query = ref({
  status: '',
  keyword: '',
  page: 1,
  pageSize: 10,
})
const interactionQuery = ref({
  interactionType: '',
  articleId: null as number | null,
  page: 1,
  pageSize: 10,
})

function statusText(status: string) {
  return (
    { draft: '草稿', pending: '待审核', published: '已发布', hidden: '已隐藏', rejected: '已驳回' }[status] ||
    status ||
    '-'
  )
}

function interactionText(type: string) {
  return ({ like: '点赞', favorite: '收藏', share: '分享' }[type] || type || '-')
}

function interactionTagType(type: string) {
  return ({ like: 'success', favorite: 'warning', share: 'primary' }[type] || 'info')
}

async function fetchList() {
  loading.value = true
  try {
    const data = await blogApi.adminList({
      status: query.value.status || undefined,
      keyword: query.value.keyword || undefined,
      page: query.value.page,
      pageSize: query.value.pageSize,
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

function openReview(row: any) {
  currentArticle.value = row
  reviewForm.value = {
    status: row.status || 'published',
    featured: Boolean(row.featured),
    reason: row.rejectReason || '',
  }
  reviewVisible.value = true
}

async function submitReview() {
  if (reviewForm.value.status === 'rejected' && !reviewForm.value.reason.trim()) {
    ElMessage.warning('驳回文章需要填写原因')
    return
  }
  saving.value = true
  try {
    await blogApi.review(currentArticle.value.id, reviewForm.value)
    ElMessage.success('文章状态已更新')
    reviewVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await blogApi.adminRemove(row.id)
  ElMessage.success('文章已删除')
  fetchList()
}

async function openComments(row: any) {
  currentArticle.value = row
  comments.value = (await blogApi.adminComments(row.id)) || []
  commentsVisible.value = true
}

async function toggleComment(comment: any) {
  const status = comment.status === 'visible' ? 'hidden' : 'visible'
  await blogApi.updateCommentStatus(comment.id, status)
  comments.value = (await blogApi.adminComments(currentArticle.value.id)) || []
  fetchList()
}

async function openInteractions(row?: any) {
  if (row) currentArticle.value = row
  else currentArticle.value = {}
  interactionQuery.value.articleId = row?.id || null
  interactionQuery.value.page = 1
  interactionsVisible.value = true
  await fetchInteractions()
}

async function fetchInteractions() {
  interactionsLoading.value = true
  try {
    const data = await blogApi.adminInteractions({
      interactionType: interactionQuery.value.interactionType || undefined,
      articleId: interactionQuery.value.articleId || undefined,
      page: interactionQuery.value.page,
      pageSize: interactionQuery.value.pageSize,
    })
    interactions.value = data?.list || []
    interactionTotal.value = Number(data?.total || 0)
  } finally {
    interactionsLoading.value = false
  }
}

function reloadInteractions() {
  interactionQuery.value.page = 1
  fetchInteractions()
}

function handleInteractionPageChange(page: number) {
  interactionQuery.value.page = page
  fetchInteractions()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.muted-line,
.danger-line {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.muted-line {
  color: var(--oa-muted);
}

.danger-line {
  color: var(--el-color-danger);
}

:global(.blog-review-dialog) {
  max-width: calc(100vw - 48px);
}

:global(.blog-review-dialog .el-dialog__body) {
  max-height: calc(100vh - 178px);
  overflow: hidden;
}

.review-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  min-height: min(620px, calc(100vh - 240px));
}

.review-panel {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
}

.review-preview {
  min-width: 0;
  overflow-y: auto;
  padding: 20px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.review-preview__header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--oa-divider);
}

.review-preview__header span {
  color: var(--oa-muted);
  font-size: 12px;
}

.review-preview__header h2 {
  margin: 6px 0 8px;
  color: var(--oa-text);
  font-size: 24px;
  line-height: 1.25;
}

.review-preview__header p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.6;
}

.review-preview__cover {
  width: 100%;
  max-height: 260px;
  margin-top: 16px;
  object-fit: cover;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.review-preview__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.review-preview__markdown {
  padding: 0;
  border: 0;
}

.comment-list {
  display: grid;
  gap: 14px;
}

.comment-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 14px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

.comment-item header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.comment-item p {
  margin: 8px 0;
  color: var(--oa-text);
  line-height: 1.6;
}

.comment-item small {
  color: var(--oa-muted);
}

.interaction-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.interaction-scope {
  margin-bottom: 12px;
}

.interaction-user {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.interaction-user span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  :global(.blog-review-dialog) {
    max-width: calc(100vw - 24px);
  }

  :global(.blog-review-dialog .el-dialog__body) {
    max-height: calc(100vh - 144px);
    overflow-y: auto;
  }

  .review-layout {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .review-preview {
    max-height: none;
  }

  .comment-item {
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .comment-item .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
