<template>
  <div class="site-page container">
    <div class="page-header">
      <div>
        <h1>消息通知</h1>
        <p>及时获取社团公告、活动进展及申请反馈</p>
      </div>
      <el-button 
        v-if="notifications.some(n => n.readFlag === 0)" 
        type="primary" 
        link 
        :icon="CircleCheck" 
        @click="markAllAsRead"
      >
        全部标记为已读
      </el-button>
    </div>

    <div v-loading="loading" class="notification-list">
      <div v-if="notifications.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无新消息" />
      </div>
      
      <div 
        v-for="item in notifications" 
        :key="item.id" 
        class="notification-item"
        :class="{ 'is-unread': item.readFlag === 0 }"
        @click="handleRead(item)"
      >
        <div class="item-icon" :class="item.type || 'other'">
          <el-icon :size="28"><Bell /></el-icon>
        </div>
        <div class="item-body">
          <div class="item-header">
            <span class="item-title">{{ item.title }}</span>
            <span class="item-time">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="item-content">{{ item.content }}</div>
        </div>
        <div v-if="item.readFlag === 0" class="unread-dot"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Bell, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

const loading = ref(false)
const notifications = ref([])

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
  const unreadIds = notifications.value
    .filter(n => n.readFlag === 0)
    .map(n => n.id)
  
  if (unreadIds.length === 0) return
  
  try {
    await Promise.all(unreadIds.map(id => notificationApi.markRead(id)))
    notifications.value.forEach(n => n.readFlag = 1)
    ElMessage.success('全部已标记为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(fetchNotifications)
</script>

<style scoped>
.site-page {
  padding-top: 40px;
  padding-bottom: 60px;
}

.page-header {
  margin-bottom: 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.page-header h1 {
  font-size: 28px;
  color: #1e293b;
  margin-bottom: 8px;
}

.page-header p {
  color: #64748b;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-item {
  display: flex;
  padding: 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  transition: all 0.3s;
  cursor: pointer;
  position: relative;
  gap: 16px;
}

.notification-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(30, 90, 235, 0.05);
}

.notification-item.is-unread {
  background: #f0f7ff;
  border-color: #bfdbfe;
}

.item-icon {
  width: 55px;
  height: 55px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 20px;
  flex-shrink: 0;
}

.item-icon.system { background: var(--oa-primary); color: #fff; }
.item-icon.activity { background: #dcfce7; color: #22c55e; }
.item-icon.approval { background: #fef9c3; color: #eab308; }
.item-icon.other { background: #f1f5f9; color: #64748b; }

.item-body {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.item-title {
  font-weight: 800;
  font-size: 18px;
  color: #1e293b;
}

.item-time {
  font-size: 13px;
  color: #94a3b8;
}

.item-content {
  font-size: 16px;
  color: #475569;
  line-height: 1.5;
}

.unread-dot {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
}
</style>
