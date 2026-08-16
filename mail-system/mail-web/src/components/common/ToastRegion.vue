<template>
  <div class="toast-region" aria-live="polite">
    <div v-if="toast" class="toast"><CircleCheck :size="17" />{{ toast }}</div>
  </div>
</template>

<script lang="ts" setup>
import { CircleCheck } from 'lucide-vue-next'
import { useUiStore } from '../../stores/ui'

const { toast } = useUiStore()
</script>
