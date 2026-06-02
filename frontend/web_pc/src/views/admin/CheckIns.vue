<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-model="query.status"
          clearable
          placeholder="签到状态"
          style="width: 150px"
          @change="fetchList"
        >
          <el-option label="草稿" value="draft" />
          <el-option label="进行中" value="open" />
          <el-option label="已关闭" value="closed" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
      <div class="toolbar__actions">
        <el-button @click="openGroupManager">签到分组</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog">发布签到</el-button>
      </div>
    </ViewToolbar>

    <el-table v-loading="loading" :data="rows" class="admin-table">
      <el-table-column label="签到" min-width="220">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <p class="muted-line">
            {{ row.activityTitle || '独立签到' }} / {{ row.location || '未设置地点' }}
          </p>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="230">
        <template #default="{ row }">{{ formatRange(row.startAt, row.endAt) }}</template>
      </el-table-column>
      <el-table-column label="进度" width="150">
        <template #default="{ row }"
          >{{ row.checkedCount || 0 }} / {{ row.targetCount || 0 }}</template
        >
      </el-table-column>
      <el-table-column label="签到积分" width="110">
        <template #default="{ row }">{{ row.checkinPoints || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row)">全屏预览</el-button>
          <el-button link type="success" @click="openRecords(row)">记录</el-button>
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button v-if="row.status === 'open'" link type="danger" @click="closeSession(row)"
            >关闭</el-button
          >
          <el-button link type="danger" @click="deleteSession(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="暂无签到" />

    <el-dialog v-model="dialogVisible" :title="editingSessionId ? '编辑内部签到' : '发布内部签到'" width="920px">
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
              <el-option
                v-for="item in activities"
                :key="item.id"
                :label="item.title"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="签到积分">
            <el-input-number v-model="form.checkinPoints" :min="0" :step="1" />
          </el-form-item>
          <el-form-item label="签到分组">
            <el-select
              v-model="form.groupId"
              clearable
              filterable
              placeholder="可选，自动带入组内人员"
              @change="applyGroupToForm"
            >
              <el-option
                v-for="item in groups"
                :key="item.id"
                :label="`${item.name}（${item.memberCount || 0}人）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-divider content-position="left">发放人员</el-divider>
        <div class="member-toolbar">
          <el-input
            v-model="memberKeyword"
            clearable
            placeholder="搜索人员姓名/学号"
            style="width: 260px"
          />
          <el-button @click="selectAllMembers">全选当前人员</el-button>
          <el-button @click="clearMemberSelection">清空</el-button>
          <span class="muted-line">已选择 {{ form.targetUserIds.length }} 人</span>
        </div>
        <el-table
          :data="filteredMembers"
          row-key="id"
          height="300"
          @selection-change="handleMemberSelection"
          ref="memberTableRef"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="realName" label="姓名" min-width="140" />
          <el-table-column prop="userName" label="用户名" min-width="140" />
          <el-table-column prop="studentId" label="学号" min-width="140" />
          <el-table-column prop="className" label="班级" min-width="130" />
          <el-table-column prop="userStatus" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.userStatus)">{{
                userStatusText(row.userStatus)
              }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{
          editingSessionId ? '保存' : '发布'
        }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupVisible" title="签到分组" width="980px">
      <div class="member-toolbar">
        <el-input v-model="groupForm.name" placeholder="分组名称" style="width: 220px" />
        <el-input
          v-model="groupKeyword"
          clearable
          placeholder="搜索人员姓名/学号"
          style="width: 260px"
        />
        <el-button @click="selectAllGroupUsers">全选当前人员</el-button>
        <el-button @click="clearGroupSelection">清空</el-button>
        <el-button type="primary" :loading="groupSaving" @click="saveGroup">{{
          groupForm.id ? '更新分组' : '创建分组'
        }}</el-button>
        <el-button v-if="groupForm.id" @click="resetGroupForm">新建</el-button>
      </div>
      <el-table
        :data="filteredGroupUsers"
        row-key="id"
        height="280"
        @selection-change="handleGroupSelection"
        ref="groupUserTableRef"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="realName" label="姓名" min-width="140" />
        <el-table-column prop="userName" label="用户名" min-width="140" />
        <el-table-column prop="studentId" label="学号" min-width="140" />
        <el-table-column prop="className" label="班级" min-width="130" />
      </el-table>
      <template v-if="groupForm.id">
        <el-divider content-position="left">当前组内成员</el-divider>
        <el-table :data="selectedGroupMembers" height="180">
          <el-table-column prop="realName" label="姓名" min-width="140" />
          <el-table-column prop="studentId" label="学号" min-width="140" />
          <el-table-column prop="className" label="班级" min-width="130" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeGroupMember(row)">移出</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-divider content-position="left">已有分组</el-divider>
      <el-table :data="groups" height="260">
        <el-table-column prop="name" label="分组名称" min-width="180" />
        <el-table-column prop="memberCount" label="人数" width="100" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="editGroup(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteGroup(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <Teleport to="body">
      <div v-if="previewVisible && previewRow" class="checkin-preview-overlay">
        <div class="preview-screen">
          <div class="preview-top">
            <el-button size="large" @click="previewVisible = false">退出全屏</el-button>
          </div>
          <div class="preview-content">
            <section class="preview-qr">
              <p class="preview-kicker">微信扫码网页签到</p>
              <h1>{{ previewRow.title }}</h1>
              <p class="preview-meta">
                {{ formatRange(previewRow.startAt, previewRow.endAt) }} ·
                {{ previewRow.location || '现场签到' }}
              </p>
              <p v-if="previewRow.checkinPoints" class="preview-count">
                本次签到 +{{ previewRow.checkinPoints }} 积分
              </p>
              <img
                class="qr-image"
                :src="qrUrl(checkInUrl(previewRow.qrPayload))"
                alt="签到二维码"
              />
              <p class="preview-count">
                已签到 {{ checkedRecords.length }} /
                {{ previewRecords.length || previewRow.targetCount || 0 }}
              </p>
            </section>
            <section class="preview-roster">
              <div class="roster-column roster-column--pending">
                <div class="roster-head">
                  <span>未签到</span>
                  <strong>{{ pendingRecords.length }}</strong>
                </div>
                <div class="roster-list">
                  <div
                    v-for="item in pendingRecords"
                    :key="`pending-${item.userId}`"
                    class="roster-item"
                  >
                    {{ displayRecordName(item) }}
                  </div>
                  <el-empty
                    v-if="!pendingRecords.length"
                    :image-size="72"
                    description="已全部签到"
                  />
                </div>
              </div>
              <div class="roster-column roster-column--checked">
                <div class="roster-head">
                  <span>已签到</span>
                  <strong>{{ checkedRecords.length }}</strong>
                </div>
                <div class="roster-list">
                  <div
                    v-for="item in checkedRecords"
                    :key="`checked-${item.userId}`"
                    class="roster-item"
                  >
                    {{ displayRecordName(item) }}
                  </div>
                  <el-empty v-if="!checkedRecords.length" :image-size="72" description="暂无签到" />
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </Teleport>

    <el-dialog v-model="recordsVisible" title="签到记录" width="980px">
      <div class="member-toolbar" v-if="recordsRow">
        <el-select
          v-model="appendUserIds"
          multiple
          filterable
          placeholder="中途添加人员"
          style="width: 360px"
        >
          <el-option
            v-for="item in appendableUsers"
            :key="item.id"
            :label="displayUserName(item)"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" @click="addTargetsToSession">添加人员</el-button>
      </div>
      <el-table :data="records">
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="studentId" label="学号" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'checked' ? 'success' : 'info'">{{
              row.status === 'checked' ? '已签到' : '未签到'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.checkinAt) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggleRecordStatus(row)">
              {{ row.status === 'checked' ? '改未签到' : '改已签到' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { activityApi, checkInApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { qrSvgDataUrl } from '@/utils/qr.ts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const loading = ref(false)

const saving = ref(false)

const dialogVisible = ref(false)

const editingSessionId = ref<any>(null)

const groupVisible = ref(false)

const previewVisible = ref(false)

const recordsVisible = ref(false)

const rows = ref<any[]>([])

const records = ref<any[]>([])

const recordsRow = ref<any>(null)

const previewRecords = ref<any[]>([])

const activities = ref<any[]>([])

const groups = ref<any[]>([])

const members = ref<any[]>([])

const appendUserIds = ref<any[]>([])

const liveTimer = ref<any>(null)

const memberKeyword = ref('')

const groupKeyword = ref('')

const previewRow = ref<any>(null)

const query = ref({ status: '' })

const form = ref({
  title: '',
  status: 'open',
  startAt: '',
  endAt: '',
  location: '',
  activityId: undefined,
  groupId: undefined,
  checkinPoints: 0,
  targetUserIds: [],
})

const groupForm = ref({ id: null, name: '', userIds: [] })

const groupSaving = ref(false)

const rules = ref({ title: [{ required: true, message: '请输入签到标题', trigger: 'blur' }] })

const formRef = ref<any>()

const memberTableRef = ref<any>()

const groupUserTableRef = ref<any>()

let syncingMemberSelection = false
let syncingGroupSelection = false
let memberSelectionSyncVersion = 0
let groupSelectionSyncVersion = 0

const filteredMembers = computed(() => {
  const keyword = memberKeyword.value.trim()
  if (!keyword) return members.value
  return members.value.filter((item) =>
    [item.realName, item.userName, item.studentId, item.college, item.major, item.className].some(
      (value) => String(value || '').includes(keyword),
    ),
  )
})

const checkedRecords = computed(() => {
  return previewRecords.value.filter((item) => item.status === 'checked')
})

const pendingRecords = computed(() => {
  return previewRecords.value.filter((item) => item.status !== 'checked')
})

const filteredGroupUsers = computed(() => {
  const keyword = groupKeyword.value.trim()
  if (!keyword) return members.value
  return members.value.filter((item) =>
    [item.realName, item.userName, item.studentId, item.college, item.major, item.className].some(
      (value) => String(value || '').includes(keyword),
    ),
  )
})

const appendableUsers = computed(() => {
  const selected = new Set((records.value || []).map((item) => item.userId))
  return members.value.filter((item) => !selected.has(item.id))
})

const selectedGroupMembers = computed(() => {
  const selected = new Set(groupForm.value.userIds || [])
  return members.value.filter((item) => selected.has(item.id))
})

function statusText(status: any) {
  return { draft: '草稿', open: '进行中', closed: '已关闭' }[status] || status || '-'
}

function userStatusText(status: any) {
  return { active: '启用', disabled: '禁用', locked: '锁定' }[status] || status || '-'
}

function formatRange(startAt: any, endAt: any) {
  return `${formatDateTime(startAt) || '不限'} - ${formatDateTime(endAt) || '不限'}`
}

async function fetchList() {
  loading.value = true
  try {
    rows.value = (await checkInApi.list(query.value)) || []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [activitiesResult, usersResult, groupsResult] = await Promise.allSettled([
    activityApi.list({ status: 'published' }),
    checkInApi.userOptions(),
    checkInApi.groups(),
  ])

  activities.value = activitiesResult.status === 'fulfilled' ? activitiesResult.value || [] : []
  members.value =
    usersResult.status === 'fulfilled' ? (usersResult.value || []).filter((item) => item.id) : []
  groups.value = groupsResult.status === 'fulfilled' ? groupsResult.value || [] : []
}

function openDialog() {
  editingSessionId.value = null
  form.value = {
    title: '',
    status: 'open',
    startAt: '',
    endAt: '',
    location: '',
    activityId: undefined,
    groupId: undefined,
    checkinPoints: 0,
    targetUserIds: [],
  }
  memberKeyword.value = ''
  dialogVisible.value = true
  syncMemberTableSelection([])
}

function openEditDialog(row: any) {
  editingSessionId.value = row.id
  form.value = {
    title: row.title || '',
    status: row.status || 'open',
    startAt: toDateTimeLocalValue(row.startAt),
    endAt: toDateTimeLocalValue(row.endAt),
    location: row.location || '',
    activityId: row.activityId || undefined,
    groupId: row.groupId || undefined,
    checkinPoints: row.checkinPoints || 0,
    targetUserIds: [...(row.targetUserIds || [])],
  }
  memberKeyword.value = ''
  dialogVisible.value = true
  syncMemberTableSelection(form.value.targetUserIds)
}

function handleMemberSelection(selection: any) {
  if (syncingMemberSelection) return
  form.value.targetUserIds = mergeVisibleSelectionIds(
    form.value.targetUserIds,
    filteredMembers.value,
    selection,
  )
}

function selectAllMembers() {
  form.value.targetUserIds = uniqueIds([
    ...form.value.targetUserIds,
    ...filteredMembers.value.map((row) => row.id),
  ])
  syncMemberTableSelection(form.value.targetUserIds)
}

function clearMemberSelection() {
  form.value.targetUserIds = []
  syncMemberTableSelection([])
}

function applyGroupToForm(groupId: any) {
  const group = groups.value.find((item) => item.id === groupId)
  form.value.targetUserIds = group?.userIds ? [...group.userIds] : []
  syncMemberTableSelection(form.value.targetUserIds)
}

function openGroupManager() {
  resetGroupForm()
  groupVisible.value = true
}

function resetGroupForm() {
  groupForm.value = { id: null, name: '', userIds: [] }
  groupKeyword.value = ''
  syncGroupTableSelection([])
}

function handleGroupSelection(selection: any) {
  if (syncingGroupSelection) return
  groupForm.value.userIds = mergeVisibleSelectionIds(
    groupForm.value.userIds,
    filteredGroupUsers.value,
    selection,
  )
}

function selectAllGroupUsers() {
  groupForm.value.userIds = uniqueIds([
    ...groupForm.value.userIds,
    ...filteredGroupUsers.value.map((row) => row.id),
  ])
  syncGroupTableSelection(groupForm.value.userIds)
}

function clearGroupSelection() {
  groupForm.value.userIds = []
  syncGroupTableSelection([])
}

function editGroup(row: any) {
  groupForm.value = { id: row.id, name: row.name, userIds: [...(row.userIds || [])] }
  syncGroupTableSelection(groupForm.value.userIds)
}

async function saveGroup() {
  if (!groupForm.value.name.trim()) {
    ElMessage.error('请输入分组名称')
    return
  }
  if (!groupForm.value.userIds.length) {
    ElMessage.error('请选择组内人员')
    return
  }
  groupSaving.value = true
  try {
    const payload = { name: groupForm.value.name.trim(), userIds: groupForm.value.userIds }
    if (groupForm.value.id) {
      await checkInApi.updateGroup(groupForm.value.id, payload)
      ElMessage.success('分组已更新')
    } else {
      await checkInApi.createGroup(payload)
      ElMessage.success('分组已创建')
    }
    groups.value = (await checkInApi.groups()) || []
    resetGroupForm()
  } finally {
    groupSaving.value = false
  }
}

async function deleteGroup(row: any) {
  await ElMessageBox.confirm(`确定删除分组“${row.name}”吗？`, '删除分组', { type: 'warning' })
  await checkInApi.deleteGroup(row.id)
  ElMessage.success('分组已删除')
  groups.value = (await checkInApi.groups()) || []
  if (groupForm.value.id === row.id) resetGroupForm()
}

async function removeGroupMember(row: any) {
  if (!groupForm.value.id) return
  await ElMessageBox.confirm(`确定将“${displayUserName(row)}”移出当前分组吗？`, '移出成员', {
    type: 'warning',
  })
  await checkInApi.removeGroupMember(groupForm.value.id, row.id)
  ElMessage.success('成员已移出')
  groupForm.value.userIds = groupForm.value.userIds.filter((id) => id !== row.id)
  groups.value = (await checkInApi.groups()) || []
  syncGroupTableSelection(groupForm.value.userIds)
}

function uniqueIds(values: any[]) {
  return [...new Set(values.filter((id) => id !== null && id !== undefined))]
}

function mergeVisibleSelectionIds(existingIds: any[], visibleRows: any[], selection: any[]) {
  const visibleIds = new Set(visibleRows.map((row) => row.id))
  const selectedVisibleIds = new Set(selection.map((row) => row.id))
  return uniqueIds([
    ...existingIds.filter((id) => !visibleIds.has(id)),
    ...visibleRows.filter((row) => selectedVisibleIds.has(row.id)).map((row) => row.id),
  ])
}

function syncMemberTableSelection(ids: any[]) {
  const selected = new Set(ids)
  const version = ++memberSelectionSyncVersion
  syncingMemberSelection = true
  nextTick(() => {
    memberTableRef.value?.clearSelection()
    filteredMembers.value.forEach((row) =>
      memberTableRef.value?.toggleRowSelection(row, selected.has(row.id)),
    )
    if (version === memberSelectionSyncVersion) syncingMemberSelection = false
  })
}

function syncGroupTableSelection(ids: any[]) {
  const selected = new Set(ids)
  const version = ++groupSelectionSyncVersion
  syncingGroupSelection = true
  nextTick(() => {
    groupUserTableRef.value?.clearSelection()
    filteredGroupUsers.value.forEach((row) =>
      groupUserTableRef.value?.toggleRowSelection(row, selected.has(row.id)),
    )
    if (version === groupSelectionSyncVersion) syncingGroupSelection = false
  })
}

function save() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!form.value.targetUserIds.length) {
      ElMessage.error('请选择发放人员')
      return
    }
    saving.value = true
    try {
      const payload = {
        ...form.value,
        activityId: form.value.activityId || undefined,
        groupId: form.value.groupId || undefined,
      }
      if (editingSessionId.value) {
        await checkInApi.update(editingSessionId.value, payload)
        ElMessage.success('签到已更新')
      } else {
        await checkInApi.create(payload)
        ElMessage.success('签到已发布')
      }
      dialogVisible.value = false
      editingSessionId.value = null
      fetchList()
    } finally {
      saving.value = false
    }
  })
}

function toDateTimeLocalValue(value: any) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const offsetMs = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function openPreview(row: any) {
  previewRow.value = row
  previewRecords.value = []
  previewVisible.value = true
  startLiveRefresh(row.id)
}

function qrUrl(payload: any) {
  return qrSvgDataUrl(payload || '')
}

function checkInUrl(payload: any) {
  const token = encodeURIComponent(payload || '')
  return `${window.location.origin}/check-in/scan?t=${token}`
}

async function openRecords(row: any) {
  recordsRow.value = row
  appendUserIds.value = []
  records.value = await checkInApi.records(row.id)
  recordsVisible.value = true
  startLiveRefresh(row.id)
}

function displayUserName(row: any) {
  return `${row.realName || row.userName || row.studentId || row.id}${row.studentId ? ` / ${row.studentId}` : ''}`
}

function displayRecordName(row: any) {
  return row.realName || row.userName || row.studentId || `用户 ${row.userId || '-'}`
}

async function closeSession(row: any) {
  await checkInApi.close(row.id)
  ElMessage.success('签到已关闭')
  fetchList()
}

async function deleteSession(row: any) {
  await ElMessageBox.confirm(
    `确定删除“${row.title}”吗？删除后签到名单和签到记录都会被清除。`,
    '删除签到',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  await checkInApi.delete(row.id)
  ElMessage.success('签到已删除')
  if (previewRow.value?.id === row.id) previewVisible.value = false
  fetchList()
}

async function addTargetsToSession() {
  if (!recordsRow.value || !appendUserIds.value.length) return
  await checkInApi.addTargets(recordsRow.value.id, { userIds: appendUserIds.value })
  ElMessage.success('人员已添加')
  appendUserIds.value = []
  records.value = await checkInApi.records(recordsRow.value.id)
  fetchList()
}

async function toggleRecordStatus(row: any) {
  if (!recordsRow.value) return
  const status = row.status === 'checked' ? 'pending' : 'checked'
  await checkInApi.updateRecordStatus(recordsRow.value.id, row.userId, { status })
  ElMessage.success(status === 'checked' ? '已改为已签到' : '已改为未签到')
  records.value = await checkInApi.records(recordsRow.value.id)
  if (previewVisible.value) previewRecords.value = records.value
  fetchList()
}

function startLiveRefresh(sessionId: any) {
  stopLiveRefresh()
  liveTimer.value = window.setInterval(async () => {
    if (!previewVisible.value && !recordsVisible.value) {
      stopLiveRefresh()
      return
    }
    const [detail, records] = await Promise.all([
      previewVisible.value ? checkInApi.detail(sessionId) : Promise.resolve(null),
      previewVisible.value || recordsVisible.value
        ? checkInApi.records(sessionId)
        : Promise.resolve(null),
    ])
    if (detail) previewRow.value = detail
    if (records && previewVisible.value) previewRecords.value = records
    if (records && recordsVisible.value) records.value = records
  }, 3000)
}

function stopLiveRefresh() {
  if (liveTimer.value) {
    window.clearInterval(liveTimer.value)
    liveTimer.value = null
  }
}

onMounted(() => {
  fetchList()
  loadOptions()
})

watch(filteredMembers, () => syncMemberTableSelection(form.value.targetUserIds))
watch(filteredGroupUsers, () => syncGroupTableSelection(groupForm.value.userIds))

watch(previewVisible, (value: any) => {
  document.body.style.overflow = value ? 'hidden' : ''
})

onBeforeUnmount(() => {
  stopLiveRefresh()
  document.body.style.overflow = ''
})
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

.checkin-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: var(--oa-page-soft-bg);
  overflow: hidden;
}

.preview-screen {
  width: 100vw;
  min-height: 100vh;
  min-height: 100dvh;
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  overflow: hidden;
}

.preview-top {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 3;
  padding: 24px;
  text-align: right;
}

.preview-content {
  display: grid;
  grid-template-columns: minmax(420px, 0.92fr) minmax(520px, 1.08fr);
  align-items: stretch;
  gap: 16px;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 4vh 32px;
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
  border: 1px solid rgba(224, 224, 224, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: none;
}

.roster-column--pending {
  border-color: rgba(224, 224, 224, 0.36);
}

.roster-column--checked {
  border-color: rgba(224, 224, 224, 0.28);
}

.roster-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--oa-border);
  color: var(--oa-text);
  font-size: 22px;
  font-weight: 600;
}

.roster-head strong {
  display: grid;
  min-width: 46px;
  height: 38px;
  place-items: center;
  border-radius: 999px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
  font-size: 22px;
}

.roster-column--pending .roster-head strong {
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
}

.roster-column--checked .roster-head strong {
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
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
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
  font-size: 20px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-kicker {
  color: var(--oa-text);
  font-size: 22px;
  font-weight: 600;
}

.preview-content h1 {
  margin: 0;
  font-size: clamp(42px, 7vw, 86px);
}

.preview-meta,
.preview-count {
  margin: 0;
  color: var(--oa-muted);
  font-size: 20px;
}

.qr-image {
  width: min(48vh, 420px);
  height: min(48vh, 420px);
  padding: 18px;
  background: var(--oa-elevated-bg);
  border-radius: 8px;
  box-shadow: none;
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
    top: 10px;
    right: 10px;
    padding: 0;
  }

  .preview-content {
    grid-template-columns: 1fr;
    min-height: 100dvh;
    overflow-y: auto;
    padding: 72px 16px 24px;
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
