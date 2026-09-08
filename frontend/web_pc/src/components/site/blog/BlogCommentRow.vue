<template>
  <article class="comment-row" :class="{ 'is-root': root }">
    <UserAvatar
      class="comment-row__avatar"
      :name="comment.userName || '匿名用户'"
      :size="root ? 44 : 32"
      :src="comment.userAvatar || ''"
    />
    <div class="comment-row__main">
      <header class="comment-row__head">
        <div class="comment-row__identity">
          <div class="comment-row__name-line">
            <strong>{{ comment.userName || '匿名用户' }}</strong>
            <span v-if="comment.author" class="comment-row__badge">作者</span>
          </div>
          <div class="comment-row__meta">
            <time>{{ formatDateTime(comment.createdAt) }}</time>
            <span v-if="comment.replyToUserName" class="comment-row__reply-to">
              回复 <b>@{{ comment.replyToUserName }}</b>
              <template v-if="comment.replyTargetHidden"> · 原内容已隐藏</template>
            </span>
            <span v-else-if="comment.replyTargetHidden" class="comment-row__reply-to">
              回复内容已隐藏
            </span>
          </div>
        </div>
        <button
          type="button"
          class="comment-row__menu"
          :class="{ 'is-danger': comment.own }"
          :aria-label="comment.own ? '删除评论' : '举报评论'"
          @click="action(comment.own ? 'delete' : 'report')"
        >
          {{ comment.own ? '删除' : '举报' }}
        </button>
      </header>

      <MarkdownContent
        class="comment-row__content"
        :content="comment.content || ''"
        mode="minimal"
      />

      <footer class="comment-row__actions">
        <button
          type="button"
          class="comment-row__action"
          :class="{ 'is-active': comment.liked }"
          :aria-pressed="Boolean(comment.liked)"
          @click="action('like')"
        >
          <Star />
          <span>{{ comment.likeCount ? `赞 ${comment.likeCount}` : '赞' }}</span>
        </button>
        <button
          v-if="replyEnabled"
          type="button"
          class="comment-row__action"
          @click="action('reply')"
        >
          <ChatLineRound />
          <span>回复</span>
        </button>
      </footer>
    </div>
  </article>
</template>

<script lang="ts" setup>
import { ChatLineRound, Star } from '@element-plus/icons-vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import { formatDateTime } from '@/utils/format.ts'

type CommentAction = 'reply' | 'like' | 'delete' | 'report'

const props = withDefaults(
  defineProps<{
    comment: Record<string, any>
    replyEnabled?: boolean
    root?: boolean
  }>(),
  { replyEnabled: true, root: false },
)

const emit = defineEmits<{
  action: [action: CommentAction, comment: Record<string, any>]
}>()

function action(name: CommentAction) {
  emit('action', name, props.comment)
}
</script>

<style scoped>
.comment-row {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
  padding: 12px 0;
}

.comment-row.is-root {
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 16px;
  padding-top: 0;
}

.comment-row__avatar {
  flex: none;
}

.comment-row__main,
.comment-row__identity {
  min-width: 0;
}

.comment-row__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.comment-row__name-line,
.comment-row__meta,
.comment-row__actions,
.comment-row__action {
  display: flex;
  align-items: center;
}

.comment-row__name-line {
  min-height: 22px;
  flex-wrap: wrap;
  gap: 7px;
}

.comment-row__name-line strong {
  color: var(--oa-text);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.45;
}

.comment-row__meta {
  min-height: 20px;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 2px;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.5;
}

.comment-row__meta time {
  font-variant-numeric: tabular-nums;
}

.comment-row__reply-to b {
  color: var(--oa-text-soft);
  font-weight: 600;
}

.comment-row__badge {
  padding: 2px 6px;
  color: var(--oa-primary);
  background: color-mix(in srgb, var(--oa-primary) 10%, transparent);
  border-radius: 999px;
  font-size: 11px;
  line-height: 1.4;
}

.comment-row__menu,
.comment-row__action {
  min-height: 44px;
  padding: 0 10px;
  color: var(--oa-muted);
  background: transparent;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  touch-action: manipulation;
}

.comment-row__menu {
  flex: 0 0 auto;
  min-width: 44px;
  margin: -8px -8px 0 0;
}

.comment-row__menu.is-danger:hover,
.comment-row__menu.is-danger:focus-visible {
  color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 8%, transparent);
}

.comment-row__menu:hover,
.comment-row__menu:focus-visible {
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  outline: 2px solid color-mix(in srgb, var(--oa-primary) 35%, transparent);
  outline-offset: 1px;
}

.comment-row__content {
  margin-top: 8px;
  color: var(--oa-text-soft);
  font-size: 15px;
  line-height: 1.7;
}

.comment-row__content :deep(.paragraph-node) {
  margin-bottom: 0.4em;
}

.comment-row__content :deep(.paragraph-node:last-child) {
  margin-bottom: 0;
}

.comment-row__actions {
  gap: 8px;
  margin-top: 6px;
}

.comment-row__action {
  min-width: 44px;
  gap: 5px;
  transition:
    color 180ms ease,
    background-color 180ms ease;
}

.comment-row__action :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
}

.comment-row__action:hover,
.comment-row__action:focus-visible,
.comment-row__action.is-active {
  color: var(--oa-primary);
  background: color-mix(in srgb, var(--oa-primary) 8%, transparent);
  outline: 2px solid transparent;
}

@media (max-width: 640px) {
  .comment-row.is-root {
    grid-template-columns: 36px minmax(0, 1fr);
    gap: 10px;
  }

  .comment-row {
    grid-template-columns: 28px minmax(0, 1fr);
    gap: 9px;
  }

  .comment-row__head {
    gap: 6px;
  }

  .comment-row__content {
    font-size: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .comment-row__action {
    transition: none;
  }
}
</style>
