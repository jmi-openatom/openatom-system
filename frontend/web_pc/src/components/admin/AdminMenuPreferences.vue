<template>
  <el-popover :placement="placement" :width="320" popper-class="admin-menu-preferences-popper" trigger="click">
    <template #reference>
      <el-button
        :aria-label="compact ? '自定义侧边栏菜单' : undefined"
        :circle="compact"
        :class="{ 'is-compact': compact }"
        :icon="Setting"
        :title="compact ? '自定义侧边栏菜单' : undefined"
        class="admin-menu-preferences__trigger"
      >
        <span v-if="!compact">自定义菜单</span>
      </el-button>
    </template>

    <div class="admin-menu-preferences">
      <div class="admin-menu-preferences__header">
        <div>
          <strong>侧边栏显示</strong>
          <p>只调整菜单显示，不会改变账号权限。</p>
        </div>
        <el-button link type="primary" @click="showAll">全部显示</el-button>
      </div>

      <el-checkbox-group :model-value="modelValue" class="admin-menu-preferences__list" @change="updateSelection">
        <div
          v-for="(item, index) in orderedItems"
          :key="item.path"
          :class="{ 'is-dragging': draggedPath === item.path }"
          class="admin-menu-preferences__item"
          draggable="true"
          @dragend="draggedPath = ''"
          @dragover.prevent
          @dragstart="draggedPath = item.path"
          @drop="dropAt(index)"
        >
          <el-icon class="admin-menu-preferences__drag" title="拖拽调整顺序">
            <Rank />
          </el-icon>
          <el-checkbox :value="item.path">
            {{ item.label }}
          </el-checkbox>
          <div class="admin-menu-preferences__sort-actions">
            <el-button
              :disabled="index === 0"
              :icon="ArrowUp"
              aria-label="上移"
              link
              title="上移"
              @click.stop="moveItem(index, -1)"
            />
            <el-button
              :disabled="index === orderedItems.length - 1"
              :icon="ArrowDown"
              aria-label="下移"
              link
              title="下移"
              @click.stop="moveItem(index, 1)"
            />
          </div>
        </div>
      </el-checkbox-group>

      <div class="admin-menu-preferences__footer">
        <span>拖拽可排序；数据概览置顶，返回官网置底</span>
        <span>{{ modelValue.length }}/{{ items.length }}</span>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown, ArrowUp, Rank, Setting } from '@element-plus/icons-vue'

interface MenuPreferenceItem {
  path: string
  label: string
}

const props = withDefaults(
  defineProps<{
    items: MenuPreferenceItem[]
    modelValue: string[]
    orderValue: string[]
    compact?: boolean
    placement?: string
  }>(),
  {
    compact: false,
    placement: 'right-end',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  'update:orderValue': [value: string[]]
}>()

const draggedPath = ref('')

const orderedItems = computed(() => {
  const itemMap = new Map(props.items.map((item) => [item.path, item]))
  const ordered = props.orderValue.map((path) => itemMap.get(path)).filter(Boolean) as MenuPreferenceItem[]
  const orderedPaths = new Set(ordered.map((item) => item.path))
  return [...ordered, ...props.items.filter((item) => !orderedPaths.has(item.path))]
})

function updateSelection(value: unknown) {
  if (!Array.isArray(value)) return
  emit(
    'update:modelValue',
    value.map((item) => String(item)),
  )
}

function emitOrder(items: MenuPreferenceItem[]) {
  emit(
    'update:orderValue',
    items.map((item) => item.path),
  )
}

function moveItem(index: number, offset: number) {
  const targetIndex = index + offset
  if (targetIndex < 0 || targetIndex >= orderedItems.value.length) return
  const nextItems = [...orderedItems.value]
  const [movedItem] = nextItems.splice(index, 1)
  nextItems.splice(targetIndex, 0, movedItem)
  emitOrder(nextItems)
}

function dropAt(targetIndex: number) {
  if (!draggedPath.value) return
  const sourceIndex = orderedItems.value.findIndex((item) => item.path === draggedPath.value)
  if (sourceIndex < 0 || sourceIndex === targetIndex) {
    draggedPath.value = ''
    return
  }
  const nextItems = [...orderedItems.value]
  const [movedItem] = nextItems.splice(sourceIndex, 1)
  nextItems.splice(targetIndex, 0, movedItem)
  emitOrder(nextItems)
  draggedPath.value = ''
}

function showAll() {
  emit(
    'update:modelValue',
    props.items.map((item) => item.path),
  )
}
</script>

<style scoped>
.admin-menu-preferences__trigger {
  width: 100%;
}

.admin-menu-preferences__trigger.is-compact {
  width: 40px;
  margin: 0 auto;
}

.admin-menu-preferences {
  display: grid;
  gap: 14px;
  color: var(--oa-text);
}

.admin-menu-preferences__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.admin-menu-preferences__header strong {
  display: block;
  font-size: 16px;
  font-weight: 600;
}

.admin-menu-preferences__header p {
  margin: 4px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.5;
}

.admin-menu-preferences__list {
  display: grid;
  max-height: min(52vh, 420px);
  gap: 4px;
  overflow-y: auto;
  padding: 10px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 10px;
  overscroll-behavior: contain;
}

.admin-menu-preferences__item {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 2px 5px;
  background: var(--oa-elevated-bg);
  border: 1px solid transparent;
  border-radius: 8px;
  transition:
    border-color 0.16s ease,
    opacity 0.16s ease;
}

.admin-menu-preferences__item:hover {
  border-color: var(--oa-border);
}

.admin-menu-preferences__item.is-dragging {
  opacity: 0.45;
}

.admin-menu-preferences__drag {
  color: var(--oa-muted);
  cursor: grab;
}

.admin-menu-preferences__drag:active {
  cursor: grabbing;
}

.admin-menu-preferences__item :deep(.el-checkbox) {
  min-width: 0;
  height: 34px;
  margin-right: 0;
}

.admin-menu-preferences__item :deep(.el-checkbox__label) {
  overflow: hidden;
  color: var(--oa-text-soft);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-menu-preferences__sort-actions {
  display: flex;
  align-items: center;
}

.admin-menu-preferences__sort-actions :deep(.el-button) {
  width: 26px;
  min-height: 26px;
  padding: 0;
}

.admin-menu-preferences__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--oa-muted);
  font-size: 11px;
}
</style>
