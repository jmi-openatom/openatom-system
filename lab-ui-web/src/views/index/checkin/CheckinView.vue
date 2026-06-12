<template>
  <main class="page">
    <div class="page-grid">
      <section class="panel">
        <div class="panel__head">
          <h1>签到</h1>
          <QrCode :size="18" />
        </div>
        <div class="panel__body">
          <div class="field">
            <label>签到码</label>
            <input v-model="token" class="input" />
          </div>
          <button class="button" style="margin-top: 12px" @click="scan"><CheckCircle2 :size="17" />签到</button>
          <p v-if="message" class="status" style="margin-left: 10px">{{ message }}</p>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head">
          <h2>今日晚自习</h2>
          <CalendarClock :size="18" />
        </div>
        <div class="panel__body list">
          <article v-for="item in today?.sessions || []" :key="item.id" class="list-item">
            <strong>{{ item.title }}</strong>
            <span class="muted">{{ item.signedCount }}/{{ item.targetCount }} 已签到 · 迟到 {{ item.lateCount }} · 旷课 {{ item.absentCount }}</span>
          </article>
          <p v-if="!today?.sessions?.length" class="muted">暂无场次</p>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { CalendarClock, CheckCircle2, QrCode } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { eveningStudyToday, scanCheckIn, type EveningStudyToday } from '@/api/checkin'

const token = ref('')
const message = ref('')
const today = ref<EveningStudyToday>()

async function load() {
  today.value = await eveningStudyToday()
}

async function scan() {
  try {
    const record = await scanCheckIn(token.value)
    message.value = record.status === 'late' ? '签到成功，已记为迟到' : '签到成功'
    await load()
  } catch (err) {
    message.value = err instanceof Error ? err.message : '签到失败'
  }
}

onMounted(load)
</script>
