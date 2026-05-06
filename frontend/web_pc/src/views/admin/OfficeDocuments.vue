<template>
  <div class="admin-page office-documents-page">
    <div class="toolbar">
      <div class="toolbar__filters">
        <el-select
          v-model="query.docType"
          clearable
          placeholder="单据类型"
          style="width: 180px"
          @change="fetchList"
        >
          <el-option label="假条" value="leave_note" />
          <el-option label="场地申请表" value="venue_application" />
        </el-select>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索标题"
          style="width: 240px"
          @keyup.enter="fetchList"
        />
        <el-button type="primary" @click="fetchList">查询</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button @click="openCreate('leave_note')">新增假条</el-button>
        <el-button type="primary" @click="openCreate('venue_application')">新增场地申请</el-button>
      </div>
    </div>

    <div class="office-layout">
      <el-card shadow="never" class="office-list-card">
        <template #header>
          <div class="card-header">
            <span>单据列表</span>
            <el-tag type="info">{{ rows.length }}</el-tag>
          </div>
        </template>

        <el-empty v-if="!rows.length && !loading" description="暂无单据" />
        <div v-else v-loading="loading" class="office-list">
          <div
            v-for="item in rows"
            :key="item.id"
            :class="['office-list-item', currentId === item.id ? 'is-active' : '']"
            @click="editRow(item)"
          >
            <div class="office-list-item__title">{{ item.title }}</div>
            <div class="office-list-item__meta">
              <span>{{ typeText(item.docType) }}</span>
              <span>{{ formatDateTime(item.createdAt) }}</span>
            </div>
            <div class="office-list-item__actions">
              <el-button link type="primary" @click.stop="editRow(item)">编辑</el-button>
              <el-button link type="success" @click.stop="exportRow(item)">导出 Word</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="office-form-card">
        <template #header>
          <div class="card-header">
            <span>{{ form.id ? '编辑单据' : '新建单据' }}</span>
            <el-tag :type="form.docType === 'leave_note' ? 'warning' : 'success'">{{
              typeText(form.docType)
            }}</el-tag>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="office-form">
          <el-form-item label="单据类型" prop="docType">
            <el-radio-group v-model="form.docType" @change="handleDocTypeChange">
              <el-radio-button label="leave_note">假条</el-radio-button>
              <el-radio-button label="venue_application">场地申请表</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="单据标题" prop="title">
            <el-input v-model="form.title" placeholder="例如：开放原子社团 E206 培训假条" />
          </el-form-item>

          <el-divider content-position="left">人员选择</el-divider>
          <el-form-item label="选择人员" prop="memberIds">
            <el-select
              v-model="form.memberIds"
              multiple
              filterable
              remote
              reserve-keyword
              placeholder="输入姓名、学号、专业搜索"
              style="width: 100%"
              :remote-method="searchUsers"
              :loading="userLoading"
            >
              <el-option
                v-for="item in userOptions"
                :key="item.id"
                :label="userLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <template v-if="form.docType === 'leave_note'">
            <el-divider content-position="left">假条信息</el-divider>
            <el-form-item label="称呼">
              <el-input v-model="form.salutation" />
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.leaveStartDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.leaveEndDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="活动时间">
              <el-input v-model="form.timeRange" placeholder="例如：18:30 - 20:00" />
            </el-form-item>
            <el-form-item label="活动地点">
              <el-input v-model="form.venueName" />
            </el-form-item>
            <el-form-item label="请假事由" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="5" />
            </el-form-item>
            <el-form-item label="落款部门">
              <el-input v-model="form.departmentName" />
            </el-form-item>
            <el-form-item label="单据日期">
              <el-date-picker v-model="form.documentDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </template>

          <template v-else>
            <el-divider content-position="left">场地申请信息</el-divider>
            <el-form-item label="表单标题">
              <el-input v-model="form.formTitle" />
            </el-form-item>
            <el-form-item label="申请方式">
              <el-radio-group v-model="form.applyMode">
                <el-radio label="personal">个人</el-radio>
                <el-radio label="team">团队</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="场地名称" prop="venueName">
              <el-input v-model="form.venueName" />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
            <el-form-item label="指导老师">
              <el-input v-model="form.teacherName" />
            </el-form-item>
            <el-form-item label="老师电话">
              <el-input v-model="form.teacherPhone" />
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="总时长">
              <el-input v-model="form.totalHours" placeholder="例如：80" />
            </el-form-item>
            <el-form-item label="每日时段">
              <el-input v-model="form.dailySchedule" placeholder="例如：每天 18:30 - 21:00" />
            </el-form-item>
            <el-form-item label="项目类型">
              <el-checkbox-group v-model="form.projectTypes">
                <el-checkbox label="职业技能强化" />
                <el-checkbox label="创新竞赛实训" />
                <el-checkbox label="跨专业人才培养" />
                <el-checkbox label="毕业设计（论文）" />
                <el-checkbox label="实习实践" />
                <el-checkbox label="其他" />
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="项目说明">
              <el-input v-model="form.projectDescription" type="textarea" :rows="5" />
            </el-form-item>
            <el-form-item label="单据日期">
              <el-date-picker v-model="form.documentDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </template>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            <el-button
              v-if="form.id"
              type="success"
              :loading="exportingId === form.id"
              @click="exportRow(form)"
              >导出 Word
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { officeDocumentApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

function createLeaveForm() {
  return {
    id: null,
    docType: 'leave_note',
    title: '',
    memberIds: [],
    salutation: '尊敬的辅导员及任课老师：',
    leaveStartDate: '',
    leaveEndDate: '',
    timeRange: '18:30 - 20:00',
    venueName: '至德楼 E206',
    reason: '',
    departmentName: '信息工程学院',
    documentDate: '',
  }
}

function createVenueForm() {
  return {
    id: null,
    docType: 'venue_application',
    title: '',
    memberIds: [],
    formTitle: '江苏海事职业技术学院实践场所预约申请表',
    applyMode: 'team',
    venueName: '',
    contactName: '',
    contactPhone: '',
    teacherName: '',
    teacherPhone: '',
    startDate: '',
    endDate: '',
    totalHours: '',
    dailySchedule: '',
    projectTypes: [],
    projectDescription: '',
    documentDate: '',
  }
}

export default {
  name: 'AdminOfficeDocuments',
  data() {
    return {
      loading: false,
      saving: false,
      userLoading: false,
      exportingId: null,
      rows: [],
      currentId: null,
      userOptions: [],
      query: {
        docType: '',
        keyword: '',
      },
      form: createLeaveForm(),
      rules: {
        docType: [{ required: true, message: '请选择单据类型', trigger: 'change' }],
        title: [{ required: true, message: '请输入单据标题', trigger: 'blur' }],
        memberIds: [{ required: true, message: '请至少选择一名人员', trigger: 'change' }],
        reason: [{ required: true, message: '请填写请假事由', trigger: 'blur' }],
        venueName: [{ required: true, message: '请填写场地名称', trigger: 'blur' }],
        contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
      },
    }
  },
  created() {
    this.fetchList()
    this.searchUsers('')
  },
  methods: {
    formatDateTime,
    typeText(type) {
      return (
        {
          leave_note: '假条',
          venue_application: '场地申请表',
        }[type] ||
        type ||
        '-'
      )
    },
    userLabel(user) {
      return `${user.realName || '未命名'} / ${user.studentId || '-'} / ${user.major || user.college || '-'}`
    },
    async fetchList() {
      this.loading = true
      try {
        this.rows = (await officeDocumentApi.list(this.query)) || []
      } finally {
        this.loading = false
      }
    },
    async searchUsers(keyword) {
      this.userLoading = true
      try {
        this.userOptions = (await officeDocumentApi.userOptions({ keyword })) || []
      } finally {
        this.userLoading = false
      }
    },
    openCreate(type) {
      this.currentId = null
      this.form = type === 'venue_application' ? createVenueForm() : createLeaveForm()
      this.form.documentDate = this.today()
    },
    handleDocTypeChange(type) {
      const currentTitle = this.form.title
      const currentMembers = [...(this.form.memberIds || [])]
      this.form = type === 'venue_application' ? createVenueForm() : createLeaveForm()
      this.form.title = currentTitle
      this.form.memberIds = currentMembers
      this.form.documentDate = this.today()
    },
    editRow(row) {
      this.currentId = row.id
      const payload = this.parsePayload(row.payload)
      this.form = {
        id: row.id,
        docType: row.docType,
        title: row.title,
        ...payload,
      }
      if (!Array.isArray(this.form.memberIds)) {
        this.form.memberIds = []
      }
      if (!this.form.documentDate) {
        this.form.documentDate = this.today()
      }
    },
    parsePayload(payload) {
      if (!payload) return {}
      if (typeof payload === 'object') return payload
      try {
        return JSON.parse(payload)
      } catch {
        return {}
      }
    },
    buildPayload() {
      if (this.form.docType === 'leave_note') {
        return {
          memberIds: this.form.memberIds,
          salutation: this.form.salutation,
          leaveStartDate: this.form.leaveStartDate,
          leaveEndDate: this.form.leaveEndDate,
          timeRange: this.form.timeRange,
          venueName: this.form.venueName,
          reason: this.form.reason,
          departmentName: this.form.departmentName,
          documentDate: this.form.documentDate,
        }
      }
      return {
        memberIds: this.form.memberIds,
        formTitle: this.form.formTitle,
        applyMode: this.form.applyMode,
        venueName: this.form.venueName,
        contactName: this.form.contactName,
        contactPhone: this.form.contactPhone,
        teacherName: this.form.teacherName,
        teacherPhone: this.form.teacherPhone,
        startDate: this.form.startDate,
        endDate: this.form.endDate,
        totalHours: this.form.totalHours,
        dailySchedule: this.form.dailySchedule,
        projectTypes: this.form.projectTypes,
        projectDescription: this.form.projectDescription,
        documentDate: this.form.documentDate,
      }
    },
    save() {
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          const payload = {
            docType: this.form.docType,
            title: this.form.title,
            payload: this.buildPayload(),
          }
          if (this.form.id) {
            await officeDocumentApi.update(this.form.id, payload)
            ElMessage.success('单据已更新')
          } else {
            const id = await officeDocumentApi.create(payload)
            this.form.id = id
            this.currentId = id
            ElMessage.success('单据已创建')
          }
          await this.fetchList()
        } finally {
          this.saving = false
        }
      })
    },
    async exportRow(row) {
      const id = row.id || this.form.id
      if (!id) return
      this.exportingId = id
      try {
        const blob = await officeDocumentApi.export(id)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${row.title || this.form.title || '文书'}.docx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        ElMessage.success('Word 已开始下载')
      } finally {
        this.exportingId = null
      }
    },
    resetForm() {
      this.openCreate(this.form.docType)
    },
    today() {
      return new Date().toISOString().slice(0, 10)
    },
  },
}
</script>

<style scoped>
.office-documents-page {
  padding: 20px;
}

.toolbar__actions {
  display: flex;
  gap: 12px;
}

.office-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
}

.office-list-card,
.office-form-card {
  min-height: 700px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.office-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.office-list-item {
  padding: 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.office-list-item.is-active {
  border-color: #2563eb;
  background: #eff6ff;
}

.office-list-item__title {
  font-weight: 700;
  color: #0f172a;
}

.office-list-item__meta {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--oa-muted);
}

.office-list-item__actions {
  margin-top: 8px;
}

.office-form :deep(.el-date-editor),
.office-form :deep(.el-select) {
  width: 100%;
}

@media (max-width: 1100px) {
  .office-layout {
    grid-template-columns: 1fr;
  }
}
</style>
