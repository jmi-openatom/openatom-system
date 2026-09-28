<template>
  <div class="page-container growth-profile-page">
    <header class="page-heading"><p class="eyebrow">GROWTH RECORD</p><h1>积分与等级</h1><p>每一笔积分都对应真实学习成果或贡献，可追溯且不会重复发放。</p></header>
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="error" icon="error" title="成长记录加载失败" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <template v-else-if="data && leaderboard">
      <section class="growth-summary"><article class="welcome-card"><div><p class="eyebrow eyebrow--inverse">CURRENT LEVEL</p><h2>{{ data.level }}</h2><p>当前累计 {{ data.points }} 积分</p></div></article><article class="content-card level-track"><div v-for="rule in data.rules" :key="rule.levelKey" :class="{ reached: data.points >= rule.minimumPoints }"><strong>{{ rule.levelKey }} · {{ rule.name }}</strong><span>{{ rule.minimumPoints }} 分</span></div></article></section>
      <section class="content-card leaderboard-card" aria-labelledby="leaderboard-title">
        <div class="section-heading leaderboard-card__heading">
          <div><p class="eyebrow">MEMBER RANKING</p><h2 id="leaderboard-title">成员排行榜</h2><p>按累计积分排序，展示各等级成员的当前成绩。</p></div>
          <div class="leaderboard-card__refresh"><span>{{ refreshError || `每 15 秒更新 · ${formatDate(leaderboard.updatedAt)}` }}</span><el-button :loading="refreshing" @click="refresh">刷新</el-button></div>
        </div>
        <div class="leaderboard-filters" aria-label="筛选等级">
          <button type="button" :class="{ active: selectedLevel === 'all' }" :aria-pressed="selectedLevel === 'all'" @click="selectedLevel = 'all'">全部 <span>{{ leaderboard.members.length }}</span></button>
          <button v-for="level in levels" :key="level.key" type="button" :class="{ active: selectedLevel === level.key }" :aria-pressed="selectedLevel === level.key" @click="selectedLevel = level.key">{{ level.key }} · {{ level.name }} <span>{{ level.count }}</span></button>
        </div>
        <div v-if="visibleMembers.length" class="leaderboard-list">
          <div v-for="member in visibleMembers" :key="member.id" class="leaderboard-row" :class="{ 'leaderboard-row--self': member.id === auth.member?.id }">
            <strong class="leaderboard-row__rank">{{ member.rank }}</strong>
            <div class="leaderboard-row__person"><span class="leaderboard-row__avatar"><img v-if="member.avatarUrl && !failedAvatars.has(member.id)" :src="member.avatarUrl" alt="" @error="markAvatarFailed(member.id)" /><span v-else>{{ member.nickname?.trim().slice(0, 1) || '成' }}</span></span><div><strong>{{ member.nickname || `成员 #${member.id}` }}</strong><small v-if="member.id === auth.member?.id">你</small></div></div>
            <span class="leaderboard-row__level">{{ member.levelKey }} · {{ levelName(member.levelKey) }}</span>
            <strong class="leaderboard-row__points">{{ member.points }} <small>分</small></strong>
          </div>
        </div>
        <div v-else class="empty-copy">这个等级暂时没有上榜成员。</div>
      </section>
      <section class="content-card admin-table-card"><div class="section-heading"><div><p class="eyebrow">POINT LEDGER</p><h2>积分明细</h2></div></div><div v-if="data.ledger.length" class="data-table-wrap"><table class="data-table"><thead><tr><th>时间</th><th>来源</th><th>变动</th><th>余额</th></tr></thead><tbody><tr v-for="item in data.ledger" :key="item.id"><td>{{ formatDate(item.createdAt) }}</td><td>{{ item.reason }}</td><td><strong>{{ item.amount > 0 ? '+' : '' }}{{ item.amount }}</strong></td><td>{{ item.balanceAfter }}</td></tr></tbody></table></div><div v-else class="empty-copy">完成首个任务后，积分记录会显示在这里。</div></section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getErrorMessage } from '@/api/http'
import { getLeaderboard, getMemberGrowth, type LeaderboardData } from '@/api/quest'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const data = ref<Record<string, any> | null>(null)
const leaderboard = ref<LeaderboardData | null>(null)
const loading = ref(true)
const refreshing = ref(false)
const error = ref('')
const refreshError = ref('')
const selectedLevel = ref('all')
const failedAvatars = ref(new Set<number>())
let refreshTimer: number | undefined

const levels = computed(() => {
  const rules = (data.value?.rules || []) as Array<{ levelKey: string; name: string }>
  const counts = new Map<string, number>()
  for (const member of leaderboard.value?.members || []) counts.set(member.levelKey, (counts.get(member.levelKey) || 0) + 1)
  const known = rules.map(rule => ({ key: rule.levelKey, name: rule.name, count: counts.get(rule.levelKey) || 0 }))
  const missing = [...counts].filter(([key]) => !rules.some(rule => rule.levelKey === key)).map(([key, count]) => ({ key, name: key, count }))
  return [...known, ...missing]
})
const visibleMembers = computed(() => (leaderboard.value?.members || []).filter(member => selectedLevel.value === 'all' || member.levelKey === selectedLevel.value))

function levelName(key: string) { return levels.value.find(level => level.key === key)?.name || key }
function formatDate(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
function markAvatarFailed(id: number) { failedAvatars.value = new Set([...failedAvatars.value, id]) }
async function refresh() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    const [growth, ranking] = await Promise.all([getMemberGrowth(), getLeaderboard()])
    data.value = growth
    leaderboard.value = ranking
    error.value = ''
    refreshError.value = ''
  } catch (reason) {
    const message = getErrorMessage(reason)
    if (!data.value || !leaderboard.value) error.value = message
    else refreshError.value = `更新失败：${message}`
  } finally { refreshing.value = false }
}
async function load() { loading.value = true; await refresh(); loading.value = false }
function refreshWhenVisible() { if (document.visibilityState === 'visible') void refresh() }
onMounted(() => { void load(); refreshTimer = window.setInterval(refreshWhenVisible, 15_000); document.addEventListener('visibilitychange', refreshWhenVisible) })
onUnmounted(() => { if (refreshTimer !== undefined) window.clearInterval(refreshTimer); document.removeEventListener('visibilitychange', refreshWhenVisible) })
</script>

<style scoped>
.growth-profile-page { display: grid; gap: var(--space-5); }
.growth-profile-page .page-heading { margin-bottom: var(--space-3); }
.leaderboard-card { min-height: 0; }
.leaderboard-card__heading { align-items: flex-end; gap: var(--space-5); }
.leaderboard-card__heading p:last-child { margin: 6px 0 0; color: var(--color-text-secondary); font-size: 13px; }
.leaderboard-card__refresh { display: flex; align-items: center; gap: var(--space-3); color: var(--color-text-secondary); font-size: 12px; white-space: nowrap; }
.leaderboard-filters { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-5); }
.leaderboard-filters button { min-height: 36px; padding: 7px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-round); color: var(--color-text-regular); background: var(--color-bg-container); font: inherit; font-size: 12px; cursor: pointer; }
.leaderboard-filters button:hover { border-color: var(--color-text-tertiary); }
.leaderboard-filters button.active { border-color: var(--color-primary); color: var(--color-primary-foreground); background: var(--color-primary); }
.leaderboard-filters span { margin-left: 5px; opacity: .7; }
.leaderboard-list { border-top: 1px solid var(--color-border-light); }
.leaderboard-row { min-height: 72px; padding: var(--space-3) var(--space-2); display: grid; grid-template-columns: 48px minmax(0, 1fr) minmax(100px, 160px) 100px; align-items: center; gap: var(--space-3); border-bottom: 1px solid var(--color-border-light); }
.leaderboard-row--self { border-radius: var(--radius-md); background: var(--color-bg-subtle); }
.leaderboard-row__rank { color: var(--color-text-tertiary); font-size: 16px; text-align: center; }
.leaderboard-row:nth-child(-n+3) .leaderboard-row__rank { color: var(--color-text-primary); }
.leaderboard-row__person { min-width: 0; display: flex; align-items: center; gap: var(--space-3); }
.leaderboard-row__avatar { width: 40px; height: 40px; flex: 0 0 auto; overflow: hidden; display: grid; place-items: center; border-radius: 50%; color: var(--color-primary-foreground); background: var(--color-primary); font-size: 14px; }
.leaderboard-row__avatar img { width: 100%; height: 100%; object-fit: cover; }
.leaderboard-row__person strong { overflow: hidden; display: block; color: var(--color-text-primary); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.leaderboard-row__person small { color: var(--color-text-tertiary); font-size: 11px; }
.leaderboard-row__level { color: var(--color-text-secondary); font-size: 13px; }
.leaderboard-row__points { font-size: 16px; text-align: right; }
.leaderboard-row__points small { color: var(--color-text-tertiary); font-size: 11px; font-weight: 400; }
@media (max-width: 767px) {
  .leaderboard-card__heading { align-items: flex-start; flex-direction: column; }
  .leaderboard-card__refresh { justify-content: space-between; width: 100%; white-space: normal; }
  .leaderboard-row { grid-template-columns: 30px minmax(0, 1fr) 72px; gap: var(--space-2); }
  .leaderboard-row__level { grid-column: 2; grid-row: 2; margin-top: -14px; font-size: 11px; }
  .leaderboard-row__points { grid-column: 3; grid-row: 1 / 3; }
}
</style>
