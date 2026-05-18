<template>
  <ViewPage class="workspace-page notifications-page">
    <WorkspaceHero
      eyebrow="通知中心"
      title="消息通知"
      description="及时获取社团公告、活动进展及申请反馈。"
      :metrics="workspaceMetrics"
    >
      <template #actions>
        <el-button v-if="unreadCount" :icon="CircleCheck" @click="markAllAsRead">
          全部标记为已读
        </el-button>
      </template>
    </WorkspaceHero>

    <section class="workspace-section">
      <div class="container workspace-grid workspace-grid--split notifications-shell">
        <WorkspacePanel
          class="notification-summary site-reveal"
          eyebrow="Inbox"
          title="收件箱状态"
          description="把待处理消息和系统通知放在同一张控制台卡片里。"
        >
          <div class="summary-console">
            <div class="summary-icon">
              <el-icon><Bell /></el-icon>
            </div>
            <strong>{{ unreadCount ? `${unreadCount} 条未读` : '全部已读' }}</strong>
            <p>{{ unreadCount ? '还有消息需要查看' : '当前没有待处理消息' }}</p>
          </div>

          <div class="workspace-subgrid">
            <div class="workspace-inline-stat">
              <span>已读消息</span>
              <strong>{{ readCount }}</strong>
            </div>
            <div class="workspace-inline-stat">
              <span>系统提醒</span>
              <strong>{{ systemCount }}</strong>
            </div>
          </div>
        </WorkspacePanel>

        <WorkspacePanel
          class="notification-console site-reveal"
          eyebrow="Feed"
          title="消息流"
          description="点击未读消息即可完成确认。"
        >
          <div v-loading="loading" class="notification-list">
            <div v-if="notifications.length === 0 && !loading" class="empty-state">
              <el-empty description="暂无新消息" />
            </div>

            <article
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              :class="{ 'is-unread': item.readFlag === 0 }"
              @click="handleRead(item)"
            >
              <div class="item-icon" :class="item.type || 'other'">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="item-body">
                <div class="item-header">
                  <span class="item-title">{{ item.title }}</span>
                  <span v-if="item.readFlag === 0" class="unread-pill">未读</span>
                </div>
                <div class="item-content">{{ item.content }}</div>
                <time class="item-time">{{ formatDateTime(item.createdAt) }}</time>
              </div>
            </article>
          </div>
        </WorkspacePanel>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import WorkspaceHero from '@/components/site/workspace/WorkspaceHero.vue'
import WorkspacePanel from '@/components/site/workspace/WorkspacePanel.vue'
import { computed, ref, onMounted } from 'vue'
import { Bell, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

const loading = ref(false)
const notifications = ref([])
const unreadCount = computed(() => notifications.value.filter((n: any) => n.readFlag === 0).length)
const readCount = computed(() => notifications.value.length - unreadCount.value)
const systemCount = computed(() => notifications.value.filter((n: any) => n.type === 'system').length)
const workspaceMetrics = computed(() => [
  { label: '全部消息', value: notifications.value.length, note: '累计通知' },
  { label: '未读消息', value: unreadCount.value, note: '需要处理' },
  { label: '已读消息', value: readCount.value, note: '已完成' },
  { label: '系统提醒', value: systemCount.value, note: '平台消息' },
])

const fetchNotifications = async () => {
  loading.value = true
  try {
    const res = await notificationApi.myNotifications()
    notifications.value = res || []
  } finally {
    loading.value = false
  }
}

const handleRead = async (item: any) => {
  if (item.readFlag === 1) return
  try {
    await notificationApi.markRead(item.id)
    item.readFlag = 1
  } catch (e) {}
}

const markAllAsRead = async () => {
  const unreadIds = notifications.value.filter((n) => n.readFlag === 0).map((n) => n.id)

  if (unreadIds.length === 0) return

  try {
    await Promise.all(unreadIds.map((id) => notificationApi.markRead(id)))
    notifications.value.forEach((n) => (n.readFlag = 1))
    ElMessage.success('全部已标记为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(fetchNotifications)
</script>

<style scoped>
.notification-item,
.empty-state {
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 24px;
}

.notification-summary {
  display: grid;
  align-content: start;
}

.summary-console {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
}

.summary-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 12px;
  color: #ffffff;
  background: #1d1d1f;
  font-size: 22px;
}

.summary-console strong {
  color: var(--oa-text);
  font-size: 26px;
  font-weight: 600;
}

.summary-console p {
  margin: 0;
  color: var(--oa-muted);
  font-size: 14px;
  line-height: 1.7;
}

.notification-console :deep(.workspace-panel__body) {
  padding-top: 22px;
}

.notification-list {
  display: grid;
  gap: 12px;
  min-height: 320px;
}

.notification-item {
  position: relative;
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 16px;
  padding: 20px;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
  cursor: pointer;
}

.notification-item:hover {
  border-color: var(--oa-text);
}

.notification-item.is-unread {
  background: var(--oa-elevated-bg);
  border-color: var(--oa-text);
}

.item-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 12px;
  flex-shrink: 0;
  font-size: 22px;
}

.item-icon.system {
  background: var(--oa-primary);
  color: #fff;
}
.item-icon.activity {
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
}
.item-icon.approval {
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
}
.item-icon.other {
  background: var(--oa-button-subtle-bg);
  color: var(--oa-muted);
}

.item-body {
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.item-title {
  font-weight: 600;
  font-size: 17px;
  line-height: 1.35;
  color: var(--oa-text);
}

.unread-pill {
  flex: 0 0 auto;
  padding: 5px 9px;
  border-radius: 999px;
  color: #ffffff;
  background: #1d1d1f;
  font-size: 12px;
  line-height: 1;
}

.item-content {
  font-size: 14px;
  color: var(--oa-muted);
  line-height: 1.7;
}

.item-time {
  display: inline-flex;
  width: fit-content;
  margin-top: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--oa-muted);
  background: var(--oa-page-soft-bg);
  font-size: 12px;
  line-height: 1;
}

.empty-state {
  padding: 72px 24px;
}

@media (max-width: 560px) {
  .notification-item {
    padding: 20px;
  }

  .notification-item {
    grid-template-columns: 44px minmax(0, 1fr);
    gap: 12px;
  }

  .item-icon {
    width: 44px;
    height: 44px;
  }

  .item-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
