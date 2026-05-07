<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索申请人/社团"
          style="width: 220px"
          @keyup.enter="fetchList"
        />
        <el-select v-model="query.status" clearable placeholder="申请状态" style="width: 150px">
          <el-option label="草稿" value="draft" />
          <el-option label="已提交" value="submitted" />
          <el-option label="初审通过" value="pre_screen_passed" />
          <el-option label="初审驳回" value="pre_screen_rejected" />
          <el-option label="面试已安排" value="interview_scheduled" />
          <el-option label="已面试" value="interviewed" />
          <el-option label="已录用" value="final_approved" />
          <el-option label="候补" value="waitlisted" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="已撤回" value="cancelled" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
      </div>
      <el-button type="success" :disabled="!selection.length" @click="batchApprove"
        >批量通过</el-button
      >
    </div>
    <el-table
      v-loading="loading"
      :data="rows"
      class="admin-table"
      @selection-change="selection = $event"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="applicantName" label="申请人" min-width="130" />
      <el-table-column prop="clubName" label="社团" min-width="140" />
      <el-table-column prop="preferredDepartment" label="意向部门" min-width="130" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="info" @click="openProfile(row)">查看表单</el-button>
          <el-button
            v-if="canPreScreen(row)"
            link
            type="success"
            @click="openApproval(row, 'approve')"
            >通过</el-button
          >
          <el-button
            v-if="canPreScreen(row)"
            link
            type="danger"
            @click="openApproval(row, 'reject')"
            >驳回</el-button
          >
          <el-button
            v-if="canScheduleInterview(row)"
            link
            type="primary"
            @click="openInterview(row)"
            >安排面试
          </el-button>
          <el-button
            v-if="canFinalDecision(row)"
            link
            type="warning"
            @click="openFinalDecision(row)"
            >终审</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      @change="fetchList"
    />

    <el-dialog v-model="approvalVisible" title="处理申请" width="520px">
      <el-form :model="approvalForm" label-width="86px">
        <el-form-item label="处理结果">
          <el-tag :type="approvalForm.action === 'approve' ? 'success' : 'danger'"
            >{{ approvalLabel(approvalForm.action) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.comment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="profileVisible" title="表单详情" width="720px">
      <div v-if="currentProfileRow" class="profile-panel">
        <div class="profile-header">
          <div>
            <strong>{{ currentProfileRow.applicantName || `申请 ${currentProfileRow.id}` }}</strong>
            <p>
              {{ currentProfileRow.clubName || '-' }} /
              {{ currentProfileRow.preferredDepartment || '未选择部门' }}
            </p>
          </div>
          <el-tag :type="statusType(currentProfileRow.status)">{{
            currentProfileRow.status || '-'
          }}</el-tag>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请ID">{{ currentProfileRow.id }}</el-descriptions-item>
          <el-descriptions-item label="提交时间"
            >{{ formatDateTime(currentProfileRow.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">填写内容</el-divider>
        <el-empty v-if="!profileEntries.length" description="暂无表单内容" />
        <el-descriptions v-else :column="1" border>
          <el-descriptions-item v-for="item in profileEntries" :key="item.key" :label="item.label">
            <div class="profile-value">{{ item.value }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="approvalRecords.length">
          <el-divider content-position="left">审核历史</el-divider>
          <el-timeline>
            <el-timeline-item
              v-for="record in approvalRecords"
              :key="record.id"
              :timestamp="formatDateTime(record.createdAt)"
              :type="record.action === 'approve' ? 'success' : record.action === 'reject' ? 'danger' : 'info'"
            >
              <strong>{{ approvalLabel(record.action) }}</strong>
              <p v-if="record.comment" class="record-comment">{{ record.comment }}</p>
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="interviewVisible" title="安排面试" width="560px">
      <el-form :model="interviewForm" label-width="98px">
        <el-form-item label="申请ID">
          <el-input v-model="interviewForm.applicationId" disabled />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="interviewForm.scheduledStartAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="interviewForm.scheduledEndAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="地点/链接">
          <el-input v-model="interviewForm.location" />
        </el-form-item>
        <el-form-item label="方式">
          <el-select v-model="interviewForm.mode">
            <el-option label="线下" value="offline" />
            <el-option label="线上" value="online" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interviewVisible = false">取消</el-button>
        <el-button type="primary" @click="createInterview">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finalVisible" title="终审决策" width="600px">
      <el-form :model="finalForm" label-width="98px">
        <el-form-item label="申请ID">
          <el-input v-model="finalForm.applicationId" disabled />
        </el-form-item>
        <el-form-item label="终审结果">
          <el-select v-model="finalForm.decision" style="width: 220px">
            <el-option label="录用" value="approved" />
            <el-option label="候补" value="waitlisted" />
            <el-option label="拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="finalForm.decision === 'approved'" label="部门">
          <el-select
            v-model="finalForm.departmentId"
            filterable
            placeholder="请选择部门"
            style="width: 320px"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name || `部门 ${item.id}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="finalForm.decision === 'approved'" label="岗位">
          <el-select
            v-model="finalForm.positionId"
            filterable
            placeholder="请选择岗位"
            style="width: 320px"
          >
            <el-option
              v-for="item in positions"
              :key="item.id"
              :label="item.name || `岗位 ${item.id}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="finalForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finalVisible = false">取消</el-button>
        <el-button type="primary" :loading="finalSubmitting" @click="submitFinalDecision"
          >提交</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { applicationApi, approvalApi, clubApi, interviewApi, membershipApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'

export default {
  name: 'AdminApplications',
  data() {
    return {
      Search,
      loading: false,
      rows: [],
      total: 0,
      selection: [],
      query: { keyword: '', status: '', page: 1, pageSize: 10 },
      approvalVisible: false,
      approvalForm: { applicationId: '', action: 'approve', comment: '' },
      profileVisible: false,
      currentProfileRow: null,
      interviewVisible: false,
      interviewForm: {
        applicationId: '',
        round: 1,
        scheduledStartAt: '',
        scheduledEndAt: '',
        location: '',
        mode: 'offline',
      },
      finalVisible: false,
      finalSubmitting: false,
      finalForm: {
        applicationId: '',
        clubId: '',
        decision: 'approved',
        departmentId: undefined,
        positionId: undefined,
        comment: '',
      },
      departments: [],
      positions: [],
      approvalRecords: [],
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatDateTime,
    statusType,
    approvalLabel(action) {
      if (action === 'approve') return '通过'
      if (action === 'reject') return '驳回'
      if (action === 'transfer') return '转交'
      if (action === 'request_more_info') return '补充材料'
      return action || '-'
    },
    canPreScreen(row) {
      return ['draft', 'submitted', 'pre_screen_rejected'].includes(row?.status)
    },
    canScheduleInterview(row) {
      return ['pre_screen_passed'].includes(row?.status)
    },
    canFinalDecision(row) {
      return ['interviewed'].includes(row?.status)
    },
    async openProfile(row) {
      this.currentProfileRow = row
      this.approvalRecords = []
      this.profileVisible = true
      try {
        const records = await approvalApi.records(row.id)
        this.approvalRecords = records || []
      } catch (e) {
        console.error('Failed to fetch approval records', e)
      }
    },
    async fetchList() {
      this.loading = true
      try {
        const result = await applicationApi.list(this.query)
        this.rows = result?.list || result || []
        this.total = result?.total || this.rows.length
      } finally {
        this.loading = false
      }
    },
    openApproval(row, action) {
      this.approvalForm = { applicationId: row.id, action, comment: '' }
      this.approvalVisible = true
    },
    async submitApproval() {
      await approvalApi.approve(this.approvalForm.applicationId, {
        action: this.approvalForm.action,
        node: 'pre_screen',
        comment: this.approvalForm.comment,
      })
      ElMessage.success('审批已提交')
      this.approvalVisible = false
      this.fetchList()
    },
    async batchApprove() {
      await approvalApi.batch({
        applicationIds: this.selection.map((item) => item.id),
        approval: { action: 'approve', node: 'pre_screen', comment: '批量通过' },
      })
      ElMessage.success('批量审批完成')
      this.fetchList()
    },
    openInterview(row) {
      this.interviewForm = {
        applicationId: row.id,
        round: 1,
        scheduledStartAt: '',
        scheduledEndAt: '',
        location: '',
        mode: 'offline',
      }
      this.interviewVisible = true
    },
    async createInterview() {
      await interviewApi.create(this.interviewForm)
      ElMessage.success('面试已安排')
      this.interviewVisible = false
      this.fetchList()
    },
    async openFinalDecision(row) {
      this.finalForm = {
        applicationId: row.id,
        clubId: row.clubId,
        decision: 'approved',
        departmentId: row.firstChoiceDepartmentId || row.secondChoiceDepartmentId || undefined,
        positionId: undefined,
        comment: '',
      }
      this.departments = []
      this.positions = []
      if (row.clubId) {
        const [departments, positions] = await Promise.all([
          clubApi.departments(row.clubId),
          clubApi.positions(row.clubId),
        ])
        this.departments = departments || []
        this.positions = positions || []
      }
      this.finalVisible = true
    },
    prettifyProfileKey(key) {
      return (
        {
          applicantName: '联系人',
          name: '姓名',
          realName: '姓名',
          contact: '联系方式',
          studentId: '学号/工号',
          reason: '申请理由',
          strengths: '个人优势',
        }[key] || key
      )
    },
    async submitFinalDecision() {
      if (this.finalSubmitting) return
      if (this.finalForm.decision === 'approved') {
        if (!this.finalForm.departmentId) {
          ElMessage.error('请选择部门')
          return
        }
        if (!this.finalForm.positionId) {
          ElMessage.error('请选择岗位')
          return
        }
      }
      this.finalSubmitting = true
      try {
        await membershipApi.finalDecision(this.finalForm.applicationId, {
          decision: this.finalForm.decision,
          departmentId:
            this.finalForm.decision === 'approved' ? this.finalForm.departmentId : undefined,
          positionId:
            this.finalForm.decision === 'approved' ? this.finalForm.positionId : undefined,
          comment: this.finalForm.comment,
        })
        ElMessage.success('终审已提交')
        this.finalVisible = false
        this.fetchList()
      } finally {
        this.finalSubmitting = false
      }
    },
  },
  computed: {
    profileEntries() {
      const profile = this.currentProfileRow?.profile || {}
      return Object.entries(profile)
        .filter(([, value]) => value !== null && value !== undefined && String(value).trim() !== '')
        .map(([key, value]) => ({
          key,
          label: this.prettifyProfileKey(key),
          value: Array.isArray(value)
            ? value.join(' / ')
            : typeof value === 'object'
              ? JSON.stringify(value)
              : String(value),
        }))
    },
  },
}
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.profile-panel {
  display: grid;
  gap: 16px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.profile-header p {
  margin: 6px 0 0;
  color: var(--oa-muted);
}

.profile-value {
  white-space: pre-wrap;
  word-break: break-word;
}

.record-comment {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
}
</style>
