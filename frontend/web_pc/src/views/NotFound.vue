<template>
  <main class="system-state-page">
    <section class="system-state" role="status">
      <span class="system-state__code">404</span>
      <h1>页面不存在</h1>
      <p>访问地址可能已失效，或页面已被移动。</p>
      <div class="system-state__actions">
        <el-button :icon="ArrowLeft" @click="$router.back()">返回上一页</el-button>
        <el-button type="primary" :icon="HomeFilled" @click="$router.push('/')">返回首页</el-button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
</script>
