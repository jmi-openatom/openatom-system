<template>
  <ViewPage class="partners-page">
    <section class="partners-hero" aria-labelledby="partners-title">
      <div class="container partners-hero__inner">
        <div>
          <span class="partners-eyebrow">开放协作</span>
          <h1 id="partners-title">开源伙伴</h1>
          <p>开源因连接而生长。这里汇聚与我们并肩交流、共同实践的校园社团与开源组织。</p>
        </div>
        <p v-if="!loading && !loadFailed" class="partners-count" aria-live="polite">
          <strong>{{ clubs.length }}</strong>
          <span>个合作组织</span>
        </p>
      </div>
    </section>

    <section class="partners-directory" aria-label="开源伙伴列表">
      <div class="container">
        <div v-if="loading" class="partners-grid" aria-label="正在加载开源伙伴" role="status">
          <article v-for="index in 6" :key="index" class="partner-skeleton">
            <div class="partner-skeleton__header">
              <span class="partner-skeleton__logo" />
              <span class="partner-skeleton__title" />
            </div>
            <span class="partner-skeleton__line" />
            <span class="partner-skeleton__line partner-skeleton__line--short" />
            <span class="partner-skeleton__button" />
          </article>
        </div>

        <div v-else-if="loadFailed" class="partners-state" role="alert">
          <CircleAlert aria-hidden="true" :size="34" :stroke-width="1.6" />
          <h2>开源伙伴加载失败</h2>
          <p>网络或服务暂时不可用，请稍后重新加载。</p>
          <button type="button" @click="fetchClubs">重新加载</button>
        </div>

        <div v-else-if="clubs.length" class="partners-grid">
          <PartnerClubCard v-for="club in clubs" :key="club.id" :club="club" />
        </div>

        <div v-else class="partners-state">
          <UsersRound aria-hidden="true" :size="34" :stroke-width="1.6" />
          <h2>暂未添加开源伙伴</h2>
          <p>新的伙伴信息将在确认后展示在这里。</p>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { CircleAlert, UsersRound } from 'lucide-vue-next'
import ViewPage from '@/components/common/ViewPage.vue'
import PartnerClubCard from '@/components/site/PartnerClubCard.vue'
import { siteApi } from '@/api'
import type { PartnerClub } from '@/types/partner-club'

const clubs = ref<PartnerClub[]>([])
const loading = ref(true)
const loadFailed = ref(false)

async function fetchClubs() {
  loading.value = true
  loadFailed.value = false
  try {
    const data = await siteApi.partnerClubs()
    clubs.value = Array.isArray(data) ? data : []
  } catch {
    clubs.value = []
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(fetchClubs)
</script>

<style scoped>
.partners-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.partners-hero {
  position: relative;
  min-height: clamp(440px, 38vw, 560px);
  overflow: hidden;
  background: var(--oa-page-soft-bg);
  border-bottom: 1px solid var(--oa-border);
}

.partners-hero::before {
  position: absolute;
  inset: 0;
  background: #faf9f7 url('/partners-hero-wide-light.png?v=3') right center / auto 100% no-repeat;
  content: '';
  pointer-events: none;
}

.partners-hero__inner {
  position: relative;
  z-index: 1;
  display: grid;
  min-height: clamp(440px, 38vw, 560px);
  align-content: center;
  gap: 24px;
  padding-top: clamp(48px, 7vw, 84px);
  padding-bottom: clamp(48px, 7vw, 84px);
}

.partners-hero__inner > div {
  position: relative;
  z-index: 1;
  width: min(47%, 520px);
}

.partners-eyebrow {
  display: block;
  margin-bottom: 16px;
  color: var(--oa-muted-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.partners-hero h1 {
  margin: 0;
  color: var(--oa-text);
  font-size: clamp(40px, 4.6vw, 58px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.025em;
}

.partners-hero__inner > div > p {
  max-width: 500px;
  margin: 16px 0 0;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.72;
}

.partners-count {
  position: relative;
  z-index: 1;
  display: flex;
  min-width: 120px;
  flex-direction: column;
  align-items: flex-start;
  margin: 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.partners-count strong {
  color: var(--oa-text);
  font-size: 30px;
  font-weight: 650;
  line-height: 1.1;
}

.partners-directory {
  padding: clamp(36px, 5vw, 64px) 0 clamp(72px, 9vw, 112px);
}

.partners-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.partners-state {
  display: flex;
  min-height: 360px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 48px 24px;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-elevated-bg);
  color: var(--oa-muted);
  text-align: center;
}

.partners-state h2 {
  margin: 18px 0 8px;
  color: var(--oa-text);
  font-size: 21px;
}

.partners-state p {
  margin: 0;
  font-size: 15px;
}

.partners-state button {
  min-height: 44px;
  margin-top: 24px;
  padding: 0 20px;
  border: 1px solid var(--oa-active-bg);
  border-radius: var(--radius-md);
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  cursor: pointer;
  font: inherit;
  font-weight: 550;
}

.partners-state button:hover {
  background: var(--oa-active-hover-bg);
}

.partners-state button:focus-visible {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: 3px;
}

.partner-skeleton {
  min-height: 300px;
  padding: 24px;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-elevated-bg);
}

.partner-skeleton__header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.partner-skeleton__logo,
.partner-skeleton__title,
.partner-skeleton__line,
.partner-skeleton__button {
  display: block;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--oa-border) 64%, var(--oa-page-soft-bg));
  animation: partner-skeleton-pulse 1.4s ease-in-out infinite;
}

.partner-skeleton__logo {
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  border-radius: var(--radius-md);
}

.partner-skeleton__title {
  width: 54%;
  height: 22px;
}

.partner-skeleton__line {
  width: 100%;
  height: 14px;
  margin-top: 28px;
}

.partner-skeleton__line--short {
  width: 72%;
  margin-top: 12px;
}

.partner-skeleton__button {
  width: 112px;
  height: 44px;
  margin-top: 54px;
  border-radius: var(--radius-md);
}

@keyframes partner-skeleton-pulse {
  0%,
  100% {
    opacity: 0.48;
  }
  50% {
    opacity: 0.9;
  }
}

@media (max-width: 1199px) {
  .partners-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }
}

@media (max-width: 767px) {
  .partners-hero {
    min-height: 650px;
  }

  .partners-hero__inner {
    min-height: 650px;
    align-content: start;
    padding-top: 36px;
    padding-bottom: 330px;
  }

  .partners-hero::before {
    background-position: 68% bottom;
    background-size: auto 340px;
  }

  .partners-hero__inner > div {
    width: 100%;
  }

  .partners-hero h1 {
    font-size: clamp(44px, 15vw, 64px);
  }

  .partners-count {
    align-items: flex-start;
  }

  .partners-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }

  .partner-skeleton {
    min-height: 270px;
    padding: 20px;
  }
}

:global(html.dark .partners-hero::before) {
  background-color: #0d0d0f;
  background-image: url('/partners-hero-wide-dark.png?v=3');
}

@media (prefers-reduced-motion: reduce) {
  .partner-skeleton__logo,
  .partner-skeleton__title,
  .partner-skeleton__line,
  .partner-skeleton__button {
    animation: none;
  }
}
</style>
