<template>
  <div class="page-container review-page">
    <header class="page-heading"><p class="eyebrow">MENTOR REVIEW</p><h1>审核工作台</h1><p>优先处理等待时间最长的提交，并提供可执行的反馈。</p></header>
    <section v-if="appeals.length" class="content-card appeal-panel"><div class="section-heading"><div><p class="eyebrow">APPEALS</p><h2>待复核申诉</h2></div><span>{{ appeals.length }} 项</span></div><div v-for="item in appeals" :key="item.id" class="appeal-row"><div><strong>{{ item.memberName }} · {{ item.taskTitle }}</strong><p>{{ item.reason }}</p><small>原审核人：{{ item.originalReviewer }} · {{ item.originalComment }}</small></div><div class="inline-actions"><el-button @click="resolve(item, 'UPHELD')">维持结论</el-button><el-button type="primary" @click="resolve(item, 'OVERTURNED')">撤销并允许修改</el-button></div></div></section>
    <el-skeleton v-if="loading" :rows="7" animated />
    <el-result v-else-if="error" icon="error" title="审核队列加载失败" :sub-title="error" />
    <section v-else-if="queue.length" class="review-layout">
      <nav class="review-queue" aria-label="待审核提交">
        <button v-for="item in queue" :key="Number(item.submissionId)" type="button" :class="{ active: selected?.submissionId === item.submissionId }" @click="select(item)">
          <span>{{ item.memberName }} · v{{ item.version }}</span><strong>{{ item.taskTitle }}</strong><small>已等待 {{ item.waitingHours }} 小时</small>
        </button>
      </nav>
      <article v-if="selected" class="content-card review-sheet">
        <div class="section-heading"><div><p class="eyebrow">SUBMISSION V{{ selected.version }}</p><h2>{{ selected.taskTitle }}</h2></div><span>{{ selected.memberName }}</span></div>
        <section class="submission-preview"><h3>完成情况</h3><p>{{ selected.completionNote }}</p><h3 v-if="selected.problemsAndLearning">问题与学习总结</h3><p v-if="selected.problemsAndLearning">{{ selected.problemsAndLearning }}</p><h3>成果链接</h3><div class="link-stack"><a v-for="link in submissionLinks" :key="link.value" :href="link.value" target="_blank" rel="noopener noreferrer">{{ link.label }} ↗</a><span v-if="!submissionLinks.length">未提供外部链接</span></div><div v-if="selected.aiUsed" class="ai-disclosure"><strong>AI 使用说明</strong><p>{{ selected.aiUsageDetail }}</p></div></section>
        <el-divider />
        <el-form label-position="top">
          <el-form-item label="审核结论" required><el-radio-group v-model="form.result"><el-radio-button label="PASSED">通过</el-radio-button><el-radio-button label="REVISION_REQUIRED">退回修改</el-radio-button><el-radio-button label="FAILED">不通过</el-radio-button></el-radio-group></el-form-item>
          <el-form-item label="审核评语" required><el-input v-model="form.comment" type="textarea" :rows="4" placeholder="总结本次审核结论" /></el-form-item>
          <el-form-item v-if="form.result !== 'PASSED'" label="必须修改项" required><el-input v-model="requiredChangesText" type="textarea" :rows="3" placeholder="每行一项，说明具体问题和修改方式" /></el-form-item>
          <div class="form-grid"><el-form-item label="做得好的部分"><el-input v-model="strengthsText" type="textarea" :rows="3" /></el-form-item><el-form-item label="建议优化项"><el-input v-model="suggestionsText" type="textarea" :rows="3" /></el-form-item></div>
          <div class="review-options"><el-checkbox v-model="form.resubmissionAllowed">允许再次提交</el-checkbox><el-checkbox v-model="form.excellent">标记为优秀成果</el-checkbox></div>
          <div class="inline-actions"><el-button type="primary" size="large" :loading="submitting" @click="submitReview">提交审核结果</el-button></div>
        </el-form>
      </article>
    </section>
    <div v-else class="content-card empty-panel"><strong>待审核队列已清空</strong><p>新的成员提交会按等待时间出现在这里。</p></div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { getErrorMessage } from '@/api/http'
import { getAppealQueue, getReviewQueue, resolveAppeal, reviewSubmission } from '@/api/quest'
const queue = ref<Record<string, any>[]>([])
const appeals = ref<Record<string, any>[]>([])
const selected = ref<Record<string, any> | null>(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const requiredChangesText = ref('')
const suggestionsText = ref('')
const strengthsText = ref('')
const form = reactive({ result: 'PASSED', comment: '', resubmissionAllowed: true, excellent: false })
const submissionLinks = computed(() => selected.value ? [{ label: '代码仓库', value: selected.value.repositoryUrl }, { label: 'Pull Request', value: selected.value.pullRequestUrl }, { label: '在线演示', value: selected.value.demoUrl }, { label: '视频演示', value: selected.value.videoUrl }].filter((item) => item.value) : [])
function lines(value: string) { return value.split('\n').map((item) => item.trim()).filter(Boolean) }
function select(item: Record<string, any>) { selected.value = item; form.result = 'PASSED'; form.comment = ''; form.resubmissionAllowed = true; form.excellent = false; requiredChangesText.value = ''; suggestionsText.value = ''; strengthsText.value = '' }
async function load() { loading.value = true; try { [queue.value, appeals.value] = await Promise.all([getReviewQueue(), getAppealQueue()]); if (queue.value.length) select(queue.value[0]) } catch (reason) { error.value = getErrorMessage(reason) } finally { loading.value = false } }
async function submitReview() {
  if (!selected.value || !form.comment.trim()) return ElMessage.warning('请填写审核评语')
  if (form.result !== 'PASSED' && !requiredChangesText.value.trim()) return ElMessage.warning('退回或不通过时必须填写修改项')
  submitting.value = true
  try { await reviewSubmission(Number(selected.value.submissionId), { ...form, scores: {}, requiredChanges: lines(requiredChangesText.value), suggestions: lines(suggestionsText.value), strengths: lines(strengthsText.value) }); ElMessage.success('审核结果已提交'); await load() }
  catch (reason) { ElMessage.error(getErrorMessage(reason, '审核提交失败')) }
  finally { submitting.value = false }
}
async function resolve(item: Record<string, any>, status: 'UPHELD' | 'OVERTURNED') {
  try {
    const { value } = await ElMessageBox.prompt('请输入复核结论与处理依据。', status === 'UPHELD' ? '维持原审核结论' : '撤销原审核结论', { inputType:'textarea', inputPattern:/\S{5,}/, inputErrorMessage:'请至少填写 5 个字符' })
    await resolveAppeal(Number(item.id), status, value)
    ElMessage.success('申诉已处理')
    await load()
  } catch (reason) { if (reason !== 'cancel' && reason !== 'close') ElMessage.error(getErrorMessage(reason, '申诉处理失败')) }
}
onMounted(load)
</script>
