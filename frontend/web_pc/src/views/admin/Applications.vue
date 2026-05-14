<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
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
      <div class="toolbar__actions">
        <el-button type="info" :loading="exporting" @click="exportExcel">导出 Excel</el-button>
        <el-button
          type="success"
          :disabled="!batchApproveCandidates.length"
          @click="openBatchPreScreen('approve')"
          >批量初审通过</el-button
        >
        <el-button
          type="danger"
          :disabled="!batchApproveCandidates.length"
          @click="openBatchPreScreen('reject')"
          >批量驳回</el-button
        >
        <el-button
          type="primary"
          :disabled="!batchInterviewCandidates.length"
          @click="openBatchInterview"
          >批量安排面试</el-button
        >
        <el-button
          type="warning"
          :disabled="!batchFinalCandidates.length"
          @click="openBatchFinalDecision"
          >批量终审</el-button
        >
      </div>
    </ViewToolbar>
    <el-table
      v-loading="loading"
      :data="rows"
      class="admin-table"
      @selection-change="selection = $event"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column type="index" label="编号" width="72" />
      <el-table-column prop="id" label="申请ID" width="90" />
      <el-table-column prop="applicantName" label="申请人" min-width="130" />
      <el-table-column prop="clubName" label="社团" min-width="140" />
      <el-table-column prop="preferredDepartment" label="意向部门" min-width="130" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ applicationStatusText(row.status) }}</el-tag>
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

    <el-dialog v-model="batchPreScreenVisible" title="批量处理申请" width="620px">
      <el-form :model="batchPreScreenForm" label-width="98px">
        <el-form-item label="处理人数">
          <el-input :model-value="String(batchApproveCandidates.length)" disabled />
        </el-form-item>
        <el-form-item label="处理动作">
          <el-tag :type="batchPreScreenForm.action === 'approve' ? 'success' : 'danger'">
            {{ approvalLabel(batchPreScreenForm.action) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="batchPreScreenForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-divider content-position="left">涉及申请</el-divider>
      <el-table :data="batchApproveCandidates" max-height="280">
        <el-table-column prop="id" label="申请ID" width="90" />
        <el-table-column prop="applicantName" label="申请人" min-width="120" />
        <el-table-column prop="clubName" label="社团" min-width="130" />
        <el-table-column prop="preferredDepartment" label="意向部门" min-width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{
              applicationStatusText(row.status)
            }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batchPreScreenVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchPreScreen">确认提交</el-button>
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
            applicationStatusText(currentProfileRow.status)
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
              :type="
                record.action === 'approve'
                  ? 'success'
                  : record.action === 'reject'
                    ? 'danger'
                    : 'info'
              "
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

    <el-dialog v-model="batchInterviewVisible" title="批量安排面试" width="760px">
      <el-form :model="batchInterviewForm" label-width="110px">
        <el-form-item label="处理人数">
          <el-input :model-value="String(batchInterviewTargets.length)" disabled />
        </el-form-item>
        <el-form-item label="首场开始时间">
          <el-date-picker
            v-model="batchInterviewForm.scheduledStartAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="每场时长">
          <el-input-number v-model="batchInterviewForm.durationMinutes" :min="5" :step="5" />
          <span class="form-suffix">分钟</span>
        </el-form-item>
        <el-form-item label="场次间隔">
          <el-input-number v-model="batchInterviewForm.gapMinutes" :min="0" :step="5" />
          <span class="form-suffix">分钟</span>
        </el-form-item>
        <el-form-item label="地点/链接">
          <el-input v-model="batchInterviewForm.location" />
        </el-form-item>
        <el-form-item label="方式">
          <el-select v-model="batchInterviewForm.mode" style="width: 180px">
            <el-option label="线下" value="offline" />
            <el-option label="线上" value="online" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">自动编排预览</el-divider>
      <el-table :data="batchInterviewPreview" max-height="320">
        <el-table-column prop="order" label="编号" width="72" />
        <el-table-column prop="applicationId" label="申请ID" width="90" />
        <el-table-column prop="applicantName" label="申请人" min-width="120" />
        <el-table-column prop="preferredDepartment" label="意向部门" min-width="120" />
        <el-table-column label="排期时间" min-width="220">
          <template #default="{ row }">
            {{ formatRange(row.scheduledStartAt, row.scheduledEndAt) }}
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="batchInterviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchInterviewSubmitting" @click="submitBatchInterview"
          >保存</el-button
        >
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
            @change="finalForm.positionId = undefined"
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
            clearable
            placeholder="请选择岗位（可选）"
            style="width: 320px"
          >
            <el-option
              v-for="item in availablePositions"
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

    <el-dialog v-model="batchFinalVisible" title="批量终审" width="760px">
      <el-form :model="batchFinalForm" label-width="110px">
        <el-form-item label="处理人数">
          <el-input :model-value="String(batchFinalTargets.length)" disabled />
        </el-form-item>
        <el-form-item label="终审结果">
          <el-select v-model="batchFinalForm.decision" style="width: 220px">
            <el-option label="录用" value="approved" />
            <el-option label="候补" value="waitlisted" />
            <el-option label="拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchFinalForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="batchFinalForm.decision === 'approved'"
        type="info"
        show-icon
        :closable="false"
        title="批量终审通过时，系统会默认加入第一志愿部门；未选择第一志愿部门的申请将自动跳过。"
      />

      <el-divider content-position="left">处理预览</el-divider>
      <el-table :data="batchFinalPreview" max-height="320">
        <el-table-column prop="order" label="编号" width="72" />
        <el-table-column prop="applicationId" label="申请ID" width="90" />
        <el-table-column prop="applicantName" label="申请人" min-width="120" />
        <el-table-column prop="firstChoiceDepartmentName" label="第一志愿部门" min-width="150" />
        <el-table-column prop="resultText" label="处理结果" min-width="210" />
      </el-table>

      <template #footer>
        <el-button @click="batchFinalVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchFinalSubmitting" @click="submitBatchFinalDecision"
          >提交</el-button
        >
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { applicationApi, approvalApi, clubApi, interviewApi, membershipApi } from '@/api'
import { applicationStatusText, formatDateTime, statusType } from '@/utils/format.ts'
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)

const exporting = ref(false)

const rows = ref<any[]>([])

const total = ref(0)

const selection = ref<any[]>([])

const query = ref({ keyword: '', status: '', page: 1, pageSize: 10 })

const approvalVisible = ref(false)

const approvalForm = ref({ applicationId: '', action: 'approve', comment: '' })

const batchPreScreenVisible = ref(false)

const batchPreScreenForm = ref({ action: 'approve', comment: '' })

const profileVisible = ref(false)

const currentProfileRow = ref<any>(null)

const interviewVisible = ref(false)

const interviewForm = ref({
  applicationId: '',
  round: 1,
  scheduledStartAt: '',
  scheduledEndAt: '',
  location: '',
  mode: 'offline',
})

const batchInterviewVisible = ref(false)

const batchInterviewSubmitting = ref(false)

const batchInterviewTargets = ref<any[]>([])

const batchInterviewForm = ref({
  scheduledStartAt: '',
  durationMinutes: 30,
  gapMinutes: 10,
  location: '',
  mode: 'offline',
})

const finalVisible = ref(false)

const finalSubmitting = ref(false)

const finalForm = ref({
  applicationId: '',
  clubId: '',
  decision: 'approved',
  departmentId: undefined,
  positionId: undefined,
  comment: '',
})

const batchFinalVisible = ref(false)

const batchFinalSubmitting = ref(false)

const batchFinalTargets = ref<any[]>([])

const batchFinalForm = ref({
  decision: 'approved',
  comment: '',
})

const departments = ref<any[]>([])

const positions = ref<any[]>([])

const approvalRecords = ref<any[]>([])

const batchApproveCandidates = computed(() => {
  return selection.value.filter((item) => canPreScreen(item))
})

const batchInterviewCandidates = computed(() => {
  return selection.value.filter((item) => canScheduleInterview(item))
})

const batchFinalCandidates = computed(() => {
  return selection.value.filter((item) => canFinalDecision(item))
})

const batchInterviewPreview = computed(() => {
  const duration = Number(batchInterviewForm.value.durationMinutes) || 30
  const gap = Number(batchInterviewForm.value.gapMinutes) || 0
  const baseTime = batchInterviewForm.value.scheduledStartAt
    ? new Date(batchInterviewForm.value.scheduledStartAt).getTime()
    : Number.NaN
  return batchInterviewTargets.value.map((item, index) => {
    const startTime = Number.isNaN(baseTime)
      ? ''
      : toDateTimeValue(baseTime + index * (duration + gap) * 60 * 1000)
    const endTime = startTime
      ? toDateTimeValue(new Date(startTime).getTime() + duration * 60 * 1000)
      : ''
    return {
      order: index + 1,
      applicationId: item.id,
      applicantName: item.applicantName || `申请 ${item.id}`,
      preferredDepartment: item.preferredDepartment || '未选择部门',
      scheduledStartAt: startTime,
      scheduledEndAt: endTime,
    }
  })
})

const batchFinalPreview = computed(() => {
  return batchFinalTargets.value.map((item, index) => {
    const firstChoiceDepartmentName = item.firstChoiceDepartmentName || '未选择'
    const resultText =
      batchFinalForm.value.decision === 'approved'
        ? item.firstChoiceDepartmentId
          ? `录用至 ${firstChoiceDepartmentName}`
          : '跳过（未选择第一志愿部门）'
        : batchFinalForm.value.decision === 'waitlisted'
          ? '候补'
          : '拒绝'
    return {
      order: index + 1,
      applicationId: item.id,
      applicantName: item.applicantName || `申请 ${item.id}`,
      firstChoiceDepartmentName,
      resultText,
    }
  })
})

const availablePositions = computed(() => {
  if (!finalForm.value.departmentId) return positions.value
  return positions.value.filter((item) => item.departmentId === finalForm.value.departmentId)
})

const profileEntries = computed(() => {
  const profile = currentProfileRow.value?.profile || {}
  return Object.entries(profile)
    .filter(([, value]) => value !== null && value !== undefined && String(value).trim() !== '')
    .map(([key, value]) => ({
      key,
      label: prettifyProfileKey(key),
      value: Array.isArray(value)
        ? value.join(' / ')
        : typeof value === 'object'
          ? JSON.stringify(value)
          : String(value),
    }))
})

function approvalLabel(action: any) {
  if (action === 'approve') return '通过'
  if (action === 'reject') return '驳回'
  if (action === 'transfer') return '转交'
  if (action === 'request_more_info') return '补充材料'
  return action || '-'
}

function canPreScreen(row: any) {
  return ['draft', 'submitted', 'pre_screen_rejected'].includes(row?.status)
}

function canScheduleInterview(row: any) {
  return ['pre_screen_passed'].includes(row?.status)
}

function canFinalDecision(row: any) {
  return ['interviewed'].includes(row?.status)
}

async function openProfile(row: any) {
  currentProfileRow.value = row
  approvalRecords.value = []
  profileVisible.value = true
  try {
    const records = await approvalApi.records(row.id)
    approvalRecords.value = records || []
  } catch (e) {
    console.error('Failed to fetch approval records', e)
  }
}

async function fetchList() {
  loading.value = true
  try {
    const result = await applicationApi.list(query.value)
    rows.value = result?.list || result || []
    total.value = result?.total || rows.value.length
  } finally {
    loading.value = false
  }
}

function openApproval(row: any, action: any) {
  approvalForm.value = { applicationId: row.id, action, comment: '' }
  approvalVisible.value = true
}

async function submitApproval() {
  await approvalApi.approve(approvalForm.value.applicationId, {
    action: approvalForm.value.action,
    node: 'pre_screen',
    comment: approvalForm.value.comment,
  })
  ElMessage.success('审批已提交')
  approvalVisible.value = false
  fetchList()
}

function openBatchPreScreen(action: any) {
  if (!batchApproveCandidates.value.length) {
    ElMessage.warning('请选择可初审的申请')
    return
  }
  batchPreScreenForm.value = { action, comment: action === 'approve' ? '批量通过' : '批量驳回' }
  batchPreScreenVisible.value = true
}

async function submitBatchPreScreen() {
  await approvalApi.batch({
    applicationIds: batchApproveCandidates.value.map((item) => item.id),
    approval: {
      action: batchPreScreenForm.value.action,
      node: 'pre_screen',
      comment: batchPreScreenForm.value.comment || undefined,
    },
  })
  ElMessage.success(
    `批量${approvalLabel(batchPreScreenForm.value.action)}完成，已处理 ${batchApproveCandidates.value.length} 条`,
  )
  batchPreScreenVisible.value = false
  fetchList()
}

function openInterview(row: any) {
  interviewForm.value = {
    applicationId: row.id,
    round: 1,
    scheduledStartAt: '',
    scheduledEndAt: '',
    location: '',
    mode: 'offline',
  }
  interviewVisible.value = true
}

function openBatchInterview() {
  if (!batchInterviewCandidates.value.length) {
    ElMessage.warning('请选择可安排面试的申请')
    return
  }
  batchInterviewTargets.value = [...batchInterviewCandidates.value]
  batchInterviewForm.value = {
    scheduledStartAt: '',
    durationMinutes: 30,
    gapMinutes: 10,
    location: '',
    mode: 'offline',
  }
  batchInterviewVisible.value = true
}

async function createInterview() {
  await interviewApi.create(interviewForm.value)
  ElMessage.success('面试已安排')
  interviewVisible.value = false
  fetchList()
}

async function submitBatchInterview() {
  if (batchInterviewSubmitting.value) return
  if (!batchInterviewForm.value.scheduledStartAt) {
    ElMessage.error('请选择首场开始时间')
    return
  }
  batchInterviewSubmitting.value = true
  try {
    await Promise.all(
      batchInterviewPreview.value.map((item) =>
        interviewApi.create({
          applicationId: item.applicationId,
          round: 1,
          scheduledStartAt: item.scheduledStartAt,
          scheduledEndAt: item.scheduledEndAt,
          location: batchInterviewForm.value.location,
          mode: batchInterviewForm.value.mode,
        }),
      ),
    )
    ElMessage.success(`已批量安排 ${batchInterviewTargets.value.length} 场面试`)
    batchInterviewVisible.value = false
    fetchList()
  } finally {
    batchInterviewSubmitting.value = false
  }
}

async function openFinalDecision(row: any) {
  finalForm.value = {
    applicationId: row.id,
    clubId: row.clubId,
    decision: 'approved',
    departmentId: row.firstChoiceDepartmentId || undefined,
    positionId: undefined,
    comment: '',
  }
  departments.value = []
  positions.value = []
  if (row.clubId) {
    const [departments, positions] = await Promise.all([
      clubApi.departments(row.clubId),
      clubApi.positions(row.clubId),
    ])
    departments.value = departments || []
    positions.value = positions || []
  }
  finalVisible.value = true
}

function openBatchFinalDecision() {
  if (!batchFinalCandidates.value.length) {
    ElMessage.warning('请选择可终审的申请')
    return
  }
  batchFinalTargets.value = [...batchFinalCandidates.value]
  batchFinalForm.value = {
    decision: 'approved',
    comment: '',
  }
  batchFinalVisible.value = true
}

function prettifyProfileKey(key: any) {
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
}

async function submitFinalDecision() {
  if (finalSubmitting.value) return
  if (finalForm.value.decision === 'approved') {
    if (!finalForm.value.departmentId) {
      ElMessage.error('请选择部门')
      return
    }
  }
  finalSubmitting.value = true
  try {
    await membershipApi.finalDecision(finalForm.value.applicationId, {
      decision: finalForm.value.decision,
      departmentId:
        finalForm.value.decision === 'approved' ? finalForm.value.departmentId : undefined,
      positionId: finalForm.value.decision === 'approved' ? finalForm.value.positionId : undefined,
      comment: finalForm.value.comment,
    })
    ElMessage.success('终审已提交')
    finalVisible.value = false
    fetchList()
  } finally {
    finalSubmitting.value = false
  }
}

async function submitBatchFinalDecision() {
  if (batchFinalSubmitting.value) return
  const targets =
    batchFinalForm.value.decision === 'approved'
      ? batchFinalTargets.value.filter((item) => item.firstChoiceDepartmentId)
      : batchFinalTargets.value
  const skippedCount = batchFinalTargets.value.length - targets.length
  if (!targets.length) {
    ElMessage.warning('没有可提交的终审记录')
    return
  }
  batchFinalSubmitting.value = true
  try {
    await Promise.all(
      targets.map((item) =>
        membershipApi.finalDecision(item.id, {
          decision: batchFinalForm.value.decision,
          departmentId:
            batchFinalForm.value.decision === 'approved' ? item.firstChoiceDepartmentId : undefined,
          comment: batchFinalForm.value.comment,
        }),
      ),
    )
    ElMessage.success(
      skippedCount
        ? `批量终审完成，已处理 ${targets.length} 条，跳过 ${skippedCount} 条`
        : `批量终审完成，已处理 ${targets.length} 条`,
    )
    batchFinalVisible.value = false
    fetchList()
  } finally {
    batchFinalSubmitting.value = false
  }
}

function toDateTimeValue(value: any) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function exportExcel() {
  exporting.value = true
  try {
    const blob = await applicationApi.export({
      campaignId: query.value.campaignId || undefined,
      clubId: query.value.clubId || undefined,
      status: query.value.status || undefined,
      keyword: query.value.keyword || undefined,
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '入会申请记录.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Excel 已开始下载')
  } catch (e) {
    console.error('导出失败', e)
  } finally {
    exporting.value = false
  }
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

.toolbar__actions {
  display: flex;
  gap: 12px;
}

.form-suffix {
  margin-left: 10px;
  color: var(--oa-muted);
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
  background: #f5f5f7;
  border-radius: 6px;
  font-size: 13px;
  color: #7a7a7a;
}
</style>
