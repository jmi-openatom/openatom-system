<template>
  <ViewPage :loading="loading" class="partner-detail">
    <section class="partner-detail-hero" aria-labelledby="partner-detail-title">
      <div class="container partner-detail-hero__inner">
        <RouterLink class="partner-detail-back" to="/partners">
          <ArrowLeft aria-hidden="true" :size="18" :stroke-width="1.8" />
          <span>返回开源伙伴</span>
        </RouterLink>

        <div class="partner-detail-hero__card">
          <div class="partner-detail-hero__logo" :aria-busy="!logoLoaded && !logoFailed">
            <span v-if="logoFailed" aria-hidden="true" class="partner-detail-hero__initial">
              {{ initial }}
            </span>
            <img
              v-else
              :alt="`${club.name} Logo`"
              :class="{ 'is-loaded': logoLoaded }"
              :src="club.logoUrl"
              height="96"
              width="96"
              @error="logoFailed = true"
              @load="logoLoaded = true"
            />
          </div>

          <div class="partner-detail-hero__identity">
            <div class="partner-detail-hero__eyebrow">
              <span v-if="club.category">{{ club.category }}</span>
              <span v-if="club.category && club.organization" aria-hidden="true">/</span>
              <span v-if="club.organization">{{ club.organization }}</span>
            </div>
            <h1 id="partner-detail-title">{{ club.name || '开源伙伴' }}</h1>
          </div>
        </div>
      </div>
    </section>

    <section class="partner-detail-body" aria-label="开源伙伴详情">
      <div class="container partner-detail-body__grid">
        <article class="partner-detail-story">
          <p v-if="club.description" class="partner-detail-story__text">{{ club.description }}</p>
          <div v-else class="partner-detail-story__empty">该伙伴暂未填写详细介绍。</div>

          <ul v-if="club.tags?.length" class="partner-detail-tags">
            <li v-for="tag in club.tags" :key="tag">{{ tag }}</li>
          </ul>
        </article>

        <aside class="partner-detail-panel">
          <div v-if="club.presidentName" class="partner-detail-president">
            <span class="partner-detail-panel__label">社长</span>
            <UserAvatar :name="club.presidentName" :size="44" :src="club.presidentAvatarUrl" />
            <p>{{ club.presidentName }}</p>
          </div>

          <div v-if="club.organization || club.category" class="partner-detail-facts">
            <div v-if="club.organization" class="partner-detail-fact">
              <span>所属组织</span>
              <strong>{{ club.organization }}</strong>
            </div>
            <div v-if="club.category" class="partner-detail-fact">
              <span>伙伴类型</span>
              <strong>{{ club.category }}</strong>
            </div>
          </div>

          <LinkPreview
            v-if="safeWebsiteUrl"
            :aria-label="`访问${club.name}官网（在新标签页打开）`"
            class="partner-detail-website"
            link-class="partner-detail-website__trigger"
            rel="noopener noreferrer"
            target="_blank"
            :url="safeWebsiteUrl"
            :width="320"
            :height="200"
          >
            <span>访问官网</span>
            <ExternalLink aria-hidden="true" :size="16" :stroke-width="1.8" />
          </LinkPreview>
          <span v-else class="partner-detail-website partner-detail-website--unavailable">
            暂无官网
          </span>
        </aside>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ExternalLink } from 'lucide-vue-next'
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { LinkPreview } from '@/components/ui/link-preview'
import { siteApi } from '@/api'
import { SITE_NAME, updateSeo } from '@/utils/seo.ts'
import type { PartnerClub } from '@/types/partner-club'

const route = useRoute()
const loading = ref(true)
const club = ref<PartnerClub>({}) as any
const logoLoaded = ref(false)
const logoFailed = ref(false)

const initial = computed(
  () =>
    String(club.value.name || '')
      .trim()
      .slice(0, 1)
      .toUpperCase() || '社',
)
const safeWebsiteUrl = computed(() => {
  if (!club.value.websiteUrl) return ''
  try {
    const url = new URL(club.value.websiteUrl)
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : ''
  } catch {
    return ''
  }
})

async function fetchDetail() {
  loading.value = true
  try {
    club.value = await siteApi.partnerClubDetail(route.params.id)
    updateSeo(
      {
        title: `${club.value.name || '开源伙伴'}｜${SITE_NAME}`,
        description:
          club.value.description || '认识与 JMI-OPENATOM 共同建设校园开源生态的伙伴社团。',
        image: club.value.logoUrl,
      },
      route.path,
    )
  } catch {
    club.value = {}
  } finally {
    loading.value = false
  }
}

watch(
  () => club.value.logoUrl,
  () => {
    logoLoaded.value = false
    logoFailed.value = false
  },
)

onMounted(fetchDetail)
</script>

<style scoped>
.partner-detail {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.partner-detail-hero {
  position: relative;
  padding: clamp(36px, 5vw, 64px) 0 clamp(56px, 7vw, 88px);
  border-bottom: 1px solid var(--oa-border);
  background: var(--oa-page-soft-bg);
}

.partner-detail-hero__inner {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.partner-detail-back {
  display: inline-flex;
  align-self: flex-start;
  min-height: 40px;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid var(--oa-button-border);
  border-radius: 999px;
  background: var(--oa-button-bg);
  color: var(--oa-button-text);
  font-size: 14px;
  font-weight: 550;
  transition:
    border-color var(--oa-duration-fast) ease,
    background-color var(--oa-duration-fast) ease;
}

.partner-detail-back:hover {
  border-color: var(--oa-button-hover-border);
  background: var(--oa-button-hover-bg);
}

.partner-detail-back:focus-visible {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: 3px;
}

.partner-detail-hero__card {
  display: flex;
  align-items: center;
  gap: 28px;
}

.partner-detail-hero__logo {
  position: relative;
  display: grid;
  width: 104px;
  height: 104px;
  flex: 0 0 104px;
  place-items: center;
  overflow: hidden;
  padding: 12px;
  border: 1px solid var(--oa-border);
  border-radius: 20px;
  background: var(--oa-elevated-bg);
}

.partner-detail-hero__logo::before {
  position: absolute;
  inset: 12px;
  border-radius: 10px;
  background: var(--oa-page-soft-bg);
  content: '';
  animation: partner-detail-logo-pulse 1.4s ease-in-out infinite;
}

.partner-detail-hero__logo img {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 0;
  transition: opacity var(--oa-duration-base) ease;
}

.partner-detail-hero__logo img.is-loaded {
  opacity: 1;
}

.partner-detail-hero__logo:has(img.is-loaded)::before,
.partner-detail-hero__logo:has(.partner-detail-hero__initial)::before {
  display: none;
}

.partner-detail-hero__initial {
  position: relative;
  z-index: 1;
  color: var(--oa-text);
  font-size: 40px;
  font-weight: 650;
}

.partner-detail-hero__identity {
  min-width: 0;
}

.partner-detail-hero__eyebrow {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
  color: var(--oa-muted-strong);
  font-size: 13px;
  font-weight: 600;
}

.partner-detail-hero__identity h1 {
  margin: 0;
  color: var(--oa-text);
  font-size: clamp(32px, 4vw, 44px);
  font-weight: 680;
  line-height: 1.15;
  letter-spacing: -0.02em;
  overflow-wrap: anywhere;
}

.partner-detail-body {
  padding: clamp(36px, 5vw, 64px) 0 clamp(72px, 9vw, 112px);
}

.partner-detail-body__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 32px;
  align-items: start;
}

.partner-detail-story {
  min-width: 0;
  padding: clamp(24px, 3vw, 36px);
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-elevated-bg);
}

.partner-detail-story__text {
  margin: 0;
  color: var(--oa-text-soft);
  font-size: 17px;
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.partner-detail-story__empty {
  color: var(--oa-muted);
  font-size: 16px;
}

.partner-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
}

.partner-detail-tags li {
  padding: 6px 12px;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted-strong);
  font-size: 13px;
  line-height: 1.2;
}

.partner-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.partner-detail-president,
.partner-detail-facts {
  padding: 20px;
  border: 1px solid var(--oa-border);
  border-radius: var(--radius-lg);
  background: var(--oa-elevated-bg);
}

.partner-detail-president {
  display: flex;
  align-items: center;
  gap: 12px;
}

.partner-detail-president p {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--oa-text);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
}

.partner-detail-panel__label {
  margin-right: auto;
  color: var(--oa-muted);
  font-size: 13px;
  font-weight: 500;
}

.partner-detail-facts {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.partner-detail-fact {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.partner-detail-fact span {
  color: var(--oa-muted);
  font-size: 12px;
  font-weight: 500;
}

.partner-detail-fact strong {
  color: var(--oa-text);
  font-size: 15px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.partner-detail-website {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 20px;
  border: 1px solid var(--oa-button-border);
  border-radius: 999px;
  background: var(--oa-button-bg);
  color: var(--oa-button-text);
  font-size: 15px;
  font-weight: 550;
  transition:
    border-color var(--oa-duration-fast) ease,
    background-color var(--oa-duration-fast) ease,
    color var(--oa-duration-fast) ease;
}

.partner-detail-website :deep(.partner-detail-website__trigger) {
  display: inline-flex;
  width: 100%;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: inherit;
}

.partner-detail-website:hover {
  border-color: var(--oa-button-hover-border);
  background: var(--oa-button-hover-bg);
  color: var(--oa-button-hover-text);
}

.partner-detail-website:has(:focus-visible) {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: 3px;
}

.partner-detail-website--unavailable {
  color: var(--oa-muted);
  font-weight: 500;
}

@keyframes partner-detail-logo-pulse {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 0.9;
  }
}

@media (max-width: 991px) {
  .partner-detail-body__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .partner-detail-panel {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .partner-detail-president,
  .partner-detail-facts {
    flex: 1;
  }
}

@media (max-width: 767px) {
  .partner-detail-hero__card {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .partner-detail-hero__logo {
    width: 88px;
    height: 88px;
    flex-basis: 88px;
    border-radius: 18px;
  }

  .partner-detail-story {
    padding: 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .partner-detail-back,
  .partner-detail-hero__logo img,
  .partner-detail-website {
    transition: none;
  }

  .partner-detail-hero__logo::before {
    animation: none;
  }
}
</style>
