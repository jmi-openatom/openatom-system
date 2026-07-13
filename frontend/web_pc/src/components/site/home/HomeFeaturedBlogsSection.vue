<template>
  <section
    v-if="articles.length"
    id="featured-blogs"
    class="featured-blog-section home-interactive-section"
  >
    <HomeInteractiveBackdrop :radius="220" :spacing="66" :strength="18" />

    <div class="container featured-blog-section__inner">
      <div class="section-heading reveal-block">
        <span>精选阅读</span>
        <h2>推荐文章</h2>
        <p>从社团成员的项目实践、开源笔记和技术复盘里，挑出值得先读的内容。</p>
      </div>

      <div class="featured-blog-grid">
        <article
          v-for="(article, index) in articles"
          :key="article.id || article.title"
          :class="{ 'is-primary': index === 0 }"
          class="featured-blog-card reveal-card"
          @click="openArticle(article)"
        >
          <div class="featured-blog-card__media" :class="{ 'is-empty': !article.coverUrl }">
            <img
              v-if="article.coverUrl"
              :alt="article.title"
              :src="article.coverUrl"
              decoding="async"
              loading="lazy"
            />
            <span v-else>{{ coverInitial(article.title) }}</span>
          </div>

          <div class="featured-blog-card__body">
            <div class="featured-blog-card__meta">
              <span>{{ article.category || '未分类' }}</span>
              <span>{{ article.authorName || '匿名作者' }}</span>
              <time>{{ formatBlogDate(article.publishedAt || article.createdAt) }}</time>
            </div>

            <h3>{{ article.title }}</h3>
            <p>{{ article.summary || '作者暂未填写摘要' }}</p>

            <div v-if="article.tags?.length" class="featured-blog-card__tags">
              <span v-for="tag in article.tags.slice(0, 3)" :key="tag">{{ tag }}</span>
            </div>

            <footer>
              <span>{{ article.viewCount || 0 }} 阅读</span>
              <span>{{ article.likeCount || 0 }} 点赞</span>
              <span>{{ article.commentCount || 0 }} 评论</span>
            </footer>
          </div>
        </article>
      </div>

      <button class="featured-blog-section__more" type="button" @click="router.push('/blog')">
        查看全部文章
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { monthDayParts } from '@/utils/format'
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

defineProps<{
  articles: any[]
}>()

const router = useRouter()

function openArticle(article: any) {
  if (!article?.id) return
  router.push(`/blog/${article.id}`)
}

function coverInitial(title: string) {
  return String(title || 'B')
    .slice(0, 1)
    .toUpperCase()
}

function formatBlogDate(value: string) {
  const parts = monthDayParts(value)
  return parts ? `${parts.month}.${parts.day}` : '近期'
}
</script>

<style scoped>
.featured-blog-section {
  position: relative;
  overflow: hidden;
  min-height: 100svh;
  display: flex;
  align-items: center;
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
}

.featured-blog-section__inner {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 34px;
  padding: 96px 24px;
}

.featured-blog-section .section-heading {
  max-width: 760px;
}

.featured-blog-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 18px;
  align-items: stretch;
}

.featured-blog-card {
  position: relative;
  display: grid;
  min-height: 260px;
  overflow: hidden;
  border: 1px solid rgba(29, 29, 31, 0.1);
  border-radius: 28px;
  background: var(--oa-elevated-bg);
  cursor: pointer;
  isolation: isolate;
  box-shadow: 0 18px 44px rgba(29, 29, 31, 0.08);
  transition:
    transform 0.24s ease,
    border-color 0.24s ease,
    box-shadow 0.24s ease;
}

.featured-blog-card:hover {
  border-color: rgba(29, 29, 31, 0.24);
  box-shadow: 0 28px 70px rgba(29, 29, 31, 0.14);
  transform: translateY(-4px);
}

.featured-blog-card.is-primary {
  grid-row: span 2;
  min-height: 560px;
}

.featured-blog-card__media {
  position: absolute;
  inset: 0;
  background: #1d1d1f;
}

.featured-blog-card__media::after {
  position: absolute;
  inset: 0;
  content: '';
  background:
    linear-gradient(
      180deg,
      rgba(0, 0, 0, 0.02) 0%,
      rgba(0, 0, 0, 0.28) 42%,
      rgba(0, 0, 0, 0.86) 100%
    ),
    linear-gradient(
      90deg,
      rgba(0, 0, 0, 0.54) 0%,
      rgba(0, 0, 0, 0.08) 64%,
      rgba(0, 0, 0, 0.28) 100%
    );
}

.featured-blog-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(1.05) contrast(1.04);
  transition: transform 0.44s ease;
}

.featured-blog-card:hover .featured-blog-card__media img {
  transform: scale(1.04);
}

.featured-blog-card__media.is-empty {
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 18% 16%, rgba(255, 255, 255, 0.16), transparent 30%),
    radial-gradient(circle at 86% 18%, rgba(161, 161, 170, 0.12), transparent 28%), #1d1d1f;
}

.featured-blog-card__media span {
  color: rgba(255, 255, 255, 0.2);
  font-size: clamp(84px, 14vw, 180px);
  font-weight: 760;
}

.featured-blog-card__body {
  position: relative;
  z-index: 1;
  align-self: end;
  display: grid;
  gap: 12px;
  padding: 28px;
  color: #ffffff;
}

.featured-blog-card__meta,
.featured-blog-card footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  letter-spacing: 0.04em;
  text-shadow: 0 1px 14px rgba(0, 0, 0, 0.48);
}

.featured-blog-card h3 {
  max-width: 780px;
  margin: 0;
  color: #ffffff;
  font-size: clamp(24px, 2.8vw, 44px);
  line-height: 1.12;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.55);
}

.featured-blog-card:not(.is-primary) h3 {
  font-size: clamp(22px, 2vw, 30px);
}

.featured-blog-card p {
  display: -webkit-box;
  max-width: 650px;
  margin: 0;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.7;
  text-shadow: 0 1px 16px rgba(0, 0, 0, 0.5);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.featured-blog-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.featured-blog-card__tags span {
  padding: 5px 9px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
}

.featured-blog-section__more {
  justify-self: center;
  min-width: 152px;
  height: 44px;
  padding: 0 20px;
  border: 1px solid rgba(29, 29, 31, 0.14);
  border-radius: 999px;
  background: var(--oa-elevated-bg);
  color: var(--oa-text);
  cursor: pointer;
  transition:
    transform 0.22s ease,
    background-color 0.22s ease,
    color 0.22s ease,
    border-color 0.22s ease;
}

.featured-blog-section__more:hover {
  border-color: #1d1d1f;
  background: #1d1d1f;
  color: #ffffff;
  transform: translateY(-2px);
}

@media (max-width: 900px) {
  .featured-blog-section {
    min-height: auto;
  }

  .featured-blog-section__inner {
    padding: 72px 20px;
  }

  .featured-blog-grid {
    grid-template-columns: 1fr;
  }

  .featured-blog-card,
  .featured-blog-card.is-primary {
    min-height: 360px;
  }

  .featured-blog-card__body {
    padding: 22px;
  }
}
</style>
