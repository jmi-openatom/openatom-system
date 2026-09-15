<template>
  <div class="page-container detail-page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="error" icon="error" title="路线加载失败" :sub-title="error" />
    <template v-else-if="routeData">
      <nav class="breadcrumb"><router-link to="/routes">成长路线</router-link><span>/</span><span>{{ routeData.name }}</span></nav>
      <header class="task-hero route-hero"><div><p class="eyebrow">{{ routeData.directionName }}</p><h1>{{ routeData.name }}</h1><p>{{ routeData.description }}</p></div><el-button v-if="routeData.memberStatus === 'NOT_ENROLLED'" type="primary" size="large" :loading="enrolling" @click="enroll">加入路线</el-button><span v-else class="status-chip status-chip--large" data-status="IN_PROGRESS">{{ routeData.memberStatus === 'COMPLETED' ? '已完成' : '进行中' }}</span></header>
      <section class="stage-timeline">
        <article v-for="(stage, index) in routeData.stages" :key="stage.id" class="stage-row">
          <div class="stage-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div><div class="task-card__meta"><span>{{ stage.stageKey }}</span><span>{{ Number(stage.passedTasks || 0) }}/{{ Number(stage.publishedTasks || 0) }} 已通过</span></div><h2>{{ stage.name }}</h2><p>{{ stage.objective }}</p><el-progress :percentage="progress(stage)" :show-text="false" /></div>
          <router-link :to="`/tasks?stage=${stage.stageKey}`">查看任务 →</router-link>
        </article>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getErrorMessage } from '@/api/http'
import { enrollRoute, getRoute } from '@/api/quest'
const route = useRoute()
const routeData = ref<Record<string, any> | null>(null)
const loading = ref(true)
const enrolling = ref(false)
const error = ref('')
function progress(stage: Record<string, any>) { const total = Number(stage.publishedTasks || 0); return total ? Math.round(Number(stage.passedTasks || 0) * 100 / total) : 0 }
async function load() { try { routeData.value = await getRoute(Number(route.params.id)) } catch (reason) { error.value = getErrorMessage(reason) } finally { loading.value = false } }
async function enroll() { enrolling.value = true; try { await enrollRoute(Number(route.params.id)); ElMessage.success('已加入成长路线'); await load() } catch (reason) { ElMessage.error(getErrorMessage(reason, '加入失败')) } finally { enrolling.value = false } }
onMounted(load)
</script>
