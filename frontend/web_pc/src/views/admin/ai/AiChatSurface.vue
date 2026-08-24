<template>
  <div class="chat-surface">
    <div class="chat-scroll">
      <div
        v-for="message in messages"
        :key="message.id"
        class="message"
        :class="`message--${message.role}`"
      >
        <div class="message-role">{{ message.role === 'user' ? '我' : 'AI' }}</div>
        <div v-if="isStructuredAssistant(message)" class="ai-card">
          <p v-if="structuredMessage(message).summary" class="ai-summary">
            {{ structuredMessage(message).summary }}
          </p>

          <div v-if="structuredMessage(message).suggestions?.length" class="ai-block">
            <strong>建议</strong>
            <ul>
              <li v-for="item in structuredMessage(message).suggestions" :key="item">{{ item }}</li>
            </ul>
          </div>

          <div v-if="structuredMessage(message).questions?.length" class="ai-block">
            <strong>需要补充</strong>
            <div class="question-list">
              <button
                v-for="question in structuredMessage(message).questions"
                :key="question"
                type="button"
                class="question-item"
                @click="useQuestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </div>

          <div v-if="structuredMessage(message).missingFields?.length" class="field-tags">
            <el-tag
              v-for="field in structuredMessage(message).missingFields"
              :key="field"
              size="small"
              type="info"
            >
              {{ field }}
            </el-tag>
          </div>
        </div>
        <pre v-else>{{ displayMessage(message) }}</pre>
      </div>
      <div v-if="pending" class="message message--assistant">
        <div class="message-role">AI</div>
        <div class="ai-skeleton">
          <span />
          <span />
          <span />
        </div>
      </div>
    </div>
    <div class="composer">
      <el-input
        v-model="text"
        type="textarea"
        :autosize="{ minRows: 3, maxRows: 6 }"
        placeholder="补充活动时间、地点、人数、预算、志愿者要求，或直接让 AI 继续追问..."
        @keydown.meta.enter.prevent="submit"
        @keydown.ctrl.enter.prevent="submit"
      />
      <el-button type="primary" :loading="sending" :disabled="streaming" @click="submit">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  messages: any[]
  pending?: boolean
  streaming?: boolean
  sending?: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
}>()

const text = ref('')

function structuredMessage(message: any) {
  try {
    return JSON.parse(message.structuredPayload || '{}')
  } catch {
    return {}
  }
}

function isStructuredAssistant(message: any) {
  return message.role === 'assistant' && Boolean(message.structuredPayload)
}

function displayMessage(message: any) {
  if (message.role !== 'assistant') return message.content
  const payload = structuredMessage(message)
  if (payload.summary || payload.suggestions || payload.questions) {
    return message.content || '（结构化内容见上方卡片）'
  }
  return message.content
}

function submit() {
  const value = text.value.trim()
  if (!value) return
  text.value = ''
  emit('send', value)
}

function useQuestion(question: string) {
  text.value = text.value ? `${text.value}\n${question}：` : `${question}：`
}
</script>

<style scoped>
.chat-surface {
  display: flex;
  flex-direction: column;
  min-height: 460px;
  gap: 14px;
}

.chat-scroll {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 52vh;
  min-height: 320px;
  overflow-y: auto;
  padding: 4px 2px;
  -webkit-overflow-scrolling: touch;
}

.message {
  display: grid;
  gap: 8px;
  max-width: 84%;
}

.message--user {
  align-self: flex-end;
  justify-items: end;
}

.message--assistant {
  align-self: flex-start;
}

.message-role {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.message--user pre {
  margin: 0;
  padding: 12px 16px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  border-radius: 14px 14px 4px 14px;
  color: var(--el-text-color-primary);
  background: var(--el-color-primary);
}

.message--assistant pre {
  margin: 0;
  padding: 12px 16px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  border-radius: 14px 14px 14px 4px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-light);
}

.ai-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px 14px 14px 4px;
  background: var(--el-fill-color-light);
}

.ai-summary {
  margin: 0;
  line-height: 1.7;
}

.ai-block strong {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
}

.ai-block ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.question-item {
  padding: 8px 12px;
  text-align: left;
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    color 0.15s ease;
}

.question-item:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.field-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ai-skeleton {
  display: grid;
  gap: 8px;
  width: 220px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px 14px 14px 4px;
  background: var(--el-fill-color-light);
}

.ai-skeleton span {
  height: 10px;
  border-radius: 5px;
  background: var(--el-fill-color);
  animation: ai-pulse 1.2s ease-in-out infinite;
}

.ai-skeleton span:nth-child(2) {
  width: 78%;
  animation-delay: 0.15s;
}

.ai-skeleton span:nth-child(3) {
  width: 56%;
  animation-delay: 0.3s;
}

@keyframes ai-pulse {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 1;
  }
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>