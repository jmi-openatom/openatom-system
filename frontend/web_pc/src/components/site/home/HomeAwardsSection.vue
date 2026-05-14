<template>
  <section id="achievements" class="container section achievements-section">
    <div class="section-heading reveal-block">
      <span>成果展示</span>
      <h2>比赛获奖展示</h2>
      <p>{{ awards.length ? `当前展示 ${awards.length} 项获奖记录` : '暂无获奖记录' }}</p>
    </div>
    <div class="award-grid">
      <article
        v-for="award in awards"
        :key="award.id || `${award.year}-${award.title}`"
        class="award-card reveal-card"
      >
        <div class="award-card__year">{{ award.year }}</div>
        <h3>{{ award.title }}</h3>
        <p>{{ award.competitionName }}</p>
        <strong>{{ award.awardLevel }}</strong
        ><br /><br />
        <small>{{ award.teamName }}</small>
      </article>
    </div>
    <el-empty v-if="!awards.length && !loading" description="暂无比赛获奖数据" />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  awards: any[]
  loading: boolean
}>()
</script>
