<template>
  <ViewPage class="image-hosting-page" :loading="loading">
    <section class="image-hosting-hero home-interactive-section">
      <div class="container image-hosting-hero__inner">
        <div class="section-heading reveal-block image-hosting-heading">
          <span>Image Hosting</span>
          <h1>图床</h1>
          <p>上传项目截图、文章配图和活动素材，生成可复制的图片链接与 Markdown 片段。</p>
        </div>
        <div class="image-hosting-uploader">
          <el-upload
            accept="image/jpeg,image/png,image/gif,image/webp"
            :auto-upload="false"
            drag
            :file-list="uploadFiles"
            :limit="1"
            :on-change="handleImageChange"
            :on-remove="handleImageRemove"
          >
            <el-icon class="el-icon--upload">
              <UploadFilled />
            </el-icon>
            <div class="el-upload__text">拖拽图片到这里，或点击选择图片</div>
            <template #tip>
              <div class="el-upload__tip">支持 JPG、PNG、GIF、WebP，单张最大 10MB。</div>
            </template>
          </el-upload>
          <div class="image-hosting-uploader__actions">
            <el-button :icon="Refresh" @click="reload">刷新</el-button>
            <el-button type="primary" :icon="Upload" :loading="uploading" @click="uploadImage">
              上传图片
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="image-hosting-main home-interactive-section">
      <div class="container image-hosting-shell">
        <ViewToolbar>
          <div class="toolbar__filters">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="搜索文件名或链接"
              style="width: 260px"
              @keyup.enter="reload"
              @clear="reload"
            />
            <el-button :icon="Refresh" @click="reload">刷新</el-button>
          </div>
        </ViewToolbar>

        <div v-if="uploadResult.url" class="upload-result">
          <el-image :src="uploadResult.url" fit="cover" />
          <div class="upload-result__content">
            <strong>上传成功</strong>
            <el-input :model-value="uploadResult.url" readonly />
            <el-input :model-value="uploadResult.markdown" readonly />
            <div class="upload-result__actions">
              <el-button :icon="Link" @click="copyText(uploadResult.url, '链接已复制')">复制链接</el-button>
              <el-button :icon="DocumentCopy" @click="copyText(uploadResult.markdown, 'Markdown 已复制')">
                复制 Markdown
              </el-button>
            </div>
          </div>
        </div>

        <div class="image-grid">
          <article v-for="image in rows" :key="image.id" class="image-tile">
            <div class="image-tile__media">
              <el-image :src="image.url" fit="cover" lazy :preview-src-list="[image.url]" />
            </div>
            <div class="image-tile__body">
              <strong>{{ image.originalName || image.fileName }}</strong>
              <span>{{ formatFileSize(image.fileSize) }} · {{ formatDateTime(image.createdAt) }}</span>
              <div class="image-tile__actions">
                <el-button :icon="Link" circle @click="copyText(image.url, '链接已复制')" />
                <el-button :icon="DocumentCopy" circle @click="copyText(image.markdown, 'Markdown 已复制')" />
                <el-popconfirm title="删除后图片链接会失效，确定删除？" @confirm="removeImage(image)">
                  <template #reference>
                    <el-button :icon="Delete" circle type="danger" />
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-if="!rows.length && !loading" description="暂无图片" />

        <el-pagination
          v-if="total > query.pageSize"
          class="pager"
          background
          layout="prev, pager, next"
          :current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { imageHostingApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Delete, DocumentCopy, Link, Refresh, Upload, UploadFilled } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'

const loading = ref(false)
const uploading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const uploadFiles = ref<any[]>([])
const uploadResult = ref<Record<string, any>>({})
const query = ref({
  keyword: '',
  page: 1,
  pageSize: 12,
})

async function fetchList() {
  loading.value = true
  try {
    const data = await imageHostingApi.myImages({
      keyword: query.value.keyword || undefined,
      page: query.value.page,
      pageSize: query.value.pageSize,
    })
    rows.value = data?.list || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function handleImageChange(_file: any, fileList: any[]) {
  uploadFiles.value = fileList.slice(-1)
  uploadResult.value = {}
}

function handleImageRemove(_file: any, fileList: any[]) {
  uploadFiles.value = fileList
  uploadResult.value = {}
}

async function uploadImage() {
  const file = uploadFiles.value[0]?.raw
  if (!file) {
    ElMessage.warning('请先选择图片')
    return
  }
  uploading.value = true
  try {
    uploadResult.value = await imageHostingApi.upload(file)
    uploadFiles.value = []
    ElMessage.success('图片上传成功')
    reload()
  } finally {
    uploading.value = false
  }
}

async function removeImage(image: any) {
  await imageHostingApi.remove(image.id)
  ElMessage.success('图片已删除')
  fetchList()
}

async function copyText(value: string, message: string) {
  if (!value) return
  try {
    await navigator.clipboard?.writeText(value)
    ElMessage.success(message)
  } catch (_error) {
    ElMessage.warning('当前浏览器不允许直接复制，请手动选择文本')
  }
}

function formatFileSize(value: number) {
  const size = Number(value || 0)
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${size} B`
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.image-hosting-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.image-hosting-hero,
.image-hosting-main {
  position: relative;
}

.image-hosting-hero {
  background: var(--oa-elevated-bg);
  border-bottom: 1px solid var(--oa-border);
}

.image-hosting-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 460px);
  gap: 32px;
  align-items: center;
  padding: 96px 0 54px;
}

.image-hosting-heading {
  max-width: 760px;
}

.image-hosting-heading h1 {
  margin: 10px 0 12px;
  color: var(--oa-text);
  font-size: clamp(36px, 5vw, 64px);
  line-height: 1.08;
  letter-spacing: 0;
}

.image-hosting-heading p {
  max-width: 720px;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.7;
}

.image-hosting-uploader,
.upload-result {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.image-hosting-uploader__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.image-hosting-main {
  padding: 38px 0 64px;
}

.image-hosting-shell {
  display: grid;
  gap: 18px;
}

.upload-result {
  grid-template-columns: 220px minmax(0, 1fr);
  align-items: start;
}

.upload-result .el-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.upload-result__content {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.upload-result__actions,
.image-tile__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.image-tile {
  overflow: hidden;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.image-tile__media {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--oa-page-bg);
}

.image-tile__media .el-image {
  width: 100%;
  height: 100%;
}

.image-tile__body {
  display: grid;
  gap: 8px;
  padding: 14px;
}

.image-tile__body strong {
  overflow: hidden;
  color: var(--oa-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-tile__body span {
  color: var(--oa-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .image-hosting-hero__inner,
  .upload-result {
    grid-template-columns: 1fr;
  }

  .image-hosting-hero__inner {
    padding-top: 78px;
  }
}
</style>
