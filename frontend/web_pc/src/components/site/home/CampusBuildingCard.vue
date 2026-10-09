<template>
  <section class="building-card" aria-labelledby="building-card-title" aria-live="polite">
    <header class="building-card__header">
      <div>
        <p class="building-card__eyebrow"><MapPin :size="12" aria-hidden="true" />楼宇档案</p>
        <h2 id="building-card-title">{{ building.name }}</h2>
      </div>
      <button class="building-card__close" aria-label="关闭楼宇卡片" @click="emit('close')">
        <X :size="18" aria-hidden="true" />
      </button>
    </header>
    <div class="building-card__content" data-lenis-prevent>
      <p v-if="loading" class="building-card__status" role="status">正在加载楼宇信息…</p>
      <div v-else-if="loadError" class="building-card__status" role="alert">
        <p>楼宇信息加载失败</p>
        <button class="building-card__retry" @click="loadDetail()">重新加载</button>
      </div>
      <template v-else>
        <div v-if="detail?.photos.length" class="building-gallery">
          <el-image
            :key="activePhoto?.id"
            class="building-gallery__image"
            :src="activePhoto?.url"
            :alt="`${building.name}实拍图 ${photoIndex + 1}`"
            fit="cover"
            :preview-src-list="photoUrls"
            :initial-index="photoIndex"
            preview-teleported
            hide-on-click-modal
          >
            <template #error><div class="building-gallery__error">图片加载失败</div></template>
          </el-image>
          <span class="building-gallery__count"
            >{{ photoIndex + 1 }} / {{ detail.photos.length }}</span
          >
          <template v-if="detail.photos.length > 1">
            <button
              class="building-gallery__arrow building-gallery__arrow--previous"
              aria-label="上一张实拍图"
              @click="movePhoto(-1)"
            >
              <ChevronLeft :size="18" aria-hidden="true" />
            </button>
            <button
              class="building-gallery__arrow building-gallery__arrow--next"
              aria-label="下一张实拍图"
              @click="movePhoto(1)"
            >
              <ChevronRight :size="18" aria-hidden="true" />
            </button>
          </template>
        </div>
        <div v-else class="building-card__empty-photo">
          <ImageIcon :size="28" :stroke-width="1.3" aria-hidden="true" />
          <span>暂无实拍图</span>
        </div>
        <div
          v-if="detail && detail.photos.length > 1"
          class="building-gallery__thumbnails"
          aria-label="选择实拍图"
        >
          <button
            v-for="(photo, index) in detail.photos"
            :key="photo.id"
            :aria-label="`查看第 ${index + 1} 张实拍图`"
            :aria-pressed="index === photoIndex"
            @click="photoIndex = index"
          >
            <img :src="photo.url" alt="" loading="lazy" />
          </button>
        </div>
        <div class="building-card__description">
          <h3>建筑介绍</h3>
          <p :class="{ 'building-card__muted': !detail?.description }">
            {{ detail?.description || '暂无介绍' }}
          </p>
        </div>
      </template>
    </div>
    <footer class="building-card__footer">
      <button
        class="building-card__contribute"
        :disabled="loading || loadError"
        @click="openSubmission"
      >
        <SquarePen :size="15" aria-hidden="true" />{{
          loggedIn ? '完善楼宇信息' : '登录后完善楼宇信息'
        }}
        <ArrowUpRight :size="14" aria-hidden="true" />
      </button>
      <p>介绍与实拍图均在审核通过后展示</p>
    </footer>
  </section>

  <el-dialog
    v-model="submissionOpen"
    :title="`完善 · ${building.name}`"
    width="min(620px, calc(100vw - 32px))"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @closed="clearDraft"
  >
    <form class="building-submission" @submit.prevent="submit">
      <p class="building-submission__notice">
        可以修改介绍、添加或移除实拍图。审核期间，卡片继续展示原内容。
      </p>
      <el-checkbox v-model="replaceDescription">修改建筑介绍</el-checkbox>
      <el-input
        v-if="replaceDescription"
        v-model="description"
        type="textarea"
        :rows="5"
        maxlength="4000"
        show-word-limit
        placeholder="介绍这栋建筑的用途、特色或你熟悉的校园故事…"
        aria-label="建筑介绍"
      />
      <p v-if="replaceDescription" class="building-submission__hint">
        清空介绍并通过审核后，将显示“暂无介绍”。
      </p>
      <div v-if="detail?.photos.length" class="building-submission__existing">
        <h3>现有实拍图 <small>勾选要移除的照片</small></h3>
        <div class="building-submission__photos">
          <label
            v-for="(photo, index) in detail.photos"
            :key="photo.id"
            :class="{ 'is-removed': removedPhotoIds.includes(photo.id) }"
          >
            <img :src="photo.url" :alt="`${building.name}现有实拍图 ${index + 1}`" />
            <span><input v-model="removedPhotoIds" type="checkbox" :value="photo.id" />移除</span>
          </label>
        </div>
      </div>
      <div class="building-submission__uploads">
        <h3>添加实拍图 <small>每次最多 9 张，每张不超过 10MB</small></h3>
        <label class="building-submission__picker">
          <Plus :size="18" aria-hidden="true" />选择照片
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            :disabled="submitting"
            @change="addFiles"
          />
        </label>
        <p class="building-submission__hint">支持 JPG、PNG、WebP，请提交与这栋楼对应的实拍照片。</p>
        <div v-if="draftPhotos.length" class="building-submission__photos">
          <div
            v-for="(photo, index) in draftPhotos"
            :key="photo.preview"
            class="building-submission__draft"
          >
            <img :src="photo.preview" :alt="photo.file.name" />
            <button
              type="button"
              :aria-label="`移除新照片 ${index + 1}`"
              :disabled="submitting"
              @click="removeDraft(index)"
            >
              <X :size="14" aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>
      <div v-if="loggedIn" class="building-submission__history">
        <h3>我的提交</h3>
        <p v-if="historyLoading" class="building-submission__hint">正在加载提交记录…</p>
        <p v-else-if="historyError" class="building-submission__hint">
          提交记录加载失败
          <button type="button" class="building-card__retry" @click="loadHistory()">重试</button>
        </p>
        <p v-else-if="!history.length" class="building-submission__hint">
          你还没有为这栋楼提交过内容
        </p>
        <ul v-else>
          <li v-for="item in history" :key="item.id">
            <div>
              <el-tag
                size="small"
                :type="
                  item.status === 'pending'
                    ? 'warning'
                    : item.status === 'approved'
                      ? 'success'
                      : 'info'
                "
                >{{ campusSubmissionLabels[item.status] }}</el-tag
              ><span
                >{{ item.replaceDescription ? '修改介绍 · ' : '' }}新增 {{ item.photos.length }} 张
                · 移除 {{ item.removedPhotos.length }} 张</span
              >
            </div>
            <small>{{ formatDateTime(item.createdAt) }}</small>
            <p v-if="item.reviewReason">审核意见：{{ item.reviewReason }}</p>
          </li>
        </ul>
        <el-pagination
          v-if="historyTotal > 5"
          small
          layout="prev, pager, next"
          :current-page="historyPage"
          :page-size="5"
          :total="historyTotal"
          @current-change="loadHistory"
        />
      </div>
      <p v-if="hasPending" class="building-submission__notice">
        这栋楼已有你的待审核提交，审核完成后可以再次修改。
      </p>
      <div class="building-submission__actions">
        <el-button :disabled="submitting" @click="submissionOpen = false">取消</el-button>
        <el-button
          type="primary"
          native-type="submit"
          :loading="submitting"
          :disabled="!hasChanges || hasPending || historyLoading || historyError"
          >提交审核</el-button
        >
      </div>
    </form>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  Image as ImageIcon,
  MapPin,
  Plus,
  SquarePen,
  X,
} from 'lucide-vue-next'
import {
  campusBuildingApi,
  campusSubmissionLabels,
  type CampusBuildingDetail,
  type CampusSubmission,
} from '@/api/campusBuildings'
import { getToken } from '@/utils/auth'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{ building: { id: string | number; name: string } }>()
const emit = defineEmits<{ close: [] }>()
const router = useRouter()
const loggedIn = computed(() => Boolean(getToken()))
const detail = ref<CampusBuildingDetail>()
const loading = ref(true)
const loadError = ref(false)
const photoIndex = ref(0)
const activePhoto = computed(() => detail.value?.photos[photoIndex.value])
const photoUrls = computed(() => detail.value?.photos.map((photo) => photo.url) || [])
const submissionOpen = ref(false)
const submitting = ref(false)
const description = ref('')
const replaceDescription = ref(false)
const removedPhotoIds = ref<number[]>([])
const draftPhotos = ref<{ file: File; preview: string }[]>([])
const history = ref<CampusSubmission[]>([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historyLoading = ref(false)
const historyError = ref(false)
const hasPending = ref(false)
const hasChanges = computed(
  () =>
    (replaceDescription.value &&
      description.value.trim() !== (detail.value?.description || '').trim()) ||
    draftPhotos.value.length > 0 ||
    removedPhotoIds.value.length > 0,
)

async function loadDetail() {
  loading.value = true
  loadError.value = false
  try {
    detail.value = await campusBuildingApi.detail(props.building.id)
    photoIndex.value = 0
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function movePhoto(direction: number) {
  const count = detail.value?.photos.length || 0
  if (count) photoIndex.value = (photoIndex.value + direction + count) % count
}

async function openSubmission() {
  if (!getToken()) {
    await router.push({
      path: '/login',
      query: { redirect: `/campus-map?building=${encodeURIComponent(props.building.id)}` },
    })
    return
  }
  description.value = detail.value?.description || ''
  replaceDescription.value = !detail.value?.description
  submissionOpen.value = true
  await loadHistory()
}

async function loadHistory(page = 1) {
  historyLoading.value = true
  historyError.value = false
  try {
    const data = await campusBuildingApi.mine(props.building.id, page)
    history.value = data.list
    historyTotal.value = data.total
    historyPage.value = page
    // Pending is always newer than this user's reviewed submissions when created.
    if (page === 1) hasPending.value = data.list.some((item) => item.status === 'pending')
  } catch {
    historyError.value = true
  } finally {
    historyLoading.value = false
  }
}

function addFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (draftPhotos.value.length + files.length > 9) {
    ElMessage.warning('每次最多提交 9 张实拍图')
    return
  }
  if (
    files.some(
      (file) =>
        !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) ||
        file.size === 0 ||
        file.size > 10 * 1024 * 1024,
    )
  ) {
    ElMessage.warning('请选择 JPG、PNG、WebP 图片，每张不超过 10MB')
    return
  }
  draftPhotos.value.push(...files.map((file) => ({ file, preview: URL.createObjectURL(file) })))
}

function removeDraft(index: number) {
  const photo = draftPhotos.value[index]
  if (photo) URL.revokeObjectURL(photo.preview)
  draftPhotos.value.splice(index, 1)
}

function clearDraft() {
  draftPhotos.value.forEach((photo) => URL.revokeObjectURL(photo.preview))
  draftPhotos.value = []
  removedPhotoIds.value = []
  replaceDescription.value = false
}

async function submit() {
  if (!hasChanges.value || hasPending.value || submitting.value) return
  submitting.value = true
  try {
    const changedDescription =
      replaceDescription.value &&
      description.value.trim() !== (detail.value?.description || '').trim()
    await campusBuildingApi.submit(
      props.building.id,
      changedDescription ? description.value : '',
      changedDescription,
      draftPhotos.value.map((photo) => photo.file),
      removedPhotoIds.value,
    )
    ElMessage.success('已提交，审核通过后会展示在楼宇卡片中')
    clearDraft()
    description.value = detail.value?.description || ''
    await loadHistory()
  } catch {
    // The request layer displays upload and validation errors; retain the draft.
  } finally {
    submitting.value = false
  }
}

function onKeydown(event: KeyboardEvent) {
  if (
    event.key === 'Escape' &&
    !submissionOpen.value &&
    !document.querySelector('.el-image-viewer__wrapper')
  )
    emit('close')
}
function onVisibilityChange() {
  if (!document.hidden && !submissionOpen.value) void loadDetail()
}
onMounted(() => {
  void loadDetail()
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onBeforeUnmount(() => {
  clearDraft()
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.building-card {
  position: absolute;
  z-index: 7;
  top: 24px;
  right: 64px;
  width: 360px;
  max-height: calc(100svh - 72px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--oa-border, #e4e5e0);
  border-radius: 20px;
  color: var(--oa-text, #243630);
  background: var(--oa-surface, #fff);
  box-shadow: 0 18px 60px #0b171b26;
}
.building-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 20px 16px;
}
.building-card__eyebrow {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 8px;
  font-size: 11px;
  color: var(--oa-muted);
  letter-spacing: 0.12em;
}
.building-card h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
  font-weight: 600;
  letter-spacing: -0.03em;
}
.building-card__close {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted);
  cursor: pointer;
}
.building-card__content {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 20px;
}
.building-card__empty-photo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  aspect-ratio: 16 / 9;
  border: 1px dashed var(--oa-border);
  border-radius: 12px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted);
  font-size: 13px;
}
.building-card__description {
  padding: 20px 0;
}
.building-card h3,
.building-submission h3 {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
}
.building-card__description p {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.building-card__muted {
  color: var(--oa-muted);
}
.building-card__footer {
  flex-shrink: 0;
  padding: 14px 20px 16px;
  border-top: 1px solid var(--oa-border);
}
.building-card__footer p {
  margin: 9px 0 0;
  color: var(--oa-muted);
  font-size: 10px;
  text-align: center;
}
.building-card__contribute {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 40px;
  border: 0;
  border-radius: 9px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-text);
  font-size: 12px;
  cursor: pointer;
}
.building-card__contribute:disabled {
  opacity: 0.5;
  cursor: default;
}
.building-card__status {
  padding: 30px 0;
  color: var(--oa-muted);
  font-size: 13px;
  text-align: center;
}
.building-card__retry {
  padding: 4px;
  border: 0;
  background: transparent;
  color: var(--oa-text);
  font-size: 12px;
  text-decoration: underline;
  cursor: pointer;
}
.building-gallery {
  position: relative;
  aspect-ratio: 16 / 10;
  border-radius: 12px;
  overflow: hidden;
  background: var(--oa-page-soft-bg);
}
.building-gallery__image {
  width: 100%;
  height: 100%;
  display: block;
}
.building-gallery__error {
  display: grid;
  place-items: center;
  height: 100%;
  color: var(--oa-muted);
  font-size: 12px;
}
.building-gallery__count {
  position: absolute;
  bottom: 10px;
  right: 10px;
  padding: 4px 8px;
  border-radius: 20px;
  color: #fff;
  background: #15222b99;
  font-size: 10px;
  pointer-events: none;
}
.building-gallery__arrow {
  position: absolute;
  top: calc(50% - 16px);
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #ffffff66;
  background: #15222b99;
  color: #fff;
  cursor: pointer;
}
.building-gallery__arrow--previous {
  left: 8px;
}
.building-gallery__arrow--next {
  right: 8px;
}
.building-gallery__thumbnails {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 10px 1px 2px;
}
.building-gallery__thumbnails button {
  flex-shrink: 0;
  width: 52px;
  height: 38px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  opacity: 0.65;
}
.building-gallery__thumbnails button[aria-pressed='true'] {
  border-color: #679ba7;
  opacity: 1;
}
.building-gallery__thumbnails img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.building-card button:focus-visible,
.building-submission__picker:focus-within {
  outline: 2px solid #679ba7;
  outline-offset: 3px;
}
.building-submission {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.building-submission__notice {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: var(--oa-page-soft-bg);
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.7;
}
.building-submission h3 small {
  margin-left: 6px;
  color: var(--oa-muted);
  font-size: 11px;
  font-weight: 400;
}
.building-submission__hint {
  margin: 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--oa-muted);
}
.building-submission__uploads,
.building-submission__existing,
.building-submission__history {
  padding-top: 8px;
}
.building-submission__picker {
  display: inline-flex;
  position: relative;
  align-items: center;
  gap: 6px;
  padding: 9px 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  color: var(--oa-text);
  font-size: 12px;
  cursor: pointer;
  margin-bottom: 8px;
}
.building-submission__picker input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}
.building-submission__photos {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}
.building-submission__photos img {
  display: block;
  width: 100%;
  height: 82px;
  object-fit: cover;
  border-radius: 8px;
}
.building-submission__photos label {
  cursor: pointer;
}
.building-submission__photos label span {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 5px;
  font-size: 11px;
}
.building-submission__photos label.is-removed img {
  opacity: 0.4;
}
.building-submission__draft {
  position: relative;
}
.building-submission__draft button {
  position: absolute;
  top: 4px;
  right: 4px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 50%;
  background: #15222bcc;
  color: #fff;
  cursor: pointer;
}
.building-submission__history ul {
  margin: 0;
  padding: 0;
  list-style: none;
}
.building-submission__history li {
  padding: 10px 0;
  border-bottom: 1px solid var(--oa-border);
}
.building-submission__history li div {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.building-submission__history li small {
  display: block;
  margin-top: 5px;
  color: var(--oa-muted);
  font-size: 10px;
}
.building-submission__history li p {
  margin: 6px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.building-submission__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
}
@media (max-width: 899px) {
  .building-card {
    top: auto;
    right: 62px;
    bottom: 28px;
    left: 12px;
    width: auto;
    max-height: min(50svh, 500px);
    border-radius: 16px;
  }
  .building-card__header {
    padding: 14px 16px 12px;
  }
  .building-card h2 {
    font-size: 18px;
  }
  .building-card__eyebrow {
    margin-bottom: 4px;
    font-size: 10px;
  }
  .building-card__content {
    padding: 0 16px;
  }
  .building-card__empty-photo {
    aspect-ratio: auto;
    min-height: 105px;
    gap: 8px;
  }
  .building-gallery {
    aspect-ratio: 16 / 8;
  }
  .building-card__description {
    padding: 14px 0;
  }
  .building-card__footer {
    padding: 10px 16px;
  }
  .building-card__footer p {
    display: none;
  }
  .building-submission__photos {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
