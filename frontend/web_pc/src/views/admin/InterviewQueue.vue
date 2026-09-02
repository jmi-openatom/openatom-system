<template>
  <ViewPage class="queue-page">
    <div class="queue-header">
      <div>
        <h2>面试现场</h2>
        <p>工作人员手动签到，面试间完成全组评价后方可叫下一位</p>
      </div>
      <div class="queue-header__actions">
        <el-select
          v-model="sessionId"
          filterable
          placeholder="选择面试场次"
          style="width: 280px"
          @change="loadQueue(true)"
        >
          <el-option
            v-for="session in sessions"
            :key="session.id"
            :label="session.name"
            :value="session.id"
          />
        </el-select>
        <el-button :disabled="!sessionId" @click="openScreen">打开叫号大屏</el-button>
        <el-button :disabled="!sessionId" :loading="exporting" @click="exportSummary"
          >导出评价汇总</el-button
        >
        <el-button :disabled="!sessionId" @click="showOperations">操作记录</el-button>
        <el-button v-if="queue?.sessionStatus === 'completed'" type="warning" @click="reopenSession"
          >重新开启</el-button
        >
        <el-button v-else-if="sessionId" type="danger" plain @click="completeSession"
          >结束场次</el-button
        >
        <el-button :loading="loading" @click="loadQueue(true)">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="!sessionId && !loading" description="请先选择已发布的面试场次" />
    <template v-else-if="queue">
      <section class="stats-grid" aria-label="现场实时统计">
        <article v-for="stat in stats" :key="stat.key" class="stat-card">
          <span>{{ stat.label }}</span
          ><strong>{{ stat.value }}</strong
          ><small>{{ stat.hint }}</small>
        </article>
      </section>
      <el-alert
        v-if="sessionClosed"
        title="该场次已结束，现场操作已锁定；如需纠错，请先重新开启场次。"
        type="warning"
        show-icon
        :closable="false"
      />
      <section class="room-grid" aria-label="面试间叫号状态">
        <article v-for="room in queue.rooms" :key="room.roomId" class="room-card">
          <header>
            <div>
              <span>面试间</span>
              <h3>{{ room.name }}</h3>
              <p>{{ room.location || '地点待定' }}</p>
            </div>
            <el-tag effect="plain">候场 {{ room.waitingCount }} 人</el-tag>
          </header>
          <div v-if="room.current" class="room-current">
            <span>当前叫号</span>
            <strong>{{ queueNumber(room.current) }} · {{ room.current.applicantName }}</strong>
            <small>{{
              room.current.interviewStatus === 'completed'
                ? '全组评价已完成，可以叫下一位'
                : '面试进行中，等待全组提交评价'
            }}</small>
          </div>
          <div v-else class="room-empty">尚未叫号</div>
          <footer>
            <el-button
              v-if="room.current"
              type="warning"
              plain
              :disabled="sessionClosed"
              :loading="callingRoomId === room.roomId"
              @click="recoverRoom(room)"
              >异常恢复</el-button
            >
            <el-button
              :disabled="!room.current || sessionClosed"
              :loading="callingRoomId === room.roomId"
              @click="callAgain(room)"
              >再次呼叫</el-button
            >
            <el-button
              type="primary"
              :disabled="!room.waitingCount || sessionClosed"
              :loading="callingRoomId === room.roomId"
              @click="callNext(room)"
              >叫下一位</el-button
            >
          </footer>
        </article>
      </section>

      <el-card shadow="never" class="candidate-table-card">
        <template #header>
          <div class="table-toolbar">
            <div>
              <strong>候选人签到</strong
              ><span>{{ checkedInCount }}/{{ queue.candidates.length }} 人已签到</span>
            </div>
            <div>
              <el-input
                v-model="keyword"
                clearable
                placeholder="搜索姓名、学号或编号"
                style="width: 240px"
              />
              <el-select
                v-model="statusFilter"
                clearable
                placeholder="签到状态"
                style="width: 140px"
              >
                <el-option label="未签到" value="not_checked_in" /><el-option
                  label="候场中"
                  value="waiting"
                />
                <el-option label="已叫号" value="called" /><el-option
                  label="已完成"
                  value="completed"
                />
                <el-option label="缺席/过号" value="no_show" />
              </el-select>
            </div>
          </div>
        </template>
        <el-table v-loading="loading" :data="filteredCandidates" class="admin-table">
          <el-table-column label="编号" width="100"
            ><template #default="{ row }"
              ><strong class="number-cell">{{ queueNumber(row) }}</strong></template
            ></el-table-column
          >
          <el-table-column prop="applicantName" label="姓名" min-width="130" />
          <el-table-column prop="studentId" label="学号" min-width="130"
            ><template #default="{ row }">{{ row.studentId || '-' }}</template></el-table-column
          >
          <el-table-column prop="roomName" label="面试间" min-width="130" />
          <el-table-column label="计划时间" min-width="170"
            ><template #default="{ row }">{{
              formatDateTime(row.scheduledStartAt)
            }}</template></el-table-column
          >
          <el-table-column label="现场状态" width="120"
            ><template #default="{ row }"
              ><el-tag :type="statusType(row.queueStatus)">{{
                statusText(row.queueStatus)
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.queueStatus === 'not_checked_in' || row.queueStatus === 'cancelled'"
                link
                type="primary"
                :loading="checkingId === row.interviewId"
                :disabled="sessionClosed"
                @click="checkIn(row)"
                >签到</el-button
              >
              <el-button
                v-else-if="row.queueStatus === 'waiting'"
                link
                type="warning"
                :loading="checkingId === row.interviewId"
                :disabled="sessionClosed"
                @click="undoCheckIn(row)"
                >撤销签到</el-button
              >
              <el-button
                v-if="['not_checked_in', 'waiting', 'cancelled'].includes(row.queueStatus)"
                link
                type="danger"
                :disabled="sessionClosed"
                :loading="checkingId === row.interviewId"
                @click="markNoShow(row)"
                >{{ row.queueStatus === 'waiting' ? '标记过号' : '标记缺席' }}</el-button
              >
              <el-button
                v-if="row.queueStatus === 'no_show'"
                link
                type="success"
                :disabled="sessionClosed"
                :loading="checkingId === row.interviewId"
                @click="restoreWaiting(row)"
                >恢复候场</el-button
              >
              <el-button
                v-if="
                  ['not_checked_in', 'waiting', 'cancelled', 'no_show'].includes(row.queueStatus)
                "
                link
                type="primary"
                :disabled="sessionClosed"
                @click="openMove(row)"
                >换房</el-button
              >
              <span
                v-if="['called', 'completed'].includes(row.queueStatus)"
                class="operation-hint"
                >{{ row.queueStatus === 'called' ? '已通知' : '已处理' }}</span
              >
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-dialog v-model="moveVisible" title="临时调整面试间" width="480px">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="换房后会自动改为目标房间的固定面试官组。"
      />
      <el-form label-width="92px" style="margin-top: 18px">
        <el-form-item label="候选人"
          ><el-input :model-value="movingCandidate?.applicantName" disabled
        /></el-form-item>
        <el-form-item label="目标面试间">
          <el-select v-model="targetRoomId" placeholder="选择目标面试间" style="width: 100%">
            <el-option
              v-for="room in queue?.rooms || []"
              :key="room.roomId"
              :label="`${room.name} · 候场 ${room.waitingCount} 人`"
              :value="room.roomId"
              :disabled="room.roomId === movingCandidate?.roomId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="moveVisible = false">取消</el-button
        ><el-button type="primary" :disabled="!targetRoomId" :loading="moving" @click="moveRoom"
          >确认调整</el-button
        ></template
      >
    </el-dialog>

    <el-drawer v-model="operationsVisible" title="现场操作记录" size="min(720px, 92%)">
      <el-table v-loading="operationsLoading" :data="operations" max-height="calc(100vh - 130px)">
        <el-table-column label="时间" min-width="170"
          ><template #default="{ row }">{{
            formatDateTime(row.createdAt)
          }}</template></el-table-column
        >
        <el-table-column label="操作" min-width="130"
          ><template #default="{ row }">{{ actionText(row.action) }}</template></el-table-column
        >
        <el-table-column prop="interviewId" label="面试ID" width="90"
          ><template #default="{ row }">{{ row.interviewId || '-' }}</template></el-table-column
        >
        <el-table-column prop="roomId" label="房间ID" width="90"
          ><template #default="{ row }">{{ row.roomId || '-' }}</template></el-table-column
        >
        <el-table-column prop="operatorId" label="操作人" width="90"
          ><template #default="{ row }">{{ row.operatorId || '-' }}</template></el-table-column
        >
      </el-table>
    </el-drawer>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { interviewSessionApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const sessions = ref<any[]>([]),
  sessionId = ref<number | null>(null),
  queue = ref<any>(null)
const loading = ref(false),
  checkingId = ref<number | null>(null),
  callingRoomId = ref<number | null>(null)
const exporting = ref(false),
  moveVisible = ref(false),
  moving = ref(false)
const movingCandidate = ref<any>(null),
  targetRoomId = ref<number | null>(null)
const operationsVisible = ref(false),
  operationsLoading = ref(false),
  operations = ref<any[]>([])
const keyword = ref(''),
  statusFilter = ref('')
let refreshTimer: number | null = null
const filteredCandidates = computed(() =>
  (queue.value?.candidates || []).filter((item: any) => {
    const text =
      `${item.applicantName || ''} ${item.studentId || ''} ${item.queueNumber || ''}`.toLowerCase()
    return (
      (!keyword.value || text.includes(keyword.value.toLowerCase())) &&
      (!statusFilter.value || item.queueStatus === statusFilter.value)
    )
  }),
)
const checkedInCount = computed(
  () =>
    (queue.value?.candidates || []).filter((item: any) =>
      ['waiting', 'called', 'completed'].includes(item.queueStatus),
    ).length,
)
const sessionClosed = computed(() => queue.value?.sessionStatus === 'completed')
const stats = computed(() => {
  const value = queue.value?.stats || {}
  return [
    { key: 'total', label: '总候选人', value: value.total || 0, hint: '本场安排' },
    {
      key: 'checkedIn',
      label: '已到场',
      value: value.checkedIn || 0,
      hint: `到场率 ${value.total ? Math.round((value.checkedIn / value.total) * 100) : 0}%`,
    },
    { key: 'waiting', label: '正在候场', value: value.waiting || 0, hint: '已签到未叫号' },
    { key: 'completed', label: '已完成', value: value.completed || 0, hint: '全组评价已提交' },
    { key: 'noShow', label: '缺席/过号', value: value.noShow || 0, hint: '可恢复候场' },
    {
      key: 'notCheckedIn',
      label: '尚未到场',
      value: value.notCheckedIn || 0,
      hint: '未签到或已撤销',
    },
  ]
})
async function loadSessions() {
  const data = await interviewSessionApi.list()
  sessions.value = (Array.isArray(data) ? data : []).filter((item: any) =>
    ['published', 'completed'].includes(item.status),
  )
  sessionId.value = sessions.value[0]?.id ?? null
  if (sessionId.value) await loadQueue(true)
}
async function loadQueue(showLoading = false) {
  if (!sessionId.value) return
  if (showLoading) loading.value = true
  try {
    queue.value = await interviewSessionApi.queue(sessionId.value)
  } finally {
    loading.value = false
  }
}
async function checkIn(row: any) {
  checkingId.value = row.interviewId
  try {
    await interviewSessionApi.checkIn(row.interviewId)
    ElMessage.success(`${row.applicantName} 签到成功`)
    await loadQueue()
  } finally {
    checkingId.value = null
  }
}
async function undoCheckIn(row: any) {
  checkingId.value = row.interviewId
  try {
    await interviewSessionApi.undoCheckIn(row.interviewId)
    ElMessage.success('已撤销签到')
    await loadQueue()
  } finally {
    checkingId.value = null
  }
}
async function markNoShow(row: any) {
  await ElMessageBox.confirm(
    `确认将“${row.applicantName}”标记为缺席/过号？之后仍可恢复候场。`,
    '确认现场状态',
    { type: 'warning', confirmButtonText: '确认标记', cancelButtonText: '取消' },
  )
  checkingId.value = row.interviewId
  try {
    await interviewSessionApi.markNoShow(row.interviewId)
    ElMessage.success('已标记缺席/过号')
    await loadQueue()
  } finally {
    checkingId.value = null
  }
}
async function restoreWaiting(row: any) {
  checkingId.value = row.interviewId
  try {
    await interviewSessionApi.restoreWaiting(row.interviewId)
    ElMessage.success('已恢复到候场队列')
    await loadQueue()
  } finally {
    checkingId.value = null
  }
}
function openMove(row: any) {
  movingCandidate.value = row
  targetRoomId.value = null
  moveVisible.value = true
}
async function moveRoom() {
  if (!movingCandidate.value || !targetRoomId.value) return
  moving.value = true
  try {
    await interviewSessionApi.moveRoom(movingCandidate.value.interviewId, targetRoomId.value)
    ElMessage.success('面试间及面试官组已调整')
    moveVisible.value = false
    await loadQueue()
  } finally {
    moving.value = false
  }
}
async function callNext(room: any) {
  callingRoomId.value = room.roomId
  try {
    const candidate = await interviewSessionApi.callNext(room.roomId)
    ElMessage.success(`已呼叫 ${candidate.applicantName}`)
    await loadQueue()
  } finally {
    callingRoomId.value = null
  }
}
async function callAgain(room: any) {
  callingRoomId.value = room.roomId
  try {
    await interviewSessionApi.callAgain(room.roomId)
    ElMessage.success('已再次发送叫号')
    await loadQueue()
  } finally {
    callingRoomId.value = null
  }
}
async function recoverRoom(room: any) {
  await ElMessageBox.confirm(
    '异常恢复会取消当前叫号；未完成面试的候选人将回到候场队列。确认继续吗？',
    '恢复面试间状态',
    { type: 'warning', confirmButtonText: '确认恢复', cancelButtonText: '取消' },
  )
  callingRoomId.value = room.roomId
  try {
    await interviewSessionApi.recoverRoom(room.roomId)
    ElMessage.success('面试间状态已恢复')
    await loadQueue()
  } finally {
    callingRoomId.value = null
  }
}
async function completeSession() {
  await ElMessageBox.confirm(
    '结束后将锁定签到、叫号和调整操作。请确认所有候选人已完成或已标记缺席。',
    '结束面试场次',
    { type: 'warning', confirmButtonText: '确认结束', cancelButtonText: '取消' },
  )
  await interviewSessionApi.completeSession(sessionId.value!)
  ElMessage.success('场次已结束')
  await loadQueue()
  await loadSessionsKeepingCurrent()
}
async function reopenSession() {
  await ElMessageBox.confirm('重新开启后可继续处理签到、换房和叫号。', '重新开启场次', {
    type: 'warning',
    confirmButtonText: '重新开启',
    cancelButtonText: '取消',
  })
  await interviewSessionApi.reopenSession(sessionId.value!)
  ElMessage.success('场次已重新开启')
  await loadQueue()
  await loadSessionsKeepingCurrent()
}
async function loadSessionsKeepingCurrent() {
  const current = sessionId.value
  const data = await interviewSessionApi.list()
  sessions.value = (Array.isArray(data) ? data : []).filter((item: any) =>
    ['published', 'completed'].includes(item.status),
  )
  sessionId.value = current
}
async function exportSummary() {
  if (!sessionId.value) return
  exporting.value = true
  try {
    const blob = await interviewSessionApi.exportEvaluationSummary(sessionId.value)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `面试评价汇总-${queue.value?.sessionName || sessionId.value}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('评价汇总已导出')
  } finally {
    exporting.value = false
  }
}
async function showOperations() {
  if (!sessionId.value) return
  operationsVisible.value = true
  operationsLoading.value = true
  try {
    const data = await interviewSessionApi.operations(sessionId.value)
    operations.value = Array.isArray(data) ? data : []
  } finally {
    operationsLoading.value = false
  }
}
function openScreen() {
  window.open(`/interview-call-screen/${sessionId.value}`, '_blank', 'noopener,noreferrer')
}
function queueNumber(row: any) {
  return row.queueNumber ? `#${String(row.queueNumber).padStart(3, '0')}` : `#${row.interviewId}`
}
function statusText(status: string) {
  return (
    (
      {
        not_checked_in: '未签到',
        waiting: '候场中',
        called: '已叫号',
        completed: '已完成',
        cancelled: '已撤销',
        no_show: '缺席/过号',
      } as Record<string, string>
    )[status] || status
  )
}
function statusType(status: string) {
  return (
    (
      {
        waiting: 'warning',
        called: 'primary',
        completed: 'success',
        cancelled: 'info',
        no_show: 'danger',
      } as Record<string, string>
    )[status] || 'info'
  )
}
function actionText(action: string) {
  return (
    (
      {
        check_in: '签到',
        undo_check_in: '撤销签到',
        call_next: '叫下一位',
        call_again: '再次呼叫',
        mark_no_show: '标记缺席/过号',
        restore_waiting: '恢复候场',
        move_room: '临时换房',
        recover_room: '异常恢复',
        complete_session: '结束场次',
        reopen_session: '重新开启场次',
      } as Record<string, string>
    )[action] || action
  )
}
onMounted(async () => {
  await loadSessions()
  refreshTimer = window.setInterval(() => loadQueue(), 3000)
})
onBeforeUnmount(() => {
  if (refreshTimer !== null) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.queue-header,
.queue-header__actions,
.room-card header,
.room-card footer,
.table-toolbar,
.table-toolbar > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.queue-header__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}
.queue-page :deep(.el-button) {
  min-height: 44px;
}
.queue-page :deep(.el-input__wrapper) {
  min-height: 42px;
}
.queue-header {
  margin-bottom: 20px;
}
.queue-header h2,
.room-card h3 {
  margin: 0;
}
.queue-header p,
.room-card p {
  margin: 5px 0 0;
  color: var(--oa-muted);
}
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  display: grid;
  gap: 4px;
  padding: 15px;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  background: var(--el-bg-color);
}
.stat-card span,
.stat-card small {
  color: var(--oa-muted);
}
.stat-card strong {
  font-size: 28px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.queue-page :deep(.el-alert) + .room-grid {
  margin-top: 16px;
}
.room-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}
.room-card header span,
.room-current span {
  font-size: 12px;
  color: var(--oa-muted);
}
.room-card footer {
  justify-content: flex-end;
}
.room-current {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
}
.room-current strong {
  font-size: 20px;
}
.room-current small {
  color: var(--oa-muted);
}
.room-empty {
  display: grid;
  min-height: 82px;
  place-items: center;
  border: 1px dashed var(--el-border-color);
  border-radius: 12px;
  color: var(--oa-muted);
}
.table-toolbar > div:first-child {
  display: grid;
  gap: 3px;
}
.table-toolbar span,
.operation-hint {
  font-size: 13px;
  color: var(--oa-muted);
}
.number-cell {
  font-variant-numeric: tabular-nums;
  color: var(--el-color-primary);
}
@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .queue-header,
  .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .queue-header__actions,
  .table-toolbar > div {
    width: 100%;
    flex-wrap: wrap;
  }
}
@media (max-width: 560px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .queue-header__actions > * {
    width: 100% !important;
  }
  .table-toolbar > div:last-child > * {
    width: 100% !important;
  }
}
</style>
