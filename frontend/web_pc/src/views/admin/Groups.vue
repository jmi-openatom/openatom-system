<template>
  <ViewPage class="admin-page groups-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-model="selectedClubId"
          filterable
          placeholder="选择社团"
          style="width: 220px"
          @change="refreshGroups"
        >
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          :prefix-icon="Search"
          placeholder="搜索分组名称、描述或群号"
          style="width: 280px"
        />
        <el-select v-model="typeFilter" placeholder="分组类型" style="width: 150px">
          <el-option label="全部类型" value="all" />
          <el-option
            v-for="item in availableTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
      <div class="toolbar__actions">
        <el-button :icon="Refresh" :loading="loading" @click="refreshGroups">刷新</el-button>
        <el-button
          type="primary"
          :icon="Plus"
          :disabled="!creatableTypes.length"
          @click="openCreatePage"
        >
          新建分组
        </el-button>
      </div>
    </ViewToolbar>

    <section class="summary-grid" aria-label="分组统计">
      <button
        class="summary-card summary-card--all"
        :class="{ 'is-active': typeFilter === 'all' }"
        type="button"
        @click="typeFilter = 'all'"
      >
        <span class="summary-card__icon"
          ><el-icon><Collection /></el-icon
        ></span>
        <span class="summary-card__copy"
          ><small>全部分组</small><strong>{{ groups.length }}</strong></span
        >
      </button>
      <button
        v-for="item in typeSummaries"
        :key="item.value"
        class="summary-card"
        :class="[`summary-card--${item.value}`, { 'is-active': typeFilter === item.value }]"
        type="button"
        @click="typeFilter = item.value"
      >
        <span class="summary-card__icon"
          ><el-icon><component :is="item.icon" /></el-icon
        ></span>
        <span class="summary-card__copy"
          ><small>{{ item.label }}</small
          ><strong>{{ item.count }}</strong></span
        >
      </button>
    </section>

    <section class="groups-workspace">
      <aside class="type-panel" aria-label="分组类型导航">
        <div class="type-panel__head">
          <span>分组目录</span>
          <small>{{ selectedClubName }}</small>
        </div>
        <button
          class="type-nav-item"
          :class="{ 'is-active': typeFilter === 'all' }"
          type="button"
          @click="typeFilter = 'all'"
        >
          <span
            ><el-icon><Collection /></el-icon>全部分组</span
          >
          <b>{{ groups.length }}</b>
        </button>
        <button
          v-for="item in typeSummaries"
          :key="item.value"
          class="type-nav-item"
          :class="{ 'is-active': typeFilter === item.value }"
          type="button"
          @click="typeFilter = item.value"
        >
          <span
            ><el-icon><component :is="item.icon" /></el-icon>{{ item.label }}</span
          >
          <b>{{ item.count }}</b>
        </button>
        <div class="type-panel__note">
          <el-icon><InfoFilled /></el-icon>
          <p>这里统一管理分组入口，签到规则、部门岗位和机器人配置仍在对应业务页维护。</p>
        </div>
      </aside>

      <div class="group-list-panel">
        <div class="list-head">
          <div>
            <h2>{{ activeTypeLabel }}</h2>
            <p>共 {{ filteredGroups.length }} 个结果</p>
          </div>
          <el-tag v-if="partialFailures.length" type="warning" effect="plain">
            {{ partialFailures.length }} 类数据暂不可用
          </el-tag>
        </div>

        <el-table
          v-loading="loading"
          :data="filteredGroups"
          class="admin-table groups-table"
          row-key="key"
          highlight-current-row
          @row-click="selectGroup"
        >
          <el-table-column label="分组" min-width="240">
            <template #default="{ row }">
              <div class="group-name-cell">
                <span class="group-type-icon" :class="`group-type-icon--${row.type}`">
                  <el-icon><component :is="typeMeta[row.type].icon" /></el-icon>
                </span>
                <div>
                  <strong>{{ row.name }}</strong>
                  <p>{{ row.description || typeMeta[row.type].hint }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="typeMeta[row.type].tagType" effect="light">{{
                typeMeta[row.type].label
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="成员" width="100" align="right">
            <template #default="{ row }">
              <span class="member-count">{{ row.memberCount ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="负责人 / 状态" min-width="170">
            <template #default="{ row }">
              <span>{{ row.owner || row.statusText || '未设置' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="关联业务" min-width="170">
            <template #default="{ row }">
              <span>{{ row.bindingText }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="112" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="manageGroup(row)">进入管理</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!loading && !filteredGroups.length"
          :description="keyword ? '没有匹配的分组' : '当前类型暂无分组'"
        >
          <el-button v-if="creatableTypes.length" type="primary" @click="openCreatePage"
            >新建分组</el-button
          >
        </el-empty>
      </div>

      <aside class="detail-panel" aria-label="分组详情">
        <template v-if="activeGroup">
          <div class="detail-panel__head">
            <span
              class="group-type-icon group-type-icon--large"
              :class="`group-type-icon--${activeGroup.type}`"
            >
              <el-icon><component :is="typeMeta[activeGroup.type].icon" /></el-icon>
            </span>
            <div>
              <el-tag :type="typeMeta[activeGroup.type].tagType" size="small" effect="plain">
                {{ typeMeta[activeGroup.type].label }}
              </el-tag>
              <h2>{{ activeGroup.name }}</h2>
            </div>
          </div>
          <p class="detail-description">
            {{ activeGroup.description || typeMeta[activeGroup.type].hint }}
          </p>
          <dl class="detail-stats">
            <div>
              <dt>成员数量</dt>
              <dd>{{ activeGroup.memberCount ?? '未同步' }}</dd>
            </div>
            <div>
              <dt>负责人</dt>
              <dd>{{ activeGroup.owner || '未设置' }}</dd>
            </div>
            <div>
              <dt>关联业务</dt>
              <dd>{{ activeGroup.bindingText }}</dd>
            </div>
            <div>
              <dt>来源标识</dt>
              <dd>{{ activeGroup.sourceId }}</dd>
            </div>
          </dl>
          <div v-if="activeMembers.length" class="member-preview">
            <div class="member-preview__head">
              <span>成员预览</span><small>最多显示 6 人</small>
            </div>
            <div v-for="member in activeMembers" :key="member.userId" class="member-row">
              <el-avatar :size="30">{{ member.name.charAt(0) }}</el-avatar>
              <div>
                <strong>{{ member.name }}</strong
                ><small>{{ member.secondary }}</small>
              </div>
            </div>
          </div>
          <el-button class="detail-manage-button" type="primary" @click="manageGroup(activeGroup)">
            进入{{ typeMeta[activeGroup.type].label }}管理
          </el-button>
        </template>
        <el-empty v-else description="选择一个分组查看详情" />
      </aside>
    </section>

  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { clubApi, unifiedGroupApi } from '@/api'
import { hasPermission } from '@/utils/permission.ts'
import {
  ChatDotRound,
  Collection,
  Grid,
  InfoFilled,
  Plus,
  Refresh,
  Search,
  Tickets,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, markRaw, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type GroupType = 'department' | 'checkin' | 'alumni' | 'external'
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

interface UnifiedGroupRow {
  key: string
  id: string | number
  legacyId: string | number
  sourceId: string
  type: GroupType
  name: string
  description: string
  memberCount: number | null
  owner: string
  statusText: string
  bindingText: string
  userIds: Array<string | number>
  raw: Record<string, any>
}

interface MemberPreview {
  userId: string | number
  name: string
  secondary: string
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const clubs = ref<any[]>([])
const activeMemberRows = ref<any[]>([])
const groups = ref<UnifiedGroupRow[]>([])
const activeGroup = ref<UnifiedGroupRow | null>(null)
const partialFailures = ref<string[]>([])
const selectedClubId = ref<string | number>('')
const keyword = ref('')
const typeFilter = ref<GroupType | 'all'>('all')

const typeMeta: Record<GroupType, { label: string; hint: string; icon: any; tagType: TagType }> = {
  department: {
    label: '组织部门',
    hint: '正式组织架构，可关联负责人、岗位和部门群',
    icon: markRaw(Grid),
    tagType: 'primary',
  },
  checkin: {
    label: '签到分组',
    hint: '用于晚自习、活动签到和应到人员管理',
    icon: markRaw(Tickets),
    tagType: 'success',
  },
  alumni: {
    label: '往届分组',
    hint: '用于归档和展示往届管理人员',
    icon: markRaw(UserFilled),
    tagType: 'warning',
  },
  external: {
    label: '外部群组',
    hint: '来自 QQ 等外部平台的群组及机器人配置',
    icon: markRaw(ChatDotRound),
    tagType: 'info',
  },
}

const selectedClubName = computed(() => {
  return clubs.value.find((item) => item.id === selectedClubId.value)?.name || '当前社团'
})

const availableTypes = computed(() => {
  return (Object.keys(typeMeta) as GroupType[])
    .filter((type) => groups.value.some((group) => group.type === type) || canReadType(type))
    .map((value) => ({ value, ...typeMeta[value] }))
})

const creatableTypes = computed(() => {
  const rows: Array<{ value: Exclude<GroupType, 'external'>; label: string }> = []
  if (!hasPermission('group:create')) return rows
  rows.push(
    { value: 'department', label: '组织部门' },
    { value: 'checkin', label: '签到分组' },
    { value: 'alumni', label: '往届分组' },
  )
  return rows
})

const typeSummaries = computed(() => {
  return availableTypes.value.map((item) => ({
    ...item,
    count: groups.value.filter((group) => group.type === item.value).length,
  }))
})

const activeTypeLabel = computed(() => {
  return typeFilter.value === 'all' ? '全部分组' : typeMeta[typeFilter.value].label
})

const filteredGroups = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return groups.value.filter((group) => {
    if (typeFilter.value !== 'all' && group.type !== typeFilter.value) return false
    if (!normalizedKeyword) return true
    return [group.name, group.description, group.sourceId, group.owner].some((value) =>
      String(value || '')
        .toLowerCase()
        .includes(normalizedKeyword),
    )
  })
})

const activeMembers = computed<MemberPreview[]>(() => {
  return activeMemberRows.value.slice(0, 6).map(toMemberPreview)
})

function canReadType(type: GroupType) {
  if (type === 'department') return hasPermission('department:list')
  if (type === 'checkin') return hasPermission('check-in:create')
  if (type === 'alumni') return hasPermission('membership:list')
  return hasPermission('bot-management:list')
}

function normalizeList(result: any): any[] {
  return result?.list || result || []
}

function toMemberPreview(member: any): MemberPreview {
  return {
    userId: member.userId,
    name: member.realName || member.userName || `用户 ${member.userId}`,
    secondary: member.memberRole || member.status || '社团成员',
  }
}

async function loadClubs() {
  const result = await clubApi.list({ page: 1, pageSize: 100 })
  clubs.value = normalizeList(result)
  if (!selectedClubId.value && clubs.value.length) {
    const defaultClub = clubs.value.find((item) => item.code === 'JMI-OPENATOM') || clubs.value[0]
    selectedClubId.value = defaultClub.id
  }
}

async function refreshGroups() {
  if (!selectedClubId.value) return
  loading.value = true
  partialFailures.value = []
  try {
    const rows = normalizeList(await unifiedGroupApi.list({ clubId: selectedClubId.value }))
    groups.value = rows.map(normalizeUnifiedGroup)

    if (!activeGroup.value || !groups.value.some((item) => item.key === activeGroup.value?.key)) {
      activeGroup.value = groups.value[0] || null
    } else {
      activeGroup.value = groups.value.find((item) => item.key === activeGroup.value?.key) || null
    }
    await loadActiveMembers()
  } catch {
    groups.value = []
    activeGroup.value = null
    activeMemberRows.value = []
    partialFailures.value.push('统一分组')
  } finally {
    loading.value = false
  }
}

function normalizeUnifiedGroup(row: any): UnifiedGroupRow {
  return {
    key: row.key || row.sourceId || `${row.sourceType}:${row.legacyId}`,
    id: row.id,
    legacyId: row.legacyId,
    sourceId: row.sourceId || `${row.sourceType}:${row.legacyId}`,
    type: row.type,
    name: row.name,
    description: row.description || '',
    memberCount: row.memberCount ?? null,
    owner: row.owner || '',
    statusText: row.statusText || row.status || '',
    bindingText: row.bindingText || '',
    userIds: row.userIds || [],
    raw: row,
  }
}

async function selectGroup(row: UnifiedGroupRow) {
  activeGroup.value = row
  await loadActiveMembers()
}

async function loadActiveMembers() {
  if (!activeGroup.value || activeGroup.value.type === 'external') {
    activeMemberRows.value = []
    return
  }
  activeMemberRows.value = normalizeList(await unifiedGroupApi.members(activeGroup.value.id))
}

function manageGroup(row: UnifiedGroupRow) {
  router.push(`/admin/groups/${row.id}`)
}

function openCreatePage() {
  router.push({
    path: '/admin/groups/create',
    query: {
      clubId: String(selectedClubId.value),
      type: typeFilter.value === 'all' ? undefined : typeFilter.value,
    },
  })
}

onMounted(async () => {
  const requestedType = String(route.query.type || '') as GroupType
  if (Object.hasOwn(typeMeta, requestedType)) typeFilter.value = requestedType
  loading.value = true
  try {
    await loadClubs()
    await refreshGroups()
    const sourceType = String(route.query.sourceType || '')
    const sourceId = String(route.query.sourceId || '')
    if (sourceType && sourceId) {
      const requestedGroup = groups.value.find(
        (item) => item.type === sourceType && String(item.legacyId) === sourceId,
      )
      if (requestedGroup && requestedGroup.type !== 'external') {
        await router.replace(`/admin/groups/${requestedGroup.id}`)
      }
    }
  } finally {
    loading.value = false
  }
})

</script>

<style scoped>
.groups-page {
  --group-blue: var(--el-color-primary);
  --group-green: var(--el-color-success);
  --group-amber: var(--el-color-warning);
  --group-slate: var(--el-color-info);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  min-height: 88px;
  align-items: center;
  gap: 14px;
  padding: 16px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.summary-card:hover,
.summary-card.is-active {
  border-color: color-mix(in srgb, var(--el-color-primary) 48%, var(--el-border-color));
  box-shadow: 0 8px 24px color-mix(in srgb, var(--el-color-primary) 9%, transparent);
  transform: translateY(-1px);
}

.summary-card:focus-visible,
.type-nav-item:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--el-color-primary) 35%, transparent);
  outline-offset: 2px;
}

.summary-card__icon,
.group-type-icon {
  display: inline-flex;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 10px;
  font-size: 20px;
}

.summary-card__copy {
  display: grid;
  gap: 3px;
}

.summary-card__copy small {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.summary-card__copy strong {
  font-size: 25px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.summary-card--checkin .summary-card__icon,
.group-type-icon--checkin {
  color: var(--group-green);
  background: var(--el-color-success-light-9);
}

.summary-card--alumni .summary-card__icon,
.group-type-icon--alumni {
  color: var(--group-amber);
  background: var(--el-color-warning-light-9);
}

.summary-card--external .summary-card__icon,
.group-type-icon--external {
  color: var(--group-slate);
  background: var(--el-color-info-light-9);
}

.groups-workspace {
  display: grid;
  grid-template-columns: 210px minmax(520px, 1fr) 280px;
  min-height: 600px;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
}

.type-panel,
.detail-panel {
  padding: 18px;
  background: color-mix(in srgb, var(--el-bg-color-page) 70%, var(--el-bg-color));
}

.type-panel {
  border-right: 1px solid var(--el-border-color-lighter);
}

.type-panel__head {
  display: grid;
  gap: 4px;
  padding: 2px 8px 14px;
}

.type-panel__head span {
  font-weight: 650;
}

.type-panel__head small,
.list-head p,
.group-name-cell p,
.member-row small {
  color: var(--el-text-color-secondary);
}

.type-nav-item {
  display: flex;
  width: 100%;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  padding: 0 10px;
  color: var(--el-text-color-regular);
  background: transparent;
  border: 0;
  border-radius: 9px;
  cursor: pointer;
}

.type-nav-item span {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.type-nav-item b {
  min-width: 24px;
  padding: 2px 7px;
  color: var(--el-text-color-secondary);
  text-align: center;
  background: var(--el-fill-color);
  border-radius: 999px;
  font-size: 12px;
}

.type-nav-item:hover,
.type-nav-item.is-active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.type-panel__note {
  display: flex;
  gap: 8px;
  margin-top: 22px;
  padding: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.6;
}

.type-panel__note p {
  margin: 0;
}

.group-list-panel {
  min-width: 0;
  padding: 20px;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.list-head h2,
.list-head p,
.detail-panel h2 {
  margin: 0;
}

.list-head h2,
.detail-panel h2 {
  font-size: 18px;
}

.list-head p {
  margin-top: 4px;
  font-size: 13px;
}

.groups-table :deep(.el-table__row) {
  cursor: pointer;
}

.group-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-name-cell strong {
  display: block;
  margin-bottom: 4px;
}

.group-name-cell p {
  max-width: 340px;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.member-count {
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.detail-panel {
  border-left: 1px solid var(--el-border-color-lighter);
}

.detail-panel__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-type-icon--large {
  width: 48px;
  height: 48px;
  border-radius: 13px;
  font-size: 23px;
}

.detail-panel__head h2 {
  margin-top: 5px;
}

.detail-description {
  margin: 18px 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
}

.detail-stats {
  display: grid;
  gap: 0;
  margin: 0;
}

.detail-stats div {
  display: flex;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.detail-stats dt {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.detail-stats dd {
  margin: 0;
  text-align: right;
  font-size: 13px;
}

.member-preview {
  margin-top: 20px;
}

.member-preview__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 9px;
  font-size: 13px;
  font-weight: 600;
}

.member-preview__head small {
  color: var(--el-text-color-secondary);
  font-weight: 400;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 0;
}

.member-row div {
  display: grid;
  gap: 2px;
}

.member-row strong {
  font-size: 13px;
}

.member-row small {
  font-size: 11px;
}

.detail-manage-button {
  width: 100%;
  min-height: 42px;
  margin-top: 22px;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .groups-workspace {
    grid-template-columns: 190px minmax(480px, 1fr);
  }

  .detail-panel {
    display: none;
  }
}

@media (max-width: 820px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .groups-workspace {
    display: block;
  }

  .type-panel {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .type-panel__head,
  .type-panel__note {
    display: none;
  }

  .type-nav-item {
    width: auto;
    flex: 0 0 auto;
    margin: 0;
  }
}
</style>
