<template>
  <div class="calendar-page">
    <section class="container calendar-shell">
      <div class="section-head">
        <el-tag effect="plain">校历</el-tag>
        <h1>学期校历</h1>
        <p v-if="calendar.startDate">{{ calendar.startDate }} 至 {{ calendar.endDate }}，共 {{ calendar.weekCount }} 周。</p>
        <p v-else>校历暂未发布。</p>
      </div>

      <el-empty v-if="!calendar.days?.length" description="暂无校历" />
      <div v-for="week in weekGroups" :key="week.weekIndex" class="week-card">
        <div class="week-title">第 {{ week.weekIndex }} 周</div>
        <div class="day-grid">
          <div
            v-for="day in week.days"
            :key="day.date"
            class="day-cell"
            :class="{ 'day-cell--rest': day.restDay, 'day-cell--adjusted': day.source === 'adjustment' }"
          >
            <strong>{{ day.dayName }}</strong>
            <span>{{ day.date.slice(5) }}</span>
            <small>{{ day.restDay ? '休息' : '上课' }}</small>
            <em v-if="day.reason">{{ day.reason }}</em>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { schoolCalendarApi } from '@/api'

export default {
  name: 'SiteSchoolCalendar',
  data() {
    return {
      calendar: { days: [] },
    }
  },
  computed: {
    weekGroups() {
      const groups = new Map()
      ;(this.calendar.days || []).forEach((day) => {
        if (!groups.has(day.weekIndex)) groups.set(day.weekIndex, { weekIndex: day.weekIndex, days: [] })
        groups.get(day.weekIndex).days.push(day)
      })
      return Array.from(groups.values())
    },
  },
  async created() {
    this.calendar = (await schoolCalendarApi.siteDetail()) || { days: [] }
  },
}
</script>

<style scoped>
.calendar-page {
  padding: 64px 0 80px;
  background: #f5f5f7;
}

.calendar-shell {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.section-head {
  padding: 48px;
  border: 0;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
  text-align: center;
}

.section-head h1 {
  margin: 14px 0 8px;
  font-family: 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 48px;
  font-weight: 600;
  line-height: 1.1;
}

.section-head p {
  margin: 0;
  color: var(--oa-muted);
}

.week-card {
  padding: 24px;
  border: 0;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
  animation: oaFadeUp 0.42s ease both;
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
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
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
  color: #1d1d1f;
}

.day-cell em {
  margin-top: 6px;
  color: var(--oa-muted);
  font-style: normal;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.day-cell--rest {
  background: #f5f5f7;
}

.day-cell--rest small {
  color: #1d1d1f;
}

.day-cell--adjusted {
  border-color: #1d1d1f;
  box-shadow: none;
}

@media (max-width: 760px) {
  .calendar-page {
    padding: 18px 0 44px;
  }

  .section-head {
    padding: 18px;
  }

  .section-head h1 {
    font-size: 28px;
  }

  .day-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
