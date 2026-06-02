<template>
  <ViewPage class="admin-page">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="操作日志" name="operation" />
      <el-tab-pane label="登录日志" name="login" />
    </el-tabs>
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索用户/IP/操作"
          style="width: 240px"
          @keyup.enter="reload"
        />
        <el-button :icon="Search" type="primary" @click="reload">查询</el-button>
      </div>
    </ViewToolbar>
    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column v-if="activeTab === 'operation'" label="操作" prop="action" />
      <el-table-column v-if="activeTab === 'operation'" label="详细" prop="content" />
      <el-table-column label="模块" prop="module" width="120">
        <template #default="{ row }">{{ row.module || 'login' }}</template>
      </el-table-column>
      <el-table-column label="时间" min-width="60" prop="createdAt">
        <template #default="{ row }">{{ formatDateTime(row.createdAt || row.loginAt) }}</template>
      </el-table-column>
      <el-table-column label="登陆IP" min-width="70">
        <template #default="{ row }">{{ row.ip || '无数据' }}</template>
      </el-table-column>
      <el-table-column label="用户" min-width="70">
        <template #default="{ row }">{{ row.userId || '无数据' }}</template>
      </el-table-column>
      <el-table-column label="设备" min-width="70">
        <template #default="{ row }">{{ row.userAgent || '无数据' }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      class="pager"
      layout="total, prev, pager, next, sizes"
      @current-change="handlePageChange"
      @size-change="handlePageSizeChange"
    />
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { Search } from '@element-plus/icons-vue'
import { logApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const activeTab = ref('operation')

const loading = ref(false)

const rows = ref<any[]>([])

const total = ref(0)

const query = ref({ keyword: '', page: 1, pageSize: 20 })

async function fetchList() {
  loading.value = true
  try {
    const api = activeTab.value === 'operation' ? logApi.operation : logApi.login
    const result = await api({
      keyword: query.value.keyword || undefined,
      page: query.value.page,
      pageSize: query.value.pageSize,
    })
    rows.value = result?.list || []
    total.value = Number(result?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  fetchList()
}

function handleTabChange() {
  reload()
}

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function handlePageSizeChange(pageSize: number) {
  query.value.pageSize = pageSize
  query.value.page = 1
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
