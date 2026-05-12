<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="false" title="消息"/>
        <scroll-view
            :refresher-enabled="true"
            :refresher-triggered="refreshing"
            class="main-scroll"
            scroll-y
            @refresherrefresh="refresh"
        >
        <MessageSummary :unread-count="unreadCount" @read-all="markAllRead"/>
        <view v-if="loading && !messages.length" class="loading">加载中...</view>
        <view v-else-if="messages.length" class="msg-list">
            <MessageItem
                v-for="(msg, index) in messages"
                :key="msg.id"
                :index="index"
                :message="msg"
                @tap="onTap"
            />
        </view>
        <MessageEmpty v-else/>
        <view class="bottom-pad"/>
        </scroll-view>
        <Tabbar :activeIndex="3"></Tabbar>
    </view>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import Tabbar from "@/components/Tabbar.vue";
import MessageEmpty from './components/MessageEmpty.vue'
import MessageItem from './components/MessageItem.vue'
import MessageSummary from './components/MessageSummary.vue'
import {notificationApi} from '@/api'

const loading = ref(false)
const refreshing = ref(false)
const messages = ref<any[]>([])
const unreadCount = computed(() => messages.value.filter((item) => item.readFlag === 0).length)

async function load() {
    loading.value = true
    try {
        const res: any = await notificationApi.myNotifications()
        messages.value = Array.isArray(res) ? res : []
    } finally {
        loading.value = false
        refreshing.value = false
    }
}

function refresh() {
    refreshing.value = true
    load()
}

const onTap = async (msg: any) => {
    if (msg.readFlag === 0) {
        await notificationApi.markRead(msg.id)
        msg.readFlag = 1
    }
}

async function markAllRead() {
    const unread = messages.value.filter((item) => item.readFlag === 0)
    await Promise.all(unread.map((item) => notificationApi.markRead(item.id)))
    unread.forEach((item) => {
        item.readFlag = 1
    })
    uni.showToast({title: '已全部标记为已读', icon: 'none'})
}

onMounted(load)
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: #f7fafc;
}

.main-scroll {
    flex: 1;
    height: 0;
}

.msg-list {
    padding: 0 24rpx;
}

.loading {
    padding: 100rpx 40rpx;
    text-align: center;
    color: #64748b;
}

.bottom-pad {
    height: 170rpx;
}
</style>
