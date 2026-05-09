<!-- ============================================================
View: Messages page template
============================================================ -->
<template>
  <view class="page">
    <HeaderBar title="Messages" />

    <view class="msg-list">
      <view class="msg-item" v-for="msg in messages" :key="msg.id" @click="onTap(msg)">
        <view class="msg-avatar">
          <component :is="msg.avatarIcon" :size="22" color="#666" />
        </view>
        <view class="msg-body">
          <view class="msg-top">
            <text class="msg-name">{{ msg.name }}</text>
            <text class="msg-time">{{ msg.time }}</text>
          </view>
          <text class="msg-preview">{{ msg.preview }}</text>
        </view>
        <view class="msg-badge" v-if="msg.unread">{{ msg.unread }}</view>
      </view>
    </view>

    <van-empty v-if="!messages.length" description="暂无消息" />
    <CustomTabBar current="pages/messages/index" :badge="unreadTotal" />
  </view>
</template>

<!-- ============================================================
ViewModel: Messages page logic
============================================================ -->
<script setup>
import { ref, computed } from 'vue'
import { Empty as VanEmpty } from 'vant'
import { User, MessageCircle, Bell } from '@boxicons/vue'
import CustomTabBar from '@/components/CustomTabBar.vue'
import HeaderBar from "@/components/HeaderBar.vue";

const messages = ref([
  { id: 1, avatarIcon: User, name: '系统通知', time: '10:30', preview: '欢迎加入OpenAtom平台', unread: 1 },
  { id: 2, avatarIcon: MessageCircle, name: '团队协作', time: '昨天', preview: '你的项目有新更新', unread: 3 },
  { id: 3, avatarIcon: Bell, name: '活动提醒', time: '周三', preview: '社区大会将于明天举行', unread: 0 }
])

const unreadTotal = computed(() => messages.value.reduce((sum, m) => sum + (m.unread || 0), 0))

const onTap = (msg) => {
  uni.showToast({ title: `打开 ${msg.name}`, icon: 'none' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 140rpx;
  background: #f5f5f5;
}
.header {
  padding: 30rpx;
  background: var(--topbar-color-light);
}
.header-title {
  font-size: 36rpx;
  color: var(--topbar-text-color-light);
  font-weight: 600;
}
.msg-list {
  margin: 20rpx 24rpx;
}
.msg-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  margin-bottom: 16rpx;
  background: #fff;
  border-radius: 12rpx;
}
.msg-avatar {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.msg-body {
  flex: 1;
}
.msg-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.msg-name {
  font-size: 30rpx;
  font-weight: 500;
}
.msg-time {
  font-size: 24rpx;
  color: #999;
}
.msg-preview {
  font-size: 26rpx;
  color: #999;
  margin-top: 6rpx;
}
.msg-badge {
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  font-size: 22rpx;
  color: #fff;
  background: #ee0a24;
  border-radius: 50%;
  padding: 0 8rpx;
}
</style>
