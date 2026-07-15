<template>
  <ViewPage :loading="loading" class="member-profile-page">
    <div class="container member-profile-page__toolbar">
      <el-button text @click="$router.push('/members')">← 返回成员列表</el-button>
      <div v-if="profile">
        <el-button
          :loading="liking"
          :plain="!profile.liked"
          :type="profile.liked ? 'primary' : 'default'"
          @click="toggleLike"
        >
          <el-icon><Pointer /></el-icon>
          {{ profile.liked ? '已点赞' : '点赞' }} · {{ profile.likeCount || 0 }}
        </el-button>
        <template v-if="profile.owner">
          <el-tag v-if="profile.status !== 'published'" effect="plain">{{
            profile.status === 'default' ? '默认主页' : '草稿'
          }}</el-tag>
          <el-button @click="$router.push('/settings/account')">账号与安全</el-button>
          <el-button type="primary" @click="$router.push('/profile/edit')">编辑主页</el-button>
        </template>
      </div>
    </div>
    <div v-if="profile" class="container member-profile-page__content">
      <MemberProfileRenderer :profile="profile" />
      <p v-if="profile.owner && profile.status !== 'published'" class="member-profile-page__notice">
        当前展示的是你的草稿预览；其他成员暂时看到系统默认主页。
      </p>

      <section class="profile-comments">
        <header class="profile-comments__head">
          <div>
            <span>DISCUSSION</span>
            <h2>留言与讨论</h2>
          </div>
          <strong>{{ totalComments }} 条评论</strong>
        </header>

        <div class="comment-composer">
          <UserAvatar
            :name="currentUserName"
            :qq-openid="currentUserQqOpenid"
            :size="44"
            :src="currentUserAvatar"
          />
          <div class="comment-composer__main">
            <div v-if="replyTarget" class="comment-composer__replying">
              <span>正在回复 {{ replyTarget.userName || '社团成员' }}</span>
              <el-button link type="primary" @click="cancelReply">取消</el-button>
            </div>
            <el-input
              v-model="commentForm.content"
              :placeholder="commentPlaceholder"
              :rows="4"
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

        <div class="comment-list">
          <BlogCommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            @reply="startReply"
          />
          <el-empty v-if="!comments.length" description="还没有评论，来留下第一条留言吧" />
        </div>
      </section>
    </div>
    <el-result
      v-else-if="!loading"
      icon="warning"
      sub-title="该成员主页不存在或不可访问"
      title="无法打开主页"
    >
      <template #extra
        ><el-button type="primary" @click="$router.push('/members')"
          >查看成员列表</el-button
        ></template
      >
    </el-result>
  </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import BlogCommentItem from '@/components/site/blog/BlogCommentItem.vue'
import MemberProfileRenderer from '@/components/site/member/MemberProfileRenderer.vue'
import { memberProfileApi } from '@/api'
import type { MemberProfile } from '@/types/member-profile'
import { getCurrentUser } from '@/utils/auth.ts'
import { Pointer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const loading = ref(false)
const liking = ref(false)
const commenting = ref(false)
const profile = ref<MemberProfile | null>(null)
const comments = ref<Record<string, any>[]>([])
const commentForm = ref({ content: '', parentId: null as number | null })
const replyTarget = ref<Record<string, any> | null>(null)

const currentUser = computed(() => getCurrentUser())
const currentUserName = computed(
  () => currentUser.value?.realName || currentUser.value?.userName || '我',
)
const currentUserAvatar = computed(() =>
  String(currentUser.value?.displayAvatar || currentUser.value?.avatar || ''),
)
const currentUserQqOpenid = computed(() => String(currentUser.value?.qqOpenid || ''))
const totalComments = computed(() => countComments(comments.value))
const commentPlaceholder = computed(() =>
  replyTarget.value ? `回复 ${replyTarget.value.userName || '社团成员'}` : '写下你的留言',
)

async function fetchProfile() {
  loading.value = true
  profile.value = null
  try {
    const data =
      route.path === '/profile'
        ? await memberProfileApi.mine()
        : await memberProfileApi.detail(String(route.params.slug || ''))
    profile.value = data
    comments.value = (await memberProfileApi.comments(data.slug)) || []
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!profile.value || liking.value) return
  liking.value = true
  try {
    const result = await memberProfileApi.toggleLike(profile.value.slug)
    profile.value.likeCount = result.likeCount
    profile.value.liked = result.liked
    ElMessage.success(result.liked ? '已点赞' : '已取消点赞')
  } finally {
    liking.value = false
  }
}

async function refreshComments() {
  if (!profile.value) return
  comments.value = (await memberProfileApi.comments(profile.value.slug)) || []
  profile.value.commentCount = totalComments.value
}

async function submitComment() {
  if (!profile.value || !commentForm.value.content.trim()) {
    ElMessage.warning('请填写评论内容')
    return
  }
  commenting.value = true
  try {
    await memberProfileApi.createComment(profile.value.slug, {
      content: commentForm.value.content,
      parentId: commentForm.value.parentId || undefined,
    })
    commentForm.value = { content: '', parentId: null }
    replyTarget.value = null
    await refreshComments()
    ElMessage.success('评论已发布')
  } finally {
    commenting.value = false
  }
}

function startReply(comment: Record<string, any>) {
  replyTarget.value = comment
  commentForm.value.parentId = comment.id
}

function cancelReply() {
  replyTarget.value = null
  commentForm.value.parentId = null
}

function countComments(list: Record<string, any>[]): number {
  return list.reduce((total, comment) => total + 1 + countComments(comment.replies || []), 0)
}

onMounted(fetchProfile)
watch(() => route.fullPath, fetchProfile)
</script>

<style scoped>
.member-profile-page {
  min-height: 100vh;
  padding: 26px 0 80px;
  background: var(--oa-page-soft-bg);
}
.member-profile-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}
.member-profile-page__toolbar > div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.member-profile-page__notice {
  margin: 18px 0 0;
  padding: 14px 18px;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
}
.profile-comments {
  margin-top: 34px;
  padding: 34px clamp(20px, 4vw, 48px);
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 22px;
}
.profile-comments__head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}
.profile-comments__head span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}
.profile-comments__head h2 {
  margin: 8px 0 0;
  font-size: clamp(26px, 4vw, 40px);
  letter-spacing: -0.04em;
}
.profile-comments__head strong,
.comment-composer__footer > span {
  color: var(--oa-muted);
  font-size: 13px;
}
.comment-composer {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 14px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--oa-divider);
}
.comment-composer__main {
  min-width: 0;
}
.comment-composer__replying,
.comment-composer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.comment-composer__replying {
  margin-bottom: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}
.comment-composer__footer {
  margin-top: 12px;
}
.comment-list {
  margin-top: 12px;
}
@media (max-width: 620px) {
  .member-profile-page__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .member-profile-page__toolbar > div {
    flex-wrap: wrap;
  }
  .profile-comments__head,
  .comment-composer__footer {
    align-items: flex-start;
    flex-direction: column;
  }
  .comment-composer {
    grid-template-columns: 1fr;
  }
}
</style>
