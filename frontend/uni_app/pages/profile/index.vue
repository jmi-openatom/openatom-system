<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="false" title="我的"/>
        <scroll-view class="page" scroll-y>
            <ProfileHeader
                :avatar-text="avatarText"
                :is-login="isLogin"
                :user-name="userName"
                @login="goLogin"
            />

            <ProfileMenu :menus="menus" @select="onMenu"/>

            <view v-if="isLogin" class="logout-wrap">
                <button class="logout-button" @click="onLogout">退出登录</button>
            </view>
        </scroll-view>

        <Tabbar :activeIndex="4"></Tabbar>
    </view>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import Tabbar from "@/components/Tabbar.vue";
import ProfileHeader from './components/ProfileHeader.vue'
import ProfileMenu from './components/ProfileMenu.vue'
import {authApi, notificationApi, siteApi} from '@/api'
import {clearSession, getCurrentUser, getToken, setSession} from '@/utils/auth'
import store from '@/store'

interface ProfileMenuItem {
    icon: string
    label: string
    path?: string
    badge?: string | number
}

const user = ref<Record<string, any>>(getCurrentUser())
const unread = ref(0)
const applicationCount = ref(0)
const isLogin = computed(() => Boolean(getToken()))
const userName = computed(() => user.value.realName || user.value.userName || user.value.username || '游客')
const avatarText = computed(() => String(userName.value || 'U').slice(0, 1).toUpperCase())
const menus = computed<ProfileMenuItem[]>(() => [
    {icon: 'P', label: '我的申请', path: '/pages/progress/index', badge: applicationCount.value || ''},
    {icon: 'J', label: '招新报名', path: '/pages/recruitment/index'},
    {icon: 'M', label: '消息通知', path: '/pages/messages/index', badge: unread.value || ''},
    {icon: 'A', label: '近期活动', path: '/pages/activity/index'},
])

async function load() {
    if (!isLogin.value) return
    try {
        const me: any = await authApi.me()
        user.value = me?.user || me || {}
        setSession({
            accessToken: getToken() || '',
            user: user.value,
            roles: me?.roles || [],
            permissions: me?.permissions || []
        })
    } catch {
    }
    try {
        const res: any = await notificationApi.unreadCount()
        unread.value = Number(res || 0)
        store.setUnreadCount(unread.value)
    } catch {
    }
    try {
        const progress: any = await siteApi.progress()
        applicationCount.value = progress?.applications?.length || 0
    } catch {
    }
}

const onMenu = (item: ProfileMenuItem) => {
    if (!item.path) return
    uni.reLaunch({url: item.path})
}

function goLogin() {
    uni.navigateTo({url: '/pages/login/index'})
}

const onLogout = () => {
    uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
            if (res.confirm) {
                authApi.logout().catch(() => {
                })
                clearSession()
                store.logout()
                user.value = {}
                unread.value = 0
                applicationCount.value = 0
                uni.showToast({title: '已退出', icon: 'none'})
            }
        }
    })
}

onMounted(load)
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 92vh;
    padding-bottom: 140rpx;
    background: #f7fafc;
}

.logout-wrap {
    margin: 80rpx 24rpx;
}

.logout-button {
    height: 88rpx;
    line-height: 88rpx;
    color: #fff;
    background: #ee0a24;
    border-radius: 44rpx;
    font-size: 30rpx;
}

.logout-button::after {
    border: 0;
}
</style>
