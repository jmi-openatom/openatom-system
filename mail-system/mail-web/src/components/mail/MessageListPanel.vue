<template>
  <section :class="{ 'mobile-hidden': !!selectedEmail }" aria-label="邮件列表" class="message-list-panel">
    <header class="panel-heading">
      <div><p>{{ activeFolderName }}</p><span>{{ emails.length }} 封邮件</span></div>
      <button aria-label="更多筛选" class="icon-button" type="button"><SlidersHorizontal :size="17" /></button>
    </header>
    <div v-if="mailLoading" aria-label="正在加载邮件" aria-live="polite" class="email-skeletons">
      <div v-for="index in 6" :key="index" class="email-skeleton"><i></i><span></span><b></b></div>
    </div>
    <div v-else-if="errorMessage" class="empty-state" role="alert">
      <CircleAlert :size="30" /><h2>暂时无法读取邮件</h2><p>{{ errorMessage }}</p>
      <button class="secondary-button" type="button" @click="emit('refresh')">重新加载</button>
    </div>
    <div v-else-if="!emails.length" class="empty-state">
      <MailOpen :size="32" /><h2>这里还没有邮件</h2><p>新邮件到达后会显示在这里。</p>
      <button class="secondary-button" type="button" @click="emit('compose')">写第一封邮件</button>
    </div>
    <ol v-else class="email-list">
      <li v-for="email in emails" :key="email.id" v-memo="[email.id, email.keywords.$seen, selectedEmail?.id]">
        <button
          :class="{ selected: selectedEmail?.id === email.id, unread: !email.keywords.$seen }"
          type="button"
          @click="emit('select', email.id)"
        >
          <span class="sender-avatar">{{ senderInitial(email) }}</span>
          <span class="email-copy">
            <span class="email-line"><strong>{{ senderName(email) }}</strong><time>{{ formatListDate(email.receivedAt) }}</time></span>
            <span class="email-subject">{{ email.subject || '（无主题）' }}</span>
            <span class="email-preview">{{ email.preview }}</span>
          </span>
          <i v-if="!email.keywords.$seen" aria-label="未读" class="unread-dot"></i>
        </button>
      </li>
    </ol>
  </section>
</template>

<script lang="ts" setup>
import { CircleAlert, MailOpen, SlidersHorizontal } from 'lucide-vue-next'
import type { EmailSummary } from '../../models'
import { formatListDate, senderInitial, senderName } from '../../stores/mailbox'

defineProps<{
  emails: EmailSummary[]
  selectedEmail: EmailSummary | null
  selectedMailboxId: string | null
  activeFolderName: string
  mailLoading: boolean
  errorMessage: string
}>()
const emit = defineEmits<{
  (e: 'select', id: string): void
  (e: 'refresh'): void
  (e: 'compose'): void
}>()
</script>
