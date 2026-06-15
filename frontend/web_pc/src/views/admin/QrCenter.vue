<template>
  <ViewPage class="admin-page qr-center-page">
    <ViewToolbar>
      <div>
        <h2 class="page-title">二维码中心</h2>
        <p class="section-subtitle">统一生成任意链接二维码，并投放到独立大屏。</p>
      </div>
      <div class="toolbar__actions">
        <el-button :icon="CopyDocument" @click="copyScreenLink">复制大屏链接</el-button>
        <el-button type="primary" :icon="Monitor" @click="openScreen">打开大屏</el-button>
      </div>
    </ViewToolbar>

    <section class="qr-center">
      <div class="panel qr-center__form">
        <el-form label-position="top">
          <el-form-item label="二维码内容 URL">
            <el-input
              v-model="targetUrl"
              type="textarea"
              :rows="5"
              placeholder="https://example.com/path"
            />
          </el-form-item>
          <el-form-item label="大屏标题">
            <el-input v-model="title" placeholder="扫码加入 / 扫码填写 / 扫码签到" />
          </el-form-item>
          <el-form-item label="大屏副标题">
            <el-input v-model="subtitle" placeholder="OpenAtom 二维码系统" />
          </el-form-item>
        </el-form>

        <div class="qr-center__quick">
          <button
            v-for="preset in presets"
            :key="preset.title"
            type="button"
            @click="applyPreset(preset)"
          >
            {{ preset.title }}
          </button>
        </div>
      </div>

      <div class="panel qr-center__preview">
        <div class="qr-preview__frame">
          <img v-if="targetUrl" :src="qrPreviewUrl" alt="二维码预览" />
          <span v-else>输入 URL 后预览</span>
        </div>
        <div class="qr-preview__meta">
          <strong>{{ title || '二维码大屏' }}</strong>
          <span>{{ subtitle || 'OpenAtom 二维码系统' }}</span>
          <code>{{ targetUrl || '等待 URL' }}</code>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { qrSvgDataUrl } from '@/utils/qr.ts'
import { CopyDocument, Monitor } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const targetUrl = ref('')
const title = ref('扫码进入')
const subtitle = ref('OpenAtom 二维码系统')

const presets = [
  { title: '扫码填写', subtitle: '打开在线表单，完成信息提交' },
  { title: '扫码签到', subtitle: '进入签到页面，完成现场确认' },
  { title: '扫码查看', subtitle: '打开活动、公告或项目详情' },
]

const qrPreviewUrl = computed(() => (targetUrl.value ? qrSvgDataUrl(targetUrl.value) : ''))

function buildScreenLink() {
  const href = router.resolve({
    path: '/qr-screen',
    query: {
      url: targetUrl.value,
      title: title.value || '扫码进入',
      subtitle: subtitle.value || 'OpenAtom 二维码系统',
    },
  }).href
  return `${window.location.origin}${href}`
}

function applyPreset(preset: { title: string; subtitle: string }) {
  title.value = preset.title
  subtitle.value = preset.subtitle
}

async function copyScreenLink() {
  if (!targetUrl.value) {
    ElMessage.warning('请先输入 URL')
    return
  }
  const link = buildScreenLink()
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('大屏链接已复制')
  } catch {
    ElMessage.info(link)
  }
}

function openScreen() {
  if (!targetUrl.value) {
    ElMessage.warning('请先输入 URL')
    return
  }
  window.open(buildScreenLink(), '_blank')
}
</script>

<style scoped>
.qr-center-page {
  max-width: 1180px;
  margin: 0 auto;
}

.qr-center {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.qr-center__form,
.qr-center__preview {
  padding: 22px;
}

.qr-center__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.qr-center__quick button {
  min-height: 40px;
  padding: 0 14px;
  color: var(--oa-button-text);
  background: var(--oa-button-bg);
  border: 1px solid var(--oa-button-border);
  border-radius: var(--oa-radius-pill);
  cursor: pointer;
  transition:
    transform 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.qr-center__quick button:hover {
  background: var(--oa-button-hover-bg);
  border-color: var(--oa-button-hover-border);
}

.qr-center__quick button:active {
  transform: scale(0.95);
}

.qr-center__preview {
  display: grid;
  align-content: start;
  gap: 18px;
}

.qr-preview__frame {
  display: grid;
  aspect-ratio: 1;
  place-items: center;
  padding: 22px;
  background: #ffffff;
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius-md);
}

.qr-preview__frame img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-preview__frame span {
  color: var(--oa-muted);
  font-weight: 600;
}

.qr-preview__meta {
  display: grid;
  gap: 8px;
}

.qr-preview__meta strong {
  font-size: 22px;
  font-weight: 600;
}

.qr-preview__meta span {
  color: var(--oa-muted);
}

.qr-preview__meta code {
  overflow: hidden;
  padding: 10px 12px;
  color: var(--oa-text-soft);
  background: var(--oa-page-soft-bg);
  border-radius: var(--oa-radius-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 860px) {
  .qr-center {
    grid-template-columns: 1fr;
  }
}
</style>
