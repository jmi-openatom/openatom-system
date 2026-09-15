<template>
  <div class="page-container detail-page">
    <el-skeleton v-if="loading" :rows="10" animated />
    <el-result v-else-if="error" icon="error" title="任务加载失败" :sub-title="error">
      <template #extra><el-button @click="router.push('/tasks')">返回任务中心</el-button></template>
    </el-result>
    <template v-else-if="task">
      <nav class="breadcrumb"><router-link to="/tasks">任务中心</router-link><span>/</span><span>{{ task.title }}</span></nav>
      <header class="task-hero">
        <div>
          <div class="task-card__meta"><span>{{ task.stageKey || 'OPEN' }}</span><span>{{ task.directionName || '通用方向' }}</span></div>
          <h1>{{ task.title }}</h1>
          <p>{{ task.summary }}</p>
        </div>
        <aside class="task-hero__action">
          <strong>{{ task.points }}</strong><small>完成积分</small>
          <el-button v-if="!task.assignmentId" type="primary" size="large" :disabled="!prerequisitesMet" :loading="claiming" @click="claim">领取任务</el-button>
          <router-link v-else :to="`/assignments/${task.assignmentId}`"><el-button type="primary" size="large">查看我的任务</el-button></router-link>
          <small v-if="!prerequisitesMet" class="danger-copy">请先完成前置任务</small>
        </aside>
      </header>

      <section class="detail-layout">
        <main class="detail-content">
          <article class="content-card rich-section"><p class="eyebrow">OBJECTIVES</p><h2>学习目标</h2><ul><li v-for="item in parseList(task.learningObjectives)" :key="item">{{ item }}</li></ul></article>
          <article class="content-card rich-section"><p class="eyebrow">STEPS</p><h2>操作步骤</h2><div class="pre-wrap">{{ task.instructions }}</div></article>
          <article class="content-card rich-section"><p class="eyebrow">SUBMISSION</p><h2>提交要求</h2><div class="pre-wrap">{{ task.submissionRequirements }}</div></article>
          <article class="content-card rich-section"><p class="eyebrow">ACCEPTANCE</p><h2>验收标准</h2><div class="pre-wrap">{{ task.acceptanceCriteria }}</div></article>
        </main>
        <aside class="detail-aside">
          <section class="content-card fact-list">
            <div><span>难度</span><strong>{{ difficultyText[task.difficulty] || task.difficulty }}</strong></div>
            <div><span>预计时间</span><strong>{{ task.estimatedMinutes }} 分钟</strong></div>
            <div><span>负责人</span><strong>{{ task.ownerName }}</strong></div>
            <div><span>提交上限</span><strong>{{ task.submissionLimit || '不限' }}</strong></div>
          </section>
          <section v-if="task.prerequisites.length" class="content-card rich-section">
            <h2>前置任务</h2>
            <router-link v-for="item in task.prerequisites" :key="item.id" :to="`/tasks/${item.id}`" class="prerequisite-row">
              <span>{{ item.completed ? '已完成' : '未完成' }}</span><strong>{{ item.title }}</strong>
            </router-link>
          </section>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getErrorMessage } from '@/api/http'
import { claimTask, getTask, type TaskDetail } from '@/api/quest'

const route = useRoute()
const router = useRouter()
const task = ref<TaskDetail | null>(null)
const loading = ref(true)
const claiming = ref(false)
const error = ref('')
const prerequisitesMet = computed(() => task.value?.prerequisites.every((item) => item.completed) ?? false)
const difficultyText: Record<string, string> = { ENTRY: '入门', BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级' }

function parseList(value: string) {
  try { return JSON.parse(value) as string[] } catch { return [] }
}

async function load() {
  try { task.value = await getTask(Number(route.params.id)) }
  catch (reason) { error.value = getErrorMessage(reason) }
  finally { loading.value = false }
}

async function claim() {
  claiming.value = true
  try {
    const result = await claimTask(Number(route.params.id))
    ElMessage.success('任务已领取')
    await router.push(`/assignments/${result.assignmentId}`)
  } catch (reason) { ElMessage.error(getErrorMessage(reason, '领取失败')) }
  finally { claiming.value = false }
}

onMounted(load)
</script>
