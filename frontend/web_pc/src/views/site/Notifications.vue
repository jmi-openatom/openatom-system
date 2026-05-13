<template>
  <div class="notifications-page">
    <section class="notifications-hero">
      <div class="notifications-hero__copy">
        <span>通知中心</span>
        <h1>消息通知</h1>
        <p>及时获取社团公告、活动进展及申请反馈。</p>
      </div>
      <div class="notifications-hero__stats">
        <div>
          <strong>{{ notifications.length }}</strong>
          <span>全部消息</span>
        </div>
        <div>
          <strong>{{ unreadCount }}</strong>
          <span>未读消息</span>
        </div>
      </div>
    </section>

    <section class="notifications-workspace">
      <aside class="notification-summary">
        <div class="summary-icon">
          <el-icon><Bell /></el-icon>
        </div>
        <h2>收件箱</h2>
        <p>{{ unreadCount ? `还有 ${unreadCount} 条消息需要查看` : '当前所有消息均已读' }}</p>
        <el-button
          v-if="unreadCount"
          type="primary"
          :icon="CircleCheck"
          @click="markAllAsRead"
        >
          全部标记为已读
        </el-button>
      </aside>

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
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Bell, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

const loading = ref(false)
const notifications = ref([])
const unreadCount = computed(() => notifications.value.filter((n: any) => n.readFlag === 0).length)

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
.notifications-page {
  min-height: calc(100vh - 58px);
  padding: 72px max(24px, calc((100vw - 1180px) / 2)) 96px;
  background: #f5f5f7;
}

.notifications-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
  animation: oaFadeUp 0.42s ease both;
}

.notifications-hero__copy,
.notifications-hero__stats,
.notification-summary,
.notification-item,
.empty-state {
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 18px;
}

.notifications-hero__copy {
  flex: 1 1 auto;
  min-height: 220px;
  padding: 48px;
}

.notifications-hero__copy span {
  display: block;
  color: #7a7a7a;
  font-size: 14px;
  line-height: 1;
  margin-bottom: 14px;
}

.notifications-hero__copy h1 {
  margin: 0 0 8px;
  font-family: 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 48px;
  font-weight: 600;
  line-height: 1.1;
  color: #1d1d1f;
}

.notifications-hero__copy p {
  margin: 0;
  color: #7a7a7a;
  font-size: 18px;
  line-height: 1.7;
}

.notifications-hero__stats {
  display: grid;
  flex: 0 0 280px;
  grid-template-columns: 1fr;
  overflow: hidden;
}

.notifications-hero__stats div {
  display: grid;
  align-content: center;
  gap: 8px;
  min-height: 110px;
  padding: 26px;
}

.notifications-hero__stats div + div {
  border-top: 1px solid #e0e0e0;
}

.notifications-hero__stats strong {
  color: #1d1d1f;
  font-family: 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 46px;
  font-weight: 600;
  line-height: 1;
}

.notifications-hero__stats span {
  color: #7a7a7a;
  font-size: 14px;
}

.notifications-workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.notification-summary {
  position: sticky;
  top: 82px;
  display: grid;
  gap: 14px;
  padding: 24px;
  animation: oaFadeUp 0.5s ease both;
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

.notification-summary h2 {
  margin: 6px 0 0;
  color: #1d1d1f;
  font-size: 24px;
  font-weight: 600;
}

.notification-summary p {
  margin: 0;
  color: #7a7a7a;
  font-size: 14px;
  line-height: 1.7;
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
  animation: oaFadeUp 0.38s ease both;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    background-color 0.18s ease;
  cursor: pointer;
}

.notification-item:hover {
  border-color: #1d1d1f;
  transform: translateY(-2px);
}

.notification-item.is-unread {
  background: #ffffff;
  border-color: #1d1d1f;
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
  background: #f5f5f7;
  color: #1d1d1f;
}
.item-icon.approval {
  background: #f5f5f7;
  color: #1d1d1f;
}
.item-icon.other {
  background: #fafafc;
  color: #7a7a7a;
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
  color: #1d1d1f;
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
  color: #7a7a7a;
  line-height: 1.7;
}

.item-time {
  display: inline-flex;
  width: fit-content;
  margin-top: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  color: #7a7a7a;
  background: #f5f5f7;
  font-size: 12px;
  line-height: 1;
}

.empty-state {
  padding: 72px 24px;
}

@media (max-width: 900px) {
  .notifications-page {
    padding: 48px 16px 72px;
  }

  .notifications-hero,
  .notifications-workspace {
    grid-template-columns: 1fr;
  }

  .notifications-hero {
    display: grid;
  }

  .notifications-hero__copy {
    min-height: auto;
    padding: 32px;
  }

  .notifications-hero__copy h1 {
    font-size: 36px;
  }

  .notifications-hero__stats {
    grid-template-columns: 1fr 1fr;
    flex-basis: auto;
  }

  .notifications-hero__stats div + div {
    border-top: 0;
    border-left: 1px solid #e0e0e0;
  }

  .notification-summary {
    position: static;
  }
}

@media (max-width: 560px) {
  .notifications-hero__copy,
  .notification-summary,
  .notification-item {
    padding: 20px;
  }

  .notifications-hero__stats strong {
    font-size: 34px;
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
