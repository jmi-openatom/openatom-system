<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <span class="toolbar__label">选择社团：</span>
        <el-select
          v-model="selectedClubId"
          filterable
          placeholder="请选择社团"
          style="width: 260px"
          @change="onClubChange"
        >
          <el-option
            v-for="club in clubs"
            :key="club.id"
            :label="club.name"
            :value="club.id"
          />
        </el-select>
      </div>
    </ViewToolbar>

    <template v-if="selectedClubId">
      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="section-card__header">
            <span class="section-card__title">群信息配置</span>
          </div>
        </template>

        <el-form label-width="140px" class="group-form">
          <el-form-item label="社团总群微信二维码">
            <div class="qrcode-row">
              <template v-if="groupForm.wechatGroupQrcode">
                <el-image
                  :src="groupForm.wechatGroupQrcode"
                  style="width:120px;height:120px;border:1px solid #ebeef5;border-radius:4px"
                  fit="contain"
                  :preview-src-list="[groupForm.wechatGroupQrcode]"
                />
              </template>
              <div class="qrcode-actions">
                <el-input
                  v-model="groupForm.wechatGroupQrcode"
                  placeholder="输入微信群二维码图片URL"
                  style="width: 360px"
                  clearable
                />
                <el-upload
                  :action="uploadUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="onWechatQrcodeUploaded"
                  accept="image/*"
                  style="display:inline-block"
                >
                  <el-button type="primary" size="small">上传二维码</el-button>
                </el-upload>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="社团QQ群号">
            <el-input
              v-model="groupForm.qqGroupNumber"
              placeholder="输入QQ群号"
              style="width: 240px"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveGroupInfo" :loading="groupSaving">保存群信息</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="section-card__header">
            <span class="section-card__title">社团核心管理</span>
          </div>
        </template>

        <div class="leaders-grid">
          <div class="leader-item">
            <div class="leader-item__label">
              <el-tag type="danger" size="large">社长</el-tag>
            </div>
            <div class="leader-item__body">
              <template v-if="loadedPresident">
                <el-avatar :size="40" :src="loadedPresident.avatar">
                  {{ (loadedPresident.realName || loadedPresident.userName || '?').charAt(0) }}
                </el-avatar>
                <div class="leader-item__info">
                  <span class="leader-item__name">{{ loadedPresident.realName || loadedPresident.userName }}</span>
                  <span class="leader-item__username">@{{ loadedPresident.userName }}</span>
                </div>
              </template>
              <template v-else>
                <span class="leader-item__empty">未设置</span>
              </template>
              <el-button v-if="canUpdateClub" link type="primary" @click="openUserPicker('president')">
                {{ clubDetail.presidentUserId ? '更换' : '设置' }}
              </el-button>
              <el-button v-if="canUpdateClub && clubDetail.presidentUserId" link type="danger" @click="clearSingle('president')">清空</el-button>
            </div>
          </div>

          <div class="leader-item">
            <div class="leader-item__label">
              <el-tag style="background:#67c23a;border-color:#67c23a;color:#fff" size="large">团支书</el-tag>
            </div>
            <div class="leader-item__body">
              <template v-if="loadedLeagueSecretary">
                <el-avatar :size="40" :src="loadedLeagueSecretary.avatar">
                  {{ (loadedLeagueSecretary.realName || loadedLeagueSecretary.userName || '?').charAt(0) }}
                </el-avatar>
                <div class="leader-item__info">
                  <span class="leader-item__name">{{ loadedLeagueSecretary.realName || loadedLeagueSecretary.userName }}</span>
                  <span class="leader-item__username">@{{ loadedLeagueSecretary.userName }}</span>
                </div>
              </template>
              <template v-else>
                <span class="leader-item__empty">未设置</span>
              </template>
              <el-button v-if="canUpdateClub" link type="primary" @click="openUserPicker('leagueSecretary')">
                {{ clubDetail.leagueSecretaryUserId ? '更换' : '设置' }}
              </el-button>
              <el-button v-if="canUpdateClub && clubDetail.leagueSecretaryUserId" link type="danger" @click="clearSingle('leagueSecretary')">清空</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="section-card__header">
            <span class="section-card__title">副社长</span>
            <el-button v-if="canUpdateClub" type="primary" size="small" :icon="Plus" @click="openUserPicker('vicePresident')">添加</el-button>
          </div>
        </template>
        <el-table v-if="vicePresidentUsers.length" :data="vicePresidentUsers" class="admin-table">
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column label="姓名">
            <template #default="{ row }">
              <div class="dept-leader">
                <el-avatar :size="28" :src="row.avatar">
                  {{ (row.realName || row.userName || '?').charAt(0) }}
                </el-avatar>
                <span class="dept-leader__name">{{ row.realName || row.userName }}</span>
                <span class="dept-leader__username">@{{ row.userName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeVicePresident(row.userId)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无副社长" :image-size="60" />
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="section-card__header">
            <span class="section-card__title">部门管理</span>
            <span class="section-card__count">共 {{ departments.length }} 个部门</span>
          </div>
        </template>
        <el-table v-loading="deptLoading" :data="departments" class="admin-table">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="部门名称" width="160" />
          <el-table-column label="部长" min-width="220">
            <template #default="{ row }">
              <div class="dept-leader">
                <template v-if="row._leaderUser">
                  <el-avatar :size="28" :src="row._leaderUser.avatar">
                    {{ (row._leaderUser.realName || row._leaderUser.userName || '?').charAt(0) }}
                  </el-avatar>
                  <span class="dept-leader__name">{{ row._leaderUser.realName || row._leaderUser.userName }}</span>
                  <span class="dept-leader__username">@{{ row._leaderUser.userName }}</span>
                </template>
                <template v-else>
                  <span class="leader-item__empty">未设置</span>
                </template>
                <el-button v-if="canUpdateDept" link type="primary" size="small" @click="openUserPicker('departmentHead', row)">
                  {{ row.managerUserId ? '更换' : '设置' }}
                </el-button>
                <el-button v-if="canUpdateDept && row.managerUserId" link type="danger" size="small" @click="clearDeptField(row, 'manager')">清空</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="副部长" min-width="220">
            <template #default="{ row }">
              <div class="dept-leader">
                <template v-if="row._viceLeaderUser">
                  <el-avatar :size="28" :src="row._viceLeaderUser.avatar">
                    {{ (row._viceLeaderUser.realName || row._viceLeaderUser.userName || '?').charAt(0) }}
                  </el-avatar>
                  <span class="dept-leader__name">{{ row._viceLeaderUser.realName || row._viceLeaderUser.userName }}</span>
                  <span class="dept-leader__username">@{{ row._viceLeaderUser.userName }}</span>
                </template>
                <template v-else>
                  <span class="leader-item__empty">未设置</span>
                </template>
                <el-button v-if="canUpdateDept" link type="primary" size="small" @click="openUserPicker('viceDepartmentHead', row)">
                  {{ row.viceManagerUserId ? '更换' : '设置' }}
                </el-button>
                <el-button v-if="canUpdateDept && row.viceManagerUserId" link type="danger" size="small" @click="clearDeptField(row, 'viceManager')">清空</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="微信群二维码" width="120">
            <template #default="{ row }">
              <el-image
                v-if="row.wechatGroupQrcode"
                :src="row.wechatGroupQrcode"
                style="width:60px;height:60px;border-radius:4px"
                fit="contain"
                :preview-src-list="[row.wechatGroupQrcode]"
              />
              <span v-else class="leader-item__empty">-</span>
            </template>
          </el-table-column>
          <el-table-column label="二维码操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="(res: any) => onDeptQrcodeUploaded(row, res)"
                accept="image/*"
                style="display:inline-block;margin-right:4px"
              >
                <el-button link type="primary" size="small">上传</el-button>
              </el-upload>
              <el-button
                v-if="row.wechatGroupQrcode"
                link
                type="danger"
                size="small"
                @click="clearDeptQrcode(row)"
              >清空</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-else description="请先选择一个社团" />

    <el-dialog v-model="userPickerVisible" :title="userPickerTitle" width="600px">
      <div class="user-picker">
        <el-input
          v-model="userSearchKeyword"
          clearable
          placeholder="搜索用户名/姓名"
          style="margin-bottom: 16px"
          @keyup.enter="searchUsers"
        >
          <template #append>
            <el-button :icon="Search" @click="searchUsers" />
          </template>
        </el-input>
        <el-table
          v-loading="userSearchLoading"
          :data="userSearchRows"
          highlight-current-row
          max-height="400"
          @current-change="onUserSelect"
        >
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="userName" label="用户名" />
          <el-table-column prop="studentId" label="学号" />
          <el-table-column label="激活" width="80">
            <template #default="{ row: userRow }">
              <el-tag :type="userRow.activatedAt ? 'success' : 'info'" size="small">
                {{ userRow.activatedAt ? '已激活' : '未激活' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="userPickerVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Plus, Search } from '@element-plus/icons-vue'
import { clubApi, departmentApi, userApi, imageHostingApi } from '@/api'
import { hasPermission } from '@/utils/permission.ts'
import { getToken } from '@/utils/auth.ts'
import { computed, nextTick, onMounted, ref, watch } from 'vue'

const clubs = ref<any[]>([])
const selectedClubId = ref<number | null>(null)
const clubDetail = ref<any>({})
const departments = ref<any[]>([])
const deptLoading = ref(false)

const loadedPresident = ref<any>(null)
const loadedLeagueSecretary = ref<any>(null)
const vicePresidentUsers = ref<any[]>([])

const groupForm = ref({ wechatGroupQrcode: '', qqGroupNumber: '' })
const groupSaving = ref(false)

const uploadUrl = import.meta.env.VITE_API_BASE_URL ? `${import.meta.env.VITE_API_BASE_URL}/image-hosting/images` : '/api/v1/image-hosting/images'
const uploadHeaders = computed(() => {
  const token = getToken()
  return { Authorization: `Bearer ${token}`, jmiopenatom: token }
})

const userPickerVisible = ref(false)
const userPickerTarget = ref<'president' | 'leagueSecretary' | 'vicePresident' | 'departmentHead' | 'viceDepartmentHead'>('president')
const userPickerDept = ref<any>(null)
const userSearchKeyword = ref('')
const userSearchLoading = ref(false)
const userSearchRows = ref<any[]>([])

const canUpdateClub = computed(() => hasPermission('club:update'))
const canUpdateDept = computed(() => hasPermission('department:update'))

const userPickerTitle = computed(() => {
  const map: Record<string, string> = {
    president: '选择社长',
    leagueSecretary: '选择团支书',
    vicePresident: '选择副社长',
    departmentHead: '选择部长',
    viceDepartmentHead: '选择副部长',
  }
  return map[userPickerTarget.value] || '选择用户'
})

async function fetchClubs() {
  const res = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = res?.list || res || []
}

async function onClubChange() {
  if (!selectedClubId.value) return
  clubDetail.value = await clubApi.list({ page: 1, pageSize: 100 }).then((res) => {
    const list = res?.list || res || []
    return list.find((c: any) => c.id === selectedClubId.value) || {}
  })
  await Promise.all([loadPresident(), loadLeagueSecretary(), loadVicePresidents(), fetchDepartments()])
  groupForm.value = {
    wechatGroupQrcode: clubDetail.value.wechatGroupQrcode || '',
    qqGroupNumber: clubDetail.value.qqGroupNumber || '',
  }
}

async function loadPresident() {
  loadedPresident.value = clubDetail.value.presidentUserId
    ? await fetchUser(clubDetail.value.presidentUserId)
    : null
}

async function loadLeagueSecretary() {
  loadedLeagueSecretary.value = clubDetail.value.leagueSecretaryUserId
    ? await fetchUser(clubDetail.value.leagueSecretaryUserId)
    : null
}

async function fetchUser(userId: number): Promise<any> {
  try {
    const res = await userApi.info(userId)
    return res || {}
  } catch {
    return {}
  }
}

async function loadVicePresidents() {
  if (!selectedClubId.value) return
  try {
    const res = await clubApi.vicePresidents(selectedClubId.value)
    const list = (res?.list || res || []) as any[]
    const users: any[] = []
    for (const vp of list) {
      const user = await fetchUser(vp.userId)
      if (user) users.push({ ...vp, ...user })
    }
    vicePresidentUsers.value = users
  } catch {
    vicePresidentUsers.value = []
  }
}

async function fetchDepartments() {
  if (!selectedClubId.value) return
  deptLoading.value = true
  try {
    const res = await departmentApi.list(selectedClubId.value)
    const list = res?.list || res || []
    const enriched: any[] = []
    for (const d of list) {
      const [leader, viceLeader] = await Promise.all([
        d.managerUserId ? fetchUser(d.managerUserId).catch(() => null) : null,
        d.viceManagerUserId ? fetchUser(d.viceManagerUserId).catch(() => null) : null,
      ])
      enriched.push({ ...d, _leaderUser: leader, _viceLeaderUser: viceLeader })
    }
    departments.value = enriched
  } finally {
    deptLoading.value = false
  }
}

function openUserPicker(target: typeof userPickerTarget.value, dept?: any) {
  userPickerTarget.value = target
  userPickerDept.value = dept || null
  userSearchKeyword.value = ''
  userSearchRows.value = []
  userPickerVisible.value = true
  nextTick(() => searchUsers())
}

async function searchUsers() {
  userSearchLoading.value = true
  try {
    const res = await userApi.list({ keyword: userSearchKeyword.value, page: 1, pageSize: 50 })
    userSearchRows.value = res?.list || res || []
  } finally {
    userSearchLoading.value = false
  }
}

async function onUserSelect(user: any) {
  if (!user) return
  const target = userPickerTarget.value

  if (target === 'president') {
    await clubApi.update(selectedClubId.value!, { presidentUserId: user.id })
    clubDetail.value.presidentUserId = user.id
    loadedPresident.value = user
    ElMessage.success(`已将「${user.realName || user.userName}」设为社长`)
  } else if (target === 'leagueSecretary') {
    await clubApi.update(selectedClubId.value!, { leagueSecretaryUserId: user.id })
    clubDetail.value.leagueSecretaryUserId = user.id
    loadedLeagueSecretary.value = user
    ElMessage.success(`已将「${user.realName || user.userName}」设为团支书`)
  } else if (target === 'vicePresident') {
    await clubApi.addVicePresident(selectedClubId.value!, user.id)
    ElMessage.success(`已将「${user.realName || user.userName}」添加为副社长`)
    await loadVicePresidents()
  } else if (target === 'departmentHead' && userPickerDept.value) {
    await departmentApi.update(userPickerDept.value.id, { managerUserId: user.id })
    userPickerDept.value.managerUserId = user.id
    userPickerDept.value._leaderUser = user
    ElMessage.success(`已将「${user.realName || user.userName}」设为「${userPickerDept.value.name}」部长`)
  } else if (target === 'viceDepartmentHead' && userPickerDept.value) {
    await departmentApi.update(userPickerDept.value.id, { viceManagerUserId: user.id })
    userPickerDept.value.viceManagerUserId = user.id
    userPickerDept.value._viceLeaderUser = user
    ElMessage.success(`已将「${user.realName || user.userName}」设为「${userPickerDept.value.name}」副部长`)
  }

  userPickerVisible.value = false
}

async function clearSingle(target: 'president' | 'leagueSecretary') {
  await ElMessageBox.confirm('确认清空该职位？', '提示')
  if (target === 'president') {
    await clubApi.update(selectedClubId.value!, { presidentUserId: 0 })
    clubDetail.value.presidentUserId = null
    loadedPresident.value = null
    ElMessage.success('社长已清空')
  } else {
    await clubApi.update(selectedClubId.value!, { leagueSecretaryUserId: 0 })
    clubDetail.value.leagueSecretaryUserId = null
    loadedLeagueSecretary.value = null
    ElMessage.success('团支书已清空')
  }
}

async function removeVicePresident(userId: number) {
  await ElMessageBox.confirm('确认移除此副社长？', '提示')
  await clubApi.removeVicePresident(selectedClubId.value!, userId)
  ElMessage.success('副社长已移除')
  await loadVicePresidents()
}

async function clearDeptField(dept: any, field: 'manager' | 'viceManager') {
  const label = field === 'manager' ? '部长' : '副部长'
  await ElMessageBox.confirm(`确认清空「${dept.name}」的${label}？`, '提示')
  if (field === 'manager') {
    await departmentApi.update(dept.id, { managerUserId: 0 })
    dept.managerUserId = null
    dept._leaderUser = null
  } else {
    await departmentApi.update(dept.id, { viceManagerUserId: 0 })
    dept.viceManagerUserId = null
    dept._viceLeaderUser = null
  }
  ElMessage.success(`${label}已清空`)
}

async function saveGroupInfo() {
  groupSaving.value = true
  try {
    await clubApi.update(selectedClubId.value!, {
      wechatGroupQrcode: groupForm.value.wechatGroupQrcode || '',
      qqGroupNumber: groupForm.value.qqGroupNumber || '',
    })
    clubDetail.value.wechatGroupQrcode = groupForm.value.wechatGroupQrcode
    clubDetail.value.qqGroupNumber = groupForm.value.qqGroupNumber
    ElMessage.success('群信息已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    groupSaving.value = false
  }
}

function onWechatQrcodeUploaded(res: any) {
  const url = res?.url || res?.data?.url || res
  if (url && typeof url === 'string') {
    groupForm.value.wechatGroupQrcode = url
  }
}

async function onDeptQrcodeUploaded(row: any, res: any) {
  const url = res?.url || res?.data?.url || res
  if (url && typeof url === 'string') {
    row.wechatGroupQrcode = url
    await departmentApi.update(row.id, { wechatGroupQrcode: url })
    ElMessage.success('部门群二维码已更新')
  }
}

async function clearDeptQrcode(row: any) {
  await ElMessageBox.confirm(`确认清空「${row.name}」的群二维码？`, '提示')
  row.wechatGroupQrcode = ''
  await departmentApi.update(row.id, { wechatGroupQrcode: '' })
  ElMessage.success('已清空')
}

onMounted(() => {
  fetchClubs()
})
</script>

<style scoped>
.toolbar__label {
  font-size: 14px;
  color: #606266;
  margin-right: 8px;
}

.section-card {
  margin-bottom: 20px;
}

.section-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-card__title {
  font-size: 16px;
  font-weight: 600;
}

.section-card__count {
  font-size: 13px;
  color: #909399;
}

.leaders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

.leader-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.leader-item__label {
  flex-shrink: 0;
}

.leader-item__body {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.leader-item__info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.leader-item__name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.leader-item__username {
  font-size: 12px;
  color: #909399;
}

.leader-item__empty {
  font-size: 14px;
  color: #c0c4cc;
  margin-left: 8px;
}

.dept-leader {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dept-leader__name {
  font-size: 13px;
  font-weight: 500;
}

.dept-leader__username {
  font-size: 12px;
  color: #909399;
}

.user-picker {
  min-height: 200px;
}
</style>
