<script lang="ts" setup>
// 给 Tetris 加上大括号
import { Tetris } from '@/components/ui/tetris/index'
</script>

<template>
  <Tetris
    class="h-[500px] w-full mask-[radial-gradient(450px_circle_at_center,#1d1d1f,transparent)]"
    :base="15"
    square-color="#1d1d1f"
  />
</template>
