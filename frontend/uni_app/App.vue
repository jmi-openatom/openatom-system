<script setup>
import {onHide, onLaunch, onShow} from '@dcloudio/uni-app'
import {routeByDomain} from '@/utils/domainRoute'

onLaunch(() => {
    console.log('App Launch')
    uni.onNetworkStatusChange((result) => {
        uni.showToast({
            title: result.isConnected ? '网络已恢复' : '网络已断开，已保留当前页面',
            icon: 'none',
        })
    })
    routeByDomain()
})

onShow(() => {
    console.log('App Show')
})

onHide(() => {
    console.log('App Hide')
})
</script>

<style>
/* ---- Global page base ---- */
page {
    font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica,
    Segoe UI, Arial, Roboto, 'PingFang SC', 'miui', 'Hiragino Sans GB',
    'Microsoft Yahei', sans-serif;
    font-size: 28rpx;
    color: #333;
    background: #f5f5f5;
    -webkit-font-smoothing: antialiased;
}

/* ---- Common utility classes ---- */
.flex-center {
    display: flex;
    align-items: center;
    justify-content: center;
}

.flex-between {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

</style>
