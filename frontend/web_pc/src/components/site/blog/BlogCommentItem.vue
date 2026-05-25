<template>
  <article class="blog-comment-item" :class="{ 'is-reply': depth > 0 }">
    <UserAvatar :name="comment.userName || '匿名用户'" :size="depth > 0 ? 34 : 42" :src="comment.userAvatar || ''" />
    <div class="blog-comment-item__body">
      <header>
        <strong>{{ comment.userName || '匿名用户' }}</strong>
        <span>{{ formatDateTime(comment.createdAt) }}</span>
        <el-button link type="primary" @click="emit('reply', comment)">回复</el-button>
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
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--oa-border);
}

.blog-comment-item.is-reply {
  padding: 12px 0 0;
  border-top-style: dashed;
}

.blog-comment-item__body {
  min-width: 0;
}

.blog-comment-item header {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.blog-comment-item header strong {
  color: var(--oa-text);
}

.blog-comment-item header span {
  color: var(--oa-muted);
  font-size: 13px;
}

.blog-comment-item__content {
  margin-top: 8px;
}

.blog-comment-item__content :deep(p) {
  margin: 0 0 8px;
  color: var(--oa-text);
  font-size: 15px;
  line-height: 1.7;
}

.blog-comment-item__content :deep(pre) {
  margin: 10px 0;
}

.blog-comment-item__replies {
  display: grid;
  gap: 0;
  margin-top: 10px;
  padding-left: 12px;
  border-left: 2px solid var(--oa-border);
}

@media (max-width: 640px) {
  .blog-comment-item {
    grid-template-columns: 36px minmax(0, 1fr);
  }
}
</style>
