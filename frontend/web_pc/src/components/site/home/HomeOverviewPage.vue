<template>
  <section
    id="community"
    aria-labelledby="community-title"
    class="story-community story-section home-interactive-section"
  >
    <HomeInteractiveBackdrop :radius="230" :spacing="68" :strength="22" />
    <div class="story-container">
      <header class="story-section-head">
        <div>
          <h2 id="community-title">每一个数字背后，<br />都是一起走过的路。</h2>
        </div>
        <span class="story-community__annotation">连接 · 分享 · 生长</span>
      </header>
      <div :aria-busy="loading">
        <el-skeleton v-if="loading" :rows="3" aria-label="正在加载社团统计数据" />
        <dl v-else-if="metrics.length" class="community-metrics">
          <div v-for="metric in metrics" :key="metric.label">
            <dt>{{ metric.label }}</dt>
            <dd>{{ metric.value }}</dd>
            <p>{{ metric.note }}</p>
          </div>
        </dl>
        <p v-else class="story-empty">暂无社团统计数据</p>
      </div>
    </div>
  </section>
</template>
<script lang="ts" setup>
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

withDefaults(defineProps<{ metrics: any[]; techStack: string[]; loading?: boolean }>(), {
  loading: false,
})
</script>
