<template>
    <view class="page">
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

            <view v-else class="error">
                <text class="error__text">加载失败，请重试</text>
            </view>

            <view class="bottom-pad"/>
        </scroll-view>

        <Apply></Apply>

    </view>
</template>

<script lang="ts" setup>
import Banner from './components/Banner.vue'
import Detail from './components/Detail.vue'
import {siteApi} from '@/api'
import {onMounted, ref} from 'vue'
import {onLoad} from '@dcloudio/uni-app'
import type {ActivityDetail} from '../types'
import Apply from "@/pages/activity/detail/components/Apply.vue";

const id = ref('')
const detail = ref<ActivityDetail | null>(null)
const loading = ref(true)


async function fetchDetail() {
    if (!id.value) {
        console.warn('[detail] no id, aborting')
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
        }
    } catch {
        detail.value = null
    } finally {
        loading.value = false
    }
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
    color: #94a3b8;
}

.error {
    padding: 120rpx 0;
    text-align: center;
}

.error__text {
    font-size: 26rpx;
    color: #94a3b8;
}

.bottom-pad {
    height: 100rpx;
}
</style>
