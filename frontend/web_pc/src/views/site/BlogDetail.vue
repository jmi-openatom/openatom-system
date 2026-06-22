<template>
  <ViewPage :loading="loading" class="blog-detail">
    <section class="blog-detail__hero home-interactive-section">
      <div class="container blog-detail__hero-inner">
        <div class="blog-detail__copy">
          <div>
            <el-button size="large" type="primary" @click="$router.back()"> 返回博客 </el-button>
          </div>
          <br />

          <div class="blog-detail__meta">
            <el-tag v-if="article.featured" effect="plain" type="warning">推荐</el-tag>
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
            <el-button v-if="canEdit" size="small" @click="$router.push('/blog/my')"
              >编辑</el-button
            >
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
        <div :class="{ 'is-empty': !article.coverUrl }" class="blog-detail__cover">
          <img v-if="article.coverUrl" :alt="article.title" :src="article.coverUrl" />
          <span v-else>{{ coverInitial(article.title) }}</span>
        </div>
      </div>
    </section>

    <section class="blog-detail__body home-interactive-section">
      <div class="container blog-detail__grid">
        <article class="markdown-body blog-article">
          <div v-html="html"></div>
        </article>

        <aside class="blog-aside">
          <div class="blog-aside__block">
            <span>作者</span>
            <div class="blog-author">
              <UserAvatar
                :name="article.authorName || '匿名作者'"
                :size="42"
                :src="article.authorAvatar || ''"
              />
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
          <div>
            <span>Discussion</span>
            <h2>评论区</h2>
          </div>
          <strong>{{ totalComments }} 条讨论</strong>
        </div>

        <div v-if="isLoggedIn" class="comment-composer">
          <UserAvatar
            :name="currentUserName"
            :qq-openid="currentUserQqOpenid"
            :size="44"
            :src="currentUserAvatar"
          />
          <div class="comment-composer__main">
            <div v-if="replyTarget" class="comment-composer__replying">
              <span>正在回复 {{ replyTarget.userName || '匿名用户' }}</span>
              <el-button link type="primary" @click="cancelReply">取消</el-button>
            </div>
            <el-input
              v-model="commentForm.content"
              :placeholder="commentPlaceholder"
              :rows="4"
              class="comment-composer__input"
              maxlength="1000"
              show-word-limit
              type="textarea"
            />
            <div class="comment-composer__footer">
              <span>支持 Markdown、代码块和图片链接</span>
              <el-button :loading="commenting" type="primary" @click="submitComment">
                {{ replyTarget ? '发布回复' : '发布评论' }}
              </el-button>
            </div>
          </div>
        </div>
        <div v-else class="comment-login-tip">
          <span>登录后参与讨论</span>
          <el-button plain type="primary" @click="ensureLogin">去登录</el-button>
        </div>

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

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import BlogCommentItem from '@/components/site/blog/BlogCommentItem.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
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
const canEdit = computed(
  () => currentUser.value?.id && currentUser.value.id === article.value.authorId,
)
const html = computed(() =>
  renderMarkdown(article.value.contentMarkdown || article.value.summary || ''),
)
const totalComments = computed(() => countComments(comments.value))
const currentUserName = computed(
  () => currentUser.value?.realName || currentUser.value?.userName || '我',
)
const currentUserAvatar = computed(() =>
  String(currentUser.value?.displayAvatar || currentUser.value?.avatar || ''),
)
const currentUserQqOpenid = computed(() => String(currentUser.value?.qqOpenid || ''))
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
    await navigator.clipboard?.writeText(article.value.title + '\n' + window.location.href)
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
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

function countComments(list: any[]): number {
  return list.reduce((total, comment) => total + 1 + countComments(comment.replies || []), 0)
}

function coverInitial(value: string) {
  return String(value || 'B')
    .slice(0, 1)
    .toUpperCase()
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
  gap: 24px;
  margin-top: 32px;
  overflow: hidden;
  padding: 32px;
}

.comments-section__head {
  display: flex;
  gap: 16px;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--oa-border);
}

.comments-section__head > div {
  display: grid;
  gap: 6px;
}

.comments-section__head > div > span {
  color: var(--oa-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  opacity: 0.85;
}

.comments-section__head h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  color: var(--oa-text);
  line-height: 1.2;
}

.comments-section__head strong {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  padding: 0 16px;
  color: var(--oa-primary, #1d1d1f);
  background: color-mix(in srgb, var(--oa-primary, #1d1d1f) 8%, var(--oa-elevated-bg));
  border: 1px solid color-mix(in srgb, var(--oa-primary, #1d1d1f) 15%, var(--oa-border));
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.comment-composer {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  padding: 24px;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 18px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.01);
}

.comment-composer__main {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.comment-composer__replying {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  color: var(--oa-primary, #1d1d1f);
  background: color-mix(in srgb, var(--oa-primary, #1d1d1f) 6%, var(--oa-elevated-bg));
  border: 1px solid color-mix(in srgb, var(--oa-primary, #1d1d1f) 12%, var(--oa-border));
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
}

.comment-composer__input :deep(.el-textarea__inner) {
  min-height: 116px !important;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 12px;
  box-shadow: none;
  color: var(--oa-text);
  font-size: 14.5px;
  padding: 12px 16px;
  line-height: 1.7;
  resize: vertical;
  transition: all 0.25s ease;
}

.comment-composer__input :deep(.el-textarea__inner:focus) {
  border-color: color-mix(in srgb, var(--oa-primary, #1d1d1f) 45%, var(--oa-border));
  background: var(--oa-page-bg);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--oa-primary, #1d1d1f) 10%, transparent);
}

.comment-composer__footer {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.comment-composer__footer span {
  color: var(--oa-muted);
  font-size: 13px;
  opacity: 0.85;
}

.comment-composer__footer :deep(.el-button) {
  border-radius: 10px;
  font-weight: 500;
  padding: 10px 20px;
  height: auto;
}

.comment-login-tip {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  color: var(--oa-muted);
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 16px;
  font-size: 14.5px;
}

.comment-login-tip :deep(.el-button) {
  border-radius: 10px;
  font-weight: 500;
  padding: 8px 18px;
}

.comment-list {
  display: grid;
  gap: 16px;
  background: transparent;
  border: none;
  padding: 0;
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

@media (max-width: 640px) {
  .comments-section__head,
  .comment-composer,
  .comment-login-tip,
  .comment-composer__footer {
    align-items: stretch;
  }

  .comments-section__head,
  .comment-composer__footer,
  .comment-login-tip {
    flex-direction: column;
  }

  .comment-composer {
    grid-template-columns: 38px minmax(0, 1fr);
    padding: 12px;
  }
}
</style>
