<template>
  <main class="page">
    <section class="panel">
      <div class="panel__head">
        <h1>实验室后台</h1>
        <div class="toolbar">
          <button class="button" @click="generateProblem"><RefreshCcw :size="17" />生成今日题目</button>
          <button class="button secondary" @click="generateStudy"><CalendarPlus :size="17" />生成晚自习</button>
        </div>
      </div>
      <div class="panel__body metric-row">
        <div class="metric"><strong>{{ monitor?.pendingSubmissions ?? '-' }}</strong><span>待判题</span></div>
        <div class="metric"><strong>{{ monitor?.sandboxConfigured ? 'ON' : 'DEV' }}</strong><span>沙箱状态</span></div>
        <div class="metric"><strong>{{ monitor?.aiEnabled ? 'ON' : 'OFF' }}</strong><span>AI 出题</span></div>
      </div>
    </section>

    <div class="page-grid" style="margin-top: 16px">
      <section class="panel">
        <div class="panel__head">
          <h2>签到分组</h2>
          <Users :size="18" />
        </div>
        <div class="panel__body">
          <div class="toolbar">
            <input v-model="groupName" class="input" placeholder="组名" style="max-width: 160px" />
            <input v-model="selectedUsers" class="input" placeholder="成员ID，逗号分隔" />
            <button class="button dark" @click="saveGroup">保存分组</button>
          </div>
          <div class="list" style="margin-top: 12px">
            <article v-for="group in groups" :key="group.id" class="list-item">
              <strong>{{ group.name }}</strong>
              <span class="muted">{{ group.memberCount }} 人 · {{ group.userIds.join(', ') }}</span>
            </article>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head">
          <h2>签到场次</h2>
          <ClipboardList :size="18" />
        </div>
        <div class="panel__body">
          <div class="field"><label>标题</label><input v-model="sessionForm.title" class="input" /></div>
          <div class="field"><label>分组 ID</label><input v-model.number="sessionForm.groupId" class="input" /></div>
          <div class="field"><label>开始</label><input v-model="sessionForm.startAt" class="input" placeholder="2026-06-12 19:00:00" /></div>
          <div class="field"><label>结束</label><input v-model="sessionForm.endAt" class="input" placeholder="2026-06-12 21:30:00" /></div>
          <button class="button dark" style="margin-top: 10px" @click="saveSession">发布签到</button>
        </div>
      </section>
    </div>

    <div class="page-grid" style="margin-top: 16px">
      <section class="panel">
        <div class="panel__head"><h2>场次列表</h2><QrCode :size="18" /></div>
        <div class="panel__body list">
          <article v-for="item in checkins" :key="item.id" class="list-item">
            <strong>{{ item.title }}</strong>
            <span class="muted">{{ item.status }} · {{ item.signedCount }}/{{ item.targetCount }}</span>
            <code style="display:block; margin-top: 6px; word-break: break-all">{{ item.qrPayload }}</code>
          </article>
        </div>
      </section>

      <section class="panel">
        <div class="panel__head"><h2>信誉分榜</h2><Trophy :size="18" /></div>
        <div class="panel__body">
          <table class="table">
            <tbody>
              <tr v-for="row in board" :key="row.userId">
                <td>{{ row.nickname || row.username }}</td>
                <td>{{ row.reputationScore }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { CalendarPlus, ClipboardList, QrCode, RefreshCcw, Trophy, Users } from 'lucide-vue-next'
import { onMounted, reactive, ref } from 'vue'
import { generateTodayProblem, monitorStatus, scoreboard, type MonitorStatus, type ScoreboardRow } from '@/api/admin'
import { adminCheckIns, adminGroups, createCheckIn, createGroup, generateEveningStudy, type CheckInGroup, type CheckInSession } from '@/api/checkin'

const monitor = ref<MonitorStatus>()
const groups = ref<CheckInGroup[]>([])
const checkins = ref<CheckInSession[]>([])
const board = ref<ScoreboardRow[]>([])
const groupName = ref('算法训练一组')
const selectedUsers = ref('')
const sessionForm = reactive({
  title: '现场签到',
  groupId: undefined as number | undefined,
  startAt: '',
  endAt: '',
})

async function load() {
  ;[monitor.value, groups.value, checkins.value, board.value] = await Promise.all([
    monitorStatus(),
    adminGroups(),
    adminCheckIns(),
    scoreboard(),
  ])
}

async function generateProblem() {
  await generateTodayProblem()
  await load()
}

async function generateStudy() {
  await generateEveningStudy()
  await load()
}

async function saveGroup() {
  const userIds = selectedUsers.value.split(',').map((item) => Number(item.trim())).filter(Boolean)
  await createGroup({ name: groupName.value, userIds })
  selectedUsers.value = ''
  await load()
}

async function saveSession() {
  await createCheckIn({
    title: sessionForm.title,
    groupId: sessionForm.groupId,
    startAt: sessionForm.startAt,
    endAt: sessionForm.endAt,
    checkinPoints: 2,
  })
  await load()
}

onMounted(load)
</script>
