<template>
  <article class="comment-thread">
    <BlogCommentRow :comment="comment" :reply-enabled="replyEnabled" root @action="handleAction" />
    <section
      v-if="comment.replies?.length || comment.replyCount"
      class="comment-thread__replies"
      :aria-label="`${comment.userName || '匿名用户'}的回复列表`"
    >
      <BlogCommentRow
        v-for="reply in comment.replies || []"
        :key="reply.id"
        :comment="reply"
        :reply-enabled="replyEnabled"
        @action="handleAction"
      />
      <el-button
        v-if="remainingReplies > 0"
        :aria-expanded="false"
        :loading="loadingReplies"
        class="comment-thread__more"
        plain
        @click="emit('load-replies', comment)"
      >
        展开其余 {{ remainingReplies }} 条回复
      </el-button>
    </section>
  </article>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import BlogCommentRow from './BlogCommentRow.vue'

defineOptions({ name: 'BlogCommentItem' })
type CommentAction = 'reply' | 'like' | 'delete' | 'report'

const props = withDefaults(
  defineProps<{
    comment: Record<string, any>
    replyEnabled?: boolean
    loadingReplies?: boolean
  }>(),
  { replyEnabled: true, loadingReplies: false },
)
const emit = defineEmits<{
  reply: [comment: Record<string, any>]
  like: [comment: Record<string, any>]
  delete: [comment: Record<string, any>]
  report: [comment: Record<string, any>]
  'load-replies': [comment: Record<string, any>]
}>()
const remainingReplies = computed(() =>
  Math.max(0, Number(props.comment.replyCount || 0) - Number(props.comment.replies?.length || 0)),
)
function handleAction(action: CommentAction, comment: Record<string, any>) {
  emit(action, comment)
}
</script>

<style scoped>
.comment-thread {
  padding: 20px 0;
  border-bottom: 1px solid var(--oa-border);
}
.comment-thread__replies {
  margin: 4px 0 0 60px;
  padding: 4px 16px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 80%, var(--oa-page-soft-bg));
  border-left: 2px solid var(--oa-border);
  border-radius: 0 12px 12px 0;
}
.comment-thread__more {
  min-height: 44px;
  margin: 4px 0 4px 44px;
}
@media (max-width: 640px) {
  .comment-thread {
    padding: 16px 0;
  }
  .comment-thread__replies {
    margin-left: 18px;
    padding: 2px 0 2px 10px;
  }
  .comment-thread__more {
    margin-left: 36px;
  }
}
</style>
