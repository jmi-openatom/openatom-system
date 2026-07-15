<template>
    <view
        class="msg-item oa-animate-in"
        :class="{ 'is-unread': message.readFlag === 0 }"
        :style="{ animationDelay: `${index * 45}ms` }"
        @tap="emit('tap', message)"
    >
        <view class="msg-avatar">
            <text class="msg-avatar-icon">{{ iconFor(message.type) }}</text>
        </view>
        <view class="msg-body">
            <view class="msg-top">
                <text class="msg-name">{{ message.title }}</text>
                <text class="msg-time">{{ formatDateTime(message.createdAt) }}</text>
            </view>
            <text class="msg-preview">{{ message.content }}</text>
        </view>
        <view v-if="message.readFlag === 0" class="msg-badge"></view>
    </view>
</template>

<script setup lang="ts">
import {formatDateTime} from '@/utils/format'

defineProps<{
    message: Record<string, any>
    index: number
}>()

const emit = defineEmits<{
    tap: [message: Record<string, any>]
}>()

function iconFor(type?: string) {
    const map: Record<string, string> = {
        system: 'i',
        activity: 'A',
        approval: '✓',
        interview: 'M',
    }
    return map[type || ''] || 'N'
}
</script>

<style scoped>
.msg-item {
    display: flex;
    align-items: center;
    padding: 24rpx;
    margin-bottom: 16rpx;
    background: #fff;
    border-radius: 16rpx;
    box-shadow: 0 10rpx 26rpx rgba(0, 0, 0, .07);
    position: relative;
}

.msg-item.is-unread {
    background: #eff6ff;
}

.msg-avatar {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #1d1d1f;
    border-radius: 16rpx;
    margin-right: 20rpx;
    flex-shrink: 0;
}

.msg-avatar-icon {
    font-size: 30rpx;
    color: #fff;
    font-weight: 800;
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
    font-weight: 700;
    color: #1d1d1f;
}

.msg-time {
    font-size: 24rpx;
    color: #999;
}

.msg-preview {
    display: block;
    font-size: 26rpx;
    color: #666668;
    margin-top: 6rpx;
    line-height: 1.45;
}

.msg-badge {
    position: absolute;
    top: 20rpx;
    right: 20rpx;
    width: 14rpx;
    height: 14rpx;
    background: #ef4444;
    border-radius: 50%;
}
</style>
