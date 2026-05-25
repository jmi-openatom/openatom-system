<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select v-model="query.status" clearable placeholder="文章状态" @change="reload">
          <el-option label="草稿" value="draft" />
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
      <el-table-column label="数据" width="130">
        <template #default="{ row }">
          {{ row.viewCount || 0 }} 阅读 · {{ row.commentCount || 0 }} 评论
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReview(row)">管控</el-button>
          <el-button link type="success" @click="openComments(row)">评论</el-button>
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

    <el-dialog v-model="reviewVisible" title="文章管控" width="620px">
      <el-form label-width="92px">
        <el-form-item label="文章">
          <strong>{{ currentArticle.title }}</strong>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="reviewForm.status">
            <el-option label="草稿" value="draft" />
            <el-option label="发布" value="published" />
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
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="隐藏或驳回时填写，作者可见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReview">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="commentsVisible" size="560px" title="评论管理">
      <div class="comment-list">
        <article v-for="comment in comments" :key="comment.id" class="comment-item">
          <div>
            <header>
              <strong>{{ comment.userName || '匿名用户' }}</strong>
              <el-tag :type="comment.status === 'visible' ? 'success' : 'info'" size="small">
                {{ comment.status === 'visible' ? '显示中' : '已隐藏' }}
              </el-tag>
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
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { blogApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'

const loading = ref(false)
const saving = ref(false)
const reviewVisible = ref(false)
const commentsVisible = ref(false)
const rows = ref<any[]>([])
const comments = ref<any[]>([])
const total = ref(0)
const currentArticle = ref<Record<string, any>>({})
const reviewForm = ref({ status: 'published', featured: false, reason: '' })
const query = ref({
  status: '',
  keyword: '',
  page: 1,
  pageSize: 10,
})

function statusText(status: string) {
  return (
    { draft: '草稿', published: '已发布', hidden: '已隐藏', rejected: '已驳回' }[status] ||
    status ||
    '-'
  )
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

.comment-list {
  display: grid;
  gap: 14px;
}

.comment-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
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
</style>
