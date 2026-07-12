<template>
  <ViewPage class="site-system-page">
    <SitePageHero
      eyebrow="入会申请"
      title="加入开放原子开源社团"
      description="查看当前开放的招新计划，选择合适的表单开始申请。"
    >
      <template #actions>
        <div class="recruitment-actions">
<!--          <el-select-->
<!--            v-if="clubs.length > 1"-->
<!--            v-model="selectedClubId"-->
<!--            filterable-->
<!--            placeholder="选择社团"-->
<!--            style="width: 240px"-->
<!--            @change="fetchCampaigns"-->
<!--          >-->
<!--            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />-->
<!--          </el-select>-->
          <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
        </div>
      </template>
    </SitePageHero>

    <section class="site-system-section">
      <div class="container recruitment-shell">
        <SiteSectionHeading
          eyebrow="开放计划"
          title="当前可提交"
          description="开放中的招新表单会按收集时间展示。"
        />

        <div class="recruitment-content site-reveal">
          <el-empty
            v-if="!selectedClubId && !loading"
            class="site-system-empty"
            description="请先选择社团"
          />
          <el-empty
            v-else-if="selectedClubId && !campaigns.length && !loading"
            class="site-system-empty"
            :description="
              clubs.length > 1 ? '该社团暂无开放中的招新表单' : '当前暂无开放中的招新表单'
            "
          />
          <el-timeline v-else v-loading="loading">
            <el-timeline-item
              v-for="item in campaigns"
              :key="item.id"
              :timestamp="formatRange(item)"
              placement="top"
            >
              <div class="campaign-item site-system-surface">
                <div>
                  <h3>{{ item.name || '招新计划' }}</h3>
                  <p>提交方式：无需登录填写</p>
                </div>
                <div class="campaign-item__action">
                  <el-tag :type="statusType(item.status)">{{ statusText(item.status) }}</el-tag>
                  <el-button type="primary" @click="$router.push(`/apply/${item.id}`)">
                    立即填写
                  </el-button>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import SitePageHero from '@/components/site/shell/SitePageHero.vue'
import SiteSectionHeading from '@/components/site/shell/SiteSectionHeading.vue'
import { Refresh } from '@element-plus/icons-vue'
import { clubApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const clubs = ref<any[]>([])

const campaigns = ref<any[]>([])

const selectedClubId = ref('')

async function loadAll() {
  loading.value = true
  try {
    const result = await clubApi.list({ status: 'active' })
    clubs.value = result?.list || result || []
    if (!selectedClubId.value && clubs.value.length) {
      selectedClubId.value = clubs.value[0].id
    }
    if (selectedClubId.value) {
      await fetchCampaigns()
    }
  } finally {
    loading.value = false
  }
}

async function fetchCampaigns() {
  if (!selectedClubId.value) return
  loading.value = true
  try {
    const result = await clubApi.campaigns(selectedClubId.value)
    const list = result?.list || result || []
    campaigns.value = list.filter((item) => ['open', 'published'].includes(item.status))
  } finally {
    loading.value = false
  }
}

function formatRange(item: any) {
  return `${formatDateTime(item.applyStartAt)} - ${formatDateTime(item.applyEndAt)}`
}

function statusText(status: any) {
  return (
    {
      draft: '草稿',
      published: '已发布',
      open: '收集中',
      closed: '已结束',
      archived: '已归档',
    }[status] ||
    status ||
    '-'
  )
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.recruitment-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

.recruitment-shell {
  display: grid;
  gap: 32px;
}

.campaign-item {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: 24px;
}

.campaign-item h3 {
  margin: 0 0 8px;
}

.campaign-item p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.campaign-item__action {
  display: flex;
  flex: 0 0 150px;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.recruitment-content :deep(.el-timeline-item__node) {
  background: var(--oa-primary);
}

.recruitment-content :deep(.el-timeline-item__tail) {
  border-left-color: var(--oa-border);
}

.recruitment-content :deep(.el-timeline-item__timestamp) {
  color: var(--oa-muted);
}

@media (max-width: 720px) {
  .campaign-item {
    flex-direction: column;
  }

  .campaign-item__action {
    flex-basis: auto;
    align-items: flex-start;
  }
}
</style>
