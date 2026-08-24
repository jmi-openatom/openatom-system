<template>
  <el-dialog :model-value="visible" title="新建 AI 活动" width="680px" @update:model-value="emit('update:visible', $event)">
    <el-form label-width="96px">
      <el-form-item label="会话标题">
        <el-input v-model="title" placeholder="可选，例如：新生开源破冰活动" />
      </el-form-item>
      <el-form-item label="活动需求">
        <el-input
          v-model="message"
          type="textarea"
          :rows="7"
          placeholder="例如：我想办一个面向新生的开源社团破冰活动，轻松一点，让大家认识社团项目。"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">开始澄清需求</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  create: [payload: { title: string; initialMessage: string }]
}>()

const title = ref('')
const message = ref('')

watch(
  () => props.visible,
  (value) => {
    if (value) {
      title.value = ''
      message.value = ''
    }
  },
)

function submit() {
  if (!message.value.trim()) return
  emit('create', { title: title.value.trim(), initialMessage: message.value.trim() })
}
</script>