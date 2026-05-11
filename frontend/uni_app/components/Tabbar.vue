<script setup>
import {tabbarList} from '@/config/tabbar'


const props = defineProps({activeIndex: 2});


</script>

<template>
    <tm-tabbar
        :activeIndex="activeIndex"
        :list="tabbarList"
        :showTopBorder="true"
        outIndex="-1"
        position="fixed"
    />
</template>

<style scoped>

</style>