<template>
  <article class="comment-thread">
    <CommentRow :comment="comment" :reply-enabled="replyEnabled" root @action="handleAction" />
    <section
      v-if="comment.replies?.length || comment.replyCount"
      class="comment-thread__replies"
      :aria-label="`${comment.userName || '匿名用户'}的回复列表`"
    >
      <CommentRow
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
import { computed, defineComponent, h, type PropType } from 'vue'
import { ChatLineRound, Star } from '@element-plus/icons-vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import { formatDateTime } from '@/utils/format.ts'

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

const CommentRow = defineComponent({
  name: 'CommentRow',
  props: {
    comment: { type: Object as PropType<Record<string, any>>, required: true },
    replyEnabled: { type: Boolean, default: true },
    root: { type: Boolean, default: false },
  },
  emits: ['action'],
  setup(rowProps, { emit: rowEmit }) {
    const action = (name: CommentAction) => rowEmit('action', name, rowProps.comment)
    return () =>
      h('article', { class: ['comment-row', { 'is-root': rowProps.root }] }, [
        h(UserAvatar, {
          name: rowProps.comment.userName || '匿名用户',
          size: rowProps.root ? 44 : 32,
          src: rowProps.comment.userAvatar || '',
        }),
        h('div', { class: 'comment-row__main' }, [
          h('header', { class: 'comment-row__head' }, [
            h('div', { class: 'comment-row__identity' }, [
              h('strong', rowProps.comment.userName || '匿名用户'),
              rowProps.comment.author ? h('span', { class: 'comment-row__badge' }, '作者') : null,
              rowProps.comment.replyToUserName
                ? h('span', { class: 'comment-row__reply-to' }, [
                    '回复 ',
                    h('b', `@${rowProps.comment.replyToUserName}`),
                    rowProps.comment.replyTargetHidden ? ' · 原内容已隐藏' : '',
                  ])
                : rowProps.comment.replyTargetHidden
                  ? h('span', { class: 'comment-row__reply-to' }, '回复内容已隐藏')
                  : null,
              h('time', formatDateTime(rowProps.comment.createdAt)),
            ]),
            h(
              'button',
              {
                type: 'button',
                class: 'comment-row__menu',
                'aria-label': rowProps.comment.own ? '删除评论' : '举报评论',
                onClick: () => action(rowProps.comment.own ? 'delete' : 'report'),
              },
              rowProps.comment.own ? '删除' : '举报',
            ),
          ]),
          h(MarkdownContent, {
            class: 'comment-row__content',
            content: rowProps.comment.content || '',
            mode: 'minimal',
          }),
          h('footer', { class: 'comment-row__actions' }, [
            h(
              'button',
              {
                type: 'button',
                class: ['comment-row__action', { 'is-active': rowProps.comment.liked }],
                'aria-pressed': Boolean(rowProps.comment.liked),
                onClick: () => action('like'),
              },
              [
                h(Star),
                h('span', rowProps.comment.likeCount ? `赞 ${rowProps.comment.likeCount}` : '赞'),
              ],
            ),
            rowProps.replyEnabled
              ? h(
                  'button',
                  { type: 'button', class: 'comment-row__action', onClick: () => action('reply') },
                  [h(ChatLineRound), h('span', '回复')],
                )
              : null,
          ]),
        ]),
      ])
  },
})
</script>

<style scoped>
.comment-thread {
  padding: 20px 0;
  border-bottom: 1px solid var(--oa-border);
}
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
.comment-row__main {
  min-width: 0;
}
.comment-row__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.comment-row__identity {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}
.comment-row__identity strong {
  color: var(--oa-text);
  font-size: 15px;
}
.comment-row__identity time,
.comment-row__reply-to {
  color: var(--oa-muted);
  font-size: 12px;
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
}
.comment-row__menu {
  flex: 0 0 auto;
  min-width: 44px;
  min-height: 44px;
  margin: -10px -10px 0 0;
  padding: 0 10px;
  color: var(--oa-muted);
  background: transparent;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font-size: 12px;
}
.comment-row__menu:hover,
.comment-row__menu:focus-visible {
  color: var(--oa-text);
  background: var(--oa-page-soft-bg);
  outline: 2px solid color-mix(in srgb, var(--oa-primary) 35%, transparent);
  outline-offset: 1px;
}
.comment-row__content {
  margin-top: 6px;
  color: var(--oa-text-soft);
  font-size: 14.5px;
  line-height: 1.7;
}
.comment-row__actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.comment-row__action {
  display: inline-flex;
  min-width: 44px;
  min-height: 44px;
  align-items: center;
  gap: 5px;
  padding: 0 10px;
  color: var(--oa-muted);
  background: transparent;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  transition:
    color 180ms ease,
    background-color 180ms ease;
}
.comment-row__action svg {
  width: 15px;
}
.comment-row__action:hover,
.comment-row__action:focus-visible,
.comment-row__action.is-active {
  color: var(--oa-primary);
  background: color-mix(in srgb, var(--oa-primary) 8%, transparent);
  outline: none;
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
  .comment-row.is-root {
    grid-template-columns: 36px minmax(0, 1fr);
    gap: 10px;
  }
  .comment-row {
    grid-template-columns: 28px minmax(0, 1fr);
    gap: 8px;
  }
  .comment-thread__replies {
    margin-left: 18px;
    padding: 2px 0 2px 10px;
  }
  .comment-thread__more {
    margin-left: 36px;
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
