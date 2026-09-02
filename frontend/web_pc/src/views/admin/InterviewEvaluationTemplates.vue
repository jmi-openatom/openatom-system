<template>
  <ViewPage class="template-page">
    <ViewToolbar>
      <div class="toolbar__filters">
        <el-select
          v-model="campaignId"
          filterable
          placeholder="选择招新计划"
          style="width: 280px"
          @change="fetchVersions"
        >
          <el-option
            v-for="campaign in campaigns"
            :key="campaign.id"
            :label="campaign.name"
            :value="campaign.id"
          />
        </el-select>
        <el-button :loading="loading" @click="fetchVersions">刷新</el-button>
      </div>
      <el-button type="primary" :disabled="!campaignId" @click="openEditor">新建模板版本</el-button>
    </ViewToolbar>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="每次保存都会生成新版本；历史评价继续使用提交时的模板版本。"
    />
    <el-table v-loading="loading" :data="versions" class="admin-table" style="margin-top: 16px">
      <el-table-column prop="name" label="模板名称" min-width="220" />
      <el-table-column prop="version" label="版本" width="90"
        ><template #default="{ row }">v{{ row.version }}</template></el-table-column
      >
      <el-table-column label="评价维度" min-width="280">
        <template #default="{ row }"
          ><el-tag
            v-for="dimension in row.schema?.dimensions || []"
            :key="dimension.key"
            size="small"
            class="dimension-tag"
            >{{ dimension.label }}</el-tag
          ></template
        >
      </el-table-column>
      <el-table-column label="状态" width="110"
        ><template #default="{ row }"
          ><el-tag :type="row.status === 'active' ? 'success' : 'info'">{{
            row.status === 'active' ? '使用中' : '历史版本'
          }}</el-tag></template
        ></el-table-column
      >
    </el-table>
    <el-empty
      v-if="!loading && campaignId && !versions.length"
      description="该招新计划尚未配置专属模板，将自动使用系统默认模板"
    />

    <el-dialog v-model="visible" title="新建评价模板版本" width="820px">
      <el-form label-width="90px">
        <el-form-item label="模板名称"
          ><el-input v-model="form.name" placeholder="如：2026 秋招技术面试评价表"
        /></el-form-item>
      </el-form>
      <div class="dimension-heading">
        <strong>评价维度</strong
        ><el-button type="primary" plain @click="addDimension">添加维度</el-button>
      </div>
      <div class="dimension-editor">
        <div
          v-for="(dimension, index) in form.dimensions"
          :key="dimension.uid"
          class="dimension-row"
        >
          <el-input v-model="dimension.label" placeholder="维度名称" @blur="fillKey(dimension)" />
          <el-input v-model="dimension.key" placeholder="字段 Key" />
          <el-input v-model="dimension.description" placeholder="评分说明" />
          <el-switch v-model="dimension.required" active-text="必评" />
          <el-button link type="danger" @click="form.dimensions.splice(index, 1)">删除</el-button>
        </div>
      </div>
      <el-divider content-position="left">评分锚点</el-divider>
      <div class="anchor-grid">
        <el-input v-for="score in 5" :key="score" v-model="form.anchors[String(score)]"
          ><template #prepend>{{ score }} 分</template></el-input
        >
      </div>
      <template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" :loading="submitting" @click="save"
          >保存并启用</el-button
        ></template
      >
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { clubApi, interviewEvaluationTemplateApi } from '@/api'
import { ElMessage } from 'element-plus/es/components/message/index'
import { onMounted, reactive, ref } from 'vue'

type Dimension = {
  uid: string
  key: string
  label: string
  description: string
  required: boolean
  weight: number
}
const campaigns = ref<any[]>([]),
  versions = ref<any[]>([])
const campaignId = ref<number | null>(null),
  loading = ref(false),
  visible = ref(false),
  submitting = ref(false)
const form = reactive<{ name: string; dimensions: Dimension[]; anchors: Record<string, string> }>({
  name: '',
  dimensions: [],
  anchors: {
    '1': '明显不足',
    '2': '低于要求',
    '3': '达到要求',
    '4': '表现良好',
    '5': '明显超出预期',
  },
})
const defaults = [
  ['motivation', '加入动机', '对社团的了解与真实加入意愿'],
  ['technical', '技术基础', '报名方向的基础知识与实践能力'],
  ['problemSolving', '问题分析', '拆解问题并形成解决方案的能力'],
  ['communication', '沟通表达', '理解问题和清晰表达的能力'],
  ['collaboration', '团队协作', '合作意识、责任感与接受反馈能力'],
  ['learning', '学习潜力', '自主学习、复盘和持续成长能力'],
  ['openSourceValues', '开源价值观', '分享、协作与尊重社区规则'],
  ['overallFit', '综合匹配度', '与社团文化和实际工作的整体匹配程度'],
]
function dimension(key = '', label = '', description = ''): Dimension {
  return { uid: crypto.randomUUID(), key, label, description, required: true, weight: 1 }
}
async function loadCampaigns() {
  const clubs = await clubApi.list()
  const groups = await Promise.all(
    (Array.isArray(clubs) ? clubs : []).map((club: any) => clubApi.campaigns(club.id)),
  )
  campaigns.value = groups.flatMap((list: any) => (Array.isArray(list) ? list : []))
  campaignId.value = campaigns.value[0]?.id ?? null
  if (campaignId.value) await fetchVersions()
}
async function fetchVersions() {
  if (!campaignId.value) return
  loading.value = true
  try {
    const data = await interviewEvaluationTemplateApi.list(campaignId.value)
    versions.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}
function openEditor() {
  const active = versions.value.find((v) => v.status === 'active')
  form.name = active?.name || '面试评价模板'
  const source =
    active?.schema?.dimensions ||
    defaults.map(([key, label, description]) => ({
      key,
      label,
      description,
      required: true,
      weight: 1,
    }))
  form.dimensions = source.map((d: any) => dimension(d.key, d.label, d.description))
  form.anchors = { ...form.anchors, ...(active?.schema?.scoreAnchors || {}) }
  visible.value = true
}
function addDimension() {
  form.dimensions.push(dimension())
}
function fillKey(item: Dimension) {
  if (!item.key && item.label) item.key = `custom_${Date.now().toString(36)}`
}
async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写模板名称')
  if (!form.dimensions.length) return ElMessage.warning('至少保留一个评价维度')
  const invalid = form.dimensions.find((d) => !d.label.trim() || !d.key.trim())
  if (invalid) return ElMessage.warning('请补全维度名称和字段 Key')
  if (new Set(form.dimensions.map((d) => d.key)).size !== form.dimensions.length)
    return ElMessage.warning('维度字段 Key 不能重复')
  submitting.value = true
  try {
    await interviewEvaluationTemplateApi.save({
      campaignId: campaignId.value,
      name: form.name,
      schema: { dimensions: form.dimensions.map(({ uid, ...d }) => d), scoreAnchors: form.anchors },
    })
    ElMessage.success('新模板版本已启用')
    visible.value = false
    await fetchVersions()
  } finally {
    submitting.value = false
  }
}
onMounted(loadCampaigns)
</script>

<style scoped>
.dimension-tag {
  margin: 3px;
}
.dimension-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 10px;
}
.dimension-editor {
  display: grid;
  gap: 10px;
  max-height: 400px;
  overflow: auto;
}
.dimension-row {
  display: grid;
  grid-template-columns: 140px 140px 1fr 80px 48px;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
}
.anchor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
@media (max-width: 760px) {
  .dimension-row {
    grid-template-columns: 1fr;
  }
  .anchor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
