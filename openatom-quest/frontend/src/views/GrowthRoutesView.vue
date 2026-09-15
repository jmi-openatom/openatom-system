<template>
  <div class="page-container catalog-page">
    <header class="page-heading"><p class="eyebrow">GROWTH ROUTES</p><h1>成长路线</h1><p>路线会根据你选择的技术方向推荐，并用真实任务记录每一步进展。</p></header>
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-result v-else-if="error" icon="error" title="路线加载失败" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <section v-else-if="routes.length" class="route-grid">
      <article v-for="routeItem in routes" :key="Number(routeItem.id)" class="content-card route-card">
        <div class="task-card__meta"><span>{{ routeItem.directionName }}</span><span>{{ routeItem.stageCount }} 个阶段</span></div>
        <h2>{{ routeItem.name }}</h2><p>{{ routeItem.description }}</p>
        <div class="route-card__footer"><span>{{ routeItem.taskCount }} 项已发布任务</span><router-link :to="`/routes/${routeItem.id}`"><el-button :type="routeItem.memberStatus === 'ACTIVE' ? 'primary' : 'default'">{{ routeItem.memberStatus === 'ACTIVE' ? '继续路线' : '查看路线' }}</el-button></router-link></div>
      </article>
    </section>
    <div v-else class="content-card empty-panel"><strong>还没有适合当前方向的已发布路线</strong><p>你可以先在个人资料中确认技术方向。</p></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getErrorMessage } from '@/api/http'
import { getRoutes } from '@/api/quest'
const routes = ref<Record<string, any>[]>([])
const loading = ref(true)
const error = ref('')
async function load() { loading.value = true; try { routes.value = await getRoutes() } catch (reason) { error.value = getErrorMessage(reason) } finally { loading.value = false } }
onMounted(load)
</script>
