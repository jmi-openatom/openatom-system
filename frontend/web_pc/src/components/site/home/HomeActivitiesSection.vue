<template>
  <section id="activities" class="activity-band">
    <div class="activity-shell section">
      <div class="section-heading reveal-block">
        <span>近期活动</span>
        <h2>最新活动</h2>
      </div>

      <el-carousel
        v-if="activities.length"
        :interval="4000"
        arrow="always"
        class="activity-carousel"
        height="600px"
        type="card"
      >
        <el-carousel-item
          v-for="activity in activities"
          :key="activity.title"
          class="activity-carousel-item"
        >
          <article
            class="activity-card reveal-card"
            @click="router.push(`/activities/${activity.id}`)"
          >
            <time>{{ activity.date }}</time>
            <div>
              <h3>{{ activity.title }}</h3>
              <p>{{ activity.description }}</p>
            </div>

            <div class="activity-image">
              <img :alt="activity.title" :src="activity.coverUrl" />
            </div>
          </article>
        </el-carousel-item>
      </el-carousel>

      <el-empty v-if="!activities.length && !loading" description="暂无活动数据" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  activities: any[]
  loading: boolean
}>()

const router = useRouter()
</script>
