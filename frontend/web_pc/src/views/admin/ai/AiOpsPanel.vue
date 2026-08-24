<template>
  <div class="ops-panel">
    <section class="ops-section">
      <div class="panel-title">
        <span>模板状态</span>
        <el-button link @click="emit('manageTemplates')">管理</el-button>
      </div>
      <div class="template-list">
        <div v-for="type in documentTypes" :key="type.value" class="template-row">
          <div>
            <strong>{{ type.label }}</strong>
            <small>{{ templateByType(type.value)?.templateName || '请上传 docx 模板' }}</small>
          </div>
          <el-tag :type="templateByType(type.value) ? 'success' : 'warning'" size="small">
            {{ templateByType(type.value) ? '就绪' : '缺失' }}
          </el-tag>
        </div>
      </div>
    </section>

    <section class="ops-section">
      <div class="panel-title">
        <span>生成材料</span>
        <el-tag size="small" type="info">{{ documents?.length || 0 }} 份</el-tag>
      </div>
      <el-button
        class="full-btn"
        type="primary"
        :disabled="streaming || !canGenerateDocuments"
        :loading="generatingDocs"
        @click="emit('generateDocuments')"
      >
        生成五份材料
      </el-button>
      <div class="doc-list">
        <div v-for="doc in documents || []" :key="doc.id" class="doc-item">
          <span>{{ doc.fileName }}</span>
          <el-button link type="primary" @click="emit('download', doc)">下载</el-button>
        </div>
        <div v-if="!documents?.length" class="empty-mini">策划案确认后生成正式材料。</div>
      </div>
    </section>

    <section class="ops-section">
      <div class="panel-title">
        <span>AI 配置</span>
        <el-tag size="small" :type="aiSettings.hasApiKey ? 'success' : 'warning'">
          {{ aiSettings.hasApiKey ? '已配置' : '未配置' }}
        </el-tag>
      </div>
      <p class="config-line">{{ aiSettings.model || 'deepseek-chat' }}</p>
      <p class="config-line">{{ aiSettings.baseUrl || 'https://api.deepseek.com' }}</p>
      <el-button class="full-btn" @click="emit('manageSettings')">打开 AI 配置</el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  documentTypes: { value: string; label: string }[]
  templates: any[]
  documents?: any[]
  aiSettings: Record<string, any>
  streaming?: boolean
  canGenerateDocuments?: boolean
  generatingDocs?: boolean
}>()

const emit = defineEmits<{
  manageTemplates: []
  manageSettings: []
  generateDocuments: []
  download: [doc: any]
}>()

function templateByType(type: string) {
  return (props.templates || []).find((item) => item.templateType === type)
}
</script>

<style scoped>
.ops-panel {
  display: grid;
  gap: 16px;
  align-content: start;
}

.ops-section {
  display: grid;
  gap: 10px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.template-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
}

.template-row strong {
  display: block;
  font-size: 13px;
  line-height: 1.4;
}

.template-row small {
  display: block;
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.full-btn {
  width: 100%;
}

.doc-list {
  display: grid;
  gap: 6px;
}

.doc-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  font-size: 13px;
}

.doc-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-mini {
  padding: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  text-align: center;
}

.config-line {
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>