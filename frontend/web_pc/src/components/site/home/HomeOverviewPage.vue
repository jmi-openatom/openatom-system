<template>
  <section class="home-overview-page">
    <div class="container home-overview-page__inner">
      <div class="hero__actions">
        <el-button
          :icon="DataAnalysis"
          size="large"
          type="primary"
          @click="$emit('scrollTo', 'activities')"
        >
          查看活动
        </el-button>
        <el-button :icon="UserFilled" size="large" @click="$emit('scrollTo', 'people')">
          了解成员
        </el-button>
      </div>

      <div class="command-panel">
        <div v-if="metrics.length" aria-label="社团关键指标" class="signal-grid" role="list">
          <div
            v-for="metric in metrics"
            :key="metric.label"
            :class="`signal-card--${metricTone(metric.label)}`"
            class="signal-card"
            role="listitem"
          >
            <div class="signal-card__head">
              <el-icon aria-hidden="true" class="signal-card__icon">
                <component :is="metricIcon(metric.label)" />
              </el-icon>
              <span class="signal-card__label">{{ metric.label }}</span>
            </div>
            <strong class="signal-card__value">{{ metric.value }}</strong>
            <small class="signal-card__note">{{ metric.note }}</small>
          </div>
        </div>
        <el-empty v-else :image-size="72" description="暂无社团统计数据" />
        <div v-if="techStack.length" class="terminal-strip">
          <span v-for="item in techStack" :key="item">{{ item }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { type Component, markRaw } from 'vue'
import {
  Calendar,
  Collection,
  DataAnalysis as DataAnalysisIcon,
  Trophy,
  UserFilled as UserFilledIcon,
} from '@element-plus/icons-vue'

defineProps<{
  metrics: any[]
  techStack: string[]
}>()

defineEmits<{
  scrollTo: [id: string]
}>()

const DataAnalysis = markRaw(DataAnalysisIcon)
const UserFilled = markRaw(UserFilledIcon)

const metricIcons = {
  Calendar: markRaw(Calendar),
  Collection: markRaw(Collection),
  DataAnalysis,
  Trophy: markRaw(Trophy),
  UserFilled,
} satisfies Record<string, Component>

function metricIcon(label?: string) {
  if (!label) return metricIcons.DataAnalysis
  if (label.includes('成员')) return metricIcons.UserFilled
  if (label.includes('活动')) return metricIcons.Calendar
  if (label.includes('获奖')) return metricIcons.Trophy
  if (label.includes('招新')) return metricIcons.Collection
  return metricIcons.DataAnalysis
}

function metricTone(label?: string) {
  if (!label) return 'default'
  if (label.includes('成员')) return 'members'
  if (label.includes('活动')) return 'activity'
  if (label.includes('获奖')) return 'award'
  if (label.includes('招新')) return 'recruit'
  return 'default'
}
</script>
