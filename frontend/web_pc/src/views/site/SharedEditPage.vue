<template>
  <div class="doc-edit-page">
    <div v-if="error" class="doc-edit-error">
      <h1>无法打开编辑器</h1>
      <p>{{ error }}</p>
      <el-button type="primary" @click="$router.push('/admin/doc-center')">返回文件架</el-button>
    </div>
    <div v-else-if="loading" class="doc-edit-loading"><span class="spinner"></span><p>正在打开编辑器…</p></div>
    <div id="onlyoffice-editor" class="doc-edit-host"></div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { sharedFilesApi } from '@/api'

const route = useRoute()
const loading = ref(true)
const error = ref('')
let editorInstance: any = null
let sdkLoaded = false
let sdkLoading: Promise<void> | null = null

async function loadSdk(serverUrl: string): Promise<void> {
  if (sdkLoaded) return
  if (sdkLoading) return sdkLoading
  sdkLoading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const version = (import.meta.env.VITE_APP_VERSION || 'dev').replace(/[^a-zA-Z0-9._-]/g, '')
    script.src = `${serverUrl.replace(/\/+$/, '')}/web-apps/apps/api/documents/api.js?v=${version}`
    script.onload = () => {
      sdkLoaded = true
      resolve()
    }
    script.onerror = () => {
      sdkLoading = null
      reject(new Error('编辑器组件加载失败'))
    }
    document.head.append(script)
  })
  return sdkLoading
}

async function init() {
  const id = route.params.id
  const password = typeof route.query.password === 'string' ? route.query.password : undefined
  if (!id) {
    error.value = '缺少文件参数。'
    loading.value = false
    return
  }
  try {
    const result = await sharedFilesApi.editConfig(String(id), password)
    await loadSdk(result.documentServerUrl)
    await nextTick()
    const api = (window as any).DocsAPI
    if (!api?.DocEditor) {
      error.value = '编辑器组件加载失败，请刷新后重试。'
      return
    }
    editorInstance = new api.DocEditor('onlyoffice-editor', result.config)
  } catch (err: any) {
    error.value = err?.message || '无法打开编辑器，请稍后重试。'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (editorInstance?.destroyEditor) {
    editorInstance.destroyEditor()
  }
  editorInstance = null
})

void init()
</script>

<style scoped>
.doc-edit-page {
  position: fixed;
  inset: 0;
  background: #fff;
}

.doc-edit-host {
  width: 100%;
  height: 100%;
}

.doc-edit-loading,
.doc-edit-error {
  height: 100%;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
}

.doc-edit-error h1 {
  margin: 0;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.doc-edit-error p {
  margin: 0 0 8px;
}

.spinner {
  width: 26px;
  height: 26px;
  border: 3px solid var(--el-border-color);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: doc-edit-spin 0.8s linear infinite;
}

@keyframes doc-edit-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
