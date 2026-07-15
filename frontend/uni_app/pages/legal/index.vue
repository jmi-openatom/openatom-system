<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="true" :title="document.title"/>
        <scroll-view class="content" scroll-y>
            <view class="document">
                <text class="eyebrow">OPENATOM · {{ document.version }}</text>
                <text class="title">{{ document.title }}</text>
                <view v-for="section in document.sections" :key="section.heading" class="section">
                    <text class="heading">{{ section.heading }}</text>
                    <text class="paragraph">{{ section.content }}</text>
                </view>
            </view>
        </scroll-view>
    </view>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'

const type = ref('about')
const documents = {
    privacy: {
        title: '隐私政策', version: 'V1.0', sections: [
            {heading: '我们收集的信息', content: '为提供登录、活动报名、招新申请、消息与签到服务，我们仅在对应功能中处理账号资料、联系方式、申请内容与签到记录。'},
            {heading: '信息使用与保护', content: '相关信息仅用于完成你主动使用的服务、保障账号安全和改进产品体验。我们不会在小程序中存储密码、Token、openid 或完整二维码内容的操作日志。'},
            {heading: '权限说明', content: '相机权限仅在你主动点击扫码签到时申请；拒绝授权不会影响浏览首页、活动和招新内容。'},
        ],
    },
    agreement: {
        title: '用户协议', version: 'V1.0', sections: [
            {heading: '服务范围', content: 'OpenAtom 小程序面向高校学生和社团成员，提供社团门户、活动报名、招新申请、消息和签到服务。'},
            {heading: '账号责任', content: '请妥善保管账号信息，并确保提交的报名和申请资料真实、准确。请勿利用本服务发布违法、有害或侵犯他人权益的内容。'},
            {heading: '服务变更', content: '我们会在保障核心服务可用的前提下持续优化功能；涉及重要权利义务的更新会通过页面公告或消息通知。'},
        ],
    },
    about: {
        title: '关于 OpenAtom', version: 'V1.0', sections: [
            {heading: '开放原子开源社团', content: '连接校园开发者与开源协作，让学习、实践和真实项目在社团中自然发生。'},
            {heading: '小程序定位', content: '这是 OpenAtom System 的微信移动服务入口，用于浏览活动、提交申请、接收通知和完成现场签到。'},
            {heading: '当前版本', content: 'OpenAtom 微信小程序 V1.0。'},
        ],
    },
}

const document = computed(() => documents[type.value as keyof typeof documents] || documents.about)

onLoad((options?: {type?: string}) => {
    if (options?.type && options.type in documents) type.value = options.type
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100vh; background: #f5f5f7; }
.content { flex: 1; height: 0; }
.document { margin: 24rpx; padding: 44rpx 36rpx 64rpx; border: 1rpx solid #ececef; border-radius: 16rpx; background: #fff; }
.eyebrow { display: block; color: #8e8e93; font-size: 22rpx; letter-spacing: 2rpx; }
.title { display: block; margin-top: 16rpx; color: #1d1d1f; font-size: 48rpx; font-weight: 800; }
.section { margin-top: 44rpx; }
.heading { display: block; color: #1d1d1f; font-size: 30rpx; font-weight: 700; }
.paragraph { display: block; margin-top: 14rpx; color: #4f4f52; font-size: 28rpx; line-height: 1.8; }
</style>
