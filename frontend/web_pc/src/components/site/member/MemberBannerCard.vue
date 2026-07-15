<template>
  <router-link :to="`/members/${member.slug}`" class="member-card">
    <div
      :style="backgroundStyle"
      :class="{ 'member-card__visual--custom': member.cardBackgroundUrl }"
      class="member-card__visual"
    >
      <span class="member-card__eyebrow">MEMBER / {{ member.departmentName || 'OPENATOM' }}</span>
      <UserAvatar
        :name="member.displayName"
        :size="64"
        :src="member.avatarUrl || ''"
        class="member-card__avatar"
      />
    </div>
    <div class="member-card__body">
      <div class="member-card__title">
        <div>
          <h2>{{ member.displayName }}</h2>
          <p>{{ member.headline || '开放原子开源社团成员' }}</p>
        </div>
        <span aria-hidden="true">↗</span>
      </div>
      <div class="member-card__meta">
        <span v-if="member.positionName">{{ member.positionName }}</span>
        <span>{{ member.articleCount || 0 }} 篇文章</span>
        <span v-if="member.customized" class="member-card__custom">已定制</span>
      </div>
      <div v-if="member.skills?.length" class="member-card__skills">
        <span v-for="skill in member.skills.slice(0, 4)" :key="skill">{{ skill }}</span>
      </div>
    </div>
  </router-link>
</template>

<script lang="ts" setup>
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemberCard } from '@/types/member-profile'
import { computed } from 'vue'

const props = defineProps<{ member: MemberCard }>()

const backgroundStyle = computed(() =>
  props.member.cardBackgroundUrl
    ? {
        backgroundImage: `linear-gradient(100deg, rgba(7, 12, 22, .74), rgba(7, 12, 22, .18)), url("${props.member.cardBackgroundUrl}")`,
        backgroundPosition: `${props.member.cardFocusX ?? 50}% ${props.member.cardFocusY ?? 50}%`,
      }
    : undefined,
)
</script>

<style scoped>
.member-card {
  display: grid;
  grid-template-columns: minmax(190px, 38%) 1fr;
  min-height: 210px;
  overflow: hidden;
  color: var(--oa-text);
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  text-decoration: none;
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.member-card:hover {
  transform: translateY(-3px);
  border-color: var(--oa-border-strong);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.1);
}

.member-card:focus-visible {
  outline: 3px solid var(--oa-focus-ring);
  outline-offset: 3px;
}
.member-card__visual {
  position: relative;
  padding: 22px;
  background: linear-gradient(145deg, #111827, #334155);
  background-size: cover;
  color: #fff;
}
.member-card__visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 20%, rgba(96, 165, 250, 0.42), transparent 38%);
}
.member-card__eyebrow {
  position: relative;
  z-index: 1;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  opacity: 0.82;
}
.member-card__avatar {
  position: absolute;
  z-index: 1;
  left: 22px;
  bottom: 22px;
  border: 3px solid rgba(255, 255, 255, 0.86);
  border-radius: 50%;
}
.member-card__body {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 25px 28px 22px;
  min-width: 0;
}
.member-card__title {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}
.member-card__title h2 {
  margin: 0 0 7px;
  font-size: 22px;
}
.member-card__title p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.55;
}
.member-card__title > span {
  font-size: 22px;
}
.member-card__meta,
.member-card__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: var(--oa-muted);
  font-size: 12px;
}
.member-card__custom {
  color: #2563eb;
  font-weight: 700;
}
.member-card__skills span {
  padding: 5px 9px;
  background: var(--oa-page-soft-bg);
  border-radius: 999px;
}

@media (max-width: 680px) {
  .member-card {
    grid-template-columns: 1fr;
  }
  .member-card__visual {
    min-height: 150px;
  }
  .member-card__body {
    gap: 20px;
  }
}
</style>
