<template>
  <div class="admin-page school-calendar-page">
    <section class="setting-panel">
      <div class="panel-head">
        <div>
          <h2>校历设置</h2>
          <p>设置开学日期和周数后，系统会自动生成周六、周日休息日。</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="calendar-form">
        <el-form-item label="开学日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开学日期" />
        </el-form-item>
        <el-form-item label="学期周数" prop="weekCount">
          <el-input-number v-model="form.weekCount" :min="1" :max="60" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveCalendar">保存并生成校历</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="setting-panel">
      <div class="panel-head">
        <div>
          <h2>调休设置</h2>
          <p>把某一天覆盖为上课日或休息日，优先级高于自动生成的周末规则。</p>
        </div>
      </div>

      <el-form class="adjustment-form" label-width="92px">
        <el-form-item label="日期">
          <el-date-picker v-model="adjustment.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="类型">
          <el-segmented v-model="adjustment.type" :options="adjustmentOptions" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="adjustment.reason" placeholder="如：国庆调休上课" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveAdjustment">保存调休</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="calendar-preview">
      <div class="panel-head">
        <div>
          <h2>校历预览</h2>
          <p v-if="calendar.startDate">{{ calendar.startDate }} 至 {{ calendar.endDate }}，共 {{ calendar.weekCount }} 周</p>
          <p v-else>尚未设置校历。</p>
        </div>
        <el-button :icon="Refresh" @click="fetchCalendar">刷新</el-button>
      </div>

      <el-empty v-if="!calendar.days?.length" description="暂无校历" />
      <div v-for="week in weekGroups" :key="week.weekIndex" class="week-row">
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
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { schoolCalendarApi } from '@/api'

export default {
  name: 'AdminSchoolCalendar',
  data() {
    return {
      Refresh,
      saving: false,
      calendar: { days: [] },
      form: { startDate: '', weekCount: 20 },
      adjustment: { date: '', type: 'workday', reason: '' },
      adjustmentOptions: [
        { label: '设为上课日', value: 'workday' },
        { label: '设为休息日', value: 'restday' },
      ],
      rules: {
        startDate: [{ required: true, message: '请选择开学日期', trigger: 'change' }],
        weekCount: [{ required: true, message: '请输入周数', trigger: 'change' }],
      },
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
  created() {
    this.fetchCalendar()
  },
  methods: {
    async fetchCalendar() {
      this.calendar = (await schoolCalendarApi.detail()) || { days: [] }
      this.form.startDate = this.calendar.startDate || ''
      this.form.weekCount = this.calendar.weekCount || 20
    },
    saveCalendar() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          this.calendar = await schoolCalendarApi.save(this.form)
          ElMessage.success('校历已生成')
        } finally {
          this.saving = false
        }
      })
    },
    async saveAdjustment() {
      if (!this.adjustment.date) {
        ElMessage.error('请选择调休日期')
        return
      }
      this.calendar = await schoolCalendarApi.saveAdjustment(this.adjustment)
      ElMessage.success('调休已保存')
      this.adjustment = { date: '', type: 'workday', reason: '' }
    },
  },
}
</script>

<style scoped>
.school-calendar-page {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
  background: #e0e0e0;
}

.setting-panel,
.calendar-preview {
  padding: 24px;
  border: 0;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
  animation: oaFadeUp 0.42s ease both;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.panel-head p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.6;
}

.calendar-form,
.adjustment-form {
  max-width: 720px;
}

.week-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  margin-top: 12px;
}

.week-title {
  display: grid;
  place-items: center;
  color: #7a7a7a;
  border-radius: 8px;
  background: #f5f5f7;
  font-weight: 600;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.day-cell {
  min-height: 92px;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #ffffff;
}

.day-cell strong,
.day-cell span,
.day-cell small,
.day-cell em {
  display: block;
}

.day-cell span {
  margin-top: 6px;
  color: #1d1d1f;
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

@media (max-width: 920px) {
  .week-row {
    grid-template-columns: 1fr;
  }

  .week-title {
    justify-content: flex-start;
    padding: 10px 12px;
    place-items: center start;
  }

  .day-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
