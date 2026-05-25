<template>
  <ViewPage class="my-blog-page" :loading="loading">
    <section class="my-blog-shell">
      <div class="container">
        <ViewToolbar>
          <div class="toolbar__filters">
            <el-select v-model="query.status" clearable placeholder="文章状态" @change="reload">
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
              <el-option label="已隐藏" value="hidden" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
            <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
          </div>
          <el-button type="primary" :icon="Plus" @click="openDialog()">写文章</el-button>
        </ViewToolbar>

        <el-alert
          v-if="!canWrite"
          class="write-alert"
          type="warning"
          show-icon
          :closable="false"
          title="仅正式成员可以发布文章；如果你已经转正，请重新登录刷新权限信息。"
        />

        <el-table :data="rows" class="admin-table">
          <el-table-column prop="title" label="文章" min-width="240">
            <template #default="{ row }">
              <strong>{{ row.title }}</strong>
              <p class="muted-line">{{ row.summary || '暂无摘要' }}</p>
              <p v-if="row.rejectReason" class="danger-line">原因：{{ row.rejectReason }}</p>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="130" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="数据" width="130">
            <template #default="{ row }">
              {{ row.viewCount || 0 }} 阅读 · {{ row.commentCount || 0 }} 评论
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt || row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status !== 'published' && row.status !== 'hidden'"
                link
                type="success"
                @click="publish(row)"
              >
                发布
              </el-button>
              <el-button
                v-if="row.status === 'published'"
                link
                type="success"
                @click="$router.push(`/blog/${row.id}`)"
              >
                查看
              </el-button>
              <el-popconfirm title="确定删除这篇文章？" @confirm="remove(row)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > query.pageSize"
          class="pager"
          background
          layout="prev, pager, next"
          :current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑文章' : '写文章'" width="960px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="180" show-word-limit />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="form.category" maxlength="80" placeholder="例如：Java / 前端 / 竞赛" />
          </el-form-item>
          <el-form-item label="封面URL">
            <el-input v-model="form.coverUrl" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="tagText" placeholder="多个标签用逗号分隔" />
          </el-form-item>
        </div>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="contentMarkdown">
          <el-input v-model="form.contentMarkdown" type="textarea" :rows="16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" @click="save('draft')">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="save('published')">发布</el-button>
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { blogApi } from '@/api'
import { formatDateTime, statusType } from '@/utils/format.ts'
import { hasRole } from '@/utils/permission.ts'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const form = ref<Record<string, any>>({})
const tagText = ref('')
const formRef = ref<any>()
const query = ref({
  status: '',
  page: 1,
  pageSize: 10,
})

const canWrite = computed(() => hasRole('formal_member'))
const rules = ref({
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  contentMarkdown: [{ required: true, message: '请输入文章正文', trigger: 'blur' }],
})

function statusText(status: string) {
  return (
    { draft: '草稿', published: '已发布', hidden: '已隐藏', rejected: '已驳回' }[status] ||
    status ||
    '-'
  )
}

async function fetchList() {
  loading.value = true
  try {
    const data = await blogApi.myArticles({
      status: query.value.status || undefined,
      page: query.value.page,
      pageSize: query.value.pageSize,
    })
    rows.value = data?.list || []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  query.value.page = page
  fetchList()
}

function openDialog(row?: any) {
  form.value = row
    ? { ...row, contentMarkdown: row.contentMarkdown || '' }
    : {
        title: '',
        summary: '',
        category: '',
        coverUrl: '',
        contentMarkdown: '# 标题\n\n',
      }
  tagText.value = (row?.tags || []).join(', ')
  dialogVisible.value = true
}

function save(status: string) {
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        title: form.value.title,
        summary: form.value.summary,
        contentMarkdown: form.value.contentMarkdown,
        coverUrl: form.value.coverUrl,
        category: form.value.category,
        tags: parseTags(tagText.value),
        status,
      }
      if (form.value.id) await blogApi.update(form.value.id, payload)
      else await blogApi.create(payload)
      ElMessage.success(status === 'published' ? '文章已发布' : '草稿已保存')
      dialogVisible.value = false
      fetchList()
    } finally {
      saving.value = false
    }
  })
}

async function publish(row: any) {
  await blogApi.publish(row.id)
  ElMessage.success('文章已发布')
  fetchList()
}

async function remove(row: any) {
  await blogApi.remove(row.id)
  ElMessage.success('文章已删除')
  fetchList()
}

function parseTags(value: string) {
  return value
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 8)
}

onMounted(async () => {
  await fetchList()
  if (route.name === 'site-blog-write') {
    await nextTick()
    openDialog()
  }
})
</script>

<style scoped>
.my-blog-page {
  min-height: calc(100vh - var(--oa-site-header-height));
  background: var(--oa-page-soft-bg);
}

.my-blog-shell {
  padding: 92px 0 56px;
}

.write-alert {
  margin: 14px 0;
}

.muted-line,
.danger-line {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.muted-line {
  color: var(--oa-muted);
}

.danger-line {
  color: var(--el-color-danger);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

@media (max-width: 760px) {
  .my-blog-shell {
    padding-top: 76px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
