<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="false" title="首页"/>
        <scroll-view
            :scroll-into-view="scrollIntoView"
            class="main-scroll"
            scroll-with-animation
            scroll-y
        >
            <HomeBanner
                :club="data.club"
                :loading="loading"
                @activities="scrollToActivities"
                @join="goJoin"
            />
            <HomeMetrics :club-name="clubName" :metrics="data.metrics || []"/>
            <HomeFocusGrid :items="data.focusAreas || []"/>
            <HomeActivityScroller :activities="data.activities || []"/>
            <HomePeopleList :people="data.people || []"/>
            <HomeAwardList :awards="data.awards || []" class="award__list"/>
            <view class="scroll-spacer"/>
            <view class="bottom-pad"/>
        </scroll-view>
        <Tabbar :activeIndex="0"/>
    </view>
</template>

<script setup>
import Tabbar from '@/components/Tabbar.vue'
import HomeActivityScroller from '@/pages/home/components/HomeActivityScroller.vue'
import HomeAwardList from '@/pages/home/components/HomeAwardList.vue'
import HomeBanner from '@/pages/home/components/HomeBanner.vue'
import HomeFocusGrid from '@/pages/home/components/HomeFocusGrid.vue'
import HomeMetrics from '@/pages/home/components/HomeMetrics.vue'
import HomePeopleList from '@/pages/home/components/HomePeopleList.vue'
import {siteApi} from '@/api'
import {computed, nextTick, onMounted, ref} from 'vue'

const loading = ref(true)
const scrollIntoView = ref('')
const data = ref({
    club: {},
    metrics: [],
    focusAreas: [],
    activities: [],
    people: [],
    awards: [],
    techStack: [],
})

const clubName = computed(() => {
    const c = data.value.club
    return (c && c.name) || ''
})

async function loadClubHome() {
    loading.value = true
    try {
        const res = await siteApi.clubHome()
        data.value = res || {
            club: {},
            metrics: [],
            focusAreas: [],
            activities: [],
            people: [],
            awards: [],
            techStack: [],
        }
    } catch {
        data.value = {
            club: {},
            metrics: [],
            focusAreas: [],
            activities: [],
            people: [],
            awards: [],
            techStack: [],
        }
    } finally {
        loading.value = false
    }
}

function goJoin() {
    uni.reLaunch({url: '/pages/recruitment/index'})
}

function scrollToActivities() {
    scrollIntoView.value = ''
    nextTick(() => {
        scrollIntoView.value = 'section-activities'
        setTimeout(() => {
            scrollIntoView.value = ''
        }, 400)
    })
}

onMounted(() => {
    loadClubHome()
})
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: linear-gradient(180deg, #f1f5f9 0%, #f8fafc 32%, #f8fafc 100%);
    box-sizing: border-box;
}

.main-scroll {
    margin-top: 10px;
    flex: 1;
    height: 0;
    padding-top: 8rpx;
}

.scroll-spacer {
    height: 32rpx;
}

.bottom-pad {
    height: 300rpx;
}

.award__list {
    margin-bottom: 32rpx;
}
</style>
