<template>
  <article class="blog-comment-item" :class="{ 'is-reply': depth > 0 }">
    <div class="blog-comment-item__avatar">
      <UserAvatar :name="comment.userName || '匿名用户'" :size="depth > 0 ? 32 : 44" :src="comment.userAvatar || ''" />
    </div>
    <div class="blog-comment-item__body">
      <header class="blog-comment-item__head">
        <div class="blog-comment-item__author">
          <span class="author-name">{{ comment.userName || '匿名用户' }}</span>
          <span class="comment-time">{{ formatDateTime(comment.createdAt) }}</span>
        </div>
        <el-button class="blog-comment-item__reply" link type="primary" @click="emit('reply', comment)">
          <el-icon class="reply-icon"><ChatLineRound /></el-icon>
          <span class="reply-text">回复</span>
        </el-button>
      </header>
      <div class="markdown-body blog-comment-item__content" v-html="contentHtml"></div>
      
      <div v-if="comment.replies?.length" class="blog-comment-item__replies">
        <BlogCommentItem
          v-for="reply in comment.replies"
          :key="reply.id"
          :comment="reply"
          :depth="depth + 1"
          @reply="handleReply"
        />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import UserAvatar from '@/components/common/UserAvatar.vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import { formatDateTime } from '@/utils/format.ts'
import { renderMarkdown } from '@/utils/markdown.ts'
import { computed } from 'vue'

defineOptions({ name: 'BlogCommentItem' })

const props = withDefaults(
  defineProps<{
    comment: Record<string, any>
    depth?: number
  }>(),
  {
    depth: 0,
  },
)

const emit = defineEmits<{
  reply: [comment: Record<string, any>]
}>()

const contentHtml = computed(() => renderMarkdown(props.comment.content || ''))

function handleReply(comment: Record<string, any>) {
  emit('reply', comment)
}
</script>

<style scoped>
.blog-comment-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 16px;
  position: relative;
  padding: 20px 0 12px;
}

.blog-comment-item.is-reply {
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
  padding: 16px 0 4px;
}

.blog-comment-item__avatar {
  position: relative;
  z-index: 2;
}

.blog-comment-item__avatar::after {
  content: '';
  position: absolute;
  top: 50px;
  bottom: -24px;
  left: 50%;
  width: 1px;
  background: var(--oa-border);
  opacity: 0.6;
  transform: translateX(-50%);
}

.blog-comment-item:last-child > .blog-comment-item__avatar::after {
  display: none;
}

.blog-comment-item.is-reply .blog-comment-item__avatar::after {
  top: 38px;
  bottom: -20px;
}

.blog-comment-item__body {
  min-width: 0;
  padding: 18px 20px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.015);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.blog-comment-item__body:hover {
  border-color: color-mix(in srgb, var(--oa-primary, #2563eb) 35%, var(--oa-border));
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.045);
  transform: translateY(-1.5px);
}

.blog-comment-item__head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.blog-comment-item__author {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.author-name {
  color: var(--oa-text);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.comment-time {
  color: var(--oa-muted);
  font-size: 12px;
  opacity: 0.85;
}

.blog-comment-item__reply {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: var(--oa-muted);
  background: transparent;
  transition: all 0.2s ease;
}

.blog-comment-item__reply:hover {
  color: var(--oa-primary, #2563eb) !important;
  background: color-mix(in srgb, var(--oa-primary, #2563eb) 8%, transparent);
}

.reply-icon {
  font-size: 14px;
  transition: transform 0.2s ease;
}

.blog-comment-item__reply:hover .reply-icon {
  transform: scale(1.15) rotate(-8deg);
}

.blog-comment-item__content {
  color: var(--oa-text-soft);
  font-size: 14.5px;
  line-height: 1.7;
}

.blog-comment-item__content :deep(p) {
  margin: 0 0 10px;
  color: var(--oa-text-soft);
  font-size: 14.5px;
  line-height: 1.7;
}

.blog-comment-item__content :deep(p:last-child) {
  margin-bottom: 0;
}

.blog-comment-item__content :deep(pre) {
  margin: 14px 0;
  border-radius: 8px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
}

.blog-comment-item__content :deep(code) {
  padding: 2px 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  color: var(--oa-primary, #2563eb);
  background: color-mix(in srgb, var(--oa-primary, #2563eb) 6%, var(--oa-page-soft-bg));
  border-radius: 4px;
}

.blog-comment-item__content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 3px solid var(--oa-border);
  color: var(--oa-muted);
  background: var(--oa-page-soft-bg);
  border-radius: 0 8px 8px 0;
}

.blog-comment-item__replies {
  display: grid;
  gap: 4px;
  position: relative;
  margin-top: 18px;
  padding-left: 12px;
}

.blog-comment-item__replies::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 24px;
  left: -4px;
  width: 1px;
  background: var(--oa-border);
  opacity: 0.6;
}

@media (max-width: 640px) {
  .blog-comment-item {
    grid-template-columns: 36px minmax(0, 1fr);
    gap: 12px;
    padding: 16px 0 8px;
  }

  .blog-comment-item.is-reply {
    grid-template-columns: 28px minmax(0, 1fr);
    gap: 8px;
    padding: 12px 0 2px;
  }

  .blog-comment-item__body {
    padding: 14px 16px;
    border-radius: 12px;
  }

  .blog-comment-item__head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .blog-comment-item__reply {
    align-self: flex-end;
    margin-top: -30px;
  }
}
</style>
