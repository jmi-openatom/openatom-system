<script setup>
import {onLaunch} from '@dcloudio/uni-app'
import {routeByDomain} from '@/utils/domainRoute'

onLaunch(() => {
    uni.onNetworkStatusChange((result) => {
        uni.showToast({
            title: result.isConnected ? '网络已恢复' : '网络已断开，已保留当前页面',
            icon: 'none',
        })
    })
    routeByDomain()
})
</script>

<style>
/* ---- Global page base ---- */
page {
    font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica,
    Segoe UI, Arial, Roboto, 'PingFang SC', 'miui', 'Hiragino Sans GB',
    'Microsoft Yahei', sans-serif;
    font-size: 28rpx;
    color: #1d1d1f;
    background: #f5f5f7;
    -webkit-font-smoothing: antialiased;
}

button,
[role="button"] {
    touch-action: manipulation;
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
