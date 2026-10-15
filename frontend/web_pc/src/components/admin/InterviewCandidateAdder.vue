<template>
  <el-dialog v-model="visible" title="添加候选人" width="780px" :close-on-click-modal="false">
    <template v-if="session">
      <el-alert
        :title="
          session.status === 'published'
            ? '保存后立即发布新增安排，并向新增候选人发送面试通知。'
            : '新增安排保存为草稿，随场次统一发布。'
        "
        type="info"
        show-icon
        :closable="false"
      />
      <p>
        {{ session.sessionName }} ·
        沿用原场次时长与间隔，接在各面试间最后一人之后，结束时间自动延长。
      </p>
      <el-form label-width="90px">
        <el-form-item label="候选人">
          <el-select
            v-model="applicationIds"
            multiple
            filterable
            :loading="loading"
            placeholder="选择初审通过的申请，可搜索姓名或申请ID"
            style="width: 100%"
            :disabled="submitting"
            @change="preview = null"
          >
            <el-option
              v-for="item in candidates"
              :key="item.id"
              :value="item.id"
              :label="`${item.applicantName || '未填写姓名'} · 申请 ${item.id}${item.preferredDepartment ? ` · ${item.preferredDepartment}` : ''}`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="面试间">
          <el-select
            v-model="roomId"
            clearable
            placeholder="自动分配至最早空闲的面试间"
            style="width: 100%"
            :disabled="submitting"
            @change="preview = null"
          >
            <el-option
              v-for="room in session.rooms"
              :key="room.roomId"
              :value="room.roomId"
              :label="room.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-empty
        v-if="!loading && !candidates.length"
        description="当前批次暂无可添加的初审通过候选人"
      />
      <template v-if="preview">
        <p>
          本次新增 {{ preview.totalCandidates }} 人 · 场次预计结束：{{
            formatDateTime(preview.scheduledEndAt)
          }}
        </p>
        <el-table :data="preview.assignments" max-height="320">
          <el-table-column prop="queueNumber" label="编号" width="70" />
          <el-table-column prop="applicantName" label="候选人" min-width="110" />
          <el-table-column prop="roomName" label="面试间" min-width="120" />
          <el-table-column label="时间" min-width="270">
            <template #default="{ row }"
              >{{ formatDateTime(row.scheduledStartAt) }} —
              {{ formatDateTime(row.scheduledEndAt) }}</template
            >
          </el-table-column>
        </el-table>
      </template>
    </template>
    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button
        :loading="previewing"
        :disabled="!applicationIds.length || submitting || loading"
        @click="generatePreview"
        >预览新增排期</el-button
      >
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="!preview || previewing || loading"
        @click="save"
        >确认添加</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { applicationApi, interviewSessionApi } from '@/api'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{ modelValue: boolean; session: any }>()
const emit = defineEmits(['update:modelValue', 'saved'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const candidates = ref<any[]>([])
const applicationIds = ref<number[]>([])
const roomId = ref<number>()
const loading = ref(false)
const previewing = ref(false)
const submitting = ref(false)
const preview = ref<any>(null)
let loadVersion = 0

watch(
  () => props.modelValue,
  async (open) => {
    const version = ++loadVersion
    if (!open || !props.session) return
    applicationIds.value = []
    roomId.value = undefined
    preview.value = null
    candidates.value = []
    loading.value = true
    const session = props.session
    try {
      const all: any[] = []
      let page = 1
      let total = 0
      do {
        const result = await applicationApi.list({
          campaignId: session.campaignId,
          status: 'pre_screen_passed',
          page,
          pageSize: 100,
        })
        if (version !== loadVersion) return
        const rows = result?.list || result || []
        total = Number(result?.total || rows.length)
        all.push(...rows)
        if (!rows.length) break
        page++
      } while (all.length < total)
      const existing = new Set(session.assignments.map((item: any) => item.applicationId))
      candidates.value = all.filter((item) => !existing.has(item.id))
    } finally {
      if (version === loadVersion) loading.value = false
    }
  },
)

function payload(previewOnly: boolean) {
  return {
    applicationIds: [...applicationIds.value],
    roomId: roomId.value || undefined,
    previewOnly,
  }
}

async function generatePreview() {
  if (previewing.value || !applicationIds.value.length) return
  previewing.value = true
  preview.value = null
  const sessionId = props.session.sessionId
  const request = payload(true)
  try {
    const result = await interviewSessionApi.addCandidates(sessionId, request)
    // Ignore a response if the user changed the selection or closed/reopened this dialog.
    if (
      visible.value &&
      sessionId === props.session.sessionId &&
      JSON.stringify(request) === JSON.stringify(payload(true))
    ) {
      preview.value = result
    }
  } finally {
    previewing.value = false
  }
}

async function save() {
  if (submitting.value || !preview.value) return
  submitting.value = true
  try {
    await interviewSessionApi.addCandidates(props.session.sessionId, payload(false))
    ElMessage.success('候选人已添加至排期')
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>
