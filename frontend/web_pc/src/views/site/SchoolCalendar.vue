<template>
  <ViewPage class="site-system-page calendar-page">
    <SitePageHero
      eyebrow="校历"
      title="学期校历"
      :description="
        calendar.startDate
          ? `${calendar.startDate} 至 ${calendar.endDate}，共 ${calendar.weekCount} 周。`
          : '校历暂未发布。'
      "
      compact
    />

    <section class="site-system-section">
      <div class="container calendar-shell">
        <el-empty v-if="!calendar.days?.length" class="site-system-empty" description="暂无校历" />
        <div
          v-for="week in weekGroups"
          :key="week.weekIndex"
          class="week-card site-system-surface site-reveal"
        >
          <div class="week-title">第 {{ week.weekIndex }} 周</div>
          <div class="day-grid">
            <div
              v-for="day in week.days"
              :key="day.date"
              class="day-cell"
              :class="{
                'day-cell--rest': day.restDay,
                'day-cell--adjusted': day.source === 'adjustment',
              }"
            >
              <strong>{{ day.dayName }}</strong>
              <span>{{ day.date.slice(5) }}</span>
              <small>{{ day.restDay ? '休息' : '上课' }}</small>
              <em v-if="day.reason">{{ day.reason }}</em>
            </div>
          </div>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import { schoolCalendarApi } from '@/api'
import { computed, onMounted, ref } from 'vue'

const calendar = ref({ days: [] })

const weekGroups = computed(() => {
  const groups = new Map()
  ;(calendar.value.days || []).forEach((day) => {
    if (!groups.has(day.weekIndex))
      groups.set(day.weekIndex, { weekIndex: day.weekIndex, days: [] })
    groups.get(day.weekIndex).days.push(day)
  })
  return Array.from(groups.values())
})

onMounted(async () => {
  calendar.value = (await schoolCalendarApi.siteDetail()) || { days: [] }
})
</script>

<style scoped>
.calendar-shell {
  display: grid;
  gap: 20px;
}

.week-card {
  padding: 24px;
}

.week-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 600;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.day-cell {
  min-height: 92px;
  padding: 12px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-elevated-bg);
}

.day-cell strong,
.day-cell span,
.day-cell small,
.day-cell em {
  display: block;
}

.day-cell span {
  margin-top: 6px;
  font-weight: 600;
}

.day-cell small {
  margin-top: 4px;
  color: var(--oa-text);
}

.day-cell em {
  margin-top: 6px;
  color: var(--oa-muted);
  font-style: normal;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.day-cell--rest {
  background: var(--oa-page-soft-bg);
}

.day-cell--rest small {
  color: var(--oa-text);
}

.day-cell--adjusted {
  border-color: var(--oa-text);
  box-shadow: none;
}

@media (max-width: 760px) {
  .day-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
