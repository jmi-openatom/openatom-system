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
                <el-button :icon="Refresh" type="primary" @click="fetchList">刷新</el-button>
            </div>

            <div class="toolbar__actions">
                <el-date-picker
                    v-model="eveningDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="晚自习日期"
                    style="width: 150px"
                />
                <el-button :icon="Calendar" type="success" @click="generateEveningStudy">
                    生成晚自习
                </el-button>
                <el-button :icon="Setting" @click="openEveningManager">晚自习计划</el-button>
                <el-button @click="$router.push({ path: '/admin/groups', query: { type: 'checkin' } })">签到分组</el-button>
                <el-button :icon="Plus" type="primary" @click="openDialog">发布签到</el-button>
            </div>
        </ViewToolbar>

        <el-table v-loading="loading" :data="rows" class="admin-table">
            <el-table-column label="签到" min-width="220">
                <template #default="{ row }">
                    <strong>{{ row.title }}</strong>
                    <p class="muted-line">
                        {{ row.activityTitle || typeText(row.sessionType) }} / {{ row.groupName || '未绑定分组' }} /
                        {{ row.location || '未设置地点' }}
                    </p>
                </template>
            </el-table-column>

            <el-table-column label="时间" min-width="230">
                <template #default="{ row }">
                    {{ formatRange(row.startAt, row.endAt) }}
                </template>
            </el-table-column>

            <el-table-column label="进度" width="150">
                <template #default="{ row }">
                    已签 {{ row.signedCount || 0 }} / {{ row.targetCount || 0 }}
                    <span v-if="row.lateCount" class="muted-inline">
                        迟到 {{ row.lateCount }}
                    </span>
                    <span v-if="row.absentCount" class="muted-inline">
                        旷课 {{ row.absentCount }}
                    </span>
                    <span v-if="row.excusedCount" class="muted-inline">
                        请假 {{ row.excusedCount }}
                    </span>
                </template>
            </el-table-column>

            <el-table-column label="签到积分" width="110">
                <template #default="{ row }">
                    {{ row.checkinPoints || 0 }}
                </template>
            </el-table-column>

            <el-table-column label="状态" width="110">
                <template #default="{ row }">
                    <el-tag :type="statusType(row.status)">
                        {{ statusText(row.status) }}
                    </el-tag>
                </template>
            </el-table-column>

            <el-table-column fixed="right" label="操作" width="340">
                <template #default="{ row }">
                    <el-button link type="primary" @click="openPreview(row)">全屏预览</el-button>
                    <el-button link type="success" @click="openRecords(row)">记录</el-button>
                    <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                    <el-button
                        v-if="row.status === 'open'"
                        link
                        type="danger"
                        @click="closeSession(row)"
                    >
                        关闭
                    </el-button>
                    <el-button link type="danger" @click="deleteSession(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-empty v-if="!loading && !rows.length" description="暂无签到" />

        <el-dialog
            v-model="dialogVisible"
            :title="editingSessionId ? '编辑内部签到' : '发布内部签到'"
            width="920px"
        >
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
                        <el-input-number v-model="form.checkinPoints" :min="0" :max="POINT_AMOUNT_MAX" :step="1" />
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
                    ref="memberTableRef"
                    :data="filteredMembers"
                    height="300"
                    row-key="id"
                    @selection-change="handleMemberSelection"
                >
                    <el-table-column type="selection" width="48" />
                    <el-table-column label="姓名" min-width="140" prop="realName" />
                    <el-table-column label="用户名" min-width="140" prop="userName" />
                    <el-table-column label="学号" min-width="140" prop="studentId" />
                    <el-table-column label="班级" min-width="130" prop="className" />
                    <el-table-column label="状态" prop="userStatus" width="110">
                        <template #default="{ row }">
                            <el-tag :type="statusType(row.userStatus)">
                                {{ userStatusText(row.userStatus) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-form>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button :loading="saving" type="primary" @click="save">
                    {{ editingSessionId ? '保存' : '发布' }}
                </el-button>
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
                <el-button :loading="groupSaving" type="primary" @click="saveGroup">
                    {{ groupForm.id ? '更新分组' : '创建分组' }}
                </el-button>
                <el-button v-if="groupForm.id" @click="resetGroupForm">新建</el-button>
            </div>

            <el-table
                ref="groupUserTableRef"
                :data="filteredGroupUsers"
                height="280"
                row-key="id"
                @selection-change="handleGroupSelection"
            >
                <el-table-column type="selection" width="48" />
                <el-table-column label="姓名" min-width="140" prop="realName" />
                <el-table-column label="用户名" min-width="140" prop="userName" />
                <el-table-column label="学号" min-width="140" prop="studentId" />
                <el-table-column label="班级" min-width="130" prop="className" />
            </el-table>

            <template v-if="groupForm.id">
                <el-divider content-position="left">当前组内成员</el-divider>
                <el-table :data="selectedGroupMembers" height="180">
                    <el-table-column label="姓名" min-width="140" prop="realName" />
                    <el-table-column label="学号" min-width="140" prop="studentId" />
                    <el-table-column label="班级" min-width="130" prop="className" />
                    <el-table-column label="操作" width="120">
                        <template #default="{ row }">
                            <el-button link type="danger" @click="removeGroupMember(row)">移出</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </template>

            <el-divider content-position="left">已有分组</el-divider>

            <el-table :data="groups" height="260">
                <el-table-column label="分组名称" min-width="180" prop="name" />
                <el-table-column label="人数" prop="memberCount" width="100" />
                <el-table-column label="操作" width="180">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="editGroup(row)">编辑</el-button>
                        <el-button link type="danger" @click="deleteGroup(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>

        <el-dialog v-model="eveningVisible" title="晚自习计划" width="1080px">
            <div class="evening-form">
                <el-select v-model="eveningForm.groupId" filterable placeholder="实验室分组">
                    <el-option
                        v-for="item in groups"
                        :key="item.id"
                        :label="`${item.name}（${item.memberCount || 0}人）`"
                        :value="item.id"
                    />
                </el-select>
                <el-input v-model="eveningForm.title" placeholder="计划标题" />
                <el-input v-model="eveningForm.location" placeholder="地点" />
                <el-input v-model="eveningForm.startTime" type="time" />
                <el-input v-model="eveningForm.endTime" type="time" />
                <el-input-number v-model="eveningForm.checkinPoints" :min="0" :max="POINT_AMOUNT_MAX" :step="1" placeholder="签到积分" />
                <el-input-number v-model="eveningForm.checkinWindowMinutes" :min="1" :max="1440" :step="5" placeholder="签到窗口(分钟)" />
                <el-input-number v-model="eveningForm.lateAfterMinutes" :min="0" :max="1439" :step="5" placeholder="迟到阈值(分钟)" />
                <el-input-number v-model="eveningForm.latePenaltyPoints" :min="0" :max="POINT_AMOUNT_MAX" :step="1" placeholder="迟到扣分" />
                <el-input-number v-model="eveningForm.absentPenaltyPoints" :min="0" :max="POINT_AMOUNT_MAX" :step="1" placeholder="旷课扣分" />
                <el-switch v-model="eveningForm.enabled" active-text="启用" inactive-text="停用" />
                <el-button :loading="eveningSaving" type="primary" @click="saveEveningSchedule">
                    {{ eveningForm.id ? '更新' : '创建' }}
                </el-button>
                <el-button v-if="eveningForm.id" @click="resetEveningForm">新建</el-button>
            </div>

            <el-table v-loading="eveningLoading" :data="eveningSchedules" height="420">
                <el-table-column label="实验室分组" min-width="160" prop="groupName" />
                <el-table-column label="计划" min-width="170">
                    <template #default="{ row }">
                        <strong>{{ row.title }}</strong>
                        <p class="muted-line">{{ row.location || '未设置地点' }}</p>
                    </template>
                </el-table-column>
                <el-table-column label="时间" width="150">
                    <template #default="{ row }">
                        {{ row.startTime }} - {{ row.endTime }}
                    </template>
                </el-table-column>
                <el-table-column label="签到规则" width="190">
                    <template #default="{ row }">
                        {{ row.checkinWindowMinutes || 30 }} 分钟，{{ row.lateAfterMinutes || 10 }} 分钟后迟到
                        <p class="muted-line">
                            迟到 -{{ row.latePenaltyPoints ?? 1 }}，旷课 -{{ row.absentPenaltyPoints ?? 2 }}
                        </p>
                    </template>
                </el-table-column>
                <el-table-column label="今日进度" width="220">
                    <template #default="{ row }">
                        已签 {{ row.todaySignedCount || 0 }} / {{ row.todayTargetCount || 0 }}
                        <span v-if="row.todayLateCount" class="muted-inline">
                            迟到 {{ row.todayLateCount }}
                        </span>
                        <span v-if="row.todayAbsentCount" class="muted-inline">
                            旷课 {{ row.todayAbsentCount }}
                        </span>
                        <span v-if="row.todayExcusedCount" class="muted-inline">
                            请假 {{ row.todayExcusedCount }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="积分" width="90" prop="checkinPoints" />
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.enabled ? 'success' : 'info'">
                            {{ row.enabled ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="150">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="editEveningSchedule(row)">编辑</el-button>
                        <el-button link type="danger" @click="deleteEveningSchedule(row)">删除</el-button>
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
                            <p v-if="previewRow.sessionType === 'evening_study'" class="preview-count">
                                签到截止 {{ formatDateTime(previewRow.checkinDeadlineAt) || '-' }}，
                                迟到扣 {{ previewRow.latePenaltyPoints ?? 1 }} 分，
                                旷课扣 {{ previewRow.absentPenaltyPoints ?? 2 }} 分
                            </p>

                            <div class="qr-box">
                                <img
                                    v-if="previewRow.qrPayload"
                                    :src="qrUrl(checkInUrl(previewRow.qrPayload))"
                                    alt="签到二维码"
                                    class="qr-image"
                                />

                                <div v-else class="qr-empty">
                                    二维码生成中
                                </div>
                            </div>

                            <p class="preview-count">
                                已签到 {{ signedRecords.length }} /
                                {{ activePreviewRecords.length || previewRow.targetCount || 0 }}
                            </p>
                        </section>

                        <section class="preview-roster">
                            <div class="roster-column roster-column--pending">
                                <div class="roster-head">
                                    <span>未签到/旷课</span>
                                    <strong>{{ pendingRecords.length }}</strong>
                                </div>

                                <div class="roster-scroll">
                                    <div
                                        v-for="item in pendingRecords"
                                        :key="`pending-${item.userId}`"
                                        class="roster-item pending-item"
                                    >
                                        <span class="roster-name">{{ displayRecordName(item) }}</span>
                                        <span class="roster-status">{{ recordStatusText(item.status) }}</span>
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
                                    <strong>{{ signedRecords.length }}</strong>
                                </div>

                                <div class="roster-scroll">
                                    <div
                                        v-for="item in signedRecords"
                                        :key="`checked-${item.userId}`"
                                        :class="['roster-item', item.status === 'late' ? 'late-item' : 'checked-item']"
                                    >
                                        <span class="roster-name">{{ displayRecordName(item) }}</span>
                                        <span class="roster-status">{{ recordStatusText(item.status) }}</span>
                                    </div>

                                    <el-empty
                                    v-if="!signedRecords.length"
                                        :image-size="72"
                                        description="暂无签到"
                                    />
                                </div>
                            </div>

                            <div class="roster-column roster-column--excused">
                                <div class="roster-head">
                                    <span>请假剔除</span>
                                    <strong>{{ excusedRecords.length }}</strong>
                                </div>

                                <div class="roster-scroll">
                                    <div
                                        v-for="item in excusedRecords"
                                        :key="`excused-${item.userId}`"
                                        class="roster-item excused-item"
                                    >
                                        <span class="roster-name">{{ displayRecordName(item) }}</span>
                                        <span class="roster-status">已请假</span>
                                    </div>

                                    <el-empty
                                        v-if="!excusedRecords.length"
                                        :image-size="72"
                                        description="暂无请假"
                                    />
                                </div>
                            </div>
                        </section>
                    </div>
                </div>
            </div>
        </Teleport>

        <el-dialog v-model="recordsVisible" title="签到记录" width="980px">
            <div v-if="recordsRow" class="member-toolbar">
                <el-select
                    v-model="appendUserIds"
                    filterable
                    multiple
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
                <el-table-column label="姓名" min-width="120" prop="realName" />
                <el-table-column label="学号" min-width="140" prop="studentId" />
                <el-table-column label="手机号" min-width="140" prop="phone" />
                <el-table-column label="状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="recordStatusType(row.status)">
                            {{ recordStatusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="签到时间" min-width="180">
                    <template #default="{ row }">
                        {{ formatDateTime(row.checkinAt) || '-' }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                    <template #default="{ row }">
                        <el-dropdown
                            v-if="row.status !== 'excused'"
                            trigger="click"
                            @command="(status) => changeRecordStatus(row, status)"
                        >
                            <el-button link type="primary">改状态</el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item command="checked">正常签到</el-dropdown-item>
                                    <el-dropdown-item command="late">迟到</el-dropdown-item>
                                    <el-dropdown-item command="absent">旷课</el-dropdown-item>
                                    <el-dropdown-item command="pending">未签到</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                        <span v-else class="muted-line">{{ row.exclusionReason || '已通过请假' }}</span>
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>
    </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import {Calendar, Plus, Refresh, Setting} from '@element-plus/icons-vue'
import {activityApi, checkInApi} from '@/api'
import {formatDateTime, statusType} from '@/utils/format.ts'
import {qrSvgDataUrl} from '@/utils/qr.ts'
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'

const POINT_AMOUNT_MAX = 9_000_000_000_000_000
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingSessionId = ref<any>(null)
const groupVisible = ref(false)
const eveningVisible = ref(false)
const previewVisible = ref(false)
const recordsVisible = ref(false)
const eveningLoading = ref(false)
const eveningSaving = ref(false)

const rows = ref<any[]>([])
const records = ref<any[]>([])
const recordsRow = ref<any>(null)
const previewRecords = ref<any[]>([])
const activities = ref<any[]>([])
const groups = ref<any[]>([])
const eveningSchedules = ref<any[]>([])
const members = ref<any[]>([])
const appendUserIds = ref<any[]>([])
const liveTimer = ref<any>(null)

const memberKeyword = ref('')
const groupKeyword = ref('')
const previewRow = ref<any>(null)
const query = ref({ status: '' })
const eveningDate = ref(todayString())

const form = ref({
    title: '',
    status: 'open',
    startAt: '',
    endAt: '',
    location: '',
    activityId: undefined,
    groupId: undefined,
    checkinPoints: 0,
    targetUserIds: [] as any[],
})

const groupForm = ref({
    id: null as any,
    name: '',
    userIds: [] as any[],
})

const eveningForm = ref({
    id: null as any,
    groupId: undefined as any,
    title: '晚自习签到',
    location: '实验室',
    startTime: '19:00',
    endTime: '21:30',
    checkinPoints: 0,
    checkinWindowMinutes: 30,
    lateAfterMinutes: 10,
    latePenaltyPoints: 1,
    absentPenaltyPoints: 2,
    enabled: true,
})

const groupSaving = ref(false)

const rules = ref({
    title: [{ required: true, message: '请输入签到标题', trigger: 'blur' }],
})

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

const signedRecords = computed(() => {
    return previewRecords.value.filter((item) => ['checked', 'late'].includes(item.status))
})

const excusedRecords = computed(() => {
    return previewRecords.value.filter((item) => item.status === 'excused')
})

const activePreviewRecords = computed(() => {
    return previewRecords.value.filter((item) => item.status !== 'excused')
})

const pendingRecords = computed(() => {
    return previewRecords.value.filter((item) => !['checked', 'late', 'excused'].includes(item.status))
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

function typeText(type: any) {
    return type === 'evening_study' ? '晚自习签到' : '独立签到'
}

function userStatusText(status: any) {
    return { active: '启用', disabled: '禁用', locked: '锁定' }[status] || status || '-'
}

function recordStatusText(status: any) {
    return { checked: '正常签到', late: '迟到', absent: '旷课', pending: '未签到', excused: '已请假' }[status] || status || '-'
}

function recordStatusType(status: any) {
    return { checked: 'success', late: 'warning', absent: 'danger', pending: 'info', excused: 'warning' }[status] || 'info'
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

async function loadEveningSchedules() {
    eveningLoading.value = true

    try {
        eveningSchedules.value = (await checkInApi.eveningSchedules()) || []
    } finally {
        eveningLoading.value = false
    }
}

function openEveningManager() {
    resetEveningForm()
    eveningVisible.value = true
    loadEveningSchedules()
}

function resetEveningForm() {
    eveningForm.value = {
        id: null,
        groupId: undefined,
        title: '晚自习签到',
        location: '实验室',
        startTime: '19:00',
        endTime: '21:30',
        checkinPoints: 0,
        checkinWindowMinutes: 30,
        lateAfterMinutes: 10,
        latePenaltyPoints: 1,
        absentPenaltyPoints: 2,
        enabled: true,
    }
}

function editEveningSchedule(row: any) {
    eveningForm.value = {
        id: row.id,
        groupId: row.groupId,
        title: row.title || '晚自习签到',
        location: row.location || '',
        startTime: normalizeTimeValue(row.startTime) || '19:00',
        endTime: normalizeTimeValue(row.endTime) || '21:30',
        checkinPoints: row.checkinPoints || 0,
        checkinWindowMinutes: row.checkinWindowMinutes || 30,
        lateAfterMinutes: row.lateAfterMinutes ?? 10,
        latePenaltyPoints: row.latePenaltyPoints ?? 1,
        absentPenaltyPoints: row.absentPenaltyPoints ?? 2,
        enabled: row.enabled !== false,
    }
}

async function saveEveningSchedule() {
    if (!eveningForm.value.groupId) {
        ElMessage.error('请选择实验室分组')
        return
    }

    if (!eveningForm.value.title.trim()) {
        ElMessage.error('请输入计划标题')
        return
    }

    if ((eveningForm.value.lateAfterMinutes ?? 10) >= (eveningForm.value.checkinWindowMinutes || 30)) {
        ElMessage.error('迟到阈值必须小于签到窗口')
        return
    }

    eveningSaving.value = true

    try {
        const payload = {
            ...eveningForm.value,
            startTime: normalizeTimeValue(eveningForm.value.startTime),
            endTime: normalizeTimeValue(eveningForm.value.endTime),
        }

        if (eveningForm.value.id) {
            await checkInApi.updateEveningSchedule(eveningForm.value.id, payload)
            ElMessage.success('晚自习计划已更新')
        } else {
            await checkInApi.createEveningSchedule(payload)
            ElMessage.success('晚自习计划已创建')
        }

        resetEveningForm()
        await loadEveningSchedules()
        await fetchList()
    } finally {
        eveningSaving.value = false
    }
}

async function deleteEveningSchedule(row: any) {
    await ElMessageBox.confirm(`确定删除“${row.groupName || row.title}”的晚自习计划吗？`, '删除计划', {
        type: 'warning',
    })

    await checkInApi.deleteEveningSchedule(row.id)
    ElMessage.success('晚自习计划已删除')
    await loadEveningSchedules()
}

async function generateEveningStudy() {
    await checkInApi.generateEveningStudy({ date: eveningDate.value || todayString() })
    ElMessage.success('晚自习签到已生成')
    await fetchList()
    if (eveningVisible.value) {
        await loadEveningSchedules()
    }
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

function handleMemberSelection(selection: any[]) {
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

function handleGroupSelection(selection: any[]) {
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
    groupForm.value = {
        id: row.id,
        name: row.name,
        userIds: [...(row.userIds || [])],
    }

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
        const payload = {
            name: groupForm.value.name.trim(),
            userIds: groupForm.value.userIds,
        }

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
    await ElMessageBox.confirm(`确定删除分组“${row.name}”吗？`, '删除分组', {
        type: 'warning',
    })

    await checkInApi.deleteGroup(row.id)
    ElMessage.success('分组已删除')

    groups.value = (await checkInApi.groups()) || []

    if (groupForm.value.id === row.id) {
        resetGroupForm()
    }
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

        filteredMembers.value.forEach((row) => {
            memberTableRef.value?.toggleRowSelection(row, selected.has(row.id))
        })

        if (version === memberSelectionSyncVersion) {
            syncingMemberSelection = false
        }
    })
}

function syncGroupTableSelection(ids: any[]) {
    const selected = new Set(ids)
    const version = ++groupSelectionSyncVersion

    syncingGroupSelection = true

    nextTick(() => {
        groupUserTableRef.value?.clearSelection()

        filteredGroupUsers.value.forEach((row) => {
            groupUserTableRef.value?.toggleRowSelection(row, selected.has(row.id))
        })

        if (version === groupSelectionSyncVersion) {
            syncingGroupSelection = false
        }
    })
}

function save() {
    formRef.value.validate(async (valid: boolean) => {
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

    if (Number.isNaN(date.getTime())) {
        return String(value).slice(0, 16)
    }

    const offsetMs = date.getTimezoneOffset() * 60 * 1000
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function todayString() {
    const date = new Date()
    const offsetMs = date.getTimezoneOffset() * 60 * 1000
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 10)
}

function normalizeTimeValue(value: any) {
    if (!value) return ''
    return String(value).slice(0, 5)
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
    return `${row.realName || row.userName || row.studentId || row.id}${
        row.studentId ? ` / ${row.studentId}` : ''
    }`
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
        {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        },
    )

    await checkInApi.delete(row.id)
    ElMessage.success('签到已删除')

    if (previewRow.value?.id === row.id) {
        previewVisible.value = false
    }

    fetchList()
}

async function addTargetsToSession() {
    if (!recordsRow.value || !appendUserIds.value.length) return

    await checkInApi.addTargets(recordsRow.value.id, {
        userIds: appendUserIds.value,
    })

    ElMessage.success('人员已添加')

    appendUserIds.value = []
    records.value = await checkInApi.records(recordsRow.value.id)
    fetchList()
}

async function changeRecordStatus(row: any, status: any) {
    if (!recordsRow.value) return

    await checkInApi.updateRecordStatus(recordsRow.value.id, row.userId, {
        status,
    })

    ElMessage.success(`已改为${recordStatusText(status)}`)

    records.value = await checkInApi.records(recordsRow.value.id)

    if (previewVisible.value) {
        previewRecords.value = records.value
    }

    fetchList()
}

function startLiveRefresh(sessionId: any) {
    stopLiveRefresh()

    const refresh = async () => {
        if (!previewVisible.value && !recordsVisible.value) {
            stopLiveRefresh()
            return
        }

        const [detailResult, recordResult] = await Promise.all([
            previewVisible.value ? checkInApi.detail(sessionId) : Promise.resolve(null),
            previewVisible.value || recordsVisible.value
                ? checkInApi.records(sessionId)
                : Promise.resolve(null),
        ])

        if (detailResult) {
            previewRow.value = detailResult
        }

        if (recordResult && previewVisible.value) {
            previewRecords.value = recordResult
        }

        if (recordResult && recordsVisible.value) {
            records.value = recordResult
        }
    }

    refresh()
    liveTimer.value = window.setInterval(refresh, 3000)
}

function stopLiveRefresh() {
    if (liveTimer.value) {
        window.clearInterval(liveTimer.value)
        liveTimer.value = null
    }
}

onMounted(async () => {
    fetchList()
    await loadOptions()
    if (route.query.groupManager === '1') {
        await router.replace({
            path: '/admin/groups',
            query: { type: 'checkin', sourceType: 'checkin', sourceId: route.query.groupId },
        })
    }
})

watch(filteredMembers, () => {
    syncMemberTableSelection(form.value.targetUserIds)
})

watch(filteredGroupUsers, () => {
    syncGroupTableSelection(groupForm.value.userIds)
})

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

.muted-inline {
    display: inline-flex;
    margin-left: 8px;
    color: var(--oa-muted);
    font-size: 12px;
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

.evening-form {
    display: grid;
    grid-template-columns: repeat(5, minmax(130px, 1fr));
    gap: 10px;
    align-items: center;
    margin-bottom: 14px;
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
    grid-template-columns: minmax(360px, 0.75fr) minmax(680px, 1.25fr);
    align-items: stretch;
    gap: 20px;
    height: 100vh;
    height: 100dvh;
    padding: 4vh 32px;
    box-sizing: border-box;
    overflow: hidden;
}

.preview-qr {
    display: grid;
    align-content: center;
    justify-items: center;
    gap: 16px;
    min-height: 0;
    text-align: center;
}

.preview-kicker {
    margin: 0;
    color: var(--oa-text);
    font-size: 22px;
    font-weight: 600;
}

.preview-content h1 {
    margin: 0;
    font-size: clamp(42px, 7vw, 86px);
    line-height: 1.08;
}

.preview-meta,
.preview-count {
    margin: 0;
    color: var(--oa-muted);
    font-size: 20px;
}

.qr-box {
    display: grid;
    place-items: center;
    width: min(48vh, 420px);
    height: min(48vh, 420px);
    padding: 18px;
    box-sizing: border-box;
    border-radius: 12px;
    background: #fff;
}

.qr-image {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.qr-empty {
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    border: 1px dashed var(--oa-border);
    border-radius: 8px;
    color: var(--oa-muted);
    font-size: 18px;
}

.preview-roster {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
    min-width: 0;
    min-height: 0;
    height: 100%;
    overflow: hidden;
}

.roster-column {
    display: flex;
    min-width: 0;
    min-height: 0;
    height: 100%;
    flex-direction: column;
    overflow: hidden;
    border: 1px solid rgba(224, 224, 224, 0.95);
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.9);
}

.roster-column--pending {
    border-color: rgba(224, 224, 224, 0.5);
}

.roster-column--checked {
    border-color: rgba(224, 224, 224, 0.5);
}

.roster-head {
    flex: 0 0 auto;
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

.roster-scroll {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 16px;
    box-sizing: border-box;
}

.roster-scroll::-webkit-scrollbar {
    width: 8px;
}

.roster-scroll::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: rgba(0, 0, 0, 0.18);
}

.roster-scroll::-webkit-scrollbar-track {
    background: transparent;
}

.roster-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-width: 0;
    margin-bottom: 10px;
    padding: 12px 14px;
    border-radius: 12px;
    background: var(--oa-page-soft-bg);
    color: var(--oa-text);
    font-size: 20px;
    font-weight: 600;
}

.roster-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.roster-status {
    flex: 0 0 auto;
    font-size: 13px;
    font-weight: 500;
    color: var(--oa-muted);
}

.checked-item {
    background: rgba(103, 194, 58, 0.12);
}

.pending-item {
    background: rgba(144, 147, 153, 0.12);
}

.excused-item {
    background: rgba(230, 162, 60, 0.14);
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

    .evening-form {
        grid-template-columns: 1fr;
    }

    .preview-top {
        top: 10px;
        right: 10px;
        padding: 0;
    }

    .preview-content {
        display: flex;
        flex-direction: column;
        height: 100dvh;
        overflow-y: auto;
        padding: 72px 16px 24px;
    }

    .preview-qr {
        flex: 0 0 auto;
    }

    .preview-roster {
        flex: 1;
        display: grid;
        grid-template-columns: 1fr;
        min-height: 520px;
        overflow: visible;
    }

    .roster-column {
        min-height: 260px;
    }

    .preview-kicker,
    .preview-meta,
    .preview-count {
        font-size: 15px;
    }

    .qr-box {
        width: min(82vw, 360px);
        height: min(82vw, 360px);
    }

    .roster-head,
    .roster-item {
        font-size: 16px;
    }
}
</style>
