<template>
  <div v-if="hasContent" class="plan-editor">
    <div class="plan-toolbar">
      <div>
        <strong>{{ latestPlan?.title || 'AI 正在生成策划案' }}</strong>
        <span>{{ phase || `版本 ${latestPlan?.version || 1} · ${latestPlan?.status || 'draft'}` }}</span>
      </div>
      <div class="plan-actions">
        <el-button :loading="revising" :disabled="streaming || !reviseInstruction.trim()" @click="emitRevise">
          按要求修改
        </el-button>
        <el-button type="primary" :disabled="streaming || !planText.trim()" @click="emitSave">
          保存草稿
        </el-button>
      </div>
    </div>
    <el-input v-model="text" type="textarea" :rows="22" resize="vertical" />
    <el-input
      v-model="reviseText"
      class="revise-input"
      placeholder="例如：把活动流程写得更细，把志愿者职责独立成一段"
    />
  </div>
  <div v-else class="empty-state">
    <strong>还没有策划案</strong>
    <span>先确认需求，然后点击“生成策划案”。</span>
    <el-button type="primary" :loading="streaming" :disabled="streaming" @click="emitGenerate">
      生成策划案
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  latestPlan: any
  phase?: string
  streaming?: boolean
  revising?: boolean
  planText?: string
  reviseInstruction?: string
}>()

const emit = defineEmits<{
  'update:planText': [value: string]
  'update:reviseInstruction': [value: string]
  revise: []
  save: []
  generate: []
}>()

const text = computed({
  get: () => props.planText || '',
  set: (value: string) => emit('update:planText', value),
})

const reviseText = computed({
  get: () => props.reviseInstruction || '',
  set: (value: string) => emit('update:reviseInstruction', value),
})

const hasContent = computed(
  () =>
    Boolean(props.latestPlan) ||
    Boolean(props.streaming) ||
    Boolean(props.planText?.trim()),
)

function emitRevise() {
  emit('revise')
}

function emitSave() {
  emit('save')
}

function emitGenerate() {
  emit('generate')
}
</script>

<style scoped>
.plan-editor {
  display: grid;
  gap: 12px;
}

.plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.plan-toolbar strong {
  display: block;
  font-size: 15px;
  line-height: 1.4;
}

.plan-toolbar span {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.plan-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.revise-input {
  margin-top: 2px;
}

.empty-state {
  display: grid;
  gap: 10px;
  justify-items: center;
  padding: 56px 24px;
  color: var(--el-text-color-secondary);
}

.empty-state strong {
  font-size: 16px;
  color: var(--el-text-color-primary);
}
</style>