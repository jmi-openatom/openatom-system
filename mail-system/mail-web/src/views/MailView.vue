<template>
  <div class="mail-shell">
    <AppHeader
      :session="session"
      :search="search"
      :mail-loading="mailLoading"
      :is-admin="isAdmin"
      @update:search="onSearchInput"
      @refresh="onRefresh"
      @logout="onLogout"
      @admin="emit('admin')"
    />
    <div class="mail-workspace">
      <FolderSidebar
        :folders="visibleFolders(mailContext)"
        :selected-mailbox-id="selectedMailboxId"
        @select="onSelectFolder"
        @compose="onOpenCompose"
      />
      <MessageListPanel
        :emails="emails"
        :selected-email="selectedEmail"
        :selected-mailbox-id="selectedMailboxId"
        :active-folder-name="activeFolderName(mailContext)"
        :mail-loading="mailLoading"
        :error-message="errorMessage"
        @select="onSelectEmail"
        @refresh="onRefresh"
        @compose="onOpenCompose"
      />
      <MessageReader
        :selected-email="selectedEmail"
        :detail-loading="detailLoading"
        :action-busy="actionBusy"
        :active-folder-name="activeFolderName"
        :downloading-attachment-id="downloadingAttachmentId"
        :attachment-download-error="attachmentDownloadError"
        :selected-body="selectedBody"
        @close="selectedEmail = null"
        @reply="onReply"
        @archive="onArchive"
        @trash="onTrash"
        @toggle-seen="onToggleSeen"
        @refresh="onRefresh"
        @destroy="onDestroy"
        @download="onDownload"
      />
    </div>
    <nav aria-label="移动端邮箱导航" class="mobile-bottom-nav">
      <button class="active" type="button" @click="selectedEmail = null"><Inbox :size="20" /><span>邮件</span></button>
      <button type="button" @click="focusSearch"><Search :size="20" /><span>搜索</span></button>
      <button type="button" @click="onOpenCompose"><SquarePen :size="20" /><span>写信</span></button>
    </nav>
    <ComposeDialog :mail-context="mailContext" :session="session" @toast="showToast" />
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { Inbox, Search, SquarePen } from 'lucide-vue-next'
import type { EmailSummary, MailContext, SessionView, UploadedAttachment } from '../models'
import {
  activeFolderName,
  senderAddress,
  useMailboxStore,
  visibleFolders,
} from '../stores/mailbox'
import { useUiStore } from '../stores/ui'
import { useComposeStore } from '../stores/compose'
import AppHeader from '../components/layout/AppHeader.vue'
import FolderSidebar from '../components/layout/FolderSidebar.vue'
import MessageListPanel from '../components/mail/MessageListPanel.vue'
import MessageReader from '../components/mail/MessageReader.vue'
import ComposeDialog from '../components/mail/ComposeDialog.vue'
import { downloadAttachment } from '../api'

const props = defineProps<{ session: SessionView; mailContext: MailContext; isAdmin: boolean }>()

const {
  selectedMailboxId, emails, selectedEmail, search, mailLoading, detailLoading, actionBusy, errorMessage,
  selectedBody,
  loadMailbox, loadEmails, selectFolder, selectEmail, scheduleSearch, archiveSelected, deleteSelected,
  deleteForever, toggleSelectedSeen,
} = useMailboxStore()
const { showToast } = useUiStore()
const { openCompose } = useComposeStore()

const downloadingAttachmentId = ref('')
const attachmentDownloadError = ref('')

onMounted(() => void loadMailbox(props.mailContext))

function onSearchInput(value: string) {
  search.value = value
  scheduleSearch(props.mailContext)
}
function onRefresh() {
  void loadEmails(props.mailContext)
}
function onSelectFolder(id: string) {
  void selectFolder(id, props.mailContext)
}
function onSelectEmail(id: string) {
  void selectEmail(id, props.mailContext)
}
function onOpenCompose() {
  openCompose()
}
function onReply(email: EmailSummary) {
  openCompose({ address: senderAddress(email), subject: email.subject })
}
function onArchive() {
  void archiveSelected(props.mailContext, showToast)
}
function onTrash() {
  void deleteSelected(props.mailContext, showToast)
}
function onDestroy() {
  void deleteForever(props.mailContext, showToast)
}
function onToggleSeen() {
  void toggleSelectedSeen(props.mailContext)
}
function focusSearch() {
  document.querySelector<HTMLInputElement>('.search-field input')?.focus()
}
async function onDownload(attachment: UploadedAttachment) {
  if (downloadingAttachmentId.value) return
  downloadingAttachmentId.value = attachment.blobId
  attachmentDownloadError.value = ''
  try {
    await downloadAttachment(attachment.blobId, attachment.name)
  } catch (error) {
    attachmentDownloadError.value = error instanceof Error ? error.message : '下载附件失败。'
  } finally {
    downloadingAttachmentId.value = ''
  }
}
function onLogout() {
  // handled by session store via App root; emit to parent
  emit('logout')
}
const emit = defineEmits<{ (e: 'logout'): void; (e: 'admin'): void }>()
</script>