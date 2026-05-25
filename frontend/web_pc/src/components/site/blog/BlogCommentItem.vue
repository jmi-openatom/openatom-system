<template>
  <article class="blog-comment-item" :class="{ 'is-reply': depth > 0 }">
    <div class="blog-comment-item__avatar">
      <UserAvatar :name="comment.userName || '匿名用户'" :size="depth > 0 ? 34 : 42" :src="comment.userAvatar || ''" />
    </div>
    <div class="blog-comment-item__body">
      <header class="blog-comment-item__head">
        <div class="blog-comment-item__author">
          <strong>{{ comment.userName || '匿名用户' }}</strong>
          <span>{{ formatDateTime(comment.createdAt) }}</span>
        </div>
        <el-button class="blog-comment-item__reply" link type="primary" @click="emit('reply', comment)">
          回复
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
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 14px;
  position: relative;
  padding: 16px 0;
}

.blog-comment-item.is-reply {
  grid-template-columns: 34px minmax(0, 1fr);
  padding: 14px 0 0;
}

.blog-comment-item__avatar {
  position: relative;
  z-index: 1;
}

.blog-comment-item__avatar::after {
  content: '';
  position: absolute;
  top: 48px;
  bottom: -16px;
  left: 50%;
  width: 1px;
  background: var(--oa-border);
  transform: translateX(-50%);
}

.blog-comment-item.is-reply .blog-comment-item__avatar::after {
  top: 40px;
}

.blog-comment-item__body {
  min-width: 0;
  padding: 14px 16px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 86%, var(--oa-page-bg));
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.blog-comment-item__body:hover {
  border-color: color-mix(in srgb, var(--oa-primary, #2563eb) 32%, var(--oa-border));
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.blog-comment-item__head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.blog-comment-item__author {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px;
  align-items: baseline;
}

.blog-comment-item__author strong {
  color: var(--oa-text);
  font-size: 15px;
}

.blog-comment-item__author span {
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-comment-item__reply {
  flex: 0 0 auto;
  padding: 0;
  font-size: 13px;
}

.blog-comment-item__content {
  margin-top: 10px;
}

.blog-comment-item__content :deep(p) {
  margin: 0 0 10px;
  color: var(--oa-text);
  font-size: 15px;
  line-height: 1.75;
}

.blog-comment-item__content :deep(p:last-child) {
  margin-bottom: 0;
}

.blog-comment-item__content :deep(pre) {
  margin: 12px 0;
}

.blog-comment-item__content :deep(blockquote) {
  margin: 10px 0;
  padding-left: 12px;
}

.blog-comment-item__replies {
  display: grid;
  gap: 0;
  position: relative;
  margin-top: 14px;
  padding-left: 18px;
}

.blog-comment-item__replies::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 4px;
  left: 0;
  width: 2px;
  background: color-mix(in srgb, var(--oa-primary, #2563eb) 18%, var(--oa-border));
  border-radius: 999px;
}

@media (max-width: 640px) {
  .blog-comment-item {
    grid-template-columns: 36px minmax(0, 1fr);
    gap: 10px;
  }

  .blog-comment-item.is-reply {
    grid-template-columns: 30px minmax(0, 1fr);
  }

  .blog-comment-item__body {
    padding: 12px;
  }

  .blog-comment-item__head {
    align-items: flex-start;
  }
}
</style>
