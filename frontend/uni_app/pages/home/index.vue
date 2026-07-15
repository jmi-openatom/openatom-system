<template>
    <view class="page oa-page-transition">
        <tm-navbar :showNavBack="false" title="首页"/>
        <scroll-view
            :scroll-into-view="scrollIntoView"
            class="main-scroll"
            scroll-with-animation
            scroll-y
        >
            <PageState
                v-if="loadFailed"
                description="首页内容加载失败，请稍后重试"
                @action="loadClubHome"
            />
            <template v-else>
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
            </template>
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
import PageState from '@/components/PageState.vue'

const loading = ref(true)
const loadFailed = ref(false)
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
    loadFailed.value = false
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
        loadFailed.value = true
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
    background: linear-gradient(180deg, #ececef 0%, #f5f5f7 32%, #f5f5f7 100%);
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
