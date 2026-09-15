<template>
  <div class="page-container catalog-page">
    <header class="page-title-row">
      <div class="page-heading">
        <p class="eyebrow">TASK CENTER</p>
        <h1>任务中心</h1>
        <p>从当前阶段出发，选择一项目标清晰、可验证的任务。</p>
      </div>
      <router-link class="secondary-link" to="/assignments">我的任务</router-link>
    </header>

    <div class="filter-row" aria-label="任务筛选">
      <el-segmented v-model="activeStage" :options="stageOptions" />
    </div>

    <el-skeleton v-if="loading" :rows="5" animated />
    <el-result v-else-if="error" icon="error" title="任务加载失败" :sub-title="error">
      <template #extra><el-button type="primary" @click="load">重新加载</el-button></template>
    </el-result>
    <section v-else-if="filteredTasks.length" class="task-grid" aria-label="可领取任务">
      <router-link v-for="task in filteredTasks" :key="task.id" class="task-card" :to="`/tasks/${task.id}`">
        <div class="task-card__meta">
          <span>{{ task.stageKey || 'OPEN' }}</span>
          <span>{{ difficultyText[task.difficulty] || task.difficulty }}</span>
        </div>
        <h2>{{ task.title }}</h2>
        <p>{{ task.summary }}</p>
        <div class="task-card__footer">
          <span>{{ task.estimatedMinutes }} 分钟 · {{ task.points }} 积分</span>
          <span class="status-chip" :data-status="task.memberStatus">{{ statusText[task.memberStatus] || task.memberStatus }}</span>
        </div>
      </router-link>
    </section>
    <div v-else class="content-card empty-panel">
      <strong>当前筛选下暂无任务</strong>
      <p>管理员发布任务后会显示在这里。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getErrorMessage } from '@/api/http'
import { getTasks, type TaskSummary } from '@/api/quest'

const route = useRoute()
const tasks = ref<TaskSummary[]>([])
const loading = ref(true)
const error = ref('')
const stageOptions = ['全部', 'L0', 'L1', 'L2', 'L3', 'L4']
const requestedStage = String(route.query.stage || '')
const activeStage = ref(stageOptions.includes(requestedStage) ? requestedStage : '全部')
const filteredTasks = computed(() => activeStage.value === '全部' ? tasks.value : tasks.value.filter((task) => task.stageKey === activeStage.value))
const difficultyText: Record<string, string> = { ENTRY: '入门', BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级' }
const statusText: Record<string, string> = { NOT_STARTED: '可领取', IN_PROGRESS: '进行中', PENDING_REVIEW: '待审核', REVISION_REQUIRED: '需修改', PASSED: '已通过', OVERDUE: '已逾期', ABANDONED: '已放弃' }

async function load() {
  loading.value = true
  error.value = ''
  try { tasks.value = await getTasks() }
  catch (reason) { error.value = getErrorMessage(reason, '无法获取任务列表') }
  finally { loading.value = false }
}

onMounted(load)
</script>
