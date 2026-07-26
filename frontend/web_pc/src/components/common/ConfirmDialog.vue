<template>
  <AppDialog v-model="open" :title="title" width="min(440px, calc(100vw - 24px))">
    <p class="confirm-dialog__message">{{ message }}</p>
    <template #footer>
      <div class="page-actions">
        <el-button :disabled="loading" @click="open = false">取消</el-button>
        <el-button :loading="loading" :type="danger ? 'danger' : 'primary'" @click="confirm">
          {{ confirmText }}
        </el-button>
      </div>
    </template>
  </AppDialog>
</template>

<script setup lang="ts">
import AppDialog from '@/components/ElDialog.vue'

withDefaults(
  defineProps<{
    title: string
    message: string
    confirmText?: string
    danger?: boolean
    loading?: boolean
  }>(),
  {
    confirmText: '确认',
    danger: false,
    loading: false,
  },
)

const emit = defineEmits<{
  confirm: []
}>()

const open = defineModel<boolean>({ default: false })

function confirm() {
  emit('confirm')
}
</script>

<style scoped>
.confirm-dialog__message {
  margin: 0;
  color: var(--color-text-regular);
  line-height: var(--line-height-body);
}
</style>
