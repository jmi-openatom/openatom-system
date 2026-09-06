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
        <div v-if="headings.length" class="blog-mobile-outline">
          <BlogReadingOutline
            :key="String(route.params.id)"
            :active-id="activeHeadingId"
            :headings="headings"
            :progress="readingProgress"
            collapsible
            @select="navigateToHeading"
          />
        </div>
        <article class="blog-article">
          <div class="blog-article__reading-meta">
            <span><BookOpen :size="16" aria-hidden="true" /> 正文</span>
            <span><Clock3 :size="14" aria-hidden="true" /> 预计阅读 {{ readingMinutes }} 分钟</span>
          </div>
          <div ref="articleContent" class="blog-article__content">
            <MarkdownContent :content="content" heading-id-prefix="blog-article" />
          </div>
          <div class="blog-article__end"><span aria-hidden="true"></span> 本文完</div>
        </article>

        <aside class="blog-aside">
          <div v-if="headings.length" class="blog-aside__outline">
            <BlogReadingOutline
              :active-id="activeHeadingId"
              :headings="headings"
              :progress="readingProgress"
              @select="navigateToHeading"
            />
          </div>
          <div class="blog-aside__block blog-aside__author">
            <span>作者</span>
            <router-link
              v-if="article.authorId && isLoggedIn"
              :to="`/members/member-${article.authorId}`"
              class="blog-author"
            >
              <UserAvatar
                :name="article.authorName || '匿名作者'"
                :size="42"
                :src="article.authorAvatar || ''"
              />
              <strong>{{ article.authorName || '匿名作者' }}</strong>
              <span aria-hidden="true">查看主页 →</span>
            </router-link>
            <div v-else class="blog-author">
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

        <div class="comment-toolbar">
          <span>共 {{ commentTotal }} 个话题</span>
          <el-select
            v-model="commentSort"
            aria-label="评论排序"
            style="width: 120px"
            @change="changeCommentSort"
          >
            <el-option label="最新发布" value="latest" />
            <el-option label="最早发布" value="oldest" />
          </el-select>
        </div>

        <el-alert
          v-if="article.commentsEnabled === false"
          :closable="false"
          class="comment-closed-tip"
          show-icon
          title="作者已关闭这篇文章的评论区"
          type="info"
        />
        <div v-else-if="isLoggedIn" class="comment-composer">
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
            :loading-replies="Boolean(comment._loadingReplies)"
            :reply-enabled="article.commentsEnabled !== false"
            @reply="startReply"
            @like="toggleCommentLike"
            @delete="deleteComment"
            @report="reportComment"
            @load-replies="loadReplies"
          />
          <el-empty v-if="!comments.length" description="还没有评论" />
        </div>
        <el-pagination
          v-if="commentTotal > commentPageSize"
          v-model:current-page="commentPage"
          :page-size="commentPageSize"
          :total="commentTotal"
          background
          layout="prev, pager, next"
          @current-change="refreshComments"
        />
      </div>
    </section>
  </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import BlogCommentItem from '@/components/site/blog/BlogCommentItem.vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus'
import { Pointer, Share, Star } from '@element-plus/icons-vue'
import { siteApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'
import { getCurrentUser, getToken } from '@/utils/auth.ts'
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import BlogReadingOutline from '@/components/site/blog/BlogReadingOutline.vue'
import { SITE_NAME, SITE_URL, updateSeo } from '@/utils/seo.ts'
import { extractMarkdownHeadings, markdownToPlainText } from '@/utils/markdown.ts'
import { BookOpen, Clock3 } from 'lucide-vue-next'
import {
  usePreferredReducedMotion,
  useResizeObserver,
  useWindowScroll,
  useWindowSize,
} from '@vueuse/core'
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const commenting = ref(false)
const article = ref<Record<string, any>>({})
const comments = ref<any[]>([])
const commentForm = ref({ content: '', parentId: null as number | null })
const replyTarget = ref<Record<string, any> | null>(null)
const commentPage = ref(1)
const commentPageSize = 10
const commentTotal = ref(0)
const commentSort = ref<'latest' | 'oldest'>('latest')
const articleContent = ref<HTMLElement | null>(null)
const activeHeadingId = ref('')
const readingProgress = ref(0)
const preferredReducedMotion = usePreferredReducedMotion()
const { y: scrollY } = useWindowScroll({ throttle: 60 })
const { height: windowHeight, width: windowWidth } = useWindowSize()
let detailRequestId = 0

const isLoggedIn = computed(() => Boolean(getToken()))
const currentUser = computed(() => getCurrentUser())
const canEdit = computed(
  () => currentUser.value?.id && currentUser.value.id === article.value.authorId,
)
const content = computed(() => article.value.contentMarkdown || article.value.summary || '')
const headings = computed(() => extractMarkdownHeadings(content.value, 'blog-article'))
const readingMinutes = computed(() => {
  const text = markdownToPlainText(content.value)
  const chineseCharacters =
    text.match(/[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}]/gu)?.length || 0
  const otherWords =
    text
      .replace(/[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}]/gu, ' ')
      .match(/[\p{Letter}\p{Number}]+/gu)?.length || 0
  return Math.max(1, Math.ceil(chineseCharacters / 350 + otherWords / 220))
})
const totalComments = computed(() =>
  Number(article.value.commentCount ?? countComments(comments.value)),
)
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
  const requestId = ++detailRequestId
  const articleId = route.params.id
  loading.value = true
  article.value = {}
  comments.value = []
  commentForm.value = { content: '', parentId: null }
  replyTarget.value = null
  activeHeadingId.value = ''
  readingProgress.value = 0
  try {
    const result = await siteApi.blogArticleDetail(articleId)
    if (requestId !== detailRequestId) return
    article.value = result
    updateSeo(
      {
        title: `${article.value.title || '文章详情'}｜${SITE_NAME}`,
        description: article.value.summary || '阅读 JMI-OPENATOM 社团成员分享的技术文章。',
        image: article.value.coverUrl,
        type: 'article',
        structuredData: {
          '@context': 'https://schema.org',
          '@type': 'BlogPosting',
          headline: article.value.title,
          description: article.value.summary,
          image: article.value.coverUrl || `${SITE_URL}/logo.png`,
          datePublished: article.value.publishedAt || article.value.createdAt,
          dateModified: article.value.updatedAt || article.value.publishedAt,
          author: {
            '@type': 'Person',
            name: article.value.authorName || 'JMI-OPENATOM 成员',
          },
          publisher: {
            '@type': 'Organization',
            name: SITE_NAME,
            logo: { '@type': 'ImageObject', url: `${SITE_URL}/logo.png` },
          },
          mainEntityOfPage: `${SITE_URL}${route.path}`,
          inLanguage: 'zh-CN',
        },
      },
      route.path,
    )
    commentPage.value = 1
    const articleComments = await siteApi.blogComments(articleId, {
      page: commentPage.value,
      pageSize: commentPageSize,
      sort: commentSort.value,
    })
    if (requestId === detailRequestId) {
      comments.value = articleComments?.list || []
      commentTotal.value = Number(articleComments?.total || 0)
    }
  } finally {
    if (requestId === detailRequestId) {
      loading.value = false
      await nextTick()
      updateReadingState()
      await scrollToHash('auto')
    }
  }
}

async function refreshComments() {
  const result = await siteApi.blogComments(route.params.id, {
    page: commentPage.value,
    pageSize: commentPageSize,
    sort: commentSort.value,
  })
  comments.value = result?.list || []
  commentTotal.value = Number(result?.total || 0)
}

async function changeCommentSort() {
  commentPage.value = 1
  await refreshComments()
}

async function submitComment() {
  if (!ensureLogin()) return
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请填写评论内容')
    return
  }
  commenting.value = true
  try {
    const target = replyTarget.value
    const created = await siteApi.createBlogComment(route.params.id, {
      content: commentForm.value.content,
      parentId: commentForm.value.parentId || undefined,
    })
    if (target) {
      const rootId = target.rootId || target.id
      const root = comments.value.find((item) => item.id === rootId)
      if (root) {
        root.replyCount = Number(root.replyCount || 0) + 1
        root.replies = [...(root.replies || []), created]
      }
    } else {
      created.replies = []
      created.replyCount = 0
      commentTotal.value += 1
      if (commentPage.value === 1 && commentSort.value === 'latest') {
        comments.value.unshift(created)
        comments.value = comments.value.slice(0, commentPageSize)
      } else {
        commentPage.value = 1
        commentSort.value = 'latest'
        await refreshComments()
      }
    }
    commentForm.value = { content: '', parentId: null }
    replyTarget.value = null
    article.value.commentCount = Number(article.value.commentCount || 0) + 1
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
  if (article.value.commentsEnabled === false) {
    ElMessage.info('作者已关闭这篇文章的评论区')
    return
  }
  if (!ensureLogin()) return
  replyTarget.value = comment
  commentForm.value.parentId = Number(comment.id)
}

function cancelReply() {
  replyTarget.value = null
  commentForm.value.parentId = null
}

async function loadReplies(root: Record<string, any>) {
  if (root._loadingReplies) return
  root._loadingReplies = true
  try {
    const nextPage = Number(root._replyPage || 0) + 1
    const result = await siteApi.blogCommentReplies(route.params.id, root.id, {
      page: nextPage,
      pageSize: 20,
    })
    const incoming = result?.list || []
    root.replies =
      nextPage === 1
        ? incoming
        : [
            ...(root.replies || []),
            ...incoming.filter(
              (item: any) => !(root.replies || []).some((current: any) => current.id === item.id),
            ),
          ]
    root._replyPage = nextPage
  } finally {
    root._loadingReplies = false
  }
}

async function toggleCommentLike(comment: Record<string, any>) {
  if (!ensureLogin()) return
  const updated = await siteApi.toggleBlogCommentLike(route.params.id, comment.id)
  Object.assign(comment, updated)
}

async function deleteComment(comment: Record<string, any>) {
  await ElMessageBox.confirm('删除后评论不会再公开显示，确定继续吗？', '删除评论', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await siteApi.deleteBlogComment(route.params.id, comment.id)
  const removedCount = comment.parentId ? 1 : 1 + Number(comment.replyCount || 0)
  article.value.commentCount = Math.max(0, Number(article.value.commentCount || 0) - removedCount)
  await refreshComments()
  ElMessage.success('评论已删除')
}

async function reportComment(comment: Record<string, any>) {
  if (!ensureLogin()) return
  const { value } = await ElMessageBox.prompt('请说明举报原因，管理员会进行审核。', '举报评论', {
    inputType: 'textarea',
    inputValidator: (text) => Boolean(String(text || '').trim()) || '请填写举报原因',
    confirmButtonText: '提交',
    cancelButtonText: '取消',
  })
  await siteApi.reportBlogComment(route.params.id, comment.id, String(value).trim())
  ElMessage.success('举报已提交')
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

function readingOffset() {
  const headerHeight =
    Number.parseFloat(
      getComputedStyle(document.documentElement).getPropertyValue('--oa-site-header-height'),
    ) || 72
  return headerHeight + 32
}

function updateReadingState() {
  const element = articleContent.value
  if (!element || !content.value) return
  const rect = element.getBoundingClientRect()
  const offset = readingOffset()
  const readableDistance = Math.max(1, rect.height - windowHeight.value + offset)
  readingProgress.value = Math.round(
    Math.max(0, Math.min(1, (offset - rect.top) / readableDistance)) * 100,
  )

  let current = ''
  for (const heading of headings.value) {
    const headingElement = document.getElementById(heading.id)
    if (!headingElement || !element.contains(headingElement)) continue
    if (headingElement.getBoundingClientRect().top <= offset + 8) current = heading.id
    else break
  }
  activeHeadingId.value = current
}

async function scrollToHash(behavior: ScrollBehavior) {
  if (!route.hash || loading.value) return
  let id: string
  try {
    id = decodeURIComponent(route.hash.slice(1))
  } catch {
    return
  }
  if (!headings.value.some((heading) => heading.id === id)) return
  await nextTick()
  requestAnimationFrame(() => {
    const heading = document.getElementById(id)
    if (!heading || !articleContent.value?.contains(heading)) return
    window.scrollTo({
      top: window.scrollY + heading.getBoundingClientRect().top - readingOffset(),
      behavior: preferredReducedMotion.value === 'reduce' ? 'instant' : behavior,
    })
    heading.setAttribute('tabindex', '-1')
    heading.focus({ preventScroll: true })
    updateReadingState()
  })
}

async function navigateToHeading(id: string) {
  const hash = `#${id}`
  if (route.hash === hash) await scrollToHash('smooth')
  else await router.replace({ hash })
}

watch(() => route.params.id, fetchDetail, { immediate: true })
watch(
  () => route.hash,
  () => scrollToHash('smooth'),
  { flush: 'post' },
)
watch([scrollY, windowHeight, windowWidth], updateReadingState, { flush: 'post' })
watch(content, async () => {
  await nextTick()
  updateReadingState()
})
useResizeObserver(articleContent, updateReadingState)
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

.blog-detail__copy h1 {
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
  padding: 40px 0 72px;
  overflow: visible;
}

.blog-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 28px;
  align-items: start;
}

.blog-article,
.blog-aside,
.blog-mobile-outline,
.comments-section {
  padding: 24px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: var(--oa-radius);
}

.blog-article {
  min-width: 0;
  min-height: 420px;
  padding: clamp(24px, 4vw, 56px);
}

.blog-article__reading-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  max-width: 760px;
  margin: 0 auto 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--oa-border);
  color: var(--oa-muted);
  font-size: 12px;
}

.blog-article__reading-meta > span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.blog-article__reading-meta > span:first-child {
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 600;
}

.blog-article__content {
  max-width: 760px;
  margin: 0 auto;
}

.blog-article__end {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 48px;
  color: var(--oa-muted);
  font-size: 12px;
  letter-spacing: 0.12em;
}

.blog-article__end > span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.blog-mobile-outline {
  display: none;
}

.blog-aside {
  display: grid;
  position: sticky;
  top: calc(var(--oa-site-header-height) + 24px);
  gap: 24px;
  min-width: 0;
  max-height: calc(100dvh - var(--oa-site-header-height) - 48px);
  overflow-y: auto;
  scrollbar-width: thin;
}

.blog-aside__outline {
  padding-bottom: 24px;
  border-bottom: 1px solid var(--oa-border);
}

.blog-aside__block {
  display: grid;
  gap: 8px;
}

.blog-aside__block > span,
.blog-author > span {
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-author strong {
  color: var(--oa-text);
  font-size: 14px;
}

.blog-author {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.blog-author > span {
  margin-left: auto;
  white-space: nowrap;
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
  font-size: 12px;
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

.comment-toolbar {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--oa-muted);
  font-size: 13px;
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

@media (max-width: 900px) {
  .blog-detail__hero-inner,
  .blog-detail__grid {
    grid-template-columns: 1fr;
  }

  .blog-detail__hero-inner {
    padding-top: 78px;
  }

  .blog-detail__grid {
    gap: 18px;
  }

  .blog-mobile-outline {
    display: block;
    padding: 20px 24px;
  }

  .blog-aside {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: none;
    overflow: visible;
  }

  .blog-aside__outline {
    display: none;
  }
}

@media (max-width: 640px) {
  .blog-detail__body {
    padding-top: 20px;
  }

  .blog-article,
  .blog-mobile-outline {
    padding: 22px 18px;
  }

  .blog-article__reading-meta {
    margin-bottom: 26px;
    padding-bottom: 16px;
  }

  .blog-aside {
    grid-template-columns: 1fr;
    padding: 22px;
  }

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
