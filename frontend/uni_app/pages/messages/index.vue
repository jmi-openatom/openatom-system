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
        <PageState
            v-if="!isLogin"
            title="登录后查看消息"
            description="活动提醒、申请进度与社团通知都会汇总在这里"
            action-text="去登录"
            @action="goLogin"
        />
        <MessageSummary v-else :unread-count="unreadCount" @read-all="markAllRead"/>
        <view v-if="isLogin && loading && !messages.length" class="loading">加载中...</view>
        <PageState v-else-if="isLogin && loadFailed" description="消息加载失败，请稍后重试" @action="load"/>
        <view v-else-if="isLogin && messages.length" class="msg-list">
            <MessageItem
                v-for="(msg, index) in messages"
                :key="msg.id"
                :index="index"
                :message="msg"
                @tap="onTap"
            />
        </view>
        <MessageEmpty v-else-if="isLogin"/>
        <view class="bottom-pad"/>
        </scroll-view>
        <Tabbar :activeIndex="3"></Tabbar>
    </view>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import {onShow} from '@dcloudio/uni-app'
import Tabbar from "@/components/Tabbar.vue";
import MessageEmpty from './components/MessageEmpty.vue'
import MessageItem from './components/MessageItem.vue'
import MessageSummary from './components/MessageSummary.vue'
import {notificationApi} from '@/api'
import {getToken, loginUrl} from '@/utils/auth'
import store from '@/store'
import PageState from '@/components/PageState.vue'

const loading = ref(false)
const refreshing = ref(false)
const loadFailed = ref(false)
const messages = ref<any[]>([])
const isLogin = computed(() => Boolean(getToken()))
const unreadCount = computed(() => messages.value.filter((item) => item.readFlag === 0).length)

async function load() {
    if (!isLogin.value) return
    loading.value = true
    loadFailed.value = false
    try {
        const res: any = await notificationApi.myNotifications()
        messages.value = Array.isArray(res) ? res : []
        store.setUnreadCount(messages.value.filter((item) => item.readFlag === 0).length)
    } catch {
        loadFailed.value = true
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
        store.setUnreadCount(unreadCount.value)
    }
}

async function markAllRead() {
    const unread = messages.value.filter((item) => item.readFlag === 0)
    await Promise.all(unread.map((item) => notificationApi.markRead(item.id)))
    unread.forEach((item) => {
        item.readFlag = 1
    })
    store.setUnreadCount(0)
    uni.showToast({title: '已全部标记为已读', icon: 'none'})
}

function goLogin() {
    uni.navigateTo({url: loginUrl('/pages/messages/index')})
}

onShow(load)
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: #f5f5f7;
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
    color: #666668;
}

.bottom-pad {
    height: 170rpx;
}
</style>
