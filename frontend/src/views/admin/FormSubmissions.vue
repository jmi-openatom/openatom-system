<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-select v-if="clubs.length > 1" v-model="selectedClubId" filterable placeholder="选择社团" style="width: 220px" @change="handleClubChange">
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-select v-model="selectedCampaignId" filterable placeholder="选择表单" style="width: 260px" @change="fetchList">
          <el-option v-for="item in campaigns" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-input v-model="keyword" clearable placeholder="搜索提交人/联系方式" style="width: 240px" @keyup.enter="fetchList" />
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="success" :disabled="!selectedCampaignId" :loading="exporting" @click="exportExcel">导出 Excel</el-button>
    </div>

    <el-alert
      v-if="currentCampaign"
      type="info"
      show-icon
      :closable="false"
      :title="`当前表单：${currentCampaign.name}`"
      :description="`收集时间：${formatRange(currentCampaign.applyStartAt, currentCampaign.applyEndAt)}`"
    />

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="submitterName" label="提交人" min-width="140">
        <template #default="{ row }">{{ row.submitterName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="submitterAccount" label="账号" min-width="140">
        <template #default="{ row }">{{ row.submitterAccount || '匿名提交' }}</template>
      </el-table-column>
      <el-table-column prop="contact" label="联系方式" min-width="180">
        <template #default="{ row }">{{ row.contact || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看内容</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="当前表单暂无提交记录" />

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="detailVisible" title="提交内容" width="760px">
      <div v-if="detailRow" class="detail-panel">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提交人">{{ detailRow.submitterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ detailRow.contact || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ detailRow.submitterAccount || '匿名提交' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detailRow.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">字段内容</el-divider>
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="field in previewEntries" :key="field.key" :label="field.label">
            <div class="detail-value">{{ field.value }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { clubApi, formSubmissionApi } from '../../api'
import { formatDateTime } from '../../utils/format'

export default {
  name: 'AdminFormSubmissions',
  data() {
    return {
      Refresh,
      clubs: [],
      campaigns: [],
      selectedClubId: '',
      selectedCampaignId: '',
      keyword: '',
      loading: false,
      exporting: false,
      rows: [],
      page: 1,
      pageSize: 10,
      total: 0,
      detailVisible: false,
      detailRow: null
    }
  },
  computed: {
    currentCampaign() {
      return this.campaigns.find((item) => item.id === this.selectedCampaignId) || null
    },
    currentSchema() {
      return this.parseSchema(this.currentCampaign?.formSchema)
    },
    previewEntries() {
      const values = this.detailRow?.formData || {}
      return this.currentSchema.map((field) => ({
        key: field.key,
        label: field.label || field.key,
        value: this.formatValue(values[field.key])
      }))
    }
  },
  async created() {
    const queryClubId = Number(this.$route.query.clubId || '')
    if (queryClubId) {
      this.selectedClubId = queryClubId
    }
    const queryCampaignId = Number(this.$route.query.campaignId || '')
    if (queryCampaignId) {
      this.selectedCampaignId = queryCampaignId
    }
    await this.loadClubs()
  },
  methods: {
    formatDateTime,
    async loadClubs() {
      const result = await clubApi.list({ page: 1, pageSize: 100 })
      this.clubs = result?.list || result || []
      if (!this.selectedClubId && this.clubs.length) {
        this.selectedClubId = this.clubs[0].id
      }
      await this.handleClubChange(this.selectedClubId)
    },
    async handleClubChange(clubId) {
      if (!clubId) {
        this.campaigns = []
        this.selectedCampaignId = ''
        this.rows = []
        return
      }
      const result = await clubApi.campaigns(clubId)
      this.campaigns = result?.list || result || []
      const hasCurrent = this.campaigns.some((item) => item.id === this.selectedCampaignId)
      if (!hasCurrent) {
        this.selectedCampaignId = this.campaigns[0]?.id || ''
      }
      this.page = 1
      await this.fetchList()
    },
    async fetchList() {
      if (!this.selectedCampaignId) {
        this.rows = []
        this.total = 0
        return
      }
      this.loading = true
      try {
        const result = await formSubmissionApi.list(this.selectedCampaignId, {
          keyword: this.keyword || undefined,
          page: this.page,
          pageSize: this.pageSize
        })
        this.rows = result?.list || []
        this.total = result?.total || 0
      } finally {
        this.loading = false
      }
    },
    async exportExcel() {
      if (!this.selectedCampaignId) return
      this.exporting = true
      try {
        const blob = await formSubmissionApi.export(this.selectedCampaignId)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${this.currentCampaign?.name || '表单记录'}.xlsx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        ElMessage.success('Excel 已开始下载')
      } finally {
        this.exporting = false
      }
    },
    handlePageChange(page) {
      this.page = page
      this.fetchList()
    },
    openDetail(row) {
      this.detailRow = row
      this.detailVisible = true
    },
    parseSchema(value) {
      if (Array.isArray(value)) return value
      if (!value) return []
      try {
        return JSON.parse(value)
      } catch {
        return []
      }
    },
    formatValue(value) {
      if (Array.isArray(value)) return value.join(' / ')
      if (value && typeof value === 'object') return JSON.stringify(value)
      return value || '-'
    },
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '-'} 至 ${formatDateTime(endAt) || '-'}`
    }
  }
}
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-panel {
  display: grid;
  gap: 16px;
}

.detail-value {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
