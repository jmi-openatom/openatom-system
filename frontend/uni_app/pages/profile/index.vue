<!-- ============================================================
View: Profile page template
============================================================ -->
<template>
  <view class="page">
    <!-- User card -->
    <view class="user-card">
      <view class="avatar">
        <User :size="36" color="rgba(255,255,255,0.8)" />
      </view>
      <text class="nickname">{{ userName }}</text>
      <text class="subtitle">个人中心</text>
    </view>

    <!-- Menu -->
    <view class="menu-group">
      <view class="menu-item" v-for="item in menus" :key="item.label" @click="onMenu(item)">
        <component :is="item.icon" :size="20" color="#666" class="menu-icon" />
        <text class="menu-label">{{ item.label }}</text>
        <ChevronRight size="20" color="#ccc" />
      </view>
    </view>

    <!-- Logout -->
    <view class="logout-wrap">
      <van-button type="danger" round block @click="onLogout">退出登录</van-button>
    </view>
    <CustomTabBar current="pages/profile/index" />
  </view>
</template>

<!-- ============================================================
ViewModel: Profile page logic
============================================================ -->
<script setup>
import { ref } from 'vue'
import { Button as VanButton } from 'vant'
import { User, Package, Star, Edit, Cog, ChevronRight } from '@boxicons/vue'
import CustomTabBar from '@/components/CustomTabBar.vue'

const userName = ref('用户')
const menus = ref([
  { icon: Package, label: '我的项目', path: '' },
  { icon: Star, label: '我的收藏', path: '' },
  { icon: Edit, label: '帮助反馈', path: '' },
  { icon: Cog, label: '设置', path: '' }
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
  background: rgba(255,255,255,0.3);
  border-radius: 50%;
  margin-bottom: 16rpx;
}
.nickname {
  font-size: 36rpx;
  color: #fff;
  font-weight: 600;
}
.subtitle {
  font-size: 26rpx;
  color: rgba(255,255,255,0.7);
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
  margin-right: 20rpx;
  flex-shrink: 0;
}
.menu-label {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}
.logout-wrap {
  margin: 80rpx 24rpx;
}
</style>
