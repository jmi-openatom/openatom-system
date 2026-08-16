<template>
  <main id="mail-main" :class="{ 'mobile-visible': !!selectedEmail }" class="reader-panel" tabindex="-1">
    <div v-if="detailLoading" class="reader-loading"><span class="spinner"></span><p>正在打开邮件…</p></div>
    <article v-else-if="selectedEmail" class="message-reader">
      <header class="reader-toolbar">
        <button aria-label="返回邮件列表" class="back-button" type="button" @click="emit('close')">
          <ArrowLeft :size="19"/>
        </button>
        <div class="toolbar-group">
          <button aria-label="回复" class="icon-button" type="button" @click="emit('reply', selectedEmail)">
            <Reply :size="18"/>
          </button>
          <button aria-label="归档" class="icon-button" type="button" :disabled="actionBusy" @click="emit('archive')">
            <Archive :size="18"/>
          </button>
          <button aria-label="移到废纸篓" class="icon-button danger" type="button" :disabled="actionBusy" @click="emit('trash')">
            <Trash2 :size="18"/>
          </button>
          <div class="more-menu-wrap">
            <button aria-label="更多操作" :aria-expanded="moreMenuOpen" class="icon-button" type="button" @click="moreMenuOpen = !moreMenuOpen">
              <MoreHorizontal :size="19"/>
            </button>
            <div v-if="moreMenuOpen" class="more-menu" role="menu">
              <button type="button" role="menuitem" @click="emit('toggleSeen'); moreMenuOpen = false">
                <span>{{ selectedEmail.keywords?.$seen ? '标记为未读' : '标记为已读' }}</span>
              </button>
              <button type="button" role="menuitem" @click="emit('refresh'); moreMenuOpen = false">
                <span>刷新邮件列表</span>
              </button>
              <button type="button" role="menuitem" class="more-menu__danger" @click="emit('destroy'); moreMenuOpen = false">
                <span>彻底删除</span>
              </button>
            </div>
          </div>
        </div>
      </header>
      <div class="reader-content">
        <p class="message-kicker">{{ activeFolderName }}</p>
        <h1>{{ selectedEmail.subject || '（无主题）' }}</h1>
        <div class="message-meta">
          <span class="sender-avatar large">{{ senderInitial(selectedEmail) }}</span>
          <div><strong>{{ senderName(selectedEmail) }}</strong><small>{{ senderAddress(selectedEmail) }} → {{ recipientText(selectedEmail) }}</small></div>
          <time>{{ formatFullDate(selectedEmail.receivedAt) }}</time>
        </div>
        <div class="privacy-notice"><ShieldCheck :size="16" /><span>为保护隐私，HTML 与远程图片默认不加载；当前以安全纯文本显示。</span></div>
        <div class="message-body">{{ selectedBody }}</div>
        <section v-if="selectedEmail.attachments?.length" aria-labelledby="reader-attachments-title" class="reader-attachments">
          <h2 id="reader-attachments-title">附件（仅下载，不在线预览）</h2>
          <button
            v-for="attachment in selectedEmail.attachments"
            :key="attachment.blobId"
            :disabled="downloadingAttachmentId === attachment.blobId"
            type="button"
            @click="emit('download', attachment)"
          >
            <span v-if="downloadingAttachmentId === attachment.blobId" aria-hidden="true" class="spinner small"></span>
            <Paperclip v-else :size="17" />
            <span><strong>{{ attachment.name || '未命名附件' }}</strong><small>{{ formatBytes(attachment.size) }}</small></span>
          </button>
          <div v-if="attachmentDownloadError" class="attachment-error" role="alert">
            <CircleAlert :size="16" /> {{ attachmentDownloadError }}
          </div>
        </section>
        <button class="reply-button" type="button" @click="emit('reply', selectedEmail)">
          <Reply :size="17"/> 回复
        </button>
      </div>
    </article>
    <div v-else class="reader-empty">
      <div class="reader-empty-icon"><Mail :size="34" /></div>
      <h2>选择一封邮件开始阅读</h2>
      <p>邮件内容将以安全模式显示，远程图片默认关闭。</p>
      <span><Command :size="15" /> 使用 ↑ ↓ 浏览，Enter 打开</span>
    </div>
  </main>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import {
  Archive, ArrowLeft, CircleAlert, Command, Mail, MoreHorizontal, Paperclip, Reply, ShieldCheck, Trash2,
} from 'lucide-vue-next'
import type { EmailSummary, UploadedAttachment } from '../../models'
import { formatBytes, formatFullDate, recipientText, senderAddress, senderInitial, senderName } from '../../stores/mailbox'

defineProps<{
  selectedEmail: EmailSummary | null
  detailLoading: boolean
  actionBusy: boolean
  activeFolderName: string
  downloadingAttachmentId: string
  attachmentDownloadError: string
  selectedBody: string
}>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'reply', email: EmailSummary): void
  (e: 'archive'): void
  (e: 'trash'): void
  (e: 'toggleSeen'): void
  (e: 'refresh'): void
  (e: 'destroy'): void
  (e: 'download', attachment: UploadedAttachment): void
}>()
const moreMenuOpen = ref(false)
</script>
