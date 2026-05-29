<template>
  <ViewPage class="open-platform-page" :loading="loading">
    <section class="open-platform-hero">
      <div class="container open-platform-hero__inner">
        <div class="open-platform-heading">
          <span>Data Open Platform</span>
          <h1>数据开放平台</h1>
          <p>提交接入申请，审核通过后使用平台密钥调用开放登录接口。</p>
        </div>
        <div class="open-platform-endpoint">
          <span>POST</span>
          <code>/api/v1/public/login</code>
        </div>
      </div>
    </section>

    <section class="open-platform-main">
      <div class="container open-platform-grid">
        <div class="application-panel">
          <div class="panel-heading">
            <span>Application</span>
            <h2>接入申请</h2>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="应用名称" prop="appName">
              <el-input v-model="form.appName" maxlength="120" placeholder="例如：社团积分同步服务" />
            </el-form-item>
            <el-form-item label="申请人" prop="applicantName">
              <el-input v-model="form.applicantName" maxlength="80" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="联系方式" prop="applicantContact">
              <el-input v-model="form.applicantContact" maxlength="160" placeholder="手机号、邮箱或 QQ" />
            </el-form-item>
            <el-form-item label="组织/团队">
              <el-input v-model="form.organization" maxlength="160" placeholder="可选" />
            </el-form-item>
            <el-form-item label="使用场景" prop="purpose">
              <el-input
                v-model="form.purpose"
                maxlength="800"
                :rows="5"
                show-word-limit
                type="textarea"
                placeholder="说明调用目的、使用范围和数据安全措施"
              />
            </el-form-item>
            <div class="form-actions">
              <el-button :icon="Refresh" @click="resetForm">重置</el-button>
              <el-button type="primary" :icon="Promotion" :loading="submitting" @click="submit">
                提交申请
              </el-button>
            </div>
          </el-form>
          <div v-if="submittedApplication" class="submit-result">
            <strong>申请已提交</strong>
            <span>申请编号：{{ submittedApplication.id }}</span>
            <span>当前状态：{{ statusLabel(submittedApplication.status) }}</span>
          </div>
        </div>

        <div class="code-panel">
          <div class="panel-heading">
            <span>Examples</span>
            <h2>调用代码示例</h2>
          </div>
          <el-tabs v-model="activeExample">
            <el-tab-pane
              v-for="example in codeExamples"
              :key="example.key"
              :label="example.label"
              :name="example.key"
            />
          </el-tabs>
          <div class="code-actions">
            <span>{{ currentExample.title }}</span>
            <el-button :icon="DocumentCopy" @click="copyCode">复制代码</el-button>
          </div>
          <pre class="code-block"><code>{{ currentExample.code }}</code></pre>
        </div>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import { dataOpenApi } from '@/api'
import { computed, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { DocumentCopy, Promotion, Refresh } from '@element-plus/icons-vue'

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const submittedApplication = ref<any>(null)
const activeExample = ref('vue')

const form = ref({
  appName: '',
  applicantName: '',
  applicantContact: '',
  organization: '',
  purpose: '',
})

const rules: FormRules = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
  applicantName: [{ required: true, message: '请输入申请人', trigger: 'blur' }],
  applicantContact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  purpose: [{ required: true, message: '请输入使用场景', trigger: 'blur' }],
}

const codeExamples = [
  {
    key: 'vue',
    label: 'Vue',
    title: 'Vue 3 + fetch',
    code: [
      '<script setup lang="ts">',
      "import { ref } from 'vue'",
      '',
      "const username = ref('20260001')",
      "const password = ref('your-password')",
      "const apiKey = 'oa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'",
      '',
      'async function publicLogin() {',
      "  const response = await fetch('https://api.jmi-openatom.cn/api/v1/public/login', {",
      "    method: 'POST',",
      '    headers: {',
      "      'Content-Type': 'application/json',",
      "      'X-Openatom-Data-Key': apiKey,",
      '    },',
      '    body: JSON.stringify({ username: username.value, password: password.value }),',
      '  })',
      '  const body = await response.json()',
      '  if (body.code !== 0) throw new Error(body.message)',
      '  return body.data',
      '}',
      '</' + 'script>',
    ].join('\n'),
  },
  {
    key: 'javascript',
    label: 'JavaScript',
    title: 'JavaScript + fetch',
    code: [
      "const apiKey = 'oa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'",
      '',
      "const response = await fetch('https://api.jmi-openatom.cn/api/v1/public/login', {",
      "  method: 'POST',",
      '  headers: {',
      "    'Content-Type': 'application/json',",
      "    'X-Openatom-Data-Key': apiKey,",
      '  },',
      "  body: JSON.stringify({ username: '20260001', password: 'your-password' }),",
      '})',
      '',
      'const body = await response.json()',
      'if (body.code !== 0) throw new Error(body.message)',
      'console.log(body.data.accessToken)',
    ].join('\n'),
  },
  {
    key: 'python',
    label: 'Python',
    title: 'Python requests',
    code: [
      'import requests',
      '',
      "api_key = 'oa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'",
      "url = 'https://api.jmi-openatom.cn/api/v1/public/login'",
      'payload = {',
      "    'username': '20260001',",
      "    'password': 'your-password',",
      '}',
      "headers = {'X-Openatom-Data-Key': api_key}",
      '',
      'resp = requests.post(url, json=payload, headers=headers, timeout=10)',
      'body = resp.json()',
      "if body.get('code') != 0:",
      "    raise RuntimeError(body.get('message'))",
      "print(body['data']['accessToken'])",
    ].join('\n'),
  },
  {
    key: 'java',
    label: 'Java',
    title: 'Java 21 HttpClient',
    code: [
      'var client = java.net.http.HttpClient.newHttpClient();',
      'var body = """',
      '{"username":"20260001","password":"your-password"}',
      '""";',
      '',
      "var request = java.net.http.HttpRequest.newBuilder()",
      "    .uri(java.net.URI.create('https://api.jmi-openatom.cn/api/v1/public/login'))",
      "    .header('Content-Type', 'application/json')",
      "    .header('X-Openatom-Data-Key', 'oa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx')",
      '    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))',
      '    .build();',
      '',
      'var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());',
      'System.out.println(response.body());',
    ].join('\n').replaceAll("'", '"'),
  },
  {
    key: 'curl',
    label: 'cURL',
    title: '命令行调用',
    code: [
      "curl -X POST 'https://api.jmi-openatom.cn/api/v1/public/login' \\",
      "  -H 'Content-Type: application/json' \\",
      "  -H 'X-Openatom-Data-Key: oa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx' \\",
      '  --data \'{"username":"20260001","password":"your-password"}\'',
    ].join('\n'),
  },
]

const currentExample = computed(() => {
  return codeExamples.find((item) => item.key === activeExample.value) || codeExamples[0]
})

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    submittedApplication.value = await dataOpenApi.apply({ ...form.value })
    ElMessage.success('申请已提交')
    resetForm(false)
  } finally {
    submitting.value = false
  }
}

function resetForm(clearResult = true) {
  formRef.value?.resetFields()
  form.value.organization = ''
  if (clearResult) submittedApplication.value = null
}

async function copyCode() {
  try {
    await navigator.clipboard?.writeText(currentExample.value.code)
    ElMessage.success('示例代码已复制')
  } catch (_error) {
    ElMessage.warning('当前浏览器不允许直接复制，请手动选择代码')
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
  }
  return map[status] || status || '-'
}
</script>

<style scoped>
.open-platform-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.open-platform-hero {
  background: var(--oa-elevated-bg);
  border-bottom: 1px solid var(--oa-border);
}

.open-platform-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 420px);
  gap: 28px;
  align-items: end;
  padding: 98px 0 42px;
}

.open-platform-heading span,
.panel-heading span {
  color: var(--oa-muted);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
  text-transform: uppercase;
}

.open-platform-heading h1 {
  margin: 10px 0 12px;
  color: var(--oa-text);
  font-size: clamp(34px, 5vw, 62px);
  line-height: 1.08;
  letter-spacing: 0;
}

.open-platform-heading p {
  max-width: 720px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.7;
}

.open-platform-endpoint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--oa-page-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.open-platform-endpoint span {
  padding: 5px 9px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.12);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.open-platform-endpoint code {
  overflow-wrap: anywhere;
  color: var(--oa-text);
  font-size: 14px;
}

.open-platform-main {
  padding: 28px 0 54px;
}

.open-platform-grid {
  display: grid;
  grid-template-columns: minmax(320px, 440px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.application-panel,
.code-panel {
  padding: 20px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.panel-heading {
  margin-bottom: 18px;
}

.panel-heading h2 {
  margin: 6px 0 0;
  color: var(--oa-text);
  font-size: 22px;
  font-weight: 650;
  letter-spacing: 0;
}

.form-actions,
.code-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.submit-result {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  padding: 14px;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.22);
  border-radius: 8px;
  color: var(--oa-text);
}

.submit-result span {
  color: var(--oa-muted);
  font-size: 13px;
}

.code-panel :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.code-actions {
  margin-bottom: 10px;
  color: var(--oa-muted);
  font-size: 13px;
}

.code-block {
  min-height: 430px;
  max-height: 580px;
  margin: 0;
  padding: 18px;
  overflow: auto;
  color: #d7e0ea;
  background: #101820;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre;
}

@media (max-width: 920px) {
  .open-platform-hero__inner,
  .open-platform-grid {
    grid-template-columns: 1fr;
  }
}
</style>
