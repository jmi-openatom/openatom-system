<template>
  <div class="session-panel">
    <div class="panel-title">
      <span>活动会话</span>
      <el-button link :icon="Refresh" @click="emit('refresh')">刷新</el-button>
    </div>
    <div class="session-scroll">
      <div
        v-for="item in sessions"
        :key="item.id"
        class="session-item"
        :class="{ active: currentId === item.id }"
      >
        <button class="session-main" type="button" @click="emit('open', item.id)">
          <strong>{{ item.title }}</strong>
          <small>{{ statusText(item.status) }}</small>
        </button>
        <div class="session-actions">
          <el-tag size="small" :type="statusTypeOf(item.status)">{{ sessionBadge(item.status) }}</el-tag>
          <el-popconfirm
            title="确定删除这个对话吗？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            width="220"
            @confirm="emit('delete', item)"
          >
            <template #reference>
              <el-button
                class="session-delete"
                link
                type="danger"
                :icon="Delete"
                :disabled="deletingSessionId === item.id || streaming"
                :loading="deletingSessionId === item.id"
              />
            </template>
          </el-popconfirm>
        </div>
      </div>
      <div v-if="!sessions.length" class="empty-inline">
        <strong>还没有活动</strong>
        <span>从一个活动想法开始，AI 会帮你往下问。</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Delete, Refresh } from '@element-plus/icons-vue'

defineProps<{
  sessions: any[]
  currentId?: string | number | null
  streaming?: boolean
  deletingSessionId?: string | number | null
}>()

const emit = defineEmits<{
  open: [id: string | number]
  delete: [item: any]
  refresh: []
}>()

function statusText(status: string) {
  return (
    {
      drafting: '需求澄清中',
      requirement_confirmed: '需求已确认',
      plan_generated: '策划案已生成',
      plan_confirmed: '策划案已确认',
      documents_generated: '材料已生成',
    }[status] || status
  )
}

function sessionBadge(status: string) {
  return (
    {
      drafting: '澄清中',
      requirement_confirmed: '待策划',
      plan_generated: '待确认',
      plan_confirmed: '待材料',
      documents_generated: '已完成',
    }[status] || status
  )
}

function statusTypeOf(status: string) {
  return (
    {
      drafting: 'info',
      requirement_confirmed: 'warning',
      plan_generated: 'warning',
      plan_confirmed: 'primary',
      documents_generated: 'success',
    } as const
  )[status] || 'info'
}
</script>

<style scoped>
.session-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.session-scroll {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
}

.session-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.session-item:hover {
  border-color: var(--el-border-color);
}

.session-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}

.session-main {
  min-width: 0;
  padding: 0;
  text-align: left;
  font: inherit;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.session-main strong {
  display: block;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main small {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.empty-inline {
  display: grid;
  gap: 6px;
  padding: 28px 16px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.empty-inline strong {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
</style>