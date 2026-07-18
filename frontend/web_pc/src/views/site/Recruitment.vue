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
          <el-button
            class="refresh-button"
            :icon="Refresh"
            :loading="loading"
            aria-label="刷新招新计划"
            @click="loadAll"
          >
            刷新计划
          </el-button>
        </div>
      </template>
    </SitePageHero>

    <section class="site-system-section">
      <div class="container recruitment-shell">
        <div class="recruitment-heading">
          <SiteSectionHeading
            eyebrow="OPEN CAMPAIGNS"
            title="选择你感兴趣的计划"
            description="每一份申请都会被认真阅读。请留意截止时间，并选择合适的招新计划开始填写。"
            align="left"
          />
          <div class="campaign-count" aria-live="polite">
            <strong>{{ campaigns.length }}</strong>
            <span>个计划开放中</span>
          </div>
        </div>

        <div class="recruitment-content site-reveal">
          <el-empty
            v-if="!campaigns.length && !loading"
            class="site-system-empty"
            description="当前暂无开放中的招新表单"
          />
          <div v-else v-loading="loading" class="campaign-grid">
            <article v-for="(item, index) in campaigns" :key="item.id" class="campaign-card">
              <div class="campaign-card__topline">
                <span class="campaign-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <el-tag round effect="light" :type="statusType(item.status)">
                  {{ statusText(item.status) }}
                </el-tag>
              </div>

              <div class="campaign-card__content">
                <h3>{{ item.name || '招新计划' }}</h3>
                <p>填写申请，让我们了解你的兴趣、经历与期待。</p>
              </div>

              <dl class="campaign-meta">
                <div>
                  <dt><CalendarDays :size="17" aria-hidden="true" /> 申请时间</dt>
                  <dd>{{ formatRange(item) }}</dd>
                </div>
                <div>
                  <dt><UserRoundCheck :size="17" aria-hidden="true" /> 提交方式</dt>
                  <dd>无需登录，可直接填写</dd>
                </div>
              </dl>

              <el-button
                class="campaign-card__action"
                type="primary"
                @click="$router.push(`/apply/${item.id}`)"
              >
                开始填写
                <ArrowUpRight :size="18" aria-hidden="true" />
              </el-button>
            </article>
          </div>
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
import { ArrowUpRight, CalendarDays, UserRoundCheck } from 'lucide-vue-next'
import { siteApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { onMounted, ref } from 'vue'

const loading = ref(false)

const campaigns = ref<any[]>([])

async function loadAll() {
  loading.value = true
  try {
    const result = await siteApi.recruitment()
    campaigns.value = (result?.campaigns || []).filter((item: any) =>
      ['open', 'published'].includes(item.status),
    )
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

.refresh-button {
  min-height: 44px;
  padding-inline: 20px;
  border-color: color-mix(in srgb, var(--oa-text) 16%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 82%, transparent);
  backdrop-filter: blur(12px);
}

.recruitment-shell {
  display: grid;
  gap: clamp(32px, 5vw, 56px);
}

.recruitment-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 32px;
}

.campaign-count {
  display: grid;
  flex: 0 0 auto;
  justify-items: end;
  padding: 0 0 4px;
}

.campaign-count strong {
  color: var(--oa-text);
  font-size: clamp(36px, 5vw, 56px);
  font-weight: 600;
  line-height: 1;
}

.campaign-count span {
  margin-top: 8px;
  color: var(--oa-muted);
  font-size: 14px;
}

.campaign-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.campaign-card {
  display: flex;
  min-height: 390px;
  flex-direction: column;
  padding: clamp(24px, 3vw, 34px);
  background:
    radial-gradient(
      circle at 100% 0,
      color-mix(in srgb, var(--oa-text) 6%, transparent),
      transparent 38%
    ),
    var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 28px;
  box-shadow: 0 16px 48px color-mix(in srgb, var(--oa-text) 5%, transparent);
  transition:
    transform 220ms ease,
    border-color 220ms ease,
    box-shadow 220ms ease;
}

.campaign-card:hover {
  border-color: color-mix(in srgb, var(--oa-text) 24%, var(--oa-border));
  box-shadow: 0 22px 58px color-mix(in srgb, var(--oa-text) 9%, transparent);
  transform: translateY(-4px);
}

.campaign-card__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.campaign-index {
  color: var(--oa-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  letter-spacing: 0.12em;
}

.campaign-card__content {
  margin: clamp(32px, 5vw, 50px) 0 28px;
}

.campaign-card__content h3 {
  margin: 0;
  color: var(--oa-text);
  font-size: clamp(24px, 2.5vw, 32px);
  font-weight: 600;
  line-height: 1.25;
}

.campaign-card__content p {
  margin: 14px 0 0;
  color: var(--oa-muted);
  line-height: 1.75;
}

.campaign-meta {
  display: grid;
  gap: 14px;
  margin: auto 0 26px;
}

.campaign-meta > div {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--oa-border);
}

.campaign-meta dt {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--oa-muted);
  font-size: 13px;
}

.campaign-meta dd {
  margin: 0;
  color: var(--oa-text);
  font-size: 14px;
  line-height: 1.55;
  text-align: right;
}

.campaign-card__action {
  width: 100%;
  min-height: 48px;
  border-radius: 14px;
}

.campaign-card__action :deep(span) {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 720px) {
  .recruitment-heading {
    align-items: start;
    flex-direction: column;
  }

  .campaign-count {
    grid-template-columns: auto auto;
    align-items: baseline;
    justify-items: start;
    gap: 10px;
  }

  .campaign-count strong {
    font-size: 34px;
  }

  .campaign-count span {
    margin: 0;
  }

  .campaign-grid {
    grid-template-columns: 1fr;
  }

  .campaign-card {
    min-height: 360px;
  }

  .campaign-meta > div {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .campaign-meta dd {
    text-align: left;
  }
}

@media (prefers-reduced-motion: reduce) {
  .campaign-card {
    transition: none;
  }

  .campaign-card:hover {
    transform: none;
  }
}
</style>
