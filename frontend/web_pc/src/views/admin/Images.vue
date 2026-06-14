<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索文件名或链接"
          style="width: 240px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select v-model="query.status" clearable placeholder="状态" @change="reload">
          <el-option label="正常" value="active" />
          <el-option label="已删除" value="deleted" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="reload">刷新</el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table image-table">
      <el-table-column label="图片" width="120">
        <template #default="{ row }">
          <el-image
            v-if="row.status === 'active'"
            class="image-thumb"
            :src="row.url"
            fit="cover"
            :preview-src-list="[row.url]"
          />
          <div v-else class="image-thumb image-thumb--deleted">已删除</div>
        </template>
      </el-table-column>
      <el-table-column label="文件" min-width="240">
        <template #default="{ row }">
          <strong>{{ row.originalName || row.fileName }}</strong>
          <p class="muted-line">{{ row.fileName }}</p>
          <p class="muted-line">{{ row.contentType || '-' }} · {{ formatFileSize(row.fileSize) }}</p>
        </template>
      </el-table-column>
      <el-table-column label="上传人" min-width="150">
        <template #default="{ row }">
          <div class="uploader-cell">
            <UserAvatar :name="row.uploaderName || '未知用户'" :size="32" :src="row.uploaderAvatar || ''" />
            <span>{{ row.uploaderName || '未知用户' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">
            {{ row.status === 'active' ? '正常' : '已删除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.status !== 'active'" @click="copyText(row.url, '链接已复制')">
            复制链接
          </el-button>
          <el-button
            link
            type="primary"
            :disabled="row.status !== 'active'"
            @click="copyText(row.markdown, 'Markdown 已复制')"
          >
            Markdown
          </el-button>
          <el-popconfirm
            v-if="row.status === 'active'"
            title="删除后图片链接会失效，确定删除？"
            @confirm="remove(row)"
          >
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
  </ViewPage>
</template>

<script setup lang="ts">
import UserAvatar from '@/components/common/UserAvatar.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { imageHostingApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = ref({
  keyword: '',
  status: '',
  page: 1,
  pageSize: 10,
})

async function fetchList() {
  loading.value = true
  try {
    const data = await imageHostingApi.adminList({
      keyword: query.value.keyword || undefined,
      status: query.value.status || undefined,
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

async function remove(row: any) {
  await imageHostingApi.adminRemove(row.id)
  ElMessage.success('图片已删除')
  fetchList()
}

async function copyText(value: string, message: string) {
  if (!value) return
  try {
    await navigator.clipboard?.writeText(value)
    ElMessage.success(message)
  } catch (_error) {
    ElMessage.warning('当前浏览器不允许直接复制，请手动选择文本')
  }
}

function formatFileSize(value: number) {
  const size = Number(value || 0)
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${size} B`
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.image-table :deep(.cell) {
  line-height: 1.5;
}

.image-thumb {
  display: grid;
  width: 84px;
  height: 64px;
  overflow: hidden;
  place-items: center;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.image-thumb--deleted {
  border-style: dashed;
}

.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.uploader-cell {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.uploader-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
