<script setup>
import {tabbarList} from '@/config/tabbar'


const props = defineProps({activeIndex: 2});


</script>

<template>
    <tm-tabbar
        :activeIndex="activeIndex"
        :list="tabbarList"
        selectedColor="#1d1d1f"
        color="#8e8e93"
        borderColor="#d8d8dc"
        :showTopBorder="true"
        outIndex="-1"
        position="fixed"
    />
</template>

<style scoped>

</style>
