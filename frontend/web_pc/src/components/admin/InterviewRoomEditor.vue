<template>
  <el-button
    v-if="hasPermission('interview:update') && (room || sessionId)"
    link
    type="primary"
    :disabled="disabled"
    :aria-label="room ? `修改${room.name}的名称、地点和面试官` : '添加面试室'"
    @click="open"
    >{{ room ? '修改面试间' : '添加面试室' }}</el-button
  >
  <el-dialog
    v-model="visible"
    :title="room ? '修改面试间' : '添加面试室'"
    width="min(540px, 92vw)"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :show-close="!saving"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" @submit.prevent="save">
      <el-form-item label="面试间名称" prop="name">
        <el-input v-model="form.name" maxlength="80" show-word-limit :disabled="saving" />
      </el-form-item>
      <el-form-item label="面试地点" prop="location">
        <el-input
          v-model="form.location"
          placeholder="例如：韦立楼 A201"
          maxlength="255"
          show-word-limit
          :disabled="saving"
        />
      </el-form-item>
      <el-form-item label="面试官" prop="interviewerIds">
        <el-select
          v-model="form.interviewerIds"
          multiple
          filterable
          remote
          :remote-method="loadOptions"
          :loading="optionsLoading"
          :disabled="saving"
          placeholder="搜索并选择面试官"
          style="width: 100%"
        >
          <el-option
            v-for="person in options"
            :key="person.userId"
            :label="person.name"
            :value="person.userId"
            :disabled="!room && usedInterviewerIds?.includes(person.userId)"
          />
        </el-select>
      </el-form-item>
      <p v-if="room" class="edit-hint">
        名称、地点和面试官可随时修改。面试官调整会同步未完成的面试；已提交评价和已完成面试的原面试官记录会保留。
      </p>
      <p v-else class="edit-hint">
        新增面试室后，可重新生成本场次全部人员的排期。每位面试官只能分配到一个面试室。
      </p>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{
        room ? '保存修改' : '添加面试室'
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus/es/components/message/index'
import { interviewSessionApi } from '@/api'
import { hasPermission } from '@/utils/permission'

type Person = { userId: number; name: string }
const props = defineProps<{
  sessionId?: number
  disabled?: boolean
  usedInterviewerIds?: number[]
  room?: { roomId: number; name: string; location?: string; interviewers?: Person[] }
}>()
const emit = defineEmits<{ saved: [] }>()
const visible = ref(false),
  saving = ref(false),
  optionsLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref({ name: '', location: '', interviewerIds: [] as number[] })
const options = ref<Person[]>([])
const rules: FormRules = {
  name: [{ required: true, whitespace: true, message: '请输入面试间名称', trigger: 'blur' }],
  location: [{ required: true, whitespace: true, message: '请输入面试地点', trigger: 'blur' }],
  interviewerIds: [
    { type: 'array', required: true, min: 1, message: '请至少选择一位面试官', trigger: 'change' },
  ],
}
let searchId = 0
async function loadOptions(keyword = '') {
  const id = ++searchId
  optionsLoading.value = true
  try {
    const data = await interviewSessionApi.interviewerOptions(keyword)
    if (id !== searchId) return
    const selected = options.value.filter((person) =>
      form.value.interviewerIds.includes(person.userId),
    )
    options.value = [
      ...new Map(
        [...selected, ...(Array.isArray(data) ? data : [])].map((person) => [
          person.userId,
          person,
        ]),
      ).values(),
    ]
  } finally {
    if (id === searchId) optionsLoading.value = false
  }
}
async function open() {
  form.value = {
    name: props.room?.name || '',
    location: props.room?.location || '',
    interviewerIds: (props.room?.interviewers || []).map((person) => person.userId),
  }
  options.value = [...(props.room?.interviewers || [])]
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
  await loadOptions()
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    const data = {
      ...form.value,
      name: form.value.name.trim(),
      location: form.value.location.trim(),
    }
    if (props.room) await interviewSessionApi.updateRoom(props.room.roomId, data)
    else if (props.sessionId) await interviewSessionApi.addRoom(props.sessionId, data)
    else return
    ElMessage.success(props.room ? '面试间信息已更新' : '面试室已添加，可重新生成全员排期')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.edit-hint {
  margin: 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.6;
}
</style>
