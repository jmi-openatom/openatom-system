<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="false" title="招新"/>
        <scroll-view
            :refresher-enabled="true"
            :refresher-triggered="refreshing"
            class="main-scroll"
            scroll-y
            @refresherrefresh="refresh"
        >
            <RecruitmentHero :club-name="club.name"/>

            <RecruitmentSkeleton v-if="loading && !campaigns.length"/>

            <PageState
                v-else-if="loadFailed && !campaigns.length"
                description="招新信息加载失败，请稍后重试"
                @action="refresh"
            />

            <view v-else-if="campaigns.length" class="campaign-list">
                <CampaignCard
                    v-for="(item, index) in campaigns"
                    :key="item.id || index"
                    :campaign="item"
                    :index="index"
                    @tap="goApply"
                />
            </view>

            <RecruitmentEmpty v-else @activities="goActivities"/>

            <view class="bottom-pad"/>
        </scroll-view>
        <Tabbar :activeIndex="2"/>
    </view>
</template>

<script setup lang="ts">
import Tabbar from '@/components/Tabbar.vue'
import CampaignCard from './components/CampaignCard.vue'
import RecruitmentEmpty from './components/RecruitmentEmpty.vue'
import RecruitmentHero from './components/RecruitmentHero.vue'
import RecruitmentSkeleton from './components/RecruitmentSkeleton.vue'
import {siteApi} from '@/api'
import {ensureList} from '@/utils/format'
import {onMounted, ref} from 'vue'
import PageState from '@/components/PageState.vue'

const loading = ref(false)
const loadFailed = ref(false)
const refreshing = ref(false)
const club = ref<Record<string, any>>({})
const campaigns = ref<Record<string, any>[]>([])

async function load() {
    loading.value = true
    loadFailed.value = false
    try {
        const res: any = await siteApi.recruitment()
        club.value = res?.club || {}
        campaigns.value = ensureList(res?.campaigns).filter((item: any) =>
            !['draft', 'archived'].includes(item.status || ''),
        )
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

function goApply(item: any) {
    if (!item?.id) return
    if (!['open', 'published'].includes(item.status || '')) {
        uni.showToast({title: item.status === 'closed' ? '本批次已结束' : '本批次暂未开放', icon: 'none'})
        return
    }
    uni.navigateTo({url: `/pages/recruitment/apply?id=${item.id}`})
}

function goActivities() {
    uni.reLaunch({url: '/pages/activity/index'})
}

onMounted(load)
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

.campaign-list {
    padding: 0 24rpx;
}

.bottom-pad {
    height: 170rpx;
}

</style>
