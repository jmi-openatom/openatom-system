<script lang="ts" setup>
// 给 Tetris 加上大括号
import { Tetris } from '@/components/ui/tetris/index';
</script>

<template>
    <Tetris
        class="h-[500px] w-full mask-[radial-gradient(450px_circle_at_center,#00C16A,transparent)]"
        :base="15"
        square-color="#00C16A"
    />

</template>



<style scoped>

</style>
