<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="true" title="活动详情"/>

        <scroll-view class="main-scroll" scroll-y>
            <view v-if="loading" class="loading">
                <text class="loading__text">加载中...</text>
            </view>

            <template v-else-if="detail">
                <view class="section">
                    <Banner :data="detail"/>
                </view>

                <view class="section">
                    <Detail :content="detail.descriptionMarkdown || detail.description"/>
                </view>
            </template>

            <PageState v-else description="活动信息加载失败，请稍后重试" @action="fetchDetail"/>

            <view class="bottom-pad"/>
        </scroll-view>

        <Apply
            v-if="detail"
            :activity-id="detail.id"
            :disabled="!registrationAction.enabled"
            :fields="registrationFields"
            :title="registrationAction.label"
            @success="handleRegistrationSuccess"
        />

    </view>
</template>

<script lang="ts" setup>
import Banner from './components/Banner.vue'
import Detail from './components/Detail.vue'
import {siteApi} from '@/api'
import {computed, onMounted, ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import type {ActivityDetail} from '../types'
import Apply from "@/pages/activity/detail/components/Apply.vue";
import PageState from '@/components/PageState.vue'
import {parseFormSchema} from '@/utils/formSchema'

const id = ref('')
const detail = ref<ActivityDetail | null>(null)
const loading = ref(true)
const justRegistered = ref(false)
const registrationFields = computed(() => parseFormSchema(detail.value?.registrationFields))

const registrationAction = computed(() => {
    const item = detail.value
    if (!item?.registrationRequired) return {label: '无需报名', enabled: false}
    if (justRegistered.value || item.registered || item.isRegistered || item.registrationStatus === 'registered') {
        return {label: '已报名', enabled: false}
    }
    if (['cancelled', 'closed', 'ended'].includes(item.status || '')) {
        return {label: item.status === 'cancelled' ? '活动已取消' : '活动已结束', enabled: false}
    }
    const now = Date.now()
    const start = item.registrationStartAt ? new Date(item.registrationStartAt).getTime() : 0
    const end = item.registrationEndAt ? new Date(item.registrationEndAt).getTime() : 0
    if (start && now < start) return {label: '报名未开始', enabled: false}
    if (end && now > end) return {label: '报名已截止', enabled: false}
    const count = item.registrationCount ?? item.currentParticipants ?? 0
    if (item.maxParticipants && count >= item.maxParticipants) return {label: '名额已满', enabled: false}
    return {label: '立即报名', enabled: true}
})


async function fetchDetail() {
    if (!id.value) {
        detail.value = null
        loading.value = false
        return null
    }
    loading.value = true
    try {
        const res: any = await siteApi.activityDetail(id.value)
        const src = res?.data || res
        if (!src) {
            detail.value = null
            return null
        }

        const formatDate = (d?: string | null) => {
            if (!d) return ''
            return d.replace('T', ' ').slice(0, 16)
        }

        const dateParts: string[] = []
        if (src.activityAt) dateParts.push(formatDate(src.activityAt))
        if (src.endAt) dateParts.push('— ' + formatDate(src.endAt))

        detail.value = {
            id: src.id,
            title: src.title,
            summary: src.summary,
            description: src.description || src.summary,
            descriptionMarkdown: src.descriptionMarkdown,
            content: src.descriptionMarkdown,
            date: dateParts.join('  ') || undefined,
            activityAt: src.activityAt,
            endAt: src.endAt,
            location: src.location,
            status: src.status,
            coverUrl: src.coverUrl,
            registrationRequired: src.registrationRequired,
            registrationStartAt: src.registrationStartAt,
            registrationEndAt: src.registrationEndAt,
            tags: src.tags,
            registered: src.registered,
            isRegistered: src.isRegistered,
            registrationStatus: src.registrationStatus,
            registrationCount: src.registrationCount,
            registrationFields: src.registrationFields,
            maxParticipants: src.maxParticipants,
            currentParticipants: src.currentParticipants,
        }
    } catch {
        detail.value = null
    } finally {
        loading.value = false
    }
}

function handleRegistrationSuccess() {
    justRegistered.value = true
    fetchDetail()
}

onLoad((options?: { id?: string }) => {
    if (options?.id) {
        id.value = options.id
    }
})

onMounted(() => {
    fetchDetail()
})
</script>

<style lang="scss" scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
}

.main-scroll {
    flex: 1;
    height: 0;
}

.section {
    padding: 24rpx;
}

.loading {
    padding: 120rpx 0;
    text-align: center;
}

.loading__text {
    font-size: 26rpx;
    color: #8e8e93;
}

.error {
    padding: 120rpx 0;
    text-align: center;
}

.error__text {
    font-size: 26rpx;
    color: #8e8e93;
}

.bottom-pad {
    height: 180rpx;
}
</style>
