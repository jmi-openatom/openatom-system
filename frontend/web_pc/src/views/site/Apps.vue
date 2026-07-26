<template>
  <ViewPage :loading="loading" class="apps-page">
    <section class="apps-hero">
      <div class="container apps-hero__inner">
        <div class="section-heading reveal-block">
          <span>OPENATOM APPLICATIONS</span>
          <h1>应用展示</h1>
          <p>集中展示社团自研应用、开源项目和可下载工具，便于成员与合作方快速了解当前成果。</p>
        </div>
        <div class="apps-hero__meta">
          <span>{{ rows.length }} 个应用</span>
          <span>{{ openSourceCount }} 个已开源</span>
        </div>
      </div>
    </section>

    <section class="apps-content-section">
      <div class="container apps-content">
        <div class="apps-toolbar">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索应用、开发者、协议"
            @clear="fetchList"
            @keyup.enter="fetchList"
          />
          <el-segmented
            v-model="query.openSource"
            :options="openSourceOptions"
            @change="fetchList"
          />
          <el-button type="primary" :icon="Search" @click="fetchList">搜索</el-button>
        </div>

        <div v-if="rows.length" class="apps-grid">
          <article v-for="app in rows" :key="app.id" class="app-card reveal-card">
            <div class="app-card__media" @click="goDetail(app.id)">
              <img v-if="app.coverUrl" :alt="app.name" :src="app.coverUrl" />
              <span v-else>{{ appInitial(app.name) }}</span>
            </div>
            <div class="app-card__body">
              <div class="app-card__head">
                <div>
                  <h2 @click="goDetail(app.id)">{{ app.name }}</h2>
                  <p>{{ app.summary || '暂无简介' }}</p>
                </div>
                <div class="app-card__badges">
                  <el-tag v-if="app.appStatus" effect="plain">{{ app.appStatus }}</el-tag>
                  <el-tag :type="app.openSource ? 'success' : 'info'">
                    {{ app.openSource ? '已开源' : '未开源' }}
                  </el-tag>
                </div>
              </div>
              <dl class="app-meta">
                <div>
                  <dt>开发者</dt>
                  <dd>{{ app.developer || '-' }}</dd>
                </div>
                <div>
                  <dt>所有者</dt>
                  <dd>{{ app.owner || '-' }}</dd>
                </div>
                <div>
                  <dt>协议</dt>
                  <dd>{{ app.licenseName || '-' }}</dd>
                </div>
                <div>
                  <dt>版本</dt>
                  <dd>{{ app.version || '-' }}</dd>
                </div>
              </dl>
              <div class="app-card__links">
                <el-button
                  v-if="app.githubUrl"
                  :icon="Link"
                  plain
                  size="small"
                  @click.stop="openExternal(app.githubUrl)"
                >
                  GitHub
                </el-button>
                <el-button
                  v-if="app.giteeUrl"
                  :icon="Link"
                  plain
                  size="small"
                  @click.stop="openExternal(app.giteeUrl)"
                >
                  Gitee
                </el-button>
                <el-button
                  v-if="app.atomgitUrl"
                  :icon="Link"
                  plain
                  size="small"
                  @click.stop="openExternal(app.atomgitUrl)"
                >
                  AtomGit
                </el-button>
                <el-button
                  v-if="app.downloadUrl"
                  :icon="Download"
                  plain
                  size="small"
                  type="primary"
                  @click.stop="openExternal(app.downloadUrl)"
                >
                  下载
                </el-button>
                <el-button size="small" @click="goDetail(app.id)">详情</el-button>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-else-if="!loading" description="暂无应用" />
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, Link, Search } from '@element-plus/icons-vue'
import ViewPage from '@/components/common/ViewPage.vue'
import { siteApi } from '@/api'
import { useRouter } from 'vue-router'

interface ShowcaseApp {
  id: number
  name: string
  summary?: string
  coverUrl?: string
  openSource: boolean
  githubUrl?: string
  giteeUrl?: string
  atomgitUrl?: string
  developer?: string
  owner?: string
  licenseName?: string
  version?: string
  appStatus?: string
  downloadUrl?: string
}

const loading = ref(false)
const rows = ref<ShowcaseApp[]>([])
const router = useRouter()

const query = reactive({
  keyword: '',
  openSource: 'all',
})

const openSourceOptions = [
  { label: '全部', value: 'all' },
  { label: '已开源', value: 'true' },
  { label: '未开源', value: 'false' },
]

const openSourceCount = computed(() => rows.value.filter((app) => app.openSource).length)

async function fetchList() {
  loading.value = true
  try {
    const data = await siteApi.showcaseApps({
      keyword: query.keyword || undefined,
      openSource: query.openSource === 'all' ? undefined : query.openSource === 'true',
    })
    rows.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

function openExternal(url?: string) {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

function goDetail(id: number) {
  router.push(`/apps/${id}`)
}

function appInitial(name: string) {
  return String(name || 'A')
    .slice(0, 1)
    .toUpperCase()
}

onMounted(fetchList)
</script>

<style scoped>
.apps-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.apps-hero {
  position: relative;
  min-height: clamp(440px, 38vw, 560px);
  overflow: hidden;
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  border-bottom: 1px solid var(--oa-border);
}

.apps-hero::after {
  display: none;
}

.apps-hero::before {
  position: absolute;
  inset: 0;
  background: #faf9f7 url('/apps-hero-wide-light.png?v=3') right center / auto 100% no-repeat;
  content: '';
  pointer-events: none;
}

.apps-hero__inner {
  position: relative;
  z-index: 1;
  display: grid;
  min-height: clamp(440px, 38vw, 560px);
  align-content: center;
  gap: 24px;
  padding-top: clamp(48px, 7vw, 84px);
  padding-bottom: clamp(48px, 7vw, 84px);
}

.apps-hero .section-heading {
  position: relative;
  z-index: 1;
  width: min(47%, 520px);
  margin: 0;
  text-align: left;
}

.apps-hero .section-heading span {
  color: var(--oa-muted-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.apps-hero .section-heading h1 {
  margin: 12px 0 16px;
  color: var(--oa-text);
  font-size: clamp(40px, 4.6vw, 58px);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: 0;
}

.apps-hero .section-heading p {
  max-width: 500px;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.72;
}

.apps-hero__meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.apps-hero__meta span {
  padding-right: 12px;
  color: var(--oa-muted);
  border-right: 1px solid var(--oa-border);
  font-size: 13px;
}

.apps-hero__meta span:last-child {
  border-right: 0;
}

.apps-content-section {
  padding: 0 0 clamp(64px, 8vw, 96px);
}

.apps-content {
  display: grid;
  gap: 24px;
}

.apps-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 16px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 96%, transparent);
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  backdrop-filter: saturate(150%) blur(22px);
  -webkit-backdrop-filter: saturate(150%) blur(22px);
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.app-card {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  overflow: hidden;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.app-card__media {
  display: grid;
  min-height: 230px;
  place-items: center;
  overflow: hidden;
  color: var(--oa-active-text);
  background: var(--oa-active-bg);
  font-size: 56px;
  font-weight: 700;
  cursor: pointer;
}

.app-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__body {
  display: grid;
  gap: 18px;
  padding: 22px;
}

.app-card__head {
  display: flex;
  min-width: 0;
  justify-content: space-between;
  gap: 14px;
}

.app-card h2 {
  margin: 0;
  color: var(--oa-text);
  font-size: 24px;
  font-weight: 650;
  letter-spacing: 0;
  cursor: pointer;
}

.app-card p {
  margin: 8px 0 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.app-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.app-meta div {
  min-width: 0;
  padding: 10px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.app-meta dt {
  color: var(--oa-muted);
  font-size: 12px;
}

.app-meta dd {
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--oa-text);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.app-card__links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.app-card__links :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 1120px) {
  .apps-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .apps-hero {
    min-height: 650px;
  }

  .apps-hero__inner {
    min-height: 650px;
    align-content: start;
    padding-top: 36px;
    padding-bottom: 330px;
  }

  .apps-hero::before {
    background-position: 68% bottom;
    background-size: auto 340px;
  }

  .apps-hero .section-heading {
    width: 100%;
  }

  .apps-toolbar {
    grid-template-columns: 1fr;
  }

  .app-card {
    grid-template-columns: 1fr;
  }

  .app-card__media {
    min-height: 180px;
  }

  .app-card__head {
    flex-direction: column;
    align-items: flex-start;
  }
}

:global(html.dark .apps-hero::before) {
  background-color: #0d0d0f;
  background-image: url('/apps-hero-wide-dark.png?v=3');
}

.app-card__badges {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
</style>
