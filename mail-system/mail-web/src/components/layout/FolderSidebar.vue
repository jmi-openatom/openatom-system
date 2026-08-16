<template>
  <aside aria-label="邮箱文件夹" class="folder-sidebar">
    <button class="compose-button" type="button" @click="emit('compose')">
      <SquarePen :size="18"/> 写邮件
    </button>
    <nav class="folder-nav">
      <button
        v-for="folder in folders"
        :key="folder.id"
        :class="{ active: selectedMailboxId === folder.id }"
        type="button"
        @click="emit('select', folder.id)"
      >
        <component :is="icon(folder.role)" :size="18" />
        <span>{{ folderName(folder) }}</span>
        <b v-if="folder.unreadEmails">{{ compactNumber(folder.unreadEmails) }}</b>
      </button>
    </nav>
    <div class="storage-card">
      <div><HardDrive :size="16" /><span>邮箱空间</span><small>2 GB</small></div>
      <span class="storage-track"><i style="width: 6%"></i></span>
      <p>邮件正文与附件保存在自建服务器</p>
    </div>
  </aside>
</template>

<script lang="ts" setup>
import { HardDrive, Inbox, SquarePen } from 'lucide-vue-next'
import { Archive, FilePenLine, Mail, Send, ShieldCheck, Trash2 } from 'lucide-vue-next'
import type { Mailbox } from '../../models'
import { compactNumber, folderName } from '../../stores/mailbox'

defineProps<{
  folders: Mailbox[]
  selectedMailboxId: string | null
}>()
const emit = defineEmits<{
  (e: 'select', id: string): void
  (e: 'compose'): void
}>()

function icon(role: string | null) {
  return ({ inbox: Inbox, sent: Send, drafts: FilePenLine, trash: Trash2, junk: ShieldCheck, archive: Archive } as const)[role ?? ''] ?? Mail
}
</script>