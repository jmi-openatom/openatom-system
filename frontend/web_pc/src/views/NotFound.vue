<template>
  <main class="system-state-page">
    <StatePanel
      class="system-state"
      title="页面不存在"
      description="访问地址可能已失效，或页面已被移动。"
    >
      <template #eyebrow>
        <span aria-hidden="true" class="system-state__code">404</span>
      </template>
      <template #actions>
        <div class="system-state__actions">
          <el-button :icon="ArrowLeft" @click="$router.back()">返回上一页</el-button>
          <el-button type="primary" :icon="HomeFilled" @click="$router.push('/')">
            返回首页
          </el-button>
        </div>
      </template>
    </StatePanel>
  </main>
</template>

<script setup lang="ts">
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'
import StatePanel from '@/components/common/StatePanel.vue'
</script>
