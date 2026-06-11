<template>
  <div class="min-h-screen bg-gray-50">
    <el-container class="min-h-screen">
      <el-header class="bg-white shadow-sm border-b">
        <div class="flex items-center justify-between h-full px-6">
          <div class="flex items-center gap-4">
            <el-icon :size="28" color="#409eff"><Operation /></el-icon>
            <h1 class="text-xl font-bold">实验室管理系统</h1>
          </div>
          <div class="flex items-center gap-4">
            <el-button type="primary" @click="handleLogin">登录</el-button>
          </div>
        </div>
      </el-header>

      <el-main>
        <div class="max-w-5xl mx-auto py-8 px-4">
          <el-card class="mb-6">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-semibold">每日一练</span>
                <el-tag type="success">{{ todayDate }}</el-tag>
              </div>
            </template>

            <div v-if="problem">
              <h2 class="text-2xl font-bold mb-4">{{ problem.title }}</h2>
              <div class="mb-4 text-gray-600">
                <el-tag size="small">时间限制: {{ problem.timeLimit }}ms</el-tag>
                <el-tag size="small" class="ml-2">内存限制: {{ problem.memoryLimit }}MB</el-tag>
              </div>
              <div class="prose mb-6" v-html="problem.description"></div>

              <el-divider />

              <div class="mb-4">
                <el-select v-model="language" placeholder="选择语言">
                  <el-option label="C++" value="cpp" />
                  <el-option label="Java" value="java" />
                  <el-option label="Python" value="python" />
                </el-select>
              </div>

              <el-input
                v-model="code"
                type="textarea"
                :rows="15"
                placeholder="在此输入代码..."
                class="mb-4 font-mono"
              />

              <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
                提交代码
              </el-button>
            </div>

            <el-empty v-else description="今日题目尚未生成" />
          </el-card>

          <el-card v-if="submissions.length > 0">
            <template #header>
              <span class="font-semibold">我的提交记录</span>
            </template>
            <el-table :data="submissions">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="language" label="语言" width="100" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="timeUsed" label="时间(ms)" width="100" />
              <el-table-column prop="createdAt" label="提交时间" />
            </el-table>
          </el-card>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Operation } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { problemAPI, submissionAPI } from '@/api'

const problem = ref<any>(null)
const code = ref('')
const language = ref('cpp')
const submitting = ref(false)
const submissions = ref<any[]>([])

const todayDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN')
})

const handleLogin = () => {
  ElMessage.info('请先完成 CMS OAuth2.0 登录对接')
}

const loadProblem = async () => {
  try {
    const res = await problemAPI.getToday()
    problem.value = res.data.data
  } catch (error: any) {
    ElMessage.warning(error.response?.data?.message || '加载题目失败')
  }
}

const loadSubmissions = async () => {
  try {
    const res = await submissionAPI.getMy()
    submissions.value = res.data.data || []
  } catch (error) {
    // 未登录时忽略
  }
}

const handleSubmit = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请输入代码')
    return
  }

  submitting.value = true
  try {
    const res = await submissionAPI.submit({
      problemId: problem.value.id,
      code: code.value,
      language: language.value
    })

    const result = res.data.data
    if (result.status === 'AC') {
      ElMessage.success('AC! 已自动触发签到')
    } else {
      ElMessage.error(`提交失败: ${result.status}`)
    }

    loadSubmissions()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const getStatusType = (status: string) => {
  const map: any = {
    AC: 'success',
    WA: 'danger',
    TLE: 'warning',
    MLE: 'warning',
    RE: 'danger',
    CE: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadProblem()
  loadSubmissions()
})
</script>
