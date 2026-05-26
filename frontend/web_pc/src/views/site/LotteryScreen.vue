<template>
  <main class="lottery-screen">
    <section class="screen-header">
      <div>
        <span>OpenAtom Lottery</span>
        <h1>{{ lottery.title || '抽奖大屏' }}</h1>
        <p>{{ lottery.description || lottery.formName || '现场抽奖结果实时展示' }}</p>
      </div>
      <div class="screen-status">
        <strong>{{ statusText(lottery.status) }}</strong>
        <small>{{ formatDateTime(now) }}</small>
      </div>
    </section>

    <section class="metric-grid">
      <div>
        <span>参与人数</span>
        <strong>{{ lottery.participantCount || 0 }}</strong>
      </div>
      <div>
        <span>已抽中</span>
        <strong>{{ lottery.winnerCount || 0 }}</strong>
      </div>
      <div>
        <span>剩余奖品</span>
        <strong>{{ lottery.remainingPrizeCount || 0 }}</strong>
      </div>
      <div>
        <span>奖品总数</span>
        <strong>{{ lottery.totalPrizeCount || 0 }}</strong>
      </div>
    </section>

    <section class="screen-main">
      <div class="winner-stage">
        <div class="stage-title">
          <span>最新中奖</span>
          <small>{{ latestWinner ? formatDateTime(latestWinner.wonAt) : '等待抽取' }}</small>
        </div>
        <div v-if="latestWinner" class="winner-focus">
          <span>{{ latestWinner.prizeLevel || '奖品' }} · {{ latestWinner.prizeName }}</span>
          <strong>{{ latestWinner.winnerName }}</strong>
          <p>{{ latestWinner.winnerAccount || latestWinner.winnerContact || '匿名提交' }}</p>
        </div>
        <div v-else class="winner-empty">
          <strong>等待第一位中奖者</strong>
          <p>后台抽取后这里会自动刷新</p>
        </div>
      </div>

      <div class="prize-panel">
        <div class="panel-title">
          <span>奖品进度</span>
          <small>{{ prizes.length }} 个奖项</small>
        </div>
        <div class="prize-list">
          <div v-for="prize in prizes" :key="prize.id" class="prize-card">
            <span class="prize-bar" :style="{ backgroundColor: prize.color || '#39c5bb' }" />
            <div>
              <strong>{{ prize.level || '奖品' }} · {{ prize.name }}</strong>
              <p>已抽 {{ prize.wonCount || 0 }} / {{ prize.quantity || 0 }}</p>
            </div>
            <em>{{ prize.remainingCount || 0 }}</em>
          </div>
        </div>
      </div>
    </section>

    <section class="winner-wall">
      <div class="panel-title">
        <span>中奖名单</span>
        <small>实时更新</small>
      </div>
      <div v-if="winners.length" class="winner-list">
        <div v-for="winner in winners" :key="winner.id" class="winner-item">
          <strong>{{ winner.winnerName }}</strong>
          <span>{{ winner.prizeLevel || '奖品' }} · {{ winner.prizeName }}</span>
          <small>{{ formatDateTime(winner.wonAt) }}</small>
        </div>
      </div>
      <div v-else class="wall-empty">暂无中奖记录</div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { lotteryApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const screen = ref<Record<string, any>>({})
const now = ref(new Date())
let timer: number | null = null

const lottery = computed(() => screen.value.lottery || {})
const prizes = computed(() => screen.value.prizes || [])
const winners = computed(() => screen.value.winners || [])
const latestWinner = computed(() => screen.value.latestWinner || winners.value[0] || null)

async function loadScreen() {
  screen.value = await lotteryApi.screen(String(route.params.id))
  now.value = new Date()
}

function startTimer() {
  stopTimer()
  timer = window.setInterval(() => {
    loadScreen()
  }, 2000)
}

function stopTimer() {
  if (timer === null) return
  window.clearInterval(timer)
  timer = null
}

function statusText(status: unknown) {
  return (
    {
      draft: '未开始',
      open: '进行中',
      closed: '已结束',
    }[String(status || '').toLowerCase()] ||
    String(status || '-') ||
    '-'
  )
}

onMounted(async () => {
  await loadScreen()
  startTimer()
})

onBeforeUnmount(() => {
  stopTimer()
})
</script>

<style scoped>
.lottery-screen {
  min-height: 100vh;
  padding: clamp(20px, 3vw, 40px);
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(3, 15, 34, 0.94), rgba(7, 35, 48, 0.9)),
    radial-gradient(circle at 70% 0%, rgba(57, 197, 187, 0.22), transparent 32%);
  color: #f6fbff;
}

.screen-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.screen-header span,
.panel-title span,
.stage-title span {
  color: #6de6df;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.screen-header h1 {
  margin: 10px 0 8px;
  font-size: clamp(36px, 5vw, 76px);
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1;
}

.screen-header p {
  max-width: 760px;
  margin: 0;
  color: rgba(246, 251, 255, 0.72);
  font-size: clamp(16px, 1.6vw, 22px);
}

.screen-status {
  display: grid;
  min-width: 180px;
  gap: 8px;
  justify-items: end;
}

.screen-status strong {
  padding: 8px 14px;
  border: 1px solid rgba(109, 230, 223, 0.36);
  border-radius: 999px;
  color: #a7fff9;
  background: rgba(109, 230, 223, 0.1);
}

.screen-status small,
.panel-title small,
.stage-title small {
  color: rgba(246, 251, 255, 0.62);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 24px;
}

.metric-grid > div,
.winner-stage,
.prize-panel,
.winner-wall {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
  box-shadow: 0 22px 70px rgba(0, 0, 0, 0.22);
}

.metric-grid > div {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
}

.metric-grid span {
  color: rgba(246, 251, 255, 0.62);
  font-size: 14px;
}

.metric-grid strong {
  font-size: clamp(30px, 4vw, 54px);
  line-height: 1;
}

.screen-main {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 18px;
  margin-bottom: 18px;
}

.winner-stage,
.prize-panel,
.winner-wall {
  padding: 22px;
}

.stage-title,
.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.winner-focus {
  display: grid;
  min-height: 360px;
  align-content: center;
  gap: 18px;
}

.winner-focus span {
  color: #ffd666;
  font-size: clamp(20px, 3vw, 34px);
  font-weight: 700;
}

.winner-focus strong {
  font-size: clamp(62px, 10vw, 138px);
  line-height: 1;
}

.winner-focus p,
.winner-empty p {
  margin: 0;
  color: rgba(246, 251, 255, 0.64);
  font-size: clamp(18px, 2.4vw, 28px);
}

.winner-empty {
  display: grid;
  min-height: 360px;
  place-content: center;
  gap: 12px;
  text-align: center;
}

.winner-empty strong {
  font-size: clamp(34px, 5vw, 66px);
}

.prize-list,
.winner-list {
  display: grid;
  gap: 10px;
}

.prize-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 76px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.18);
}

.prize-bar {
  width: 12px;
  height: 48px;
  border-radius: 999px;
}

.prize-card strong,
.winner-item strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prize-card p {
  margin: 6px 0 0;
  color: rgba(246, 251, 255, 0.62);
}

.prize-card em {
  color: #a7fff9;
  font-size: 30px;
  font-style: normal;
  font-weight: 800;
}

.winner-wall {
  min-height: 180px;
}

.winner-list {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.winner-item {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.18);
}

.winner-item span {
  overflow: hidden;
  color: #ffd666;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.winner-item small {
  color: rgba(246, 251, 255, 0.56);
}

.wall-empty {
  display: grid;
  min-height: 110px;
  place-items: center;
  color: rgba(246, 251, 255, 0.62);
}

@media (max-width: 1100px) {
  .screen-header,
  .screen-main {
    grid-template-columns: 1fr;
  }

  .screen-header {
    display: grid;
  }

  .screen-status {
    justify-items: start;
  }

  .metric-grid,
  .winner-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .winner-list {
    grid-template-columns: 1fr;
  }

  .winner-focus,
  .winner-empty {
    min-height: 260px;
  }
}
</style>
