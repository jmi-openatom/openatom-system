<template>
    <view class="page">
        <tm-navbar :showNavBack="false" title="消息"/>
        <view class="msg-list">
            <view v-for="msg in messages" :key="msg.id" class="msg-item" @click="onTap(msg)">
                <view class="msg-avatar">
                    <text class="msg-avatar-icon">{{ msg.avatar }}</text>
                </view>
                <view class="msg-body">
                    <view class="msg-top">
                        <text class="msg-name">{{ msg.name }}</text>
                        <text class="msg-time">{{ msg.time }}</text>
                    </view>
                    <text class="msg-preview">{{ msg.preview }}</text>
                </view>
                <view v-if="msg.unread" class="msg-badge">{{ msg.unread }}</view>
            </view>
        </view>
        <Tabbar :activeIndex="1"></Tabbar>
    </view>
</template>

<script setup>
import {ref} from 'vue'
import Tabbar from "@/components/Tabbar.vue";

const messages = ref([
    {id: 1, avatar: '👤', name: '系统通知', time: '10:30', preview: '欢迎加入OpenAtom平台', unread: 1},
    {id: 2, avatar: '💬', name: '团队协作', time: '昨天', preview: '你的项目有新更新', unread: 3},
    {id: 3, avatar: '🔔', name: '活动提醒', time: '周三', preview: '社区大会将于明天举行', unread: 0}
])

const onTap = (msg) => {
    uni.showToast({title: `打开 ${msg.name}`, icon: 'none'})
}
</script>

<style scoped>
.page {
    min-height: 100vh;
    padding-bottom: 140rpx;
    background: #f5f5f5;
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

.msg-avatar-icon {
    font-size: 36rpx;
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
