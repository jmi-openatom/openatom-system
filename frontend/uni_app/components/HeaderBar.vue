<script lang="ts" setup>
// #ifdef H5
import { NavBar as VanNavBar } from 'vant'
// #endif

defineOptions({
    name: 'HeaderBar'
})
const props = defineProps({
    title: {type: String, required: true},
    showBack: {type: Boolean, default: true}
})

const back = () => {
    if (props.showBack) {
        history.back()
    } else {
        console.log("未在二级页面")
    }
}

</script>

<template>
    <van-nav-bar :left-arrow="showBack" :title=title @click="back"/>
</template>


<style scoped>
.header {
    padding: 30rpx;
    background: var(--topbar-color-light);
    border-bottom: 1px solid #e1e1e1;
}

.header-title {
    font-size: 36rpx;
    color: var(--topbar-text-color-light);
    font-weight: 600;
}
</style>