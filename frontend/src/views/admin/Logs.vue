<template>
    <div class="admin-page">
        <el-tabs v-model="activeTab" @tab-change="fetchList">
            <el-tab-pane label="操作日志" name="operation"/>
            <el-tab-pane label="登录日志" name="login"/>
        </el-tabs>
        <div class="toolbar">
            <div class="toolbar__filters">
                <el-input v-model="query.keyword" clearable placeholder="搜索用户/IP/操作" style="width: 240px"
                          @keyup.enter="fetchList"/>
                <el-button :icon="Search" type="primary" @click="fetchList">查询</el-button>
            </div>
        </div>
        <el-table v-loading="loading" :data="rows" class="admin-table">
            <el-table-column label="ID" prop="id" width="80"/>
            <el-table-column v-if="activeTab === 'operation'" label="操作" prop="action"/>
            <el-table-column v-if="activeTab === 'operation'" label="详细" prop="content"/>
            <el-table-column label="模块" prop="module" width="120">
                <template #default="{ row }">{{ row.module || 'login' }}</template>
            </el-table-column>
            >
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
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total"
                       class="pager" layout="total, prev, pager, next, sizes" @change="fetchList"/>
    </div>
</template>

<script>
import {Search} from '@element-plus/icons-vue'
import {logApi} from '../../api'
import {formatDateTime, statusType} from '../../utils/format'

export default {
    name: 'AdminLogs',
    data() {
        return {
            Search,
            activeTab: 'operation',
            loading: false,
            rows: [],
            total: 0,
            query: {keyword: '', page: 1, pageSize: 20}
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
