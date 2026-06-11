<template>
  <ViewPage :loading="loading" class="evening-page">
    <SitePageHero
      compact
      description="按实验室分组查看当天晚自习签到，已通过请假会自动从应到名单中剔除。"
      eyebrow="晚自习"
      title="晚自习签到"
    />

    <section class="evening-section">
      <div class="container">
        <div class="evening-toolbar site-system-surface site-reveal">
          <div>
            <strong>{{ overview?.date || date }}</strong>
            <p>
              应到 {{ overview?.targetCount || 0 }} 人，已签 {{ overview?.signedCount || 0 }} 人，
              迟到 {{ overview?.lateCount || 0 }} 人，旷课 {{ overview?.absentCount || 0 }} 人，
              请假剔除 {{ overview?.excusedCount || 0 }} 人。
            </p>
          </div>
          <div class="evening-toolbar__actions">
            <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 150px"
            />
            <el-button :icon="Refresh" type="primary" @click="fetchToday">刷新</el-button>
          </div>
        </div>

        <el-empty
          v-if="!sessions.length && !loading"
          class="site-system-surface site-reveal"
          description="今天暂未生成晚自习签到"
        />

        <div v-else class="evening-grid">
          <article v-for="item in sessions" :key="item.id" class="evening-session site-system-surface site-reveal">
            <div class="session-head">
              <div>
                <span>{{ item.groupName || '实验室分组' }}</span>
                <h2>{{ item.title }}</h2>
              </div>
              <el-tag :type="statusType(item.status)">{{ statusText(item.status) }}</el-tag>
            </div>

            <div class="session-meta">
              <span>{{ formatRange(item.startAt, item.endAt) }}</span>
              <span>签到截止 {{ formatDateTime(item.checkinDeadlineAt) || '-' }}</span>
              <span>{{ item.location || '实验室' }}</span>
              <span v-if="item.checkinPoints">+{{ item.checkinPoints }} 积分</span>
              <span>迟到 -{{ item.latePenaltyPoints ?? 1 }}</span>
              <span>旷课 -{{ item.absentPenaltyPoints ?? 2 }}</span>
            </div>

            <div class="session-stats">
              <article>
                <span>应到</span>
                <strong>{{ item.targetCount || 0 }}</strong>
              </article>
              <article>
                <span>已签到</span>
                <strong>{{ item.signedCount || 0 }}</strong>
              </article>
              <article>
                <span>迟到</span>
                <strong>{{ item.lateCount || 0 }}</strong>
              </article>
              <article>
                <span>旷课</span>
                <strong>{{ item.absentCount || 0 }}</strong>
              </article>
              <article>
                <span>请假剔除</span>
                <strong>{{ item.excusedCount || 0 }}</strong>
              </article>
            </div>

            <el-button
              :disabled="item.status !== 'open'"
              type="primary"
              @click="goScan(item)"
            >
              去签到
            </el-button>
          </article>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import {checkInApi} from '@/api'
import {formatDateTime, statusType} from '@/utils/format.ts'
import {Refresh} from '@element-plus/icons-vue'
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()
const loading = ref(false)
const overview = ref<any>(null)
const date = ref(todayString())

const sessions = computed(() => overview.value?.sessions || [])

function statusText(status: any) {
  return { draft: '草稿', open: '进行中', closed: '已关闭' }[status] || status || '-'
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '不限'} - ${formatDateTime(endAt) || '不限'}`
}

function todayString() {
  const value = new Date()
  const offsetMs = value.getTimezoneOffset() * 60 * 1000
  return new Date(value.getTime() - offsetMs).toISOString().slice(0, 10)
}

async function fetchToday() {
  loading.value = true

  try {
    overview.value = await checkInApi.siteEveningStudyToday({ date: date.value || todayString() })
  } finally {
    loading.value = false
  }
}

function goScan(item: any) {
  if (!item?.qrPayload) return
  router.push({ path: '/check-in/scan', query: { t: item.qrPayload } })
}

onMounted(fetchToday)
</script>

<style scoped>
.evening-section {
  padding: 36px 0 72px;
}

.evening-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px 20px;
}

.evening-toolbar strong {
  color: var(--oa-text);
  font-size: 20px;
}

.evening-toolbar p {
  margin: 6px 0 0;
  color: var(--oa-muted);
}

.evening-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.evening-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 18px;
}

.evening-session {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.session-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.session-head span {
  color: var(--oa-muted);
  font-size: 13px;
}

.session-head h2 {
  margin: 6px 0 0;
  color: var(--oa-text);
  font-size: 22px;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.session-meta span {
  padding: 5px 8px;
  border-radius: 6px;
  background: var(--oa-page-soft-bg);
}

.session-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.session-stats article {
  padding: 12px;
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
}

.session-stats span {
  display: block;
  color: var(--oa-muted);
  font-size: 12px;
}

.session-stats strong {
  display: block;
  margin-top: 4px;
  color: var(--oa-text);
  font-size: 24px;
}

@media (max-width: 760px) {
  .evening-toolbar,
  .evening-toolbar__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .evening-toolbar__actions .el-date-editor {
    width: 100% !important;
  }

  .session-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
