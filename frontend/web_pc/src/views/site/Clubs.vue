<template>
  <div class="site-page">
    <section class="container">
      <div class="toolbar">
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
      </div>

      <el-skeleton :loading="loading" animated :rows="6">
        <div class="club-grid">
          <article class="club-card" v-for="club in clubs" :key="club.id">
            <div class="club-card__logo">{{ shortName(club.name) }}</div>
            <div class="club-card__body">
              <div class="club-card__title">
                <h3>{{ club.name }}</h3>
                <el-tag size="small" :type="statusType(club.recruitmentStatus)">
                  {{ club.recruitmentStatus || 'unknown' }}
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
  </div>
</template>

<script>
import { Search } from '@element-plus/icons-vue'
import { clubApi } from '@/api'
import { statusType } from '@/utils/format.ts'

export default {
  name: 'SiteClubs',
  data() {
    return {
      Search,
      loading: false,
      clubs: [],
      query: {
        keyword: '',
        category: '',
        status: 'enabled',
      },
    }
  },
  created() {
    this.fetchClubs()
  },
  methods: {
    statusType,
    shortName(name) {
      return (name || '社团').slice(0, 2)
    },
    async fetchClubs() {
      this.loading = true
      try {
        const result = await clubApi.list(this.query)
        // 兼容后端分页和普通数组两类返回结构。
        this.clubs = result?.list || result || []
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.site-page {
  padding: 34px 0;
}

.club-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.club-card {
  display: flex;
  gap: 16px;
  min-height: 178px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 22px;
  box-shadow: var(--oa-shadow);
  backdrop-filter: blur(14px);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.club-card:hover {
  transform: translateY(-3px);
  border-color: rgba(147, 197, 253, 0.95);
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.12);
}

.club-card__logo {
  display: grid;
  flex: 0 0 58px;
  height: 58px;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 16px;
  font-weight: 800;
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
  letter-spacing: -0.01em;
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
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 980px) {
  .club-grid {
    grid-template-columns: 1fr;
  }
}
</style>
