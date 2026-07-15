<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="false" title="活动"/>

        <scroll-view
            :refresher-enabled="true"
            :refresher-triggered="refreshing"
            :scroll-into-view="scrollIntoView"
            class="main-scroll"
            scroll-with-animation
            scroll-y
            @refresherrefresh="onRefresh"
            @scrolltolower="onLoadMore"
        >
            <FilterBar
                :active-filter="activeFilter"
                :filters="filters"
                @change="switchFilter"
            />

            <ActivitySkeleton v-if="loading && !list.length"/>

            <PageState
                v-else-if="loadFailed && !list.length"
                description="活动列表加载失败，请稍后重试"
                @action="onRefresh"
            />

            <view v-else-if="list.length" class="card-list">
                <ActivityCard
                    v-for="(item, i) in list"
                    :key="item.id || i"
                    :activity="item"
                    @tap="onTap(item)"
                />
            </view>

            <ActivityEmpty v-else/>

            <view v-if="loading && list.length" class="loading-more">
                <text class="loading-more__text">加载中...</text>
            </view>

            <view v-if="!loading && list.length && noMore" class="no-more">
                <text>— 没有更多了 —</text>
            </view>


            <view class="bottom-pad"/>
        </scroll-view>

        <Tabbar :activeIndex="1"/>
    </view>
</template>

<script lang="ts" setup>
import Tabbar from '@/components/Tabbar.vue'
import ActivityCard from './components/ActivityCard.vue'
import ActivityEmpty from './components/ActivityEmpty.vue'
import {siteApi} from '@/api'
import {onMounted, ref} from 'vue'
import type {ActivityFilter, ActivityItem, ActivityStatus} from './types'
import ActivitySkeleton from "@/pages/activity/components/ActivitySkeleton.vue";
import FilterBar from "@/pages/activity/components/FilterBar.vue";
import {ensureList, formatDateTime} from '@/utils/format'
import PageState from '@/components/PageState.vue'

const scrollIntoView = ref('')

const filters: ActivityFilter[] = [
    {label: '全部', value: ''},
    {label: '报名中', value: 'registration_open'},
    {label: '即将开始', value: 'upcoming'},
    {label: '已结束', value: 'ended'},
]

const activeFilter = ref<ActivityStatus>('')
const list = ref<ActivityItem[]>([])
const loading = ref(false)
const loadFailed = ref(false)
const refreshing = ref(false)
const page = ref(1)
const perPage = 10
const noMore = ref(false)
const allActivities = ref<ActivityItem[]>([])

function normalizeActivity(item: any): ActivityItem {
    return {
        ...item,
        date: item.date || formatDateTime(item.activityAt),
    }
}

async function fetchActivities(isRefresh = false) {
    if (loading.value) return
    loading.value = true
    loadFailed.value = false

    if (isRefresh) {
        page.value = 1
        noMore.value = false
    }

    try {
        if (isRefresh || !allActivities.value.length) {
            const res = await siteApi.activities()
            allActivities.value = ensureList<ActivityItem>(res).map(normalizeActivity)
        }

        const now = Date.now()
        const filtered = allActivities.value.filter((item) => {
            if (!activeFilter.value) return true
            const startsAt = item.activityAt ? new Date(item.activityAt).getTime() : 0
            const endsAt = item.endAt ? new Date(item.endAt).getTime() : startsAt
            const registrationStarts = item.registrationStartAt ? new Date(item.registrationStartAt).getTime() : 0
            const registrationEnds = item.registrationEndAt ? new Date(item.registrationEndAt).getTime() : 0
            if (activeFilter.value === 'registration_open') {
                return Boolean(item.registrationRequired && (!registrationStarts || registrationStarts <= now) && (!registrationEnds || registrationEnds >= now))
            }
            if (activeFilter.value === 'upcoming') return Boolean(startsAt && startsAt > now)
            return ['closed', 'ended', 'cancelled'].includes(item.status || '') || Boolean(endsAt && endsAt < now)
        })
        const start = (page.value - 1) * perPage
        const items = filtered.slice(start, start + perPage)

        if (isRefresh) {
            list.value = items
        } else {
            list.value = [...list.value, ...items]
        }

        if (items.length < perPage) {
            noMore.value = true
        }
    } catch {
        loadFailed.value = true
        if (isRefresh) {
            list.value = []
        }
    } finally {
        loading.value = false
        refreshing.value = false
    }
}

function switchFilter(value: ActivityStatus) {
    if (activeFilter.value === value) return
    activeFilter.value = value
    page.value = 1
    fetchActivities(true)
}

function onRefresh() {
    refreshing.value = true
    fetchActivities(true)
}

function onLoadMore() {
    if (loading.value || noMore.value) return
    page.value++
    fetchActivities(false)
}

function onTap(item: ActivityItem) {
    uni.navigateTo({url: `/pages/activity/detail/index?id=${item.id}`})
}

onMounted(() => {
    fetchActivities(true)
})
</script>

<style lang="scss" scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    //background: linear-gradient(180deg, #ececef 0%, #f5f5f7 32%, #f5f5f7 100%);
    box-sizing: border-box;
}

.main-scroll {
    flex: 1;
    height: 0;
}

.card-list {
    margin-top: 10px;
    padding: 12rpx 24rpx;
}

.loading-more {
    padding: 24rpx 0 32rpx;
    text-align: center;
}

.loading-more__text {
    font-size: 24rpx;
    color: #8e8e93;
}

.no-more {
    padding: 24rpx 0 32rpx;
    text-align: center;
    font-size: 22rpx;
    color: #c4c9d1;
}

.bottom-pad {
    height: 120rpx;
}
</style>
