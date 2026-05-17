<template>
  <ViewPage class="site-page">
    <section class="container">
      <ViewToolbar>
        <div>
          <h1 class="page-title">社团库</h1>
          <p class="section-subtitle">查看已上线社团，了解分类、介绍与当前招新状态。</p>
        </div>
        <div class="toolbar__filters">
          <el-input
            v-model="query.keyword"
            placeholder="搜索社团名称"
            clearable
            style="width: 220px"
            @keyup.enter="fetchClubs"
          />
          <el-select v-model="query.category" placeholder="分类" clearable style="width: 150px">
            <el-option label="学术科技" value="academic" />
            <el-option label="文体兴趣" value="interest" />
            <el-option label="公益实践" value="practice" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="fetchClubs">查询</el-button>
        </div>
      </ViewToolbar>

      <el-skeleton :loading="loading" animated :rows="6">
        <div class="club-grid">
          <article class="club-card" v-for="club in clubs" :key="club.id">
            <div class="club-card__logo">{{ shortName(club.name) }}</div>
            <div class="club-card__body">
              <div class="club-card__title">
                <h3>{{ club.name }}</h3>
                <el-tag size="small" :type="statusType(club.recruitmentStatus)">
                  {{ recruitmentStatusText(club.recruitmentStatus || 'unknown') }}
                </el-tag>
              </div>
              <p>{{ club.description || '该社团暂未填写介绍。' }}</p>
              <div class="club-card__meta">
                <span>编号：{{ club.code || '-' }}</span>
                <span>分类：{{ club.category || '-' }}</span>
              </div>
            </div>
          </article>
        </div>
      </el-skeleton>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { Search } from '@element-plus/icons-vue'
import { clubApi } from '@/api'
import { recruitmentStatusText, statusType } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const clubs = ref<any[]>([])

const query = ref({
  keyword: '',
  category: '',
  status: 'enabled',
})

function shortName(name: any) {
  return (name || '社团').slice(0, 2)
}

async function fetchClubs() {
  loading.value = true
  try {
    const result = await clubApi.list(query.value)
    // 兼容后端分页和普通数组两类返回结构。
    clubs.value = result?.list || result || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchClubs()
})
</script>

<style scoped>
.site-page {
  padding: 64px 0 80px;
  background: var(--oa-page-soft-bg);
}

.club-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  margin-top: 24px;
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-border);
}

.club-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 260px;
  padding: 24px;
  background: var(--oa-elevated-bg);
  border: 0;
  border-radius: 0;
  box-shadow: none;
  backdrop-filter: none;
  animation: oaFadeUp 0.44s ease both;
  transition: background-color 0.2s ease;
}

.club-card:hover {
  transform: none;
  background: var(--oa-button-subtle-bg);
  border-color: transparent;
  box-shadow: none;
}

.club-card__logo {
  display: grid;
  flex: 0 0 58px;
  width: 58px;
  height: 58px;
  place-items: center;
  color: #ffffff;
  background: #1d1d1f;
  border-radius: 8px;
  font-weight: 600;
}

.club-card__body {
  min-width: 0;
}

.club-card__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.club-card h3 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0;
}

.club-card p {
  min-height: 64px;
  margin: 12px 0;
  color: var(--oa-muted);
  line-height: 1.6;
}

.club-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--oa-muted);
  font-size: 13px;
}

@media (max-width: 980px) {
  .club-grid {
    grid-template-columns: 1fr;
  }
}
</style>
