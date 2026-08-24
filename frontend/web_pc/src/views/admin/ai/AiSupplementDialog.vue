<template>
  <el-dialog :model-value="visible" title="补充材料生成信息" width="720px" @update:model-value="emit('update:visible', $event)">
    <div class="supplement-dialog">
      <el-alert
        title="这些信息会写入正式申请材料，未填写的字段将继续保留“待补充”。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-width="128px">
        <el-form-item v-for="field in fields" :key="field.key" :label="field.label">
          <el-date-picker
            v-if="field.control === 'datetimeRange'"
            v-model="field.value"
            class="supplement-date-picker"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
            value-format="YYYY-MM-DD HH:mm"
            format="YYYY 年 MM 月 DD 日 HH:mm"
          />
          <el-input
            v-else
            v-model="field.value"
            :type="field.multiline ? 'textarea' : 'text'"
            :rows="field.multiline ? 3 : undefined"
            :placeholder="field.placeholder"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="emit('submit')">生成正式材料</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
export interface SupplementField {
  key: string
  label: string
  control?: string
  multiline?: boolean
  placeholder?: string
  value: any
}

defineProps<{
  visible: boolean
  fields: SupplementField[]
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()
</script>

<style scoped>
.supplement-dialog {
  display: grid;
  gap: 16px;
}

.supplement-date-picker {
  width: 100%;
}
</style>