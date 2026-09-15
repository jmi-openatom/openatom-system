<template>
  <div class="page-container catalog-page">
    <header class="page-title-row">
      <div class="page-heading"><p class="eyebrow">MY QUESTS</p><h1>我的任务</h1><p>继续进行中的任务，或根据导师反馈提交新版本。</p></div>
      <router-link class="secondary-link" to="/tasks">发现任务</router-link>
    </header>
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-result v-else-if="error" icon="error" title="加载失败" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <section v-else-if="assignments.length" class="assignment-list">
      <router-link v-for="item in assignments" :key="item.id" :to="`/assignments/${item.id}`" class="assignment-row">
        <div class="assignment-row__status"><span class="status-dot" :data-status="item.status" />{{ statusText[item.status] || item.status }}</div>
        <div><h2>{{ item.title }}</h2><p>{{ item.latestFeedback || item.summary }}</p></div>
        <div class="assignment-row__meta"><span>v{{ item.latestVersion || 0 }}</span><span>{{ item.points }} 积分</span><span aria-hidden="true">→</span></div>
      </router-link>
    </section>
    <div v-else class="content-card empty-panel"><strong>还没有领取任务</strong><p>从任务中心选择第一个适合你的挑战。</p><router-link to="/tasks"><el-button type="primary">前往任务中心</el-button></router-link></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getErrorMessage } from '@/api/http'
import { getAssignments, type Assignment } from '@/api/quest'

const assignments = ref<Assignment[]>([])
const loading = ref(true)
const error = ref('')
const statusText: Record<string, string> = { IN_PROGRESS: '进行中', PENDING_REVIEW: '待审核', REVISION_REQUIRED: '需修改', PASSED: '已通过', OVERDUE: '已逾期', ABANDONED: '已结束' }
async function load() {
  loading.value = true
  try { assignments.value = await getAssignments() }
  catch (reason) { error.value = getErrorMessage(reason) }
  finally { loading.value = false }
}
onMounted(load)
</script>
