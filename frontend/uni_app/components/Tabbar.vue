<script setup>
import {tabbarList} from '@/config/tabbar'


const props = defineProps({activeIndex: 2});


</script>

<template>
    <tm-tabbar
        :activeIndex="activeIndex"
        :list="tabbarList"
        :outIndex="activeIndex"
        :showTopBorder="true"
        position="fixed"
    />
</template>

<style scoped>

</style>