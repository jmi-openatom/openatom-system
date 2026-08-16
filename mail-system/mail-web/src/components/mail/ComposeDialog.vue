<template>
  <div v-if="open" class="modal-backdrop" role="presentation" @mousedown.self="requestCloseCompose">
    <section ref="dialog" aria-labelledby="compose-title" aria-modal="true" class="compose-dialog" role="dialog">
      <header>
        <div><span class="status-dot"></span><h2 id="compose-title">新邮件</h2></div>
        <button aria-label="关闭写信窗口" class="icon-button" type="button" @click="requestCloseCompose"><X :size="19" /></button>
      </header>
      <form @submit.prevent="onSubmit">
        <label><span>收件人</span><input v-model="compose.to" autocomplete="off" inputmode="email" placeholder="name@example.com，多个地址用逗号分隔" required type="text" /></label>
        <label><span>主题</span><input v-model="compose.subject" maxlength="200" placeholder="邮件主题" type="text" /></label>
        <label class="body-field"><span class="sr-only">邮件正文</span><RichTextEditor v-model="compose.body" /></label>
        <div v-if="attachments.length" aria-label="待发送附件" class="compose-attachment-list">
          <div v-for="attachment in attachments" :key="attachment.blobId">
            <Paperclip :size="16" />
            <span><strong>{{ attachment.name }}</strong><small>{{ formatBytes(attachment.size) }}</small></span>
            <button :aria-label="'移除附件 ' + attachment.name" type="button" @click="removeAttachment(attachment)"><X :size="16" /></button>
          </div>
        </div>
        <div v-if="error" class="form-error" role="alert"><CircleAlert :size="16" /> {{ error }}</div>
        <footer>
          <div class="attachment-actions">
            <input ref="attachmentInput" class="sr-only" multiple type="file" @change="onAttachments" />
            <button :disabled="sending || uploadingAttachment" class="attachment-button" type="button" @click="attachmentInput?.click()">
              <span v-if="uploadingAttachment" class="spinner small"></span><Paperclip v-else :size="16" />
              {{ uploadingAttachment ? '正在上传' : '添加附件' }}
            </button>
            <small>最多 10 个，总计 20 MiB</small>
          </div>
          <button :disabled="sending || uploadingAttachment" class="primary-button" type="submit">
            <span v-if="sending" class="spinner small"></span><Send v-else :size="17" />
            {{ sending ? '正在发送' : '发送邮件' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { CircleAlert, Paperclip, Send, X } from 'lucide-vue-next'
import RichTextEditor from '../common/RichTextEditor.vue'
import type { MailContext, SessionView } from '../../models'
import { formatBytes } from '../../stores/mailbox'
import { useComposeStore } from '../../stores/compose'

const props = defineProps<{
  mailContext: MailContext
  session: SessionView
}>()
const emit = defineEmits<{ (e: 'toast', msg: string): void }>()

const {
  open, dialog, attachmentInput, sending, uploadingAttachment, error, compose, attachments,
  requestCloseCompose, closeCompose, submitCompose, handleAttachmentSelection, removeAttachment,
} = useComposeStore()

function onSubmit() {
  void submitCompose(props.mailContext, props.session, (msg) => emit('toast', msg))
}
function onAttachments(event: Event) {
  void handleAttachmentSelection(event)
}
void closeCompose
</script>