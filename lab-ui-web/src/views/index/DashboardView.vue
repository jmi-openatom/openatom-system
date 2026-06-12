<template>
  <main class="page">
    <div class="page-grid">
      <section class="panel">
        <div class="panel__head">
          <h1>今日工作台</h1>
          <RouterLink class="button" to="/problem"><Code2 :size="17" />进入刷题</RouterLink>
        </div>
        <div class="panel__body">
          <div class="metric-row">
            <div class="metric"><strong>{{ score?.reputationScore ?? '-' }}</strong><span>实验室信誉分</span></div>
            <div class="metric"><strong>{{ score?.checkinCount ?? '-' }}</strong><span>累计签到</span></div>
            <div class="metric"><strong>{{ today?.sessionCount ?? 0 }}</strong><span>今日晚自习</span></div>
          </div>
        </div>
      </section>

      <section class="panel panel--dark">
        <div class="panel__head">
          <h2>{{ problem?.title || '每日一练' }}</h2>
          <span class="status">{{ problem?.difficulty || '待刷新' }}</span>
        </div>
        <div class="panel__body">
          <p class="muted">{{ problem?.problemDate || '' }}</p>
          <RouterLink class="button secondary" to="/problem"><ArrowRight :size="17" />打开题面</RouterLink>
        </div>
      </section>
    </div>

    <div class="page-grid" style="margin-top: 16px">
      <section class="panel">
        <div class="panel__head">
          <h2>通知</h2>
          <Bell :size="18" />
        </div>
        <div class="panel__body list">
          <article v-for="item in noticeList" :key="item.id" class="list-item">
            <strong>{{ item.title }}</strong>
            <span class="muted">{{ item.content }}</span>
          </article>
          <p v-if="noticeList.length === 0" class="muted">暂无通知</p>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head">
          <h2>积分流水</h2>
          <Trophy :size="18" />
        </div>
        <div class="panel__body list">
          <article v-for="log in logs" :key="log.id" class="list-item">
            <strong>{{ log.checkinDate }}</strong>
            <span class="muted">LMS {{ log.localScoreChange }} / CMS +{{ log.clubScoreChange }}</span>
          </article>
          <p v-if="logs.length === 0" class="muted">暂无流水</p>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ArrowRight, Bell, Code2, Trophy } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { todayProblem, type Problem } from '@/api/oj'
import { eveningStudyToday, type EveningStudyToday } from '@/api/checkin'
import { scoreLogs, scoreOverview, type CheckinLog, type ScoreOverview } from '@/api/score'
import { notices, type Notice } from '@/api/notice'

const problem = ref<Problem>()
const today = ref<EveningStudyToday>()
const score = ref<ScoreOverview>()
const logs = ref<CheckinLog[]>([])
const noticeList = ref<Notice[]>([])

onMounted(async () => {
  ;[problem.value, today.value, score.value, logs.value, noticeList.value] = await Promise.all([
    todayProblem(),
    eveningStudyToday(),
    scoreOverview(),
    scoreLogs(),
    notices(),
  ])
})
</script>
