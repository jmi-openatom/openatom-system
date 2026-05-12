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

const loading = ref(false)
const refreshing = ref(false)
const club = ref<Record<string, any>>({})
const campaigns = ref<Record<string, any>[]>([])

async function load() {
    loading.value = true
    try {
        const res: any = await siteApi.recruitment()
        club.value = res?.club || {}
        campaigns.value = ensureList(res?.campaigns).filter((item: any) =>
            ['open', 'published'].includes(item.status || ''),
        )
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
    background: #f7fafc;
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
