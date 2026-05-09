<template>
  <view class="page">
    <!-- User card -->
    <view class="user-card">
      <view class="avatar">
        <text class="avatar-icon">👤</text>
      </view>
      <text class="nickname">{{ userName }}</text>
      <text class="subtitle">个人中心</text>
    </view>

    <!-- Menu -->
    <view class="menu-group">
      <view class="menu-item" v-for="item in menus" :key="item.label" @click="onMenu(item)">
        <text class="menu-icon">{{ item.icon }}</text>
        <text class="menu-label">{{ item.label }}</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- Logout -->
    <view class="logout-wrap">
      <van-button type="danger" round block @click="onLogout">退出登录</van-button>
    </view>

    <CustomTabBar current="pages/profile/index" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
// #ifdef H5
import { Button as VanButton } from 'vant'
// #endif
import CustomTabBar from '@/components/CustomTabBar.vue'

const userName = ref('用户')
const menus = ref([
  { icon: '📦', label: '我的项目', path: '' },
  { icon: '⭐', label: '我的收藏', path: '' },
  { icon: '📝', label: '帮助反馈', path: '' },
  { icon: '⚙️', label: '设置', path: '' }
])

const onMenu = (item) => {
  uni.showToast({ title: item.label, icon: 'none' })
}

const onLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '已退出', icon: 'none' })
      }
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 140rpx;
  background: #f5f5f5;
}
.user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0 40rpx;
  background: #1989fa;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  margin-bottom: 16rpx;
}
.avatar-icon {
  font-size: 56rpx;
}
.nickname {
  font-size: 36rpx;
  color: #fff;
  font-weight: 600;
}
.subtitle {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8rpx;
}
.menu-group {
  margin: 24rpx;
  background: #fff;
  border-radius: 12rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.menu-item:last-child {
  border-bottom: 0;
}
.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.menu-label {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}
.menu-arrow {
  font-size: 36rpx;
  color: #ccc;
}
.logout-wrap {
  margin: 80rpx 24rpx;
}
</style>
