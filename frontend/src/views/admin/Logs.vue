<template>
  <div class="admin-page">
    <el-tabs v-model="activeTab" @tab-change="fetchList">
      <el-tab-pane label="操作日志" name="operation" />
      <el-tab-pane label="登录日志" name="login" />
    </el-tabs>
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-input v-model="query.keyword" clearable placeholder="搜索用户/IP/操作" style="width: 240px" @keyup.enter="fetchList" />
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户" min-width="120" />
      <el-table-column v-if="activeTab === 'operation'" prop="operation" label="操作" min-width="180" />
      <el-table-column v-if="activeTab === 'operation'" prop="method" label="方法" min-width="160" />
      <el-table-column prop="ip" label="IP" min-width="140" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status || row.success || '-' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt || row.loginAt) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next, sizes" :total="total" v-model:current-page="query.page" v-model:page-size="query.pageSize" @change="fetchList" />
  </div>
</template>

<script>
import { Search } from '@element-plus/icons-vue'
import { logApi } from '../../api'
import { formatDateTime, statusType } from '../../utils/format'

export default {
  name: 'AdminLogs',
  data() {
    return {
      Search,
      activeTab: 'operation',
      loading: false,
      rows: [],
      total: 0,
      query: { keyword: '', page: 1, pageSize: 20 }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDateTime,
    statusType,
    async fetchList() {
      this.loading = true
      try {
        const api = this.activeTab === 'operation' ? logApi.operation : logApi.login
        const result = await api(this.query)
        this.rows = result?.list || result || []
        this.total = result?.total || this.rows.length
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
