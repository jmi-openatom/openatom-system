<template>
  <div class="page-container catalog-page">
    <header class="page-title-row"><div class="page-heading"><p class="eyebrow">INBOX</p><h1>消息中心</h1><p>审核反馈、积分变化和路线进度都会在这里留痕。</p></div><el-switch v-model="unreadOnly" active-text="只看未读" @change="load" /></header>
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-result v-else-if="error" icon="error" title="消息加载失败" :sub-title="error" />
    <section v-else-if="items.length" class="notification-list">
      <button v-for="item in items" :key="Number(item.id)" class="notification-row" :class="{ 'is-unread': !item.readAt }" type="button" @click="open(item)">
        <span class="notification-dot" /><div><div><strong>{{ item.title }}</strong><time>{{ formatDate(String(item.createdAt)) }}</time></div><p>{{ item.content }}</p></div><span aria-hidden="true">→</span>
      </button>
    </section>
    <div v-else class="content-card empty-panel"><strong>{{ unreadOnly ? '没有未读消息' : '消息中心还是空的' }}</strong><p>任务状态发生变化时会在这里通知你。</p></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getErrorMessage } from '@/api/http'
import { getNotifications, markNotificationRead } from '@/api/quest'
const router = useRouter()
const items = ref<Record<string, any>[]>([])
const loading = ref(true)
const error = ref('')
const unreadOnly = ref(false)
function formatDate(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
async function load() { loading.value = true; try { items.value = await getNotifications(unreadOnly.value) } catch (reason) { error.value = getErrorMessage(reason) } finally { loading.value = false } }
async function open(item: Record<string, any>) { if (!item.readAt) await markNotificationRead(Number(item.id)); if (item.actionUrl) await router.push(String(item.actionUrl)); else await load() }
onMounted(load)
</script>
