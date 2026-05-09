<!-- ============================================================
Layout: Custom bottom tab bar (using Boxicons)
============================================================ -->
<template>
  <view class="tab-bar-wrap">
    <view class="tab-bar">
      <view class="tab-item" @click="switchTab('pages/home/index')">
        <Home :size="24" :color="current === 'pages/home/index' ? activeColor : color" />
        <text class="tab-text" :style="{ color: current === 'pages/home/index' ? activeColor : color }">首页</text>
      </view>

      <view class="tab-item" @click="switchTab('pages/messages/index')">
        <MessageCircle :size="24" :color="current === 'pages/messages/index' ? activeColor : color" />
        <text class="tab-text" :style="{ color: current === 'pages/messages/index' ? activeColor : color }">消息</text>
        <view v-if="badge" class="tab-badge">{{ badge > 99 ? '99+' : badge }}</view>
      </view>

      <view class="tab-item" @click="switchTab('pages/profile/index')">
        <User :size="24" :color="current === 'pages/profile/index' ? activeColor : color" />
        <text class="tab-text" :style="{ color: current === 'pages/profile/index' ? activeColor : color }">我的</text>
      </view>
    </view>
    <view class="tab-safe" />
  </view>
</template>

<script setup>
import { Home, MessageCircle, User } from '@boxicons/vue'

defineProps({
  current: { type: String, default: 'pages/home/index' },
  badge: { type: Number, default: 0 }
})

const color = '#999999'
const activeColor = '#1989fa'

const switchTab = (path) => {
  uni.switchTab({ url: `/${path}` })
}
</script>

<style scoped>
.tab-bar-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
}
.tab-bar {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 100rpx;
  background: #ffffff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
}
.tab-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
}
.tab-text {
  font-size: 20rpx;
  margin-top: 4rpx;
}
.tab-badge {
  position: absolute;
  top: 8rpx;
  right: 50%;
  transform: translateX(60rpx);
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  font-size: 20rpx;
  color: #fff;
  background: #ee0a24;
  border-radius: 50%;
  padding: 0 6rpx;
}
.tab-safe {
  height: constant(safe-area-inset-bottom);
  height: env(safe-area-inset-bottom);
}
</style>
