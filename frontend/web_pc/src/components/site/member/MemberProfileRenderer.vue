<template>
  <div
    :class="[`profile-renderer--${profile.themeKey}`, `profile-renderer--${profile.colorMode}`]"
    class="profile-renderer"
  >
    <header class="profile-hero">
      <div class="profile-hero__grid" aria-hidden="true"></div>
      <div class="profile-hero__ghost" aria-hidden="true">{{ profile.displayName }}</div>
      <div :style="bannerStyle" class="profile-hero__banner"></div>
      <div class="profile-hero__content">
        <UserAvatar
          :name="profile.displayName"
          :size="92"
          :src="profile.avatarUrl || ''"
          class="profile-hero__avatar"
        />
        <div class="profile-hero__identity">
          <p class="profile-hero__eyebrow">MEMBER PROFILE</p>
          <h1>{{ profile.displayName }}</h1>
          <p>{{ profile.headline || '开放原子开源社团成员' }}</p>
          <div class="profile-hero__meta">
            <span v-if="profile.departmentName">{{ profile.departmentName }}</span>
            <span v-if="profile.positionName">{{ profile.positionName }}</span>
            <span v-if="profile.status === 'draft'">草稿预览</span>
            <span v-if="profile.updatedAt">更新于 {{ formatUpdatedAt(profile.updatedAt) }}</span>
          </div>
        </div>
        <div v-if="profile.socialLinks?.length" class="profile-hero__links">
          <a
            v-for="link in activeLinks"
            :key="`${link.platform}-${link.url}`"
            :href="link.url"
            rel="noopener noreferrer"
            target="_blank"
          >
            {{ link.label || platformLabel(link.platform) }} ↗
          </a>
        </div>
      </div>
    </header>

    <main class="profile-modules">
      <section
        v-for="module in activeModules"
        :key="module.id || module.key"
        :class="`profile-module--span-${module.columnSpan}`"
        class="profile-module"
      >
        <div class="profile-module__heading">
          <span>{{ moduleEyebrow(module.key) }}</span>
          <h2>{{ module.title || moduleTitle(module.key) }}</h2>
        </div>

        <div
          v-if="module.key === 'about'"
          class="profile-copy"
          v-html="
            renderMarkdown(
              profile.bio || String(module.data?.content || '这个成员还没有填写介绍。'),
            )
          "
        ></div>
        <div
          v-else-if="module.key === 'markdown'"
          class="profile-copy"
          v-html="renderMarkdown(String(module.data?.content || ''))"
        ></div>
        <div v-else-if="module.key === 'hero_statement'" class="profile-statement">
          <p>{{ module.data?.text || '保持好奇，持续创造。' }}</p>
          <span v-if="module.data?.caption">{{ module.data.caption }}</span>
        </div>
        <div v-else-if="module.key === 'now'" class="profile-now">
          <article v-for="(item, index) in arrayData(module, 'items')" :key="index">
            <span>{{ item.label || '正在进行' }}</span>
            <strong>{{ item.value || item.title }}</strong>
            <p>{{ item.description }}</p>
          </article>
          <p v-if="!arrayData(module, 'items').length" class="profile-empty">
            写下你最近在学习、开发或关注的事情
          </p>
        </div>
        <div v-else-if="module.key === 'skills'" class="profile-skills">
          <span v-for="skill in profile.skills" :key="skill">{{ skill }}</span>
          <p v-if="!profile.skills.length">暂未添加技能标签</p>
        </div>
        <div v-else-if="module.key === 'tech_stack'" class="profile-stack-grid">
          <article v-for="(group, index) in arrayData(module, 'groups')" :key="index">
            <span>{{ group.title || group.name }}</span>
            <div>
              <b v-for="item in group.items || []" :key="item">{{ item }}</b>
            </div>
          </article>
          <p v-if="!arrayData(module, 'groups').length" class="profile-empty">暂未添加技术栈</p>
        </div>
        <div v-else-if="module.key === 'blog_latest'" class="profile-articles">
          <router-link
            v-for="article in moduleArticles(module)"
            :key="article.id"
            :to="`/blog/${article.id}`"
          >
            <div>
              <span>{{ formatShortDate(article.publishedAt) }}</span>
              <h3>{{ article.title }}</h3>
              <p>{{ article.summary || '阅读这篇成员文章' }}</p>
            </div>
            <b aria-hidden="true">→</b>
          </router-link>
          <p v-if="!moduleArticles(module).length" class="profile-empty">还没有已发布文章</p>
        </div>
        <div
          v-else-if="module.key === 'projects' || module.key === 'featured_work'"
          :class="{ 'profile-list--featured': module.key === 'featured_work' }"
          class="profile-list"
        >
          <article v-for="(item, index) in arrayData(module, 'items')" :key="index">
            <h3>{{ item.name || item.title || '项目' }}</h3>
            <p>{{ item.description }}</p>
            <a
              v-if="safeHref(item.url)"
              :href="safeHref(item.url)"
              rel="noopener noreferrer"
              target="_blank"
              >访问项目 ↗</a
            >
          </article>
          <p v-if="!arrayData(module, 'items').length" class="profile-empty">暂未添加项目</p>
        </div>
        <div
          v-else-if="module.key === 'timeline' || module.key === 'club_experience'"
          class="profile-timeline"
        >
          <article v-for="(item, index) in arrayData(module, 'items')" :key="index">
            <time>{{ item.date || item.year }}</time>
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </div>
          </article>
          <p v-if="!arrayData(module, 'items').length" class="profile-empty">暂未添加经历</p>
        </div>
        <div v-else-if="module.key === 'awards'" class="profile-awards">
          <article v-for="(item, index) in arrayData(module, 'items')" :key="index">
            <span>{{ item.year || item.date || 'HONOR' }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.issuer || item.description }}</p>
          </article>
          <p v-if="!arrayData(module, 'items').length" class="profile-empty">暂未添加荣誉</p>
        </div>
        <div v-else-if="module.key === 'stats'" class="profile-stats">
          <article v-for="(item, index) in arrayData(module, 'items')" :key="index">
            <strong>{{ item.value }}</strong
            ><span>{{ item.label }}</span
            ><small>{{ item.description }}</small>
          </article>
          <p v-if="!arrayData(module, 'items').length" class="profile-empty">暂未添加数据指标</p>
        </div>
        <div v-else-if="module.key === 'gallery'" class="profile-gallery">
          <figure v-for="(item, index) in arrayData(module, 'items')" :key="index">
            <img
              v-if="safeHref(item.url)"
              :alt="item.caption || `图片 ${index + 1}`"
              :src="safeHref(item.url)"
              loading="lazy"
            />
            <figcaption v-if="item.caption">{{ item.caption }}</figcaption>
          </figure>
          <p v-if="!arrayData(module, 'items').length" class="profile-empty">暂未添加图片</p>
        </div>
        <blockquote v-else-if="module.key === 'quote'" class="profile-quote">
          <p>“{{ module.data?.text || '写下一句代表你的话。' }}”</p>
          <cite v-if="module.data?.source">— {{ module.data.source }}</cite>
        </blockquote>
        <div v-else-if="module.key === 'links'" class="profile-links-list">
          <a
            v-for="link in activeLinks"
            :key="link.url"
            :href="link.url"
            rel="noopener noreferrer"
            target="_blank"
            ><span>{{ link.label || platformLabel(link.platform) }}</span
            ><b>↗</b></a
          >
        </div>
        <div v-else-if="module.key === 'contact_cta'" class="profile-contact">
          <div>
            <h3>{{ module.data?.heading || '一起做点有意思的事' }}</h3>
            <p>{{ module.data?.description || '欢迎通过公开链接找到我。' }}</p>
          </div>
          <a
            v-if="safeHref(module.data?.url)"
            :href="safeHref(module.data.url)"
            rel="noopener noreferrer"
            target="_blank"
            >{{ module.data?.buttonLabel || '联系我' }} ↗</a
          >
        </div>
      </section>
    </main>
  </div>
</template>

<script lang="ts" setup>
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { MemberProfile, ProfileArticle, ProfileModule } from '@/types/member-profile'
import { renderMarkdown } from '@/utils/markdown'
import { computed } from 'vue'

const props = defineProps<{ profile: MemberProfile }>()
const activeModules = computed(() =>
  (props.profile.modules || [])
    .filter((item) => item.enabled)
    .sort((a, b) => a.sortOrder - b.sortOrder),
)
const activeLinks = computed(() =>
  (props.profile.socialLinks || [])
    .filter((item) => item.enabled)
    .sort((a, b) => a.sortOrder - b.sortOrder),
)
const bannerStyle = computed(() =>
  props.profile.bannerUrl
    ? {
        backgroundImage: `linear-gradient(100deg, rgba(8,15,30,.76), rgba(8,15,30,.12)), url("${props.profile.bannerUrl}")`,
      }
    : undefined,
)

function moduleArticles(module: ProfileModule): ProfileArticle[] {
  return Array.isArray(module.data?.articles) ? module.data.articles : []
}
function arrayData(module: ProfileModule, key: string): any[] {
  return Array.isArray(module.data?.[key]) ? module.data[key] : []
}
function moduleEyebrow(key: string) {
  return (
    (
      {
        about: 'INTRODUCTION',
        hero_statement: 'MANIFESTO',
        now: 'RIGHT NOW',
        skills: 'TOOLKIT',
        tech_stack: 'STACK',
        blog_latest: 'WRITING',
        projects: 'SELECTED WORK',
        featured_work: 'FEATURED',
        timeline: 'JOURNEY',
        club_experience: 'COMMUNITY',
        awards: 'RECOGNITION',
        stats: 'BY THE NUMBERS',
        gallery: 'MOMENTS',
        quote: 'WORDS',
        links: 'ELSEWHERE',
        contact_cta: 'LET’S CONNECT',
        markdown: 'NOTE',
      } as Record<string, string>
    )[key] || 'PROFILE'
  )
}
function moduleTitle(key: string) {
  return (
    (
      {
        about: '关于我',
        hero_statement: '我的宣言',
        now: '最近在做',
        skills: '技能',
        tech_stack: '技术栈',
        blog_latest: '最近文章',
        projects: '项目',
        featured_work: '精选作品',
        timeline: '经历',
        club_experience: '社团经历',
        awards: '荣誉与奖项',
        stats: '个人数据',
        gallery: '图片画廊',
        quote: '一句话',
        links: '找到我',
        contact_cta: '保持联系',
        markdown: '自定义内容',
      } as Record<string, string>
    )[key] || '内容'
  )
}
function platformLabel(platform: string) {
  return (
    (
      {
        website: '个人网站',
        github: 'GitHub',
        gitee: 'Gitee',
        bilibili: '哔哩哔哩',
        zhihu: '知乎',
        weibo: '微博',
        other: '链接',
      } as Record<string, string>
    )[platform] || platform
  )
}
function safeHref(value?: string) {
  try {
    const url = new URL(String(value || ''))
    return ['http:', 'https:'].includes(url.protocol) ? url.href : ''
  } catch {
    return ''
  }
}
function formatShortDate(value?: string) {
  if (!value) return 'NEW'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(
    new Date(value),
  )
}
function formatUpdatedAt(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}
</script>

<style scoped>
.profile-renderer {
  --profile-accent: #2563eb;
  color: var(--oa-text);
}
.profile-renderer--light {
  --oa-text: #1d1d1f;
  --oa-text-soft: #3f3f46;
  --oa-muted: #6e6e73;
  --oa-border: #d8d8dc;
  --oa-divider: #ececef;
  --oa-elevated-bg: #ffffff;
  --oa-page-soft-bg: #f5f5f7;
}
.profile-renderer--dark {
  --oa-text: #f7f7f8;
  --oa-text-soft: #e2e2e5;
  --oa-muted: #c2c2c7;
  --oa-border: #4b4b50;
  --oa-divider: #29292d;
  --oa-elevated-bg: #1c1c1f;
  --oa-page-soft-bg: #141416;
  padding: 18px;
  background: #0c0c0e;
  border-radius: 30px;
}
.profile-renderer--tech {
  --profile-accent: #7c3aed;
}
.profile-renderer--warm {
  --profile-accent: #c2410c;
}
.profile-renderer--warm .profile-hero__banner {
  background:
    radial-gradient(circle at 78% 20%, rgba(251, 191, 36, 0.5), transparent 30%),
    linear-gradient(135deg, #7c2d12, #c2410c);
}
.profile-renderer--editorial {
  --profile-accent: #c2410c;
  font-family: ui-serif, Georgia, 'Songti SC', serif;
}
.profile-hero {
  overflow: hidden;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 26px;
}
.profile-hero__banner {
  height: 250px;
  background:
    radial-gradient(circle at 72% 18%, rgba(96, 165, 250, 0.55), transparent 28%),
    linear-gradient(135deg, #0f172a, #334155);
  background-position: center;
  background-size: cover;
}
.profile-hero__content {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: end;
  gap: 24px;
  padding: 0 38px 34px;
}
.profile-hero__avatar {
  margin-top: -46px;
  border: 5px solid var(--oa-elevated-bg);
  border-radius: 50%;
}
.profile-hero__identity h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 64px);
  line-height: 1;
  letter-spacing: -0.045em;
}
.profile-hero__identity > p:not(.profile-hero__eyebrow) {
  margin: 12px 0;
  color: var(--oa-muted);
  font-size: 17px;
}
.profile-hero__eyebrow,
.profile-module__heading span {
  color: var(--profile-accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}
.profile-hero__meta,
.profile-hero__links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: var(--oa-muted);
  font-size: 13px;
}
.profile-hero__links {
  justify-content: flex-end;
  max-width: 260px;
}
.profile-hero__links a,
.profile-list a {
  color: var(--oa-text);
  font-weight: 700;
  text-decoration: none;
}
.profile-modules {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 18px;
  margin-top: 20px;
}
.profile-module {
  grid-column: span 12;
  min-width: 0;
  padding: 30px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 20px;
}
.profile-module--span-6 {
  grid-column: span 6;
}
.profile-module__heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 20px;
  margin-bottom: 22px;
}
.profile-module__heading h2 {
  margin: 0;
  font-size: 24px;
}
.profile-copy {
  color: var(--oa-text-soft);
  font-size: 16px;
  line-height: 1.8;
}
.profile-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.profile-skills span {
  padding: 9px 13px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-divider);
  border-radius: 999px;
}
.profile-articles a,
.profile-links-list a {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 0;
  color: inherit;
  border-top: 1px solid var(--oa-divider);
  text-decoration: none;
}
.profile-articles a:first-child,
.profile-links-list a:first-child {
  border-top: 0;
}
.profile-articles h3,
.profile-articles p {
  margin: 5px 0 0;
}
.profile-articles span,
.profile-empty {
  color: var(--oa-muted);
  font-size: 13px;
}
.profile-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.profile-list article {
  padding: 20px;
  background: var(--oa-page-soft-bg);
  border-radius: 14px;
}
.profile-list h3,
.profile-list p {
  margin: 0 0 9px;
}
.profile-timeline article {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 18px;
  padding: 16px 0;
  border-top: 1px solid var(--oa-divider);
}
.profile-timeline article:first-child {
  border-top: 0;
}
.profile-timeline h3,
.profile-timeline p {
  margin: 0 0 7px;
}
.profile-timeline time {
  color: var(--profile-accent);
  font-weight: 800;
}

/* 连续画布：个人主页以版式和留白分区，仅内容本身需要时才使用容器。 */
.profile-renderer {
  position: relative;
  overflow: hidden;
  background: var(--oa-elevated-bg);
  border-top: 1px solid var(--oa-border);
  border-bottom: 1px solid var(--oa-border);
}
.profile-renderer::before {
  position: absolute;
  top: 420px;
  right: -240px;
  width: 620px;
  height: 620px;
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--profile-accent) 14%, transparent),
    transparent 68%
  );
  content: '';
  pointer-events: none;
}
.profile-renderer--dark {
  padding: 0;
  border-radius: 0;
}
.profile-hero {
  position: relative;
  min-height: 540px;
  overflow: hidden;
  color: #fff;
  background: #08101f;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 0;
}
.profile-hero__banner {
  position: absolute;
  inset: 0;
  height: 100%;
  filter: saturate(1.08);
  transform: scale(1.015);
}
.profile-hero__banner::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(3, 7, 18, 0.05), rgba(3, 7, 18, 0.72));
  content: '';
}
.profile-hero__grid {
  position: absolute;
  z-index: 1;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 54px 54px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.62), transparent 86%);
  pointer-events: none;
}
.profile-hero__ghost {
  position: absolute;
  z-index: 1;
  top: 44px;
  right: -0.04em;
  max-width: 92%;
  overflow: hidden;
  color: transparent;
  font-size: clamp(86px, 15vw, 230px);
  font-weight: 900;
  line-height: 0.84;
  letter-spacing: -0.085em;
  opacity: 0.32;
  -webkit-text-stroke: 1px rgba(255, 255, 255, 0.42);
  white-space: nowrap;
  pointer-events: none;
}
.profile-hero__content {
  position: relative;
  z-index: 2;
  grid-template-columns: auto minmax(0, 1fr) minmax(180px, auto);
  align-items: end;
  min-height: 540px;
  padding: 72px clamp(28px, 6vw, 84px) 58px;
}
.profile-hero__avatar {
  margin: 0;
  border-color: rgba(255, 255, 255, 0.82);
  box-shadow: 0 20px 70px rgba(0, 0, 0, 0.3);
}
.profile-hero__identity h1 {
  font-size: clamp(50px, 7.5vw, 108px);
  letter-spacing: -0.065em;
}
.profile-hero__identity > p:not(.profile-hero__eyebrow),
.profile-hero__meta,
.profile-hero__links {
  color: rgba(255, 255, 255, 0.72);
}
.profile-hero__eyebrow {
  color: #93c5fd;
}
.profile-hero__links a {
  color: #fff;
}
.profile-modules {
  gap: 0;
  margin: 0;
  padding: 0 clamp(28px, 6vw, 84px);
}
.profile-module {
  position: relative;
  padding: clamp(56px, 8vw, 104px) clamp(4px, 2vw, 30px);
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--oa-divider);
  border-radius: 0;
}
.profile-module--span-6:nth-child(even) {
  border-left: 1px solid var(--oa-divider);
}
.profile-module__heading {
  align-items: flex-start;
  flex-direction: column;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 34px;
}
.profile-module__heading h2 {
  font-size: clamp(28px, 4vw, 52px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}
.profile-copy {
  max-width: 880px;
  font-size: clamp(17px, 2vw, 21px);
}
.profile-statement p,
.profile-quote p {
  max-width: 1000px;
  margin: 0;
  font-size: clamp(36px, 6vw, 86px);
  font-weight: 760;
  line-height: 1.08;
  letter-spacing: -0.055em;
}
.profile-statement p {
  background: linear-gradient(120deg, var(--oa-text), var(--profile-accent));
  background-clip: text;
  color: transparent;
}
.profile-statement span,
.profile-quote cite {
  display: block;
  margin-top: 22px;
  color: var(--oa-muted);
  font-size: 14px;
  font-style: normal;
}
.profile-now,
.profile-stats,
.profile-awards,
.profile-stack-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: clamp(18px, 4vw, 48px);
}
.profile-now article,
.profile-stats article,
.profile-awards article,
.profile-stack-grid article {
  padding-top: 18px;
  border-top: 2px solid var(--oa-text);
}
.profile-now span,
.profile-awards span,
.profile-stack-grid article > span {
  color: var(--profile-accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.profile-now strong {
  display: block;
  margin-top: 14px;
  font-size: 22px;
}
.profile-now p,
.profile-awards p {
  color: var(--oa-muted);
  line-height: 1.7;
}
.profile-stats strong {
  display: block;
  color: var(--profile-accent);
  font-size: clamp(44px, 7vw, 88px);
  line-height: 1;
  letter-spacing: -0.06em;
}
.profile-stats span,
.profile-stats small {
  display: block;
  margin-top: 10px;
}
.profile-stats small {
  color: var(--oa-muted);
}
.profile-stack-grid article > div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
.profile-stack-grid b {
  font-size: 17px;
}
.profile-stack-grid b:not(:last-child)::after {
  margin-left: 8px;
  color: var(--profile-accent);
  content: '/';
}
.profile-skills span {
  padding: 10px 0;
  background: transparent;
  border: 0;
  border-bottom: 2px solid var(--profile-accent);
  border-radius: 0;
  font-size: clamp(18px, 3vw, 30px);
  font-weight: 700;
}
.profile-skills {
  gap: 18px 30px;
}
.profile-articles a,
.profile-links-list a {
  padding: 26px 0;
  transition:
    padding 180ms ease,
    color 180ms ease;
}
.profile-articles a:hover,
.profile-links-list a:hover {
  padding-left: 12px;
  color: var(--profile-accent);
}
.profile-articles h3 {
  font-size: clamp(22px, 3vw, 34px);
}
.profile-list {
  gap: 0;
}
.profile-list article {
  min-height: 180px;
  padding: 26px 26px 26px 0;
  background: transparent;
  border-top: 1px solid var(--oa-divider);
  border-radius: 0;
}
.profile-list article:nth-child(even) {
  padding-left: 26px;
  border-left: 1px solid var(--oa-divider);
}
.profile-list h3 {
  font-size: 25px;
}
.profile-list--featured article:first-child {
  grid-column: 1 / -1;
  min-height: 240px;
}
.profile-gallery {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}
.profile-gallery figure {
  position: relative;
  min-height: 240px;
  margin: 0;
  overflow: hidden;
  background: var(--oa-page-soft-bg);
}
.profile-gallery img {
  width: 100%;
  height: 100%;
  min-height: 240px;
  object-fit: cover;
  transition: transform 400ms cubic-bezier(0.22, 1, 0.36, 1);
}
.profile-gallery figure:hover img {
  transform: scale(1.035);
}
.profile-gallery figcaption {
  position: absolute;
  right: 12px;
  bottom: 12px;
  left: 12px;
  padding: 10px 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.58);
  backdrop-filter: blur(12px);
}
.profile-contact {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 30px;
  padding: clamp(30px, 5vw, 60px);
  color: #fff;
  background: linear-gradient(
    120deg,
    #0f172a,
    color-mix(in srgb, var(--profile-accent) 72%, #0f172a)
  );
}
.profile-contact h3 {
  margin: 0;
  font-size: clamp(30px, 5vw, 62px);
}
.profile-contact p {
  color: rgba(255, 255, 255, 0.72);
}
.profile-contact a {
  flex: none;
  padding: 13px 18px;
  color: #111827;
  background: #fff;
  text-decoration: none;
}

@media (max-width: 760px) {
  .profile-hero__banner {
    height: 180px;
  }
  .profile-hero__content {
    grid-template-columns: 1fr;
    align-items: start;
    padding: 0 22px 26px;
    min-height: 500px;
    padding-top: 180px;
  }
  .profile-hero__avatar {
    margin-top: -46px;
  }
  .profile-hero__links {
    justify-content: flex-start;
  }
  .profile-module--span-6 {
    grid-column: span 12;
  }
  .profile-module {
    padding: 54px 0;
  }
  .profile-list {
    grid-template-columns: 1fr;
  }
  .profile-module--span-6:nth-child(even),
  .profile-list article:nth-child(even) {
    padding-left: 0;
    border-left: 0;
  }
  .profile-now,
  .profile-stats,
  .profile-awards,
  .profile-stack-grid,
  .profile-gallery {
    grid-template-columns: 1fr;
  }
  .profile-contact {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .profile-gallery img,
  .profile-articles a,
  .profile-links-list a {
    transition: none;
  }
}
</style>
