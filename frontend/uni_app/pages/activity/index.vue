<template>
    <view class="page">
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
import {activityApi} from '@/api'
import {onMounted, ref} from 'vue'
import type {ActivityFilter, ActivityItem, ActivityStatus} from './types'
import ActivitySkeleton from "@/pages/activity/components/ActivitySkeleton.vue";
import FilterBar from "@/pages/activity/components/FilterBar.vue";

const scrollIntoView = ref('')

const filters: ActivityFilter[] = [
    {label: '全部', value: ''},
    {label: '进行中', value: 'published'},
    {label: '已结束', value: 'closed'},
]

const activeFilter = ref<ActivityStatus>('')
const list = ref<ActivityItem[]>([])
const loading = ref(false)
const refreshing = ref(false)
const page = ref(1)
const perPage = 10
const noMore = ref(false)

async function fetchActivities(isRefresh = false) {
    if (loading.value) return
    loading.value = true

    if (isRefresh) {
        page.value = 1
        noMore.value = false
    }

    try {
        const params: Record<string, unknown> = {
            page: page.value,
            perPage,
        }
        if (activeFilter.value) {
            params.status = activeFilter.value
        }

        const res = await activityApi.list(params)
        const items: ActivityItem[] = Array.isArray(res) ? res : (res as any)?.data || (res as any)?.items || (res as any)?.list || []

        if (isRefresh) {
            list.value = items
        } else {
            list.value = [...list.value, ...items]
        }

        if (items.length < perPage) {
            noMore.value = true
        }
    } catch {
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
    //background: linear-gradient(180deg, #f1f5f9 0%, #f8fafc 32%, #f8fafc 100%);
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
    color: #94a3b8;
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
