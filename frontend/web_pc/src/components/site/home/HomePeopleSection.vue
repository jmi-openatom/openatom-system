<template>
  <section id="people" class="container section people-section">
    <div class="section-heading reveal-block">
      <span>成员信息</span>
      <h2>主要人员</h2>
      <p>{{ people.length ? `当前展示 ${people.length} 位成员` : '暂无成员信息' }}</p>
    </div>
    <div class="people-grid">
      <article
        v-for="person in people"
        :key="person.userId || person.name"
        class="person-card reveal-card"
      >
        <div class="avatar">{{ person.initial }}</div>
        <div>
          <h3>{{ person.name }}</h3>
          <p>{{ person.role }}</p>
        </div>
        <small>{{ person.focus }}</small>
      </article>
    </div>
    <el-empty v-if="!people.length && !loading" description="暂无主要人员数据" />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  people: any[]
  loading: boolean
}>()
</script>
