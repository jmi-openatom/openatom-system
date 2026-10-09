<template>
  <ViewPage class="admin-page">
    <ViewToolbar>
      <div class="campus-review__filters">
        <el-select
          v-model="query.buildingId"
          filterable
          clearable
          placeholder="全部楼宇"
          style="width: 240px"
          @change="reload"
        >
          <el-option
            v-for="building in CAMPUS_BUILDINGS"
            :key="building.id"
            :label="building.name"
            :value="String(building.id)"
          />
        </el-select>
        <el-select
          v-model="query.status"
          clearable
          placeholder="全部状态"
          style="width: 140px"
          @change="reload"
        >
          <el-option
            v-for="(label, status) in campusSubmissionLabels"
            :key="status"
            :label="label"
            :value="status"
          />
        </el-select>
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
      </div>
      <el-button @click="$router.push('/campus-map')">查看校园地图</el-button>
    </ViewToolbar>
    <p class="campus-review__hint">
      所有登录用户都可以投稿或修改。通过审核后，介绍及照片变更会直接展示在楼宇卡片中。
    </p>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="buildingName" label="楼宇" min-width="180" />
      <el-table-column prop="authorName" label="提交者" width="130" />
      <el-table-column label="修改内容" min-width="220">
        <template #default="{ row }">
          <span
            >{{ row.replaceDescription ? '修改介绍 · ' : '' }}新增 {{ row.photos.length }} 张 · 移除
            {{ row.removedPhotos.length }} 张</span
          >
          <p v-if="row.reviewReason" class="campus-review__hint">
            审核意见：{{ row.reviewReason }}
          </p>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }"
          ><el-tag
            :type="
              row.status === 'pending' ? 'warning' : row.status === 'approved' ? 'success' : 'info'
            "
            >{{ campusSubmissionLabels[row.status as CampusSubmissionStatus] }}</el-tag
          ></template
        >
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }"
          ><el-button link type="primary" @click="openReview(row)">{{
            row.status === 'pending' && canReview ? '审核' : '查看'
          }}</el-button></template
        >
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > query.pageSize"
      class="campus-review__pager"
      background
      layout="total, prev, pager, next"
      :current-page="query.page"
      :page-size="query.pageSize"
      :total="total"
      @current-change="changePage"
    />

    <el-dialog
      v-model="reviewOpen"
      title="楼宇信息审核"
      width="min(900px, calc(100vw - 32px))"
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <div v-if="current" class="campus-review__detail">
        <header>
          <h2>{{ current.buildingName }}</h2>
          <p>
            {{ current.authorName }} · {{ formatDateTime(current.createdAt) }} ·
            {{ campusSubmissionLabels[current.status] }}
          </p>
        </header>
        <div v-if="current.replaceDescription" class="campus-review__comparison">
          <section>
            <h3>当前展示的介绍</h3>
            <p>
              {{
                publicLoading
                  ? '正在加载…'
                  : publicError
                    ? '当前内容加载失败，请重新打开'
                    : publicDetail?.description || '暂无介绍'
              }}
            </p>
          </section>
          <section>
            <h3>本次提交的介绍</h3>
            <p>{{ current.description || '清空介绍，通过后显示“暂无介绍”' }}</p>
          </section>
        </div>
        <p v-else class="campus-review__hint">本次不修改建筑介绍</p>
        <section v-if="current.photos.length">
          <h3>新增实拍图（{{ current.photos.length }} 张）</h3>
          <div class="campus-review__photos">
            <el-image
              v-for="(photo, index) in current.photos"
              :key="photo.id"
              :src="privateUrl(photo.url)"
              :alt="`新增实拍图 ${index + 1}`"
              fit="cover"
              :preview-src-list="current.photos.map((item) => privateUrl(item.url))"
              :initial-index="index"
              preview-teleported
            />
          </div>
        </section>
        <section v-if="current.removedPhotos.length">
          <h3 class="campus-review__removal">
            申请移除的照片（{{ current.removedPhotos.length }} 张）
          </h3>
          <div class="campus-review__photos">
            <el-image
              v-for="(photo, index) in current.removedPhotos"
              :key="photo.id"
              :src="privateUrl(photo.url)"
              :alt="`申请移除的实拍图 ${index + 1}`"
              fit="cover"
              :preview-src-list="current.removedPhotos.map((item) => privateUrl(item.url))"
              :initial-index="index"
              preview-teleported
            />
          </div>
        </section>
        <p v-if="current.reviewReason" class="campus-review__hint">
          审核意见：{{ current.reviewReason }}
        </p>
        <el-input
          v-if="canReview && ['pending', 'approved'].includes(current.status)"
          v-model="reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="审核意见（驳回或下架时必填）"
          aria-label="审核意见"
        />
        <p v-if="current.status === 'approved' && canReview" class="campus-review__hint">
          下架会撤回本次投稿的介绍及照片变更，保留其他已通过的内容。
        </p>
      </div>
      <template #footer>
        <el-button :disabled="saving" @click="reviewOpen = false">关闭</el-button>
        <template v-if="canReview && current?.status === 'pending'">
          <el-button type="danger" :loading="saving" @click="review('reject')">驳回</el-button>
          <el-button
            type="primary"
            :loading="saving"
            :disabled="publicLoading || publicError"
            @click="review('approve')"
            >通过并展示</el-button
          >
        </template>
        <el-button
          v-if="canReview && current?.status === 'approved'"
          type="danger"
          :loading="saving"
          @click="review('hide')"
          >下架本次投稿</el-button
        >
      </template>
    </el-dialog>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ViewPage from '@/components/common/ViewPage.vue'
import ViewToolbar from '@/components/common/ViewToolbar.vue'
import { CAMPUS_BUILDINGS } from '@/components/site/home/campusLabels'
import {
  campusBuildingApi,
  campusSubmissionLabels,
  type CampusBuildingDetail,
  type CampusSubmission,
  type CampusSubmissionStatus,
} from '@/api/campusBuildings'
import { appendTokenQuery } from '@/utils/auth'
import { hasPermission } from '@/utils/permission'
import { formatDateTime } from '@/utils/format'

const query = reactive({ buildingId: '', status: 'pending', page: 1, pageSize: 20 })
const rows = ref<CampusSubmission[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const reviewOpen = ref(false)
const current = ref<CampusSubmission>()
const reason = ref('')
const publicDetail = ref<CampusBuildingDetail>()
const publicLoading = ref(false)
const publicError = ref(false)
const canReview = computed(() => hasPermission('campus-building:review'))
const privateUrl = appendTokenQuery

async function load() {
  loading.value = true
  try {
    const data = await campusBuildingApi.submissions({
      ...query,
      buildingId: query.buildingId || undefined,
      status: query.status || undefined,
    })
    rows.value = data.list
    total.value = data.total
  } catch {
    // The request layer displays the error.
  } finally {
    loading.value = false
  }
}
function reload() {
  query.page = 1
  void load()
}
function changePage(page: number) {
  query.page = page
  void load()
}
async function openReview(row: CampusSubmission) {
  current.value = row
  reason.value = ''
  publicDetail.value = undefined
  publicError.value = false
  reviewOpen.value = true
  publicLoading.value = true
  try {
    publicDetail.value = await campusBuildingApi.detail(row.buildingId)
  } catch {
    publicError.value = true
  } finally {
    publicLoading.value = false
  }
}
async function review(action: 'approve' | 'reject' | 'hide') {
  if (!current.value || saving.value) return
  if (action !== 'approve' && !reason.value.trim()) {
    ElMessage.warning('请填写驳回或下架原因')
    return
  }
  saving.value = true
  try {
    await campusBuildingApi.review(current.value.id, action, reason.value)
    ElMessage.success(
      action === 'approve'
        ? '审核通过，楼宇卡片已更新'
        : action === 'reject'
          ? '已驳回'
          : '已下架本次投稿',
    )
    reviewOpen.value = false
    await load()
  } catch {
    // Retain the review for retrying if the request fails.
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.campus-review__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.campus-review__hint {
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.campus-review__pager {
  margin-top: 20px;
  justify-content: flex-end;
}
.campus-review__detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.campus-review__detail h2 {
  margin: 0;
  font-size: 22px;
}
.campus-review__detail h3 {
  margin: 0 0 10px;
  font-size: 14px;
}
.campus-review__detail header p {
  color: var(--oa-muted);
  font-size: 12px;
}
.campus-review__comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.campus-review__comparison section {
  padding: 16px;
  background: var(--oa-page-soft-bg);
  border-radius: 12px;
}
.campus-review__comparison p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 13px;
  line-height: 1.8;
}
.campus-review__photos {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.campus-review__photos .el-image {
  width: 100%;
  height: 160px;
  border-radius: 10px;
  background: var(--oa-page-soft-bg);
}
.campus-review__removal {
  color: var(--el-color-danger);
}
@media (max-width: 600px) {
  .campus-review__comparison {
    grid-template-columns: 1fr;
  }
  .campus-review__photos {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .campus-review__photos .el-image {
    height: 120px;
  }
}
</style>
