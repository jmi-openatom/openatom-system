<template>
  <ViewPage class="admin-page group-editor-page">
    <header class="editor-header">
      <div class="editor-header__main">
        <el-button :icon="ArrowLeft" text @click="goBack">返回分组中心</el-button>
        <div>
          <div class="editor-header__eyebrow">统一分组中心</div>
          <h2>{{ isEdit ? '管理分组' : '新建分组' }}</h2>
          <p>在一个页面完成分组信息维护、成员筛选与批量选择。</p>
        </div>
      </div>
      <el-tag v-if="isEdit" effect="plain">{{ typeLabel }}</el-tag>
    </header>

    <el-skeleton v-if="initialLoading" :rows="8" animated />
    <template v-else>
      <section class="editor-card" aria-labelledby="group-info-title">
        <div class="section-heading">
          <span class="section-heading__index">1</span>
          <div>
            <h3 id="group-info-title">分组信息</h3>
            <p>选择分组所属社团和业务类型，并填写便于识别的名称。</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-grid">
            <el-form-item label="所属社团" prop="clubId">
              <el-select
                v-model="form.clubId"
                filterable
                :disabled="isEdit"
                placeholder="选择社团"
                @change="onClubChange"
              >
                <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="分组类型" prop="type">
              <el-radio-group
                v-model="form.type"
                :disabled="isEdit"
                class="type-selector"
                :class="{ 'type-selector--four': form.type === 'external' }"
              >
                <el-radio-button value="department">组织部门</el-radio-button>
                <el-radio-button value="checkin">签到分组</el-radio-button>
                <el-radio-button value="alumni">往届分组</el-radio-button>
                <el-radio-button v-if="form.type === 'external'" value="external">外部群组</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </div>
          <div class="form-grid form-grid--details">
            <el-form-item label="分组名称" prop="name">
              <el-input
                v-model="form.name"
                :maxlength="nameMaxLength"
                :disabled="form.type === 'external'"
                show-word-limit
                placeholder="如：技术部、晚自习 A 组、2025 届"
              />
            </el-form-item>
            <el-form-item v-if="form.type === 'alumni'" label="显示排序">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
            </el-form-item>
          </div>
          <el-form-item v-if="form.type !== 'checkin'" label="分组描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="说明该分组的职责、用途或适用范围"
            />
          </el-form-item>
        </el-form>

        <el-alert
          v-if="form.type === 'department'"
          type="warning"
          :closable="false"
          show-icon
          title="保存后，所选成员的主部门将调整为当前部门；原岗位会被清空。"
        />
        <el-alert
          v-else-if="form.type === 'checkin'"
          type="info"
          :closable="false"
          show-icon
          title="签到分组至少需要一名成员，后续仍可在本页继续调整。"
        />
      </section>

      <section v-if="form.type === 'external'" class="editor-card external-card" aria-labelledby="external-title">
        <div class="section-heading">
          <span class="section-heading__index">2</span>
          <div>
            <h3 id="external-title">外部群管理</h3>
            <p>外部群成员由平台实时同步，统一分组中心保留管理入口和关联关系。</p>
          </div>
        </div>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="成员禁言、入群审核、群公告和机器人规则属于平台专属能力。"
        />
        <el-button class="external-manage-button" type="primary" @click="goExternalManagement">
          进入机器人高级配置
        </el-button>
      </section>

      <section v-else class="editor-card member-card" aria-labelledby="member-title">
        <div class="section-heading section-heading--members">
          <span class="section-heading__index">2</span>
          <div>
            <h3 id="member-title">选择成员</h3>
            <p>列表按社团成员关系查询，翻页和筛选不会丢失已选成员。</p>
          </div>
          <el-tag type="primary" effect="light" size="large">已选 {{ selectedCount }} 人</el-tag>
        </div>

        <div class="member-filters">
          <el-input
            v-model="filters.keyword"
            clearable
            :prefix-icon="Search"
            placeholder="搜索姓名、用户名、学号或班级"
            @keyup.enter="searchMembers"
            @clear="searchMembers"
          />
          <el-select v-model="filters.status" clearable placeholder="全部成员状态" @change="searchMembers">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select
            v-model="filters.departmentId"
            clearable
            filterable
            placeholder="全部部门"
            @change="searchMembers"
          >
            <el-option v-for="item in departments" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="searchMembers">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>

        <div class="selection-toolbar">
          <span>表头复选框可全选当前页</span>
          <div>
            <el-button :loading="selectAllLoading" @click="selectAllFiltered">
              全选筛选结果（{{ memberTotal }} 人）
            </el-button>
            <el-button :disabled="!selectedCount" @click="clearSelection">清空已选</el-button>
          </div>
        </div>

        <el-table
          ref="memberTableRef"
          v-loading="membersLoading"
          :data="members"
          row-key="id"
          class="admin-table member-table"
          @selection-change="onSelectionChange"
        >
          <el-table-column type="selection" width="52" :reserve-selection="true" />
          <el-table-column label="成员" min-width="190">
            <template #default="{ row }">
              <div class="member-name">
                <el-avatar :size="34" :src="row.avatar">{{ displayName(row).charAt(0) }}</el-avatar>
                <div><strong>{{ displayName(row) }}</strong><small>@{{ row.userName || '-' }}</small></div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="studentId" label="学号" min-width="130">
            <template #default="{ row }">{{ row.studentId || '-' }}</template>
          </el-table-column>
          <el-table-column prop="className" label="班级" min-width="140">
            <template #default="{ row }">{{ row.className || '-' }}</template>
          </el-table-column>
          <el-table-column prop="departmentName" label="当前部门" min-width="130">
            <template #default="{ row }">{{ row.departmentName || '未分配' }}</template>
          </el-table-column>
          <el-table-column prop="positionName" label="岗位" min-width="120">
            <template #default="{ row }">{{ row.positionName || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!membersLoading && !members.length" description="没有匹配的社团成员" />
        <div v-if="memberTotal" class="pagination-wrap">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="memberTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadMembers"
            @size-change="onPageSizeChange"
          />
        </div>
      </section>

      <footer v-if="form.type !== 'external'" class="editor-actions">
        <div><strong>已选择 {{ selectedCount }} 名成员</strong><span>保存后立即同步到统一分组中心</span></div>
        <div>
          <el-button @click="goBack">取消</el-button>
          <el-button
            type="primary"
            :loading="saving"
            :disabled="isEdit && !canUpdate"
            @click="saveGroup"
          >{{ isEdit ? '保存分组' : '创建分组' }}</el-button>
        </div>
      </footer>
    </template>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { clubApi, departmentApi, unifiedGroupApi } from '@/api'
import { hasPermission } from '@/utils/permission.ts'
import { ArrowLeft, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import type { FormInstance, FormRules, TableInstance } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type GroupType = 'department' | 'checkin' | 'alumni' | 'external'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const memberTableRef = ref<TableInstance>()
const initialLoading = ref(false)
const membersLoading = ref(false)
const selectAllLoading = ref(false)
const saving = ref(false)
const syncingSelection = ref(false)
const clubs = ref<any[]>([])
const departments = ref<any[]>([])
const members = ref<any[]>([])
const memberTotal = ref(0)
const externalLegacyId = ref('')
const selectedIds = ref(new Set<number>())
const groupId = computed(() => (route.params.groupId ? Number(route.params.groupId) : null))
const isEdit = computed(() => Number.isFinite(groupId.value))
const canUpdate = computed(() => hasPermission('group:update'))
const selectedCount = computed(() => selectedIds.value.size)
const nameMaxLength = computed(() => form.type === 'department' ? 64 : form.type === 'checkin' ? 120 : 100)
const typeLabel = computed(() => ({ department: '组织部门', checkin: '签到分组', alumni: '往届分组', external: '外部群组' })[form.type])

const form = reactive({
  clubId: '' as string | number,
  type: 'department' as GroupType,
  name: '',
  description: '',
  sortOrder: 0,
})
const filters = reactive({ keyword: '', status: '', departmentId: '' as string | number })
const pagination = reactive({ page: 1, pageSize: 20 })
const rules: FormRules = {
  clubId: [{ required: true, message: '请选择所属社团', trigger: 'change' }],
  type: [{ required: true, message: '请选择分组类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
}
const statusOptions = [
  { label: '试用成员', value: 'probation' },
  { label: '正式成员', value: 'active' },
  { label: '已暂停', value: 'suspended' },
  { label: '已退出', value: 'left' },
  { label: '已毕业', value: 'graduated' },
]

function normalizeList(result: any) {
  return result?.list || result || []
}

function displayName(row: any) {
  return row.realName || row.userName || `用户 ${row.id}`
}

function statusText(status: string) {
  return statusOptions.find((item) => item.value === status)?.label || status || '未知'
}

function statusTagType(status: string) {
  if (status === 'active') return 'success'
  if (status === 'probation') return 'warning'
  if (status === 'left' || status === 'graduated') return 'info'
  return 'danger'
}

async function loadClubs() {
  clubs.value = normalizeList(await clubApi.list({ page: 1, pageSize: 100 }))
  if (!form.clubId && clubs.value.length) {
    const requestedId = Number(route.query.clubId)
    const defaultClub = clubs.value.find((item) => item.id === requestedId)
      || clubs.value.find((item) => item.code === 'JMI-OPENATOM')
      || clubs.value[0]
    form.clubId = defaultClub.id
  }
}

async function loadGroup() {
  if (!groupId.value) return
  const detail = await unifiedGroupApi.detail(groupId.value)
  if (!['department', 'checkin', 'alumni', 'external'].includes(detail.type)) return
  Object.assign(form, {
    clubId: detail.clubId,
    type: detail.type,
    name: detail.name || '',
    description: detail.description || '',
    sortOrder: detail.sortOrder || 0,
  })
  externalLegacyId.value = String(detail.legacyId || '')
  selectedIds.value = new Set((detail.userIds || []).map((id: any) => Number(id)))
}

async function loadDepartments() {
  departments.value = form.clubId ? normalizeList(await departmentApi.list(form.clubId)) : []
}

function memberParams() {
  return {
    clubId: form.clubId,
    keyword: filters.keyword.trim() || undefined,
    status: filters.status || undefined,
    departmentId: filters.departmentId || undefined,
  }
}

async function loadMembers() {
  if (!form.clubId) return
  membersLoading.value = true
  try {
    const result = await unifiedGroupApi.userOptions({
      ...memberParams(),
      page: pagination.page,
      pageSize: pagination.pageSize,
    })
    members.value = normalizeList(result)
    memberTotal.value = Number(result?.total || 0)
    await nextTick()
    syncVisibleSelection()
  } finally {
    membersLoading.value = false
  }
}

function syncVisibleSelection() {
  if (!memberTableRef.value) return
  syncingSelection.value = true
  members.value.forEach((row) => memberTableRef.value?.toggleRowSelection(row, selectedIds.value.has(Number(row.id))))
  nextTick(() => { syncingSelection.value = false })
}

function onSelectionChange(rows: any[]) {
  if (syncingSelection.value) return
  const next = new Set(selectedIds.value)
  members.value.forEach((row) => next.delete(Number(row.id)))
  rows.forEach((row) => next.add(Number(row.id)))
  selectedIds.value = next
}

function searchMembers() {
  pagination.page = 1
  loadMembers()
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: '', departmentId: '' })
  searchMembers()
}

async function selectAllFiltered() {
  if (!form.clubId) return
  selectAllLoading.value = true
  try {
    const ids = normalizeList(await unifiedGroupApi.userOptionIds(memberParams()))
    selectedIds.value = new Set(ids.map((id: any) => Number(id)))
    syncVisibleSelection()
    ElMessage.success(`已选择全部 ${selectedIds.value.size} 名筛选结果`)
  } finally {
    selectAllLoading.value = false
  }
}

function clearSelection() {
  selectedIds.value = new Set()
  memberTableRef.value?.clearSelection()
}

function onPageSizeChange() {
  pagination.page = 1
  loadMembers()
}

async function onClubChange() {
  clearSelection()
  filters.departmentId = ''
  await loadDepartments()
  searchMembers()
}

async function saveGroup() {
  if (!(await formRef.value?.validate())) return
  if (form.type === 'checkin' && !selectedCount.value) {
    ElMessage.warning('签到分组至少需要选择一名成员')
    return
  }
  saving.value = true
  try {
    const payload = {
      clubId: form.clubId,
      type: form.type,
      name: form.name.trim(),
      description: form.description.trim(),
      sortOrder: form.sortOrder,
      userIds: Array.from(selectedIds.value),
    }
    if (isEdit.value && groupId.value) await unifiedGroupApi.update(groupId.value, payload)
    else await unifiedGroupApi.create(payload)
    ElMessage.success(isEdit.value ? '分组保存成功' : '分组创建成功')
    router.push({ path: '/admin/groups', query: { type: form.type } })
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/admin/groups')
}

function goExternalManagement() {
  router.push({ path: '/admin/bot-groups', query: { groupId: externalLegacyId.value } })
}

onMounted(async () => {
  initialLoading.value = true
  try {
    const requestedType = String(route.query.type || '')
    if (!isEdit.value && ['department', 'checkin', 'alumni'].includes(requestedType)) {
      form.type = requestedType as GroupType
    }
    await loadClubs()
    await loadGroup()
    if (form.type !== 'external') {
      await loadDepartments()
      await loadMembers()
    }
  } finally {
    initialLoading.value = false
  }
})
</script>

<style scoped>
.group-editor-page { max-width: 1280px; margin: 0 auto; padding-bottom: 96px; }
.editor-header, .editor-actions, .section-heading, .member-filters, .selection-toolbar { display: flex; align-items: center; }
.editor-header { justify-content: space-between; margin-bottom: 18px; }
.editor-header__main { display: flex; align-items: flex-start; gap: 20px; }
.editor-header h2, .editor-header p, .section-heading h3, .section-heading p { margin: 0; }
.editor-header h2 { margin-top: 3px; font-size: 26px; }
.editor-header p, .section-heading p { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 13px; }
.editor-header__eyebrow { color: var(--el-color-primary); font-size: 12px; font-weight: 700; letter-spacing: .08em; }
.editor-card { margin-bottom: 18px; padding: 24px; background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 14px; }
.section-heading { gap: 12px; margin-bottom: 22px; }
.section-heading--members { align-items: flex-start; }
.section-heading--members .el-tag { margin-left: auto; }
.section-heading__index { display: inline-flex; width: 32px; height: 32px; flex: 0 0 32px; align-items: center; justify-content: center; color: var(--el-color-primary); background: var(--el-color-primary-light-9); border-radius: 9px; font-weight: 700; }
.section-heading h3 { font-size: 18px; }
.form-grid { display: grid; grid-template-columns: minmax(220px, .8fr) minmax(420px, 1.2fr); gap: 20px; }
.form-grid--details { grid-template-columns: minmax(320px, 1fr) 180px; }
.type-selector { display: grid; grid-template-columns: repeat(3, 1fr); width: 100%; }
.type-selector--four { grid-template-columns: repeat(4, 1fr); }
.type-selector :deep(.el-radio-button__inner) { width: 100%; min-height: 40px; }
.editor-card :deep(.el-select), .editor-card :deep(.el-input-number) { width: 100%; }
.member-filters { display: grid; grid-template-columns: minmax(260px, 1fr) 160px 180px auto auto; gap: 10px; margin-bottom: 14px; }
.selection-toolbar { min-height: 48px; justify-content: space-between; gap: 16px; padding: 8px 12px; color: var(--el-text-color-secondary); background: var(--el-fill-color-light); border-radius: 9px 9px 0 0; font-size: 13px; }
.member-name { display: flex; align-items: center; gap: 10px; }
.member-name strong, .member-name small { display: block; }
.member-name small { margin-top: 2px; color: var(--el-text-color-secondary); font-size: 12px; }
.pagination-wrap { display: flex; justify-content: flex-end; padding-top: 18px; overflow-x: auto; }
.external-manage-button { margin-top: 18px; min-height: 42px; }
.editor-actions { position: fixed; z-index: 20; right: 28px; bottom: 20px; left: calc(var(--admin-sidebar-width, 220px) + 28px); justify-content: space-between; gap: 20px; max-width: 1228px; margin: 0 auto; padding: 14px 20px; background: color-mix(in srgb, var(--el-bg-color) 94%, transparent); border: 1px solid var(--el-border-color-lighter); border-radius: 13px; box-shadow: 0 12px 36px rgba(0, 0, 0, .12); backdrop-filter: blur(12px); }
.editor-actions strong, .editor-actions span { display: block; }
.editor-actions span { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 12px; }
@media (max-width: 960px) {
  .form-grid, .form-grid--details, .member-filters { grid-template-columns: 1fr 1fr; }
  .member-filters > :first-child { grid-column: 1 / -1; }
  .editor-actions { left: 20px; }
}
@media (max-width: 640px) {
  .editor-header, .editor-header__main, .selection-toolbar, .editor-actions { align-items: stretch; flex-direction: column; }
  .editor-card { padding: 18px; }
  .form-grid, .form-grid--details, .member-filters { grid-template-columns: 1fr; }
  .type-selector { grid-template-columns: 1fr; }
  .selection-toolbar > div { display: grid; gap: 8px; }
  .editor-actions { right: 12px; bottom: 12px; left: 12px; }
}
</style>
