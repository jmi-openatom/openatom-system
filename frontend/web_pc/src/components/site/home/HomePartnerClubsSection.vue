<template>
  <section
    v-if="clubs.length"
    class="home-partners home-interactive-section"
    aria-labelledby="home-partners-title"
  >
    <HomeInteractiveBackdrop :radius="220" :spacing="64" :strength="18" />

    <div class="container">
      <div class="home-partners__heading">
        <div>
          <span>开放协作</span>
          <h2 id="home-partners-title">开源伙伴</h2>
          <p>与志同道合的校园社团和开源组织并肩交流，共同建设开放、共享的技术社区。</p>
        </div>
        <RouterLink class="home-partners__all-link" to="/partners">
          <span>查看全部开源伙伴</span>
          <ArrowRight aria-hidden="true" :size="18" :stroke-width="1.8" />
        </RouterLink>
      </div>

      <div class="home-partners__grid">
        <PartnerClubCard v-for="club in clubs" :key="club.id" :club="club" variant="compact" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowRight } from 'lucide-vue-next'
import { siteApi } from '@/api'
import PartnerClubCard from '@/components/site/PartnerClubCard.vue'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'
import type { PartnerClub } from '@/types/partner-club'

const clubs = ref<PartnerClub[]>([])

onMounted(async () => {
  try {
    const data = await siteApi.partnerClubs({ featured: true, limit: 4 })
    clubs.value = Array.isArray(data) ? data.slice(0, 4) : []
  } catch {
    clubs.value = []
  }
})
</script>

<style scoped>
.home-partners {
  min-height: 100vh;
  min-height: 100svh;
  display: flex;
  align-items: center;
  padding: clamp(72px, 8vw, 112px) 0;
  border-top: 1px solid var(--oa-border);
  background: var(--oa-page-soft-bg);
}

.home-partners__heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 40px;
  margin-bottom: 40px;
}

.home-partners__heading > div {
  max-width: 760px;
}

.home-partners__heading span:first-child {
  color: var(--oa-muted-strong);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.home-partners__heading h2 {
  margin: 12px 0 14px;
  color: var(--oa-text);
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 630;
  line-height: 1.1;
}

.home-partners__heading p {
  margin: 0;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.75;
}

.home-partners__all-link {
  display: inline-flex;
  min-height: 44px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 0 18px;
  border: 1px solid var(--oa-button-border);
  border-radius: 999px;
  background: var(--oa-button-bg);
  color: var(--oa-button-text);
  font-weight: 550;
  transition:
    border-color var(--oa-duration-fast) ease,
    background-color var(--oa-duration-fast) ease;
}

.home-partners__all-link span:first-child {
  color: inherit;
  font-size: inherit;
  font-weight: inherit;
  letter-spacing: 0;
}

.home-partners__all-link:hover {
  border-color: var(--oa-button-hover-border);
  background: var(--oa-button-hover-bg);
}

.home-partners__all-link:focus-visible {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: 3px;
}

.home-partners__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1199px) {
  .home-partners__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .home-partners__heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 24px;
    margin-bottom: 28px;
  }

  .home-partners__all-link {
    width: 100%;
  }

  .home-partners__grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-partners__all-link {
    transition: none;
  }
}
</style>
