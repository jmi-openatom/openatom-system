<template>
  <ViewPage class="blog-detail" :loading="loading">
    <section class="blog-detail__hero home-interactive-section">
      <HomeInteractiveBackdrop :radius="230" :spacing="68" :strength="22" />
      <div class="container blog-detail__hero-inner">
        <div class="blog-detail__copy">
          <el-button text @click="$router.back()">返回博客</el-button>
          <div class="blog-detail__meta">
            <el-tag v-if="article.featured" type="warning" effect="plain">推荐</el-tag>
            <span>{{ article.category || '未分类' }}</span>
            <span>{{ article.authorName || '匿名作者' }}</span>
            <span>{{ formatDateTime(article.publishedAt || article.createdAt) }}</span>
          </div>
          <h1>{{ article.title || '文章详情' }}</h1>
          <p>{{ article.summary || '作者暂未填写摘要' }}</p>
          <div class="blog-detail__stats">
            <span>{{ article.viewCount || 0 }} 阅读</span>
            <span>{{ article.likeCount || 0 }} 点赞</span>
            <span>{{ article.favoriteCount || 0 }} 收藏</span>
            <span>{{ article.shareCount || 0 }} 分享</span>
            <span>{{ article.commentCount ?? totalComments }} 评论</span>
            <el-button v-if="canEdit" size="small" @click="$router.push('/blog/my')">编辑</el-button>
          </div>
          <div class="blog-detail__actions">
            <el-button
              :disabled="Boolean(article.liked)"
              :icon="Pointer"
              :type="article.liked ? 'primary' : 'default'"
              @click="likeArticle"
            >
              {{ article.liked ? '已点赞' : '点赞' }}
            </el-button>
            <el-button
              :disabled="Boolean(article.favorited)"
              :icon="Star"
              :type="article.favorited ? 'primary' : 'default'"
              @click="favoriteArticle"
            >
              {{ article.favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button :icon="Share" @click="shareArticle">分享</el-button>
          </div>
        </div>
        <div class="blog-detail__cover" :class="{ 'is-empty': !article.coverUrl }">
          <img v-if="article.coverUrl" :alt="article.title" :src="article.coverUrl" />
          <span v-else>{{ coverInitial(article.title) }}</span>
        </div>
      </div>
    </section>

    <section class="blog-detail__body home-interactive-section">
      <HomeInteractiveBackdrop :radius="210" :spacing="62" :strength="18" />
      <div class="container blog-detail__grid">
        <article class="markdown-body blog-article">
          <div v-html="html"></div>
        </article>

        <aside class="blog-aside">
          <div class="blog-aside__block">
            <span>作者</span>
            <div class="blog-author">
              <UserAvatar :name="article.authorName || '匿名作者'" :size="42" :src="article.authorAvatar || ''" />
              <strong>{{ article.authorName || '匿名作者' }}</strong>
            </div>
          </div>
          <div class="blog-aside__block">
            <span>标签</span>
            <div class="blog-aside__tags">
              <el-tag v-for="tag in article.tags || []" :key="tag" effect="plain">{{ tag }}</el-tag>
              <span v-if="!(article.tags || []).length">暂无标签</span>
            </div>
          </div>
        </aside>
      </div>

      <div class="container comments-section">
        <div class="comments-section__head">
          <h2>评论</h2>
          <span>{{ totalComments }} 条</span>
        </div>

        <div v-if="isLoggedIn" class="comment-editor">
          <div v-if="replyTarget" class="comment-editor__replying">
            <span>回复 {{ replyTarget.userName || '匿名用户' }}</span>
            <el-button link type="primary" @click="cancelReply">取消回复</el-button>
          </div>
          <el-input
            v-model="commentForm.content"
            maxlength="1000"
            :placeholder="commentPlaceholder"
            show-word-limit
            :rows="4"
            type="textarea"
          />
          <el-button type="primary" :loading="commenting" @click="submitComment">
            {{ replyTarget ? '发布回复' : '发布评论' }}
          </el-button>
        </div>
        <el-alert v-else type="info" :closable="false" title="登录后可以发表评论" />

        <div class="comment-list">
          <BlogCommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            @reply="startReply"
          />
          <el-empty v-if="!comments.length" description="还没有评论" />
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import BlogCommentItem from '@/components/site/blog/BlogCommentItem.vue'
import HomeInteractiveBackdrop from '@/components/site/home/HomeInteractiveBackdrop.vue'
import { ElMessage } from 'element-plus'
import { Pointer, Share, Star } from '@element-plus/icons-vue'
import { siteApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { getCurrentUser, getToken } from '@/utils/auth.ts'
import { renderMarkdown } from '@/utils/markdown.ts'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const commenting = ref(false)
const article = ref<Record<string, any>>({})
const comments = ref<any[]>([])
const commentForm = ref({ content: '', parentId: null as number | null })
const replyTarget = ref<Record<string, any> | null>(null)

const isLoggedIn = computed(() => Boolean(getToken()))
const currentUser = computed(() => getCurrentUser())
const canEdit = computed(() => currentUser.value?.id && currentUser.value.id === article.value.authorId)
const html = computed(() => renderMarkdown(article.value.contentMarkdown || article.value.summary || ''))
const totalComments = computed(() => countComments(comments.value))
const commentPlaceholder = computed(() =>
  replyTarget.value ? `回复 ${replyTarget.value.userName || '匿名用户'}` : '写下你的观点',
)

async function fetchDetail() {
  loading.value = true
  try {
    article.value = await siteApi.blogArticleDetail(route.params.id)
    await refreshComments()
  } finally {
    loading.value = false
  }
}

async function refreshComments() {
  comments.value = (await siteApi.blogComments(route.params.id)) || []
}

async function submitComment() {
  if (!ensureLogin()) return
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请填写评论内容')
    return
  }
  commenting.value = true
  try {
    await siteApi.createBlogComment(route.params.id, {
      content: commentForm.value.content,
      parentId: commentForm.value.parentId || undefined,
    })
    commentForm.value = { content: '', parentId: null }
    replyTarget.value = null
    await refreshComments()
    article.value.commentCount = totalComments.value
    ElMessage.success('评论已发布')
  } finally {
    commenting.value = false
  }
}

async function likeArticle() {
  if (!ensureLogin()) return
  article.value = await siteApi.likeBlogArticle(route.params.id, { channel: 'web' })
  ElMessage.success(article.value.liked ? '点赞已记录' : '操作成功')
}

async function favoriteArticle() {
  if (!ensureLogin()) return
  article.value = await siteApi.favoriteBlogArticle(route.params.id, { channel: 'web' })
  ElMessage.success(article.value.favorited ? '收藏已记录' : '操作成功')
}

async function shareArticle() {
  if (!ensureLogin()) return
  article.value = await siteApi.shareBlogArticle(route.params.id, { channel: 'copy_link' })
  try {
    await navigator.clipboard?.writeText(window.location.href)
    ElMessage.success('链接已复制，分享记录已保存')
  } catch (_error) {
    ElMessage.success('分享记录已保存')
  }
}

function startReply(comment: Record<string, any>) {
  if (!ensureLogin()) return
  replyTarget.value = comment
  commentForm.value.parentId = Number(comment.id)
}

function cancelReply() {
  replyTarget.value = null
  commentForm.value.parentId = null
}

function ensureLogin() {
  if (getToken()) return true
  router.push({ path: '/admin/login', query: { redirect: route.fullPath } })
  return false
}

function countComments(list: any[]): number {
  return list.reduce((total, comment) => total + 1 + countComments(comment.replies || []), 0)
}

function coverInitial(value: string) {
  return String(value || 'B').slice(0, 1).toUpperCase()
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.blog-detail {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.blog-detail__hero {
  background: var(--oa-elevated-bg);
  border-bottom: 1px solid var(--oa-border);
}

.blog-detail__hero-inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 420px);
  gap: 42px;
  align-items: center;
  padding: 96px 0 52px;
}

.blog-detail__copy {
  min-width: 0;
}

.blog-detail__copy > .el-button {
  padding-left: 0;
}

.blog-detail__meta,
.blog-detail__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.blog-detail h1 {
  margin: 16px 0;
  color: var(--oa-text);
  font-size: clamp(34px, 4vw, 56px);
  font-weight: 650;
  line-height: 1.12;
}

.blog-detail__copy p {
  max-width: 760px;
  margin: 0 0 18px;
  color: var(--oa-muted);
  font-size: 18px;
  line-height: 1.7;
}

.blog-detail__cover {
  display: grid;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  place-items: center;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

.blog-detail__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.blog-detail__cover span {
  color: var(--oa-muted);
  font-size: 72px;
  font-weight: 700;
}

.blog-detail__body {
  padding: 36px 0 64px;
}

.blog-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 24px;
  align-items: start;
}

.blog-article,
.blog-aside,
.comments-section {
  padding: 24px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

.blog-article {
  min-height: 420px;
  overflow: hidden;
}

.blog-aside {
  display: grid;
  gap: 18px;
}

.blog-aside__block {
  display: grid;
  gap: 8px;
}

.blog-aside__block span {
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-aside__block strong {
  color: var(--oa-text);
  font-size: 18px;
}

.blog-author {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.blog-author strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blog-aside__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--oa-muted);
}

.comments-section {
  display: grid;
  gap: 18px;
  margin-top: 24px;
}

.comments-section__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comments-section__head h2 {
  margin: 0;
  font-size: 24px;
}

.comments-section__head span {
  color: var(--oa-muted);
}

.comment-editor {
  display: grid;
  gap: 12px;
  justify-items: end;
}

.comment-editor__replying {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  color: var(--oa-muted);
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.comment-list {
  display: grid;
  gap: 12px;
}

.comment-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--oa-border);
}

.comment-item__avatar {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--oa-muted);
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 50%;
  font-weight: 700;
}

.comment-item header {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.comment-item header span {
  color: var(--oa-muted);
  font-size: 13px;
}

.comment-item p {
  margin: 8px 0 0;
  color: var(--oa-text);
  line-height: 1.7;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  color: var(--oa-text);
  line-height: 1.35;
}

.markdown-body :deep(p),
.markdown-body :deep(li) {
  color: var(--oa-muted);
  font-size: 16px;
  line-height: 1.9;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  background: var(--oa-page-soft-bg);
  border-radius: 4px;
}

@media (max-width: 900px) {
  .blog-detail__hero-inner,
  .blog-detail__grid {
    grid-template-columns: 1fr;
  }

  .blog-detail__hero-inner {
    padding-top: 78px;
  }
}
</style>
