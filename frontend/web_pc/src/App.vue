<template>
  <div class="app-route-shell">
    <AppStatusBar />
    <router-view />
  </div>
</template>

<script lang="ts" setup>
import AppStatusBar from '@/components/common/AppStatusBar.vue'
</script>

<style scoped>
.app-route-shell {
  min-height: 100vh;
  background: var(--oa-page-bg);
}
</style>
