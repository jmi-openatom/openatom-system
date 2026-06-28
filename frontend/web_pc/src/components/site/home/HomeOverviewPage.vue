<template>
  <section class="home-overview-page home-interactive-section">
    <HomeInteractiveBackdrop :radius="220" :spacing="66" :strength="20" />
    <div class="container home-overview-page__inner">
<!--      <div class="hero__actions">-->
<!--        <el-button-->
<!--          :icon="DataAnalysis"-->
<!--          size="large"-->
<!--          type="primary"-->
<!--          @click="$emit('scrollTo', 'activities')"-->
<!--        >-->
<!--          查看活动-->
<!--        </el-button>-->
<!--        <el-button :icon="UserFilled" size="large" @click="$emit('scrollTo', 'people')">-->
<!--          了解成员-->
<!--        </el-button>-->
<!--      </div>-->

      <div class="command-panel">
        <div v-if="metrics.length" aria-label="社团关键指标" class="metric-console">
          <div class="metric-console__rail"></div>

          <div class="metric-console__core">
            <Transition mode="out-in" name="metric-swap">
              <div :key="activeMetric?.label" class="metric-console__copy">
                <span>社团关键指标</span>
                <strong>{{ activeMetric?.value }}</strong>
                <p>{{ activeMetric?.label }}</p>
                <small>{{ activeMetric?.note }}</small>
              </div>
            </Transition>
          </div>

          <div class="metric-console__orbit" @pointerleave="setActive(0)">
            <button
              v-for="(metric, index) in metrics"
              :key="metric.label"
              :class="{ 'is-active': index === activeIndex }"
              class="metric-node"
              type="button"
              @focus="setActive(index)"
              @pointerenter="setActive(index)"
            >
              <i></i>
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <small>{{ metric.note }}</small>
            </button>
          </div>
        </div>

        <el-empty v-else :image-size="72" description="暂无社团统计数据" />

<!--        <div v-if="techStack.length" class="terminal-strip">-->
<!--          <span v-for="item in techStack" :key="item">{{ item }}</span>-->
<!--        </div>-->
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, markRaw, ref, type Component } from 'vue'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'
import {
  Calendar,
  Collection,
  DataAnalysis as DataAnalysisIcon,
  Trophy,
  UserFilled as UserFilledIcon,
} from '@element-plus/icons-vue'

const props = defineProps<{
  metrics: any[]
  techStack: string[]
}>()

defineEmits<{
  scrollTo: [id: string]
}>()

const activeIndex = ref(0)
const activeMetric = computed(() => props.metrics[activeIndex.value] || props.metrics[0])

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

function setActive(index: number) {
  activeIndex.value = index
}
</script>
