<template>
  <el-dialog
    v-model="visible"
    title="重新生成全部面试排期"
    width="min(1000px, 96vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :show-close="!saving"
  >
    <template v-if="currentSession">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        :title="
          currentSession.status === 'published'
            ? '确认后替换本场次全部排期，安排发生变化的候选人将收到通知，并需重新确认。'
            : '确认后替换本场次全部草稿排期，随场次统一发布。'
        "
      />
      <div class="planner-summary">
        <div>
          <strong>{{ currentSession.sessionName }}</strong>
          <p>
            {{ currentSession.assignments.length }} 位候选人 ·
            {{ currentSession.rooms.length }} 个面试室
          </p>
        </div>
        <InterviewRoomEditor
          :session-id="currentSession.sessionId"
          :disabled="busy"
          :used-interviewer-ids="usedInterviewerIds"
          @saved="roomAdded"
        />
      </div>
      <div class="planner-rooms">
        <el-tag v-for="room in currentSession.rooms" :key="room.roomId" size="large">
          {{ room.name }} · {{ room.interviewers.map((person: any) => person.name).join('、') }}
        </el-tag>
      </div>
      <el-form
        :model="form"
        label-width="110px"
        class="planner-form"
        @submit.prevent="generatePreview"
      >
        <el-form-item label="首场开始时间">
          <el-date-picker
            v-model="form.scheduledStartAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled="busy"
            placeholder="选择重新排期的开始时间"
          />
        </el-form-item>
        <el-form-item label="每场时长">
          <el-input-number v-model="form.durationMinutes" :min="5" :step="5" :disabled="busy" />
          <span class="unit">分钟</span>
        </el-form-item>
        <el-form-item label="面试间隔">
          <el-input-number v-model="form.gapMinutes" :min="0" :step="5" :disabled="busy" />
          <span class="unit">分钟</span>
        </el-form-item>
      </el-form>
      <p class="planner-hint">
        按原候选人顺序均衡分配到全部面试室，结束时间自动计算。已有叫号、评价或已完成面试的场次无法全员重排。
      </p>
      <template v-if="preview">
        <p>
          将重新安排 {{ preview.totalCandidates }} 人 · 预计结束：{{
            formatDateTime(preview.scheduledEndAt)
          }}
        </p>
        <p>
          {{
            preview.rooms.map((room: any) => `${room.name}：${room.assignedCount} 人`).join('；')
          }}
        </p>
        <el-table :data="preview.assignments" max-height="380">
          <el-table-column prop="queueNumber" label="编号" width="65" />
          <el-table-column prop="applicantName" label="候选人" min-width="100" />
          <el-table-column label="原面试室" min-width="110">
            <template #default="{ row }">{{
              originals.get(row.interviewId)?.roomName || '—'
            }}</template>
          </el-table-column>
          <el-table-column prop="roomName" label="新面试室" min-width="110" />
          <el-table-column label="原时间" min-width="170">
            <template #default="{ row }">{{
              formatDateTime(originals.get(row.interviewId)?.scheduledStartAt)
            }}</template>
          </el-table-column>
          <el-table-column label="新时间" min-width="260">
            <template #default="{ row }"
              >{{ formatDateTime(row.scheduledStartAt) }} —
              {{ formatDateTime(row.scheduledEndAt) }}</template
            >
          </el-table-column>
        </el-table>
      </template>
    </template>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button :loading="previewing" :disabled="saving || refreshing" @click="generatePreview"
        >生成全员排期预览</el-button
      >
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!preview || previewing || refreshing"
        @click="save"
        >确认替换全部排期</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import InterviewRoomEditor from '@/components/admin/InterviewRoomEditor.vue'
import { interviewSessionApi } from '@/api'
import { formatDateTime, toDateTimeInputValue } from '@/utils/format'

const props = defineProps<{ modelValue: boolean; session: any }>()
const emit = defineEmits(['update:modelValue', 'saved'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => {
    if (!saving.value) emit('update:modelValue', value)
  },
})
const currentSession = ref<any>(null)
const form = ref({ scheduledStartAt: '', durationMinutes: 30, gapMinutes: 10 })
const preview = ref<any>(null)
const previewing = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const busy = computed(() => previewing.value || saving.value || refreshing.value)
const usedInterviewerIds = computed(() =>
  (currentSession.value?.rooms || []).flatMap((room: any) =>
    room.interviewers.map((person: any) => person.userId),
  ),
)
const originals = computed<Map<number, any>>(
  () =>
    new Map((currentSession.value?.assignments || []).map((row: any) => [row.interviewId, row])),
)
let version = 0
watch(
  () => props.modelValue,
  (open) => {
    version++
    preview.value = null
    if (!open || !props.session) return
    currentSession.value = props.session
    form.value = {
      scheduledStartAt: toDateTimeInputValue(props.session.scheduledStartAt),
      durationMinutes: props.session.durationMinutes ?? 30,
      gapMinutes: props.session.gapMinutes ?? 10,
    }
  },
)
watch(
  form,
  () => {
    version++
    preview.value = null
  },
  { deep: true },
)

async function roomAdded() {
  const sessionId = currentSession.value.sessionId
  version++
  preview.value = null
  refreshing.value = true
  try {
    const detail = await interviewSessionApi.detail(sessionId)
    if (visible.value && currentSession.value?.sessionId === sessionId)
      currentSession.value = detail
    emit('saved')
  } finally {
    refreshing.value = false
  }
}

function payload(previewOnly: boolean) {
  return {
    ...form.value,
    previewOnly,
    expectedRevision: previewOnly ? undefined : preview.value?.scheduleRevision,
  }
}

async function generatePreview() {
  if (busy.value || !currentSession.value) return
  if (
    !form.value.scheduledStartAt ||
    !form.value.durationMinutes ||
    form.value.gapMinutes == null
  ) {
    ElMessage.warning('请填写开始时间、每场时长和间隔')
    return
  }
  preview.value = null
  previewing.value = true
  const requestVersion = ++version
  const sessionId = currentSession.value.sessionId
  try {
    const result = await interviewSessionApi.reschedule(sessionId, payload(true))
    if (visible.value && requestVersion === version) preview.value = result
  } finally {
    previewing.value = false
  }
}

async function save() {
  if (busy.value || !preview.value || !currentSession.value) return
  saving.value = true
  try {
    await interviewSessionApi.reschedule(currentSession.value.sessionId, payload(false))
    ElMessage.success('全部面试排期已重新生成')
    emit('update:modelValue', false)
    emit('saved')
  } catch (error) {
    preview.value = null
    throw error
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.planner-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
}
.planner-summary p {
  margin: 6px 0 0;
  color: var(--oa-muted);
}
.planner-rooms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0 24px;
}
.planner-hint {
  color: var(--oa-muted);
  line-height: 1.6;
}
.unit {
  margin-left: 8px;
}
</style>
