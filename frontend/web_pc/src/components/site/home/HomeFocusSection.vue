<template>
  <section class="container section club-brief">
    <div class="section-heading reveal-block">
      <span>社团概览</span>
      <h2>{{ club.name || '社团' }}的主要方向</h2>
      <p>从部门分工到日常活动，页面内容尽量保持清晰、真实，方便快速了解社团正在做什么。</p>
    </div>
    <div class="brief-grid">
      <article v-for="item in focusAreas" :key="item.title" class="brief-card reveal-card">
        <el-icon>
          <component :is="focusIcon(item.icon)" />
        </el-icon>
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
      </article>
    </div>
    <el-empty v-if="!focusAreas.length && !loading" description="暂无社团部门数据" />
  </section>
</template>

<script setup lang="ts">
import { type Component, markRaw } from 'vue'
import { Cpu, Lightning, Monitor } from '@element-plus/icons-vue'

defineProps<{
  club: Record<string, any>
  focusAreas: any[]
  loading: boolean
}>()

const iconMap: Record<string, Component> = {
  monitor: markRaw(Monitor),
  cpu: markRaw(Cpu),
  lightning: markRaw(Lightning),
}

function focusIcon(name?: string) {
  return iconMap[name || ''] || iconMap.monitor
}
</script>
