<template>
  <article :class="[`partner-club-card--${variant}`]" class="partner-club-card">
    <div class="partner-club-card__header">
      <div class="partner-club-card__logo" :aria-busy="!logoLoaded && !logoFailed">
        <span v-if="logoFailed" aria-hidden="true" class="partner-club-card__initial">
          {{ initial }}
        </span>
        <img
          v-else
          :alt="`${club.name} Logo`"
          :class="{ 'is-loaded': logoLoaded }"
          :src="club.logoUrl"
          height="72"
          loading="lazy"
          width="72"
          @error="logoFailed = true"
          @load="logoLoaded = true"
        />
      </div>

      <div class="partner-club-card__identity">
        <h2>{{ club.name }}</h2>
        <p v-if="club.organization || club.category">
          <span v-if="club.organization">{{ club.organization }}</span>
          <span v-if="club.organization && club.category" aria-hidden="true">/</span>
          <span v-if="club.category">{{ club.category }}</span>
        </p>
      </div>
    </div>

    <p class="partner-club-card__description">{{ club.description }}</p>

    <div v-if="club.presidentName" class="partner-club-card__president">
      <div class="partner-club-card__president-avatar">
        <span v-if="presidentAvatarFailed" aria-hidden="true">{{ presidentInitial }}</span>
        <img
          v-else-if="club.presidentAvatarUrl"
          :alt="`${club.name}社长${club.presidentName}的头像`"
          :src="club.presidentAvatarUrl"
          height="36"
          loading="lazy"
          width="36"
          @error="presidentAvatarFailed = true"
        />
        <span v-else aria-hidden="true">{{ presidentInitial }}</span>
      </div>
      <p><span>社长</span>{{ club.presidentName }}</p>
    </div>

    <ul v-if="variant === 'default' && club.tags?.length" class="partner-club-card__tags">
      <li v-for="tag in club.tags" :key="tag">{{ tag }}</li>
    </ul>

    <div class="partner-club-card__footer">
      <a
        v-if="safeWebsiteUrl"
        :aria-label="`访问${club.name}官网（在新标签页打开）`"
        class="partner-club-card__link"
        :href="safeWebsiteUrl"
        rel="noopener noreferrer"
        target="_blank"
      >
        <span>访问官网</span>
        <ExternalLink aria-hidden="true" :size="16" :stroke-width="1.8" />
      </a>
      <span v-else class="partner-club-card__unavailable">暂无官网</span>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ExternalLink } from 'lucide-vue-next'
import type { PartnerClub } from '@/types/partner-club'

const props = withDefaults(
  defineProps<{
    club: PartnerClub
    variant?: 'default' | 'compact'
  }>(),
  { variant: 'default' },
)

const logoLoaded = ref(false)
const logoFailed = ref(false)
const presidentAvatarFailed = ref(false)

const initial = computed(() => props.club.name.trim().slice(0, 1).toUpperCase() || '社')
const presidentInitial = computed(
  () => props.club.presidentName?.trim().slice(0, 1).toUpperCase() || '社',
)
const safeWebsiteUrl = computed(() => {
  if (!props.club.websiteUrl) return ''
  try {
    const url = new URL(props.club.websiteUrl)
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : ''
  } catch {
    return ''
  }
})

watch(
  () => props.club.logoUrl,
  () => {
    logoLoaded.value = false
    logoFailed.value = false
  },
)

watch(
  () => props.club.presidentAvatarUrl,
  () => {
    presidentAvatarFailed.value = false
  },
)
</script>

<style scoped>
.partner-club-card {
  display: flex;
  min-width: 0;
  min-height: 100%;
  flex-direction: column;
  padding: 24px;
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  background: var(--oa-elevated-bg);
  color: var(--oa-text);
  transition:
    border-color var(--oa-duration-base) ease,
    background-color var(--oa-duration-base) ease;
}

.partner-club-card:hover,
.partner-club-card:focus-within {
  border-color: var(--oa-border-strong);
  background: color-mix(in srgb, var(--oa-elevated-bg) 94%, var(--oa-text));
}

.partner-club-card__header {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 16px;
}

.partner-club-card__logo {
  position: relative;
  display: grid;
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  place-items: center;
  overflow: hidden;
  padding: 8px;
  border: 1px solid #dedee2;
  border-radius: 14px;
  background: #fafafa;
}

.partner-club-card__logo::before {
  position: absolute;
  inset: 8px;
  border-radius: 8px;
  background: #eeeeef;
  content: '';
  animation: partner-logo-pulse 1.4s ease-in-out infinite;
}

.partner-club-card__logo img {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 0;
  transition: opacity var(--oa-duration-base) ease;
}

.partner-club-card__logo img.is-loaded {
  opacity: 1;
}

.partner-club-card__logo:has(img.is-loaded)::before,
.partner-club-card__logo:has(.partner-club-card__initial)::before {
  display: none;
}

.partner-club-card__initial {
  position: relative;
  z-index: 1;
  color: #1d1d1f;
  font-size: 28px;
  font-weight: 650;
}

.partner-club-card__identity {
  min-width: 0;
}

.partner-club-card__identity h2 {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: var(--oa-text);
  font-size: 20px;
  font-weight: 620;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.partner-club-card__identity p {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 7px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.45;
}

.partner-club-card__description {
  display: -webkit-box;
  overflow: hidden;
  margin: 22px 0 0;
  color: var(--oa-text-soft);
  font-size: 15px;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.partner-club-card__president {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.partner-club-card__president-avatar {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 50%;
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted-strong);
  font-size: 14px;
  font-weight: 650;
}

.partner-club-card__president-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.partner-club-card__president p {
  display: flex;
  min-width: 0;
  flex-direction: column;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 580;
  line-height: 1.35;
}

.partner-club-card__president p span {
  color: var(--oa-muted);
  font-size: 12px;
  font-weight: 500;
}

.partner-club-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.partner-club-card__tags li {
  padding: 5px 10px;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted-strong);
  font-size: 12px;
  line-height: 1.2;
}

.partner-club-card__footer {
  display: flex;
  align-items: flex-end;
  flex: 1;
  padding-top: 22px;
}

.partner-club-card__link {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 17px;
  border: 1px solid var(--oa-button-border);
  border-radius: 999px;
  background: var(--oa-button-bg);
  color: var(--oa-button-text);
  font-size: 14px;
  font-weight: 550;
  transition:
    border-color var(--oa-duration-fast) ease,
    background-color var(--oa-duration-fast) ease,
    color var(--oa-duration-fast) ease;
}

.partner-club-card__link:hover {
  border-color: var(--oa-button-hover-border);
  background: var(--oa-button-hover-bg);
  color: var(--oa-button-hover-text);
}

.partner-club-card__link:focus-visible {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: 3px;
}

.partner-club-card__unavailable {
  display: inline-flex;
  align-items: center;
  padding: 7px 2px;
  color: var(--oa-muted);
  font-size: 14px;
  font-weight: 500;
}

.partner-club-card--compact {
  padding: 20px;
  border-radius: 16px;
}

.partner-club-card--compact .partner-club-card__logo {
  width: 56px;
  height: 56px;
  flex-basis: 56px;
  border-radius: 12px;
}

.partner-club-card--compact .partner-club-card__identity h2 {
  font-size: 17px;
}

.partner-club-card--compact .partner-club-card__identity p {
  display: none;
}

.partner-club-card--compact .partner-club-card__description {
  margin-top: 18px;
  -webkit-line-clamp: 2;
}

.partner-club-card--compact .partner-club-card__footer {
  padding-top: 16px;
}

.partner-club-card--compact .partner-club-card__president {
  margin-top: 14px;
}

.partner-club-card--compact .partner-club-card__president-avatar {
  width: 32px;
  height: 32px;
  flex-basis: 32px;
}

@keyframes partner-logo-pulse {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 0.9;
  }
}

@media (max-width: 767px) {
  .partner-club-card {
    padding: 20px;
  }

  .partner-club-card__logo {
    width: 56px;
    height: 56px;
    flex-basis: 56px;
  }

  .partner-club-card__description {
    margin-top: 18px;
  }

  .partner-club-card--compact .partner-club-card__logo {
    width: 48px;
    height: 48px;
    flex-basis: 48px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .partner-club-card,
  .partner-club-card__link,
  .partner-club-card__logo img {
    transition: none;
  }

  .partner-club-card__logo::before {
    animation: none;
  }
}
</style>
