<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
    <el-container class="min-h-screen">
      <el-header class="bg-white shadow-sm">
        <div class="flex items-center justify-between h-full px-6">
          <div class="flex items-center gap-3">
            <el-icon :size="28" color="#409eff">
              <Operation />
            </el-icon>
            <h1 class="text-xl font-bold text-gray-800">实验室管理系统</h1>
          </div>
          <div class="flex items-center gap-4">
            <el-tag type="info" size="small">{{ version }}</el-tag>
            <el-button type="primary" size="small" @click="$router.push('/problem')">开始刷题</el-button>
          </div>
        </div>
      </el-header>

      <el-main>
        <div class="max-w-7xl mx-auto py-8">
          <el-card class="mb-6" shadow="hover">
            <template #header>
              <div class="flex items-center gap-2">
                <el-icon><Calendar /></el-icon>
                <span class="font-semibold">每日一练</span>
              </div>
            </template>
            <div class="text-center py-8">
              <p class="text-gray-500 mb-4">今日算法题目已更新，点击下方按钮开始刷题</p>
              <el-button type="primary" size="large" @click="$router.push('/problem')">立即开始</el-button>
            </div>
          </el-card>

          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8">
              <el-card shadow="hover" class="text-center">
                <el-icon :size="48" color="#67c23a" class="mb-3">
                  <CircleCheck />
                </el-icon>
                <div class="text-2xl font-bold text-gray-800 mb-2">0</div>
                <div class="text-gray-500">今日签到</div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :md="8">
              <el-card shadow="hover" class="text-center">
                <el-icon :size="48" color="#409eff" class="mb-3">
                  <TrophyBase />
                </el-icon>
                <div class="text-2xl font-bold text-gray-800 mb-2">0</div>
                <div class="text-gray-500">本周AC数</div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :md="8">
              <el-card shadow="hover" class="text-center">
                <el-icon :size="48" color="#f56c6c" class="mb-3">
                  <Star />
                </el-icon>
                <div class="text-2xl font-bold text-gray-800 mb-2">100</div>
                <div class="text-gray-500">实验室信誉分</div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { Operation, Calendar, CircleCheck, TrophyBase, Star } from '@element-plus/icons-vue'

const version = __APP_VERSION__ || 'dev'
</script>

<style scoped>
.el-header {
  height: 60px;
  line-height: 60px;
}

.el-main {
  padding: 0;
}

.el-card {
  margin-bottom: 20px;
}
</style>
