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
          description="默认显示内容摘要，点击查看完整通知。"
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
              role="button"
              tabindex="0"
              @click="openDetail(item)"
              @keydown.enter="openDetail(item)"
              @keydown.space.prevent="openDetail(item)"
            >
              <div class="item-icon" :class="item.type || 'other'">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="item-body">
                <div class="item-header">
                  <span class="item-title">{{ item.title }}</span>
                  <span v-if="item.readFlag === 0" class="unread-pill">未读</span>
                </div>
                <div class="item-content">
                  {{ markdownToPlainText(item.content) || '暂无正文内容' }}
                </div>
                <div class="item-footer">
                  <time class="item-time">{{ formatDateTime(item.createdAt) }}</time>
                  <span class="detail-link">
                    查看详情
                    <el-icon><ArrowRight /></el-icon>
                  </span>
                </div>
              </div>
            </article>
          </div>
        </WorkspacePanel>
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="通知详情" width="min(760px, 92vw)" append-to-body>
      <div v-if="currentNotification" class="notification-detail">
        <div class="detail-heading">
          <div>
            <span>{{ typeText(currentNotification.type) }}</span>
            <h2>{{ currentNotification.title }}</h2>
          </div>
          <time>{{ formatDateTime(currentNotification.createdAt) }}</time>
        </div>
        <el-divider />
        <MarkdownContent :content="currentNotification.content" />
      </div>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import ViewPage from '@/components/common/ViewPage.vue'
import WorkspaceHero from '@/components/site/workspace/WorkspaceHero.vue'
import WorkspacePanel from '@/components/site/workspace/WorkspacePanel.vue'
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Bell, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { notificationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { markdownToPlainText } from '@/utils/markdown.ts'

const loading = ref(false)
const detailVisible = ref(false)
const notifications = ref<any[]>([])
const currentNotification = ref<any>(null)
const unreadCount = computed(() => notifications.value.filter((n: any) => n.readFlag === 0).length)
const readCount = computed(() => notifications.value.length - unreadCount.value)
const systemCount = computed(
  () => notifications.value.filter((n: any) => n.type === 'system').length,
)
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

const typeText = (type: string) =>
  ({
    system: '系统通知',
    activity: '活动通知',
    approval: '审批通知',
    other: '其他通知',
  })[type] || '通知'

const openDetail = async (item: any) => {
  currentNotification.value = item
  detailVisible.value = true
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

.notification-item:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--oa-primary) 24%, transparent);
  outline-offset: 3px;
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
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 14px;
  color: var(--oa-muted);
  line-height: 1.7;
}

.item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.item-time {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--oa-muted);
  background: var(--oa-page-soft-bg);
  font-size: 12px;
  line-height: 1;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 600;
}

.notification-detail {
  min-height: 240px;
}

.detail-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.detail-heading span,
.detail-heading time {
  color: var(--oa-muted);
  font-size: 13px;
}

.detail-heading h2 {
  margin: 8px 0 0;
  color: var(--oa-text);
  font-size: 24px;
  line-height: 1.4;
}

.detail-heading time {
  flex: 0 0 auto;
  padding-top: 4px;
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

  .item-footer,
  .detail-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
