<template>
  <ViewPage class="admin-page">
    <ViewToolbar
      title="成员主页评论"
      description="查看成员主页的全部评论，隐藏不合适内容或恢复已隐藏评论。"
    >
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索评论内容"
          style="width: 240px"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-select
          v-model="query.status"
          clearable
          placeholder="显示状态"
          style="width: 140px"
          @change="reload"
        >
          <el-option label="显示中" value="visible" />
          <el-option label="已隐藏" value="hidden" />
        </el-select>
        <el-button :icon="Refresh" type="primary" @click="reload">查询</el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table" row-key="id">
      <el-table-column label="评论者" min-width="150">
        <template #default="{ row }">
          <div class="user-cell">
            <UserAvatar :name="row.userName || '未知用户'" :size="34" :src="row.userAvatar || ''" />
            <div>
              <strong>{{ row.userName || '未知用户' }}</strong>
              <small>UID {{ row.userId }}</small>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="目标主页" min-width="170">
        <template #default="{ row }">
          <router-link :to="`/members/${row.profileSlug}`" class="profile-link" target="_blank">
            {{ row.profileName || `成员 ${row.profileUserId}` }}
          </router-link>
          <small class="secondary-line">{{ row.profileSlug }}</small>
        </template>
      </el-table-column>
      <el-table-column label="评论内容" min-width="320">
        <template #default="{ row }">
          <p class="comment-content">{{ row.content }}</p>
          <el-tag v-if="row.parentId" size="small" type="info">回复 #{{ row.parentId }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'visible' ? 'success' : 'info'">
            {{ row.status === 'visible' ? '显示中' : '已隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            :loading="updatingId === row.id"
            :type="row.status === 'visible' ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'visible' ? '隐藏' : '恢复' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="暂无符合条件的评论" />
    <el-pagination
      class="pager"
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      @change="fetchList"
    />
  </ViewPage>
</template>

<script setup lang="ts">
import UserAvatar from '@/components/common/UserAvatar.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { memberProfileCommentApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { hasPermission } from '@/utils/permission.ts'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)
const updatingId = ref<number | null>(null)
const rows = ref<any[]>([])
const total = ref(0)
const query = ref({ keyword: '', status: '', page: 1, pageSize: 10 })
const canManage = computed(() => hasPermission('member-profile-comment:manage'))

async function fetchList() {
  loading.value = true
  try {
    const data = await memberProfileCommentApi.list({
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

async function toggleStatus(row: any) {
  const hiding = row.status === 'visible'
  if (hiding) {
    await ElMessageBox.confirm(
      '隐藏后该评论将立即从成员主页消失，但可以在后台恢复。',
      '确认隐藏评论',
      { type: 'warning', confirmButtonText: '隐藏', cancelButtonText: '取消' },
    )
  }
  updatingId.value = row.id
  try {
    await memberProfileCommentApi.updateStatus(row.id, hiding ? 'hidden' : 'visible')
    ElMessage.success(hiding ? '评论已隐藏' : '评论已恢复')
    await fetchList()
  } finally {
    updatingId.value = null
  }
}

onMounted(fetchList)
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell div,
.profile-link + .secondary-line {
  min-width: 0;
}

.user-cell strong,
.user-cell small,
.secondary-line {
  display: block;
}

.user-cell small,
.secondary-line {
  margin-top: 3px;
  color: var(--oa-muted);
  font-size: 12px;
}

.profile-link {
  color: var(--el-color-primary);
  font-weight: 600;
  text-decoration: none;
}

.profile-link:hover,
.profile-link:focus-visible {
  text-decoration: underline;
}

.comment-content {
  margin: 0 0 6px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.55;
}

@media (max-width: 768px) {
  :deep(.toolbar__filters) {
    width: 100%;
  }

  :deep(.toolbar__filters .el-input),
  :deep(.toolbar__filters .el-select) {
    width: 100% !important;
  }
}
</style>
