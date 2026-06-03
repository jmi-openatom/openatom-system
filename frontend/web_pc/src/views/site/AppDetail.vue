<template>
  <ViewPage :loading="loading" class="app-detail-page">
    <section class="app-detail-hero">
      <div class="container app-detail-hero__inner">
        <el-button class="back-button" plain :icon="ArrowLeft" @click="$router.push('/apps')">
          返回应用
        </el-button>
        <div v-if="app" class="app-detail-hero__content">
          <div class="app-detail-copy">
            <div class="app-detail-badges">
              <el-tag v-if="app.appStatus" effect="plain">{{ app.appStatus }}</el-tag>
              <el-tag :type="app.openSource ? 'success' : 'info'">
                {{ app.openSource ? '已开源' : '未开源' }}
              </el-tag>
            </div>
            <h1>{{ app.name }}</h1>
            <p>{{ app.summary || '暂无简介' }}</p>
            <div class="app-detail-actions">
              <el-button
                v-if="app.githubUrl"
                :icon="Link"
                plain
                size="large"
                @click="openExternal(app.githubUrl)"
              >
                GitHub
              </el-button>
              <el-button
                v-if="app.giteeUrl"
                :icon="Link"
                plain
                size="large"
                @click="openExternal(app.giteeUrl)"
              >
                Gitee
              </el-button>
              <el-button
                v-if="app.downloadUrl"
                :icon="Download"
                size="large"
                type="primary"
                @click="openExternal(app.downloadUrl)"
              >
                下载
              </el-button>
            </div>
          </div>
          <div class="app-detail-media">
            <img v-if="app.coverUrl" :alt="app.name" :src="app.coverUrl" />
            <span v-else>{{ appInitial(app.name) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="app-detail-section">
      <div class="container app-detail-main">
        <template v-if="app">
          <div class="app-detail-panel">
            <h2>应用信息</h2>
            <dl class="app-detail-meta">
              <div>
                <dt>应用状态</dt>
                <dd>{{ app.appStatus || '-' }}</dd>
              </div>
              <div>
                <dt>开源状态</dt>
                <dd>{{ app.openSource ? '已开源' : '未开源' }}</dd>
              </div>
              <div>
                <dt>开发者</dt>
                <dd>{{ app.developer || '-' }}</dd>
              </div>
              <div>
                <dt>所有者</dt>
                <dd>{{ app.owner || '-' }}</dd>
              </div>
              <div>
                <dt>开源协议</dt>
                <dd>{{ app.licenseName || '-' }}</dd>
              </div>
              <div>
                <dt>版本</dt>
                <dd>{{ app.version || '-' }}</dd>
              </div>
            </dl>
          </div>

          <div class="app-detail-panel">
            <h2>相关链接</h2>
            <div class="link-list">
              <button v-if="app.githubUrl" type="button" @click="openExternal(app.githubUrl)">
                <span>GitHub</span>
                <strong>{{ app.githubUrl }}</strong>
              </button>
              <button v-if="app.giteeUrl" type="button" @click="openExternal(app.giteeUrl)">
                <span>Gitee</span>
                <strong>{{ app.giteeUrl }}</strong>
              </button>
              <button v-if="app.downloadUrl" type="button" @click="openExternal(app.downloadUrl)">
                <span>下载链接</span>
                <strong>{{ app.downloadUrl }}</strong>
              </button>
              <el-empty
                v-if="!app.githubUrl && !app.giteeUrl && !app.downloadUrl"
                description="暂无相关链接"
              />
            </div>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="应用不存在或未发布" />
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Download, Link } from '@element-plus/icons-vue'
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api'

interface ShowcaseApp {
  id: number
  name: string
  summary?: string
  coverUrl?: string
  openSource: boolean
  githubUrl?: string
  giteeUrl?: string
  developer?: string
  owner?: string
  licenseName?: string
  version?: string
  appStatus?: string
  downloadUrl?: string
}

const route = useRoute()
const loading = ref(false)
const app = ref<ShowcaseApp | null>(null)

async function fetchDetail() {
  const id = route.params.id
  if (!id) {
    app.value = null
    return
  }
  loading.value = true
  try {
    const data = await siteApi.showcaseAppDetail(String(id))
    app.value = data && typeof data === 'object' ? data : null
  } catch (_error) {
    app.value = null
  } finally {
    loading.value = false
  }
}

function openExternal(url?: string) {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

function appInitial(name: string) {
  return String(name || 'A').slice(0, 1).toUpperCase()
}

onMounted(fetchDetail)
watch(() => route.params.id, fetchDetail)
</script>

<style scoped>
.app-detail-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.app-detail-hero {
  padding: clamp(86px, 10vw, 126px) 0 clamp(36px, 5vw, 66px);
  background: var(--oa-elevated-bg);
  border-bottom: 1px solid var(--oa-border);
}

.app-detail-hero__inner {
  display: grid;
  gap: 26px;
}

.back-button {
  width: fit-content;
}

.app-detail-hero__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 460px);
  gap: clamp(24px, 5vw, 56px);
  align-items: center;
}

.app-detail-copy {
  min-width: 0;
}

.app-detail-badges,
.app-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.app-detail-copy h1 {
  margin: 18px 0;
  color: var(--oa-text);
  font-size: clamp(42px, 6vw, 82px);
  font-weight: 650;
  line-height: 0.98;
  letter-spacing: 0;
}

.app-detail-copy p {
  max-width: 760px;
  margin: 0 0 26px;
  color: var(--oa-muted);
  font-size: 18px;
  line-height: 1.8;
}

.app-detail-media {
  display: grid;
  aspect-ratio: 4 / 3;
  place-items: center;
  overflow: hidden;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  font-size: 72px;
  font-weight: 700;
}

.app-detail-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-detail-section {
  padding: clamp(38px, 5vw, 72px) 0;
}

.app-detail-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 420px);
  gap: 18px;
}

.app-detail-panel {
  padding: 24px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.app-detail-panel h2 {
  margin: 0 0 18px;
  color: var(--oa-text);
  font-size: 22px;
  font-weight: 650;
  letter-spacing: 0;
}

.app-detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.app-detail-meta div {
  min-width: 0;
  padding: 14px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.app-detail-meta dt,
.link-list span {
  color: var(--oa-muted);
  font-size: 12px;
}

.app-detail-meta dd {
  margin: 7px 0 0;
  overflow-wrap: anywhere;
  color: var(--oa-text);
  font-weight: 600;
}

.link-list {
  display: grid;
  gap: 10px;
}

.link-list button {
  display: grid;
  width: 100%;
  gap: 6px;
  padding: 14px;
  text-align: left;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  cursor: pointer;
}

.link-list button:hover {
  border-color: var(--oa-active-bg);
}

.link-list strong {
  overflow-wrap: anywhere;
  color: var(--oa-text);
  font-weight: 600;
}

@media (max-width: 900px) {
  .app-detail-hero__content,
  .app-detail-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .app-detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
