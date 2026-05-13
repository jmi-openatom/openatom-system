<template>
  <div class="admin-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-select v-model="query.status" clearable placeholder="签到状态" style="width: 150px" @change="fetchList">
          <el-option label="草稿" value="draft" />
          <el-option label="进行中" value="open" />
          <el-option label="已关闭" value="closed" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog">发布签到</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column label="签到" min-width="220">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted-line">{{ row.activityTitle || '独立签到' }} / {{ row.location || '未设置地点' }}</p>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="230">
        <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
      </el-table-column>
      <el-table-column label="进度" width="150">
        <template #default="{ row }">{{ row.checkedCount || 0 }} / {{ row.targetCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">全屏预览</el-button>
          <el-button link type="success" @click="openRecords(row)">记录</el-button>
          <el-button v-if="row.status === 'open'" link type="danger" @click="closeSession(row)">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="暂无签到" />

    <el-dialog v-model="dialogVisible" title="发布内部签到" width="920px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="签到标题" prop="title">
            <el-input v-model="form.title" placeholder="如：五月社团例会签到" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="立即开放" value="open" />
              <el-option label="草稿" value="draft" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input v-model="form.startAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input v-model="form.endAt" type="datetime-local" />
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="form.location" placeholder="签到地点，可选" />
          </el-form-item>
          <el-form-item label="关联活动">
            <el-select v-model="form.activityId" clearable filterable placeholder="可选">
              <el-option v-for="item in activities" :key="item.id" :label="item.title" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-divider content-position="left">发放人员</el-divider>
        <div class="member-toolbar">
          <el-input v-model="memberKeyword" clearable placeholder="搜索成员姓名/学号" style="width: 260px" />
          <el-button @click="selectAllMembers">全选当前成员</el-button>
          <el-button @click="form.targetUserIds = []">清空</el-button>
          <span class="muted-line">已选择 {{ form.targetUserIds.length }} 人</span>
        </div>
        <el-table :data="filteredMembers" height="300" @selection-change="handleMemberSelection" ref="memberTableRef">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="realName" label="姓名" min-width="140" />
          <el-table-column prop="userName" label="用户名" min-width="140" />
          <el-table-column prop="status" label="成员状态" width="110" />
          <el-table-column prop="departmentName" label="部门" min-width="130" />
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" fullscreen class="checkin-preview">
      <div v-if="previewRow" class="preview-screen">
        <div class="preview-top">
          <el-button size="large" @click="previewVisible = false">退出全屏</el-button>
        </div>
        <div class="preview-content">
          <section class="preview-qr">
            <p class="preview-kicker">微信扫码网页签到</p>
            <h1>{{ previewRow.title }}</h1>
            <p class="preview-meta">{{ formatRange(previewRow.startAt, previewRow.endAt) }} · {{ previewRow.location || '现场签到' }}</p>
            <img class="qr-image" :src="qrUrl(checkInUrl(previewRow.qrPayload))" alt="签到二维码" />
            <p class="preview-count">已签到 {{ checkedRecords.length }} / {{ previewRecords.length || previewRow.targetCount || 0 }}</p>
          </section>
          <section class="preview-roster">
            <div class="roster-column roster-column--pending">
              <div class="roster-head">
                <span>未签到</span>
                <strong>{{ pendingRecords.length }}</strong>
              </div>
              <div class="roster-list">
                <div v-for="item in pendingRecords" :key="`pending-${item.userId}`" class="roster-item">
                  {{ displayRecordName(item) }}
                </div>
                <el-empty v-if="!pendingRecords.length" :image-size="72" description="已全部签到" />
              </div>
            </div>
            <div class="roster-column roster-column--checked">
              <div class="roster-head">
                <span>已签到</span>
                <strong>{{ checkedRecords.length }}</strong>
              </div>
              <div class="roster-list">
                <div v-for="item in checkedRecords" :key="`checked-${item.userId}`" class="roster-item">
                  {{ displayRecordName(item) }}
                </div>
                <el-empty v-if="!checkedRecords.length" :image-size="72" description="暂无签到" />
              </div>
            </div>
          </section>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="recordsVisible" title="签到记录" width="860px">
      <el-table :data="records">
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'checked' ? 'success' : 'info'">{{ row.status === 'checked' ? '已签到' : '未签到' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.checkinAt) || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { activityApi, checkInApi, membershipApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { qrSvgDataUrl } from '@/utils/qr.ts'

export default {
  name: 'AdminCheckIns',
  data() {
    return {
      Plus,
      Refresh,
      loading: false,
      saving: false,
      dialogVisible: false,
      previewVisible: false,
      recordsVisible: false,
      rows: [],
      records: [],
      previewRecords: [],
      activities: [],
      members: [],
      liveTimer: null,
      memberKeyword: '',
      previewRow: null,
      query: { status: '' },
      form: { title: '', status: 'open', startAt: '', endAt: '', location: '', activityId: undefined, targetUserIds: [] },
      rules: { title: [{ required: true, message: '请输入签到标题', trigger: 'blur' }] },
    }
  },
  computed: {
    filteredMembers() {
      const keyword = this.memberKeyword.trim()
      if (!keyword) return this.members
      return this.members.filter((item) =>
        [item.realName, item.userName, item.studentId, item.departmentName].some((value) => String(value || '').includes(keyword)),
      )
    },
    checkedRecords() {
      return this.previewRecords.filter((item) => item.status === 'checked')
    },
    pendingRecords() {
      return this.previewRecords.filter((item) => item.status !== 'checked')
    },
  },
  created() {
    this.fetchList()
    this.loadOptions()
  },
  beforeUnmount() {
    this.stopLiveRefresh()
  },
  methods: {
    formatDateTime,
    statusType,
    statusText(status) {
      return { draft: '草稿', open: '进行中', closed: '已关闭' }[status] || status || '-'
    },
    formatRange(startAt, endAt) {
      return `${formatDateTime(startAt) || '不限'} - ${formatDateTime(endAt) || '不限'}`
    },
    async fetchList() {
      this.loading = true
      try {
        this.rows = (await checkInApi.list(this.query)) || []
      } finally {
        this.loading = false
      }
    },
    async loadOptions() {
      const [activities, members] = await Promise.all([
        activityApi.list({ status: 'published' }),
        membershipApi.list({ status: 'active' }),
      ])
      this.activities = activities || []
      this.members = (members || []).filter((item) => item.userId).map((item) => ({ ...item, id: item.userId }))
    },
    openDialog() {
      this.form = { title: '', status: 'open', startAt: '', endAt: '', location: '', activityId: undefined, targetUserIds: [] }
      this.memberKeyword = ''
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.memberTableRef?.clearSelection())
    },
    handleMemberSelection(selection) {
      this.form.targetUserIds = selection.map((item) => item.userId)
    },
    selectAllMembers() {
      this.$refs.memberTableRef?.clearSelection()
      this.filteredMembers.forEach((row) => this.$refs.memberTableRef?.toggleRowSelection(row, true))
    },
    save() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        if (!this.form.targetUserIds.length) {
          ElMessage.error('请选择发放人员')
          return
        }
        this.saving = true
        try {
          await checkInApi.create({ ...this.form, activityId: this.form.activityId || undefined })
          ElMessage.success('签到已发布')
          this.dialogVisible = false
          this.fetchList()
        } finally {
          this.saving = false
        }
      })
    },
    openPreview(row) {
      this.previewRow = row
      this.previewRecords = []
      this.previewVisible = true
      this.startLiveRefresh(row.id)
    },
    qrUrl(payload) {
      return qrSvgDataUrl(payload || '')
    },
    checkInUrl(payload) {
      const token = encodeURIComponent(payload || '')
      return `${window.location.origin}/check-in/scan?t=${token}`
    },
    async openRecords(row) {
      this.records = await checkInApi.records(row.id)
      this.recordsVisible = true
      this.startLiveRefresh(row.id)
    },
    displayRecordName(row) {
      return row.realName || row.userName || row.studentId || `用户 ${row.userId || '-'}`
    },
    async closeSession(row) {
      await checkInApi.close(row.id)
      ElMessage.success('签到已关闭')
      this.fetchList()
    },
    startLiveRefresh(sessionId) {
      this.stopLiveRefresh()
      this.liveTimer = window.setInterval(async () => {
        if (!this.previewVisible && !this.recordsVisible) {
          this.stopLiveRefresh()
          return
        }
        const [detail, records] = await Promise.all([
          this.previewVisible ? checkInApi.detail(sessionId) : Promise.resolve(null),
          this.previewVisible || this.recordsVisible ? checkInApi.records(sessionId) : Promise.resolve(null),
        ])
        if (detail) this.previewRow = detail
        if (records && this.previewVisible) this.previewRecords = records
        if (records && this.recordsVisible) this.records = records
      }, 3000)
    },
    stopLiveRefresh() {
      if (this.liveTimer) {
        window.clearInterval(this.liveTimer)
        this.liveTimer = null
      }
    },
  },
}
</script>

<style scoped>
.muted-line {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.member-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.preview-screen {
  min-height: 100vh;
  color: #0f172a;
  background: radial-gradient(circle at top, #e0f2fe 0, #f8fafc 42%, #ffffff 100%);
}

.preview-top {
  padding: 24px;
  text-align: right;
}

.preview-content {
  display: grid;
  grid-template-columns: minmax(420px, 0.92fr) minmax(520px, 1.08fr);
  align-items: stretch;
  gap: 16px;
  min-height: calc(100vh - 96px);
  padding: 2vh 32px 5vh;
}

.preview-qr {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 16px;
  text-align: center;
}

.preview-roster {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: 0;
}

.roster-column {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 50px rgba(15, 23, 42, .08);
}

.roster-column--pending {
  border-color: rgba(245, 158, 11, .36);
}

.roster-column--checked {
  border-color: rgba(22, 163, 74, .28);
}

.roster-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid #e2e8f0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.roster-head strong {
  display: grid;
  min-width: 46px;
  height: 38px;
  place-items: center;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 22px;
}

.roster-column--pending .roster-head strong {
  background: #fffbeb;
  color: #d97706;
}

.roster-column--checked .roster-head strong {
  background: #dcfce7;
  color: #16a34a;
}

.roster-list {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}

.roster-item {
  overflow: hidden;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-kicker {
  color: #2563eb;
  font-size: 22px;
  font-weight: 800;
}

.preview-content h1 {
  margin: 0;
  font-size: clamp(42px, 7vw, 86px);
}

.preview-meta,
.preview-count {
  margin: 0;
  color: #475569;
  font-size: 20px;
}

.qr-image {
  width: min(48vh, 420px);
  height: min(48vh, 420px);
  padding: 18px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, .14);
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .member-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .member-toolbar .el-input {
    width: 100% !important;
  }

  .preview-top {
    padding: 14px;
  }

  .preview-content {
    grid-template-columns: 1fr;
    padding: 3vh 16px 6vh;
  }

  .preview-roster {
    grid-template-columns: 1fr;
  }

  .preview-kicker,
  .preview-meta,
  .preview-count {
    font-size: 15px;
  }

  .qr-image {
    width: min(82vw, 360px);
    height: min(82vw, 360px);
  }

  .roster-head,
  .roster-item {
    font-size: 16px;
  }
}
</style>
