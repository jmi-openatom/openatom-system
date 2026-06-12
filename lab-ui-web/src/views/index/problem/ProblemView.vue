<template>
  <main class="page">
    <div class="page-grid">
      <section class="panel">
        <div class="panel__head">
          <h1>{{ problem?.title || '每日一练' }}</h1>
          <span class="status">{{ problem?.timeLimitMs }}ms / {{ problem?.memoryLimitMb }}MB</span>
        </div>
        <div class="panel__body">
          <pre class="problem-text">{{ problem?.descriptionMarkdown }}</pre>
          <div class="list" style="margin-top: 16px">
            <article v-for="sample in problem?.sampleCases || []" :key="sample.id" class="list-item">
              <strong>样例</strong>
              <code>Input: {{ sample.inputText }}</code>
              <code>Output: {{ sample.expectedOutput }}</code>
            </article>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head">
          <h2>提交</h2>
          <div class="tabs">
            <button v-for="item in languages" :key="item" :class="{ active: language === item }" @click="language = item">{{ item }}</button>
          </div>
        </div>
        <div class="panel__body">
          <textarea v-model="code" class="textarea code-editor" spellcheck="false" @input="dirty = true"></textarea>
          <div class="toolbar" style="margin-top: 12px">
            <button class="button" :disabled="submitting" @click="() => submitNow()"><Send :size="17" />提交</button>
            <span v-if="message" class="status" :class="messageClass">{{ message }}</span>
          </div>
        </div>
      </section>
    </div>

    <section class="panel" style="margin-top: 16px">
      <div class="panel__head">
        <h2>提交记录</h2>
        <RefreshCcw :size="18" />
      </div>
      <div class="panel__body">
        <table class="table">
          <thead><tr><th>ID</th><th>语言</th><th>状态</th><th>用例</th><th>消息</th></tr></thead>
          <tbody>
            <tr v-for="item in history" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.language }}</td>
              <td><span class="status" :class="{ ok: item.judgeStatus === 'AC', bad: ['WA','RE','CE'].includes(item.judgeStatus), warn: item.judgeStatus === 'PENDING' }">{{ item.judgeStatus }}</span></td>
              <td>{{ item.scorePassed }}/{{ item.totalCases }}</td>
              <td>{{ item.errorMessage }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RefreshCcw, Send } from 'lucide-vue-next'
import { submitCode, submissions, todayProblem, type Problem, type Submission } from '@/api/oj'

const languages = ['cpp', 'java', 'python']
const problem = ref<Problem>()
const language = ref('cpp')
const code = ref('// __LAB_ACCEPT__\n')
const dirty = ref(false)
const submitting = ref(false)
const message = ref('')
const history = ref<Submission[]>([])
let autoSubmitted = false

const messageClass = computed(() => ({
  ok: message.value.includes('成功') || message.value.includes('提交'),
  bad: message.value.includes('失败'),
}))

async function load() {
  problem.value = await todayProblem()
  history.value = await submissions()
}

async function submitNow(reason = '手动提交') {
  if (!problem.value || submitting.value || !code.value.trim()) return
  submitting.value = true
  try {
    await submitCode(problem.value.id, language.value, code.value)
    dirty.value = false
    message.value = `${reason}已提交`
    history.value = await submissions()
  } catch (err) {
    message.value = err instanceof Error ? err.message : '提交失败'
  } finally {
    submitting.value = false
  }
}

function autoSubmitOnFocusLost() {
  if (!dirty.value || autoSubmitted || submitting.value) return
  autoSubmitted = true
  submitNow('失焦自动')
}

function handleVisibility() {
  if (document.hidden) autoSubmitOnFocusLost()
}

onMounted(() => {
  load()
  window.addEventListener('blur', autoSubmitOnFocusLost)
  document.addEventListener('visibilitychange', handleVisibility)
})

onBeforeUnmount(() => {
  window.removeEventListener('blur', autoSubmitOnFocusLost)
  document.removeEventListener('visibilitychange', handleVisibility)
})
</script>

<style scoped>
.problem-text {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
  line-height: 1.65;
}
</style>
