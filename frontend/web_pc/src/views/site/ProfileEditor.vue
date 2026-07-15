<template>
  <div v-if="profile" class="profile-editor">
    <header class="profile-editor__topbar">
      <div>
        <el-button text @click="$router.push('/profile')">← 返回主页</el-button>
        <span :class="`is-${profile.status}`" class="profile-editor__status">{{
          statusLabel
        }}</span>
      </div>
      <div>
        <el-button :loading="saving" @click="saveDraft">保存草稿</el-button>
        <el-button v-if="profile.status === 'published'" :loading="publishing" @click="unpublish"
          >下线</el-button
        >
        <el-button :loading="publishing" type="primary" @click="publish">发布主页</el-button>
      </div>
    </header>

    <div class="profile-editor__workspace">
      <aside class="profile-editor__panel">
        <el-collapse v-model="activePanels">
          <el-collapse-item name="identity" title="基本信息">
            <label
              >展示名称<el-input v-model="profile.displayName" maxlength="80" show-word-limit
            /></label>
            <label>一句话介绍<el-input v-model="profile.headline" maxlength="160" /></label>
            <label
              >主页地址
              <div class="slug-input"><span>/members/</span><el-input v-model="profile.slug" /></div
            ></label>
            <label
              >关于我<el-input
                v-model="profile.bio"
                :rows="6"
                maxlength="4000"
                show-word-limit
                type="textarea"
            /></label>
            <label
              >技能标签<el-input v-model="skillsText" placeholder="Vue, Java, 开源协作"
            /></label>
            <div class="profile-editor__switches">
              <el-checkbox v-model="showDepartment">展示部门</el-checkbox>
              <el-checkbox v-model="showPosition">展示职位</el-checkbox>
            </div>
          </el-collapse-item>

          <el-collapse-item name="appearance" title="主题与图片">
            <label
              >页面主题
              <el-radio-group v-model="profile.themeKey" class="theme-options">
                <el-radio-button label="minimal">简约</el-radio-button
                ><el-radio-button label="tech">科技</el-radio-button
                ><el-radio-button label="warm">温暖</el-radio-button
                ><el-radio-button label="editorial">杂志</el-radio-button>
              </el-radio-group>
            </label>
            <label
              >颜色模式<el-select v-model="profile.colorMode"
                ><el-option label="跟随系统" value="system" /><el-option
                  label="浅色"
                  value="light" /><el-option label="深色" value="dark" /></el-select
            ></label>
            <label
              >谁能看到<el-select v-model="profile.visibility"
                ><el-option label="社团成员" value="members" /><el-option
                  label="仅通过链接"
                  value="unlisted" /><el-option label="仅自己" value="private" /></el-select
            ></label>
            <div class="image-actions">
              <label class="image-upload"
                >上传头像<input
                  accept="image/*"
                  type="file"
                  @change="uploadImage('avatar', $event)"
              /></label>
              <label class="image-upload"
                >上传主页横幅<input
                  accept="image/*"
                  type="file"
                  @change="uploadImage('banner', $event)"
              /></label>
              <label class="image-upload"
                >上传名片底图<input
                  accept="image/*"
                  type="file"
                  @change="uploadImage('card-background', $event)"
              /></label>
            </div>
            <template v-if="profile.cardBackgroundUrl">
              <label
                >名片图片水平焦点<el-slider v-model="profile.cardFocusX" :max="100" :min="0"
              /></label>
              <label
                >名片图片垂直焦点<el-slider v-model="profile.cardFocusY" :max="100" :min="0"
              /></label>
            </template>
          </el-collapse-item>

          <el-collapse-item name="modules" title="主页组件">
            <p class="profile-editor__hint">
              添加组件后可调整顺序、宽度和可见范围。私密组件只会出现在自己的预览中。
            </p>
            <div class="module-library">
              <button
                v-for="item in availableModules"
                :key="item.key"
                type="button"
                @click="addModule(item.key)"
              >
                ＋ {{ item.title }}
              </button>
            </div>
            <div class="module-editor-list">
              <article
                v-for="(module, index) in sortedModules"
                :key="module.key"
                class="module-editor"
              >
                <div class="module-editor__head">
                  <strong>{{ moduleLabel(module.key) }}</strong>
                  <div>
                    <el-button :disabled="index === 0" link @click="moveModule(index, -1)"
                      >上移</el-button
                    ><el-button
                      :disabled="index === sortedModules.length - 1"
                      link
                      @click="moveModule(index, 1)"
                      >下移</el-button
                    ><el-button link type="danger" @click="removeModule(module.key)"
                      >移除</el-button
                    >
                  </div>
                </div>
                <el-input v-model="module.title" placeholder="组件标题" />
                <div class="module-editor__options">
                  <el-checkbox v-model="module.enabled">启用</el-checkbox
                  ><el-select v-model="module.columnSpan"
                    ><el-option :value="12" label="整行" /><el-option
                      :value="6"
                      label="半行" /></el-select
                  ><el-select v-model="module.visibility"
                    ><el-option label="成员可见" value="members" /><el-option
                      label="仅自己"
                      value="private"
                  /></el-select>
                </div>
                <el-input
                  v-if="module.key === 'markdown'"
                  v-model="module.data.content"
                  :rows="5"
                  placeholder="支持 Markdown"
                  type="textarea"
                />
                <el-input
                  v-if="itemEditorConfig[module.key]"
                  v-model="module.data.editorText"
                  :rows="5"
                  :placeholder="itemEditorConfig[module.key].placeholder"
                  type="textarea"
                />
                <template v-if="module.key === 'hero_statement'">
                  <el-input
                    v-model="module.data.text"
                    :rows="3"
                    placeholder="一句有力量的个人宣言"
                    type="textarea"
                  />
                  <el-input v-model="module.data.caption" placeholder="补充说明（可选）" />
                </template>
                <template v-if="module.key === 'quote'">
                  <el-input
                    v-model="module.data.text"
                    :rows="3"
                    placeholder="引用内容"
                    type="textarea"
                  />
                  <el-input v-model="module.data.source" placeholder="出处或署名" />
                </template>
                <template v-if="module.key === 'contact_cta'">
                  <el-input v-model="module.data.heading" placeholder="行动标题" />
                  <el-input v-model="module.data.description" placeholder="一段邀请说明" />
                  <el-input v-model="module.data.buttonLabel" placeholder="按钮文字" />
                  <el-input v-model="module.data.url" placeholder="https://联系地址" />
                </template>
              </article>
            </div>
          </el-collapse-item>

          <el-collapse-item name="links" title="公开链接">
            <div v-for="(link, index) in profile.socialLinks" :key="index" class="social-editor">
              <el-select v-model="link.platform"
                ><el-option
                  v-for="item in platforms"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              /></el-select>
              <el-input v-model="link.label" placeholder="显示名称（可选）" />
              <el-input v-model="link.url" placeholder="https://" />
              <el-checkbox v-model="link.enabled">显示</el-checkbox>
              <el-button link type="danger" @click="profile.socialLinks.splice(index, 1)"
                >删除</el-button
              >
            </div>
            <el-button :disabled="profile.socialLinks.length >= 10" @click="addLink"
              >添加公开链接</el-button
            >
          </el-collapse-item>
        </el-collapse>
      </aside>

      <main class="profile-editor__preview">
        <div class="profile-editor__preview-label">
          <span>实时预览</span><small>保存草稿不会立即对其他成员生效</small>
        </div>
        <MemberProfileRenderer :profile="previewProfile" />
      </main>
    </div>
  </div>
  <div v-else class="profile-editor__loading"><el-skeleton :rows="10" animated /></div>
</template>

<script lang="ts" setup>
import MemberProfileRenderer from '@/components/site/member/MemberProfileRenderer.vue'
import { memberProfileApi } from '@/api'
import type { MemberProfile, ProfileModule } from '@/types/member-profile'
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onMounted, ref } from 'vue'

const profile = ref<MemberProfile | null>(null)
const skillsText = ref('')
const showDepartment = ref(true)
const showPosition = ref(true)
const saving = ref(false)
const publishing = ref(false)
const activePanels = ref(['identity', 'appearance', 'modules'])
const moduleCatalog = [
  { key: 'about', title: '关于我' },
  { key: 'hero_statement', title: '动态宣言' },
  { key: 'now', title: '最近在做' },
  { key: 'skills', title: '技能标签' },
  { key: 'tech_stack', title: '技术栈矩阵' },
  { key: 'blog_latest', title: '最近文章' },
  { key: 'projects', title: '项目作品' },
  { key: 'featured_work', title: '精选作品' },
  { key: 'timeline', title: '经历时间线' },
  { key: 'club_experience', title: '社团经历' },
  { key: 'awards', title: '荣誉与奖项' },
  { key: 'stats', title: '个人数据' },
  { key: 'gallery', title: '图片画廊' },
  { key: 'quote', title: '大字引用' },
  { key: 'links', title: '公开链接' },
  { key: 'contact_cta', title: '联系入口' },
  { key: 'markdown', title: '自定义 Markdown' },
]
const itemEditorConfig: Record<string, { placeholder: string; fields: string[] }> = {
  now: {
    placeholder: '每行一项：标签 | 当前内容 | 补充说明',
    fields: ['label', 'value', 'description'],
  },
  tech_stack: { placeholder: '每行一组：前端 | Vue, TypeScript, GSAP', fields: ['title', 'items'] },
  projects: {
    placeholder: '每行一个项目：名称 | 描述 | https://链接',
    fields: ['name', 'description', 'url'],
  },
  featured_work: {
    placeholder: '每行一个精选作品：名称 | 描述 | https://链接',
    fields: ['name', 'description', 'url'],
  },
  timeline: {
    placeholder: '每行一段经历：2026 | 标题 | 描述',
    fields: ['date', 'title', 'description'],
  },
  club_experience: {
    placeholder: '每行一段社团经历：2026 | 职位或事件 | 描述',
    fields: ['date', 'title', 'description'],
  },
  awards: {
    placeholder: '每行一项荣誉：2026 | 奖项名称 | 颁发方',
    fields: ['year', 'title', 'issuer'],
  },
  stats: {
    placeholder: '每行一个指标：12 | 开源项目 | 持续增长中',
    fields: ['value', 'label', 'description'],
  },
  gallery: { placeholder: '每行一张图片：https://图片地址 | 图片说明', fields: ['url', 'caption'] },
}
const platforms = [
  { value: 'website', label: '个人网站' },
  { value: 'github', label: 'GitHub' },
  { value: 'gitee', label: 'Gitee' },
  { value: 'bilibili', label: '哔哩哔哩' },
  { value: 'zhihu', label: '知乎' },
  { value: 'weibo', label: '微博' },
  { value: 'other', label: '其他' },
]

const sortedModules = computed(() =>
  [...(profile.value?.modules || [])].sort((a, b) => a.sortOrder - b.sortOrder),
)
const availableModules = computed(() =>
  moduleCatalog.filter((item) => !profile.value?.modules.some((module) => module.key === item.key)),
)
const statusLabel = computed(() => (profile.value?.status === 'published' ? '已发布' : '草稿'))
const previewProfile = computed(() => ({
  ...profile.value!,
  skills: parseSkills(),
  modules: sortedModules.value.map((module) => ({
    ...module,
    data: {
      ...moduleConfig(module),
      ...(module.data?.articles ? { articles: module.data.articles } : {}),
    },
  })),
}))

function parseSkills() {
  return skillsText.value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 20)
}
function moduleLabel(key: string) {
  return moduleCatalog.find((item) => item.key === key)?.title || key
}
function hydrateModule(module: ProfileModule) {
  module.data ||= {}
  const editor = itemEditorConfig[module.key]
  if (editor && !module.data.editorText) {
    const sourceKey = module.key === 'tech_stack' ? 'groups' : 'items'
    module.data.editorText = (Array.isArray(module.data[sourceKey]) ? module.data[sourceKey] : [])
      .map((item: any) =>
        editor.fields
          .map((field) =>
            field === 'items' && Array.isArray(item[field]) ? item[field].join(', ') : item[field],
          )
          .filter(Boolean)
          .join(' | '),
      )
      .join('\n')
  }
  return module
}
function moduleConfig(module: ProfileModule) {
  const data = { ...(module.data || {}) }
  const editor = itemEditorConfig[module.key]
  if (editor) {
    const items = String(data.editorText || '')
      .split('\n')
      .map((line) => line.split('|').map((item) => item.trim()))
      .filter((parts) => parts[0])
      .map((parts) =>
        Object.fromEntries(
          editor.fields.map((field, index) => [
            field,
            field === 'items'
              ? String(parts[index] || '')
                  .split(/[,，]/)
                  .map((item) => item.trim())
                  .filter(Boolean)
              : parts[index] || '',
          ]),
        ),
      )
    data[module.key === 'tech_stack' ? 'groups' : 'items'] = items
  }
  delete data.editorText
  delete data.articles
  return data
}
function addModule(key: string) {
  profile.value?.modules.push(
    hydrateModule({
      key,
      title: moduleLabel(key),
      sortOrder: (profile.value.modules.length + 1) * 10,
      columnSpan: 12,
      enabled: true,
      visibility: 'members',
      data: {},
    }),
  )
}
function removeModule(key: string) {
  if (profile.value)
    profile.value.modules = profile.value.modules.filter((item) => item.key !== key)
}
function moveModule(index: number, direction: number) {
  const items = sortedModules.value
  const target = index + direction
  if (target < 0 || target >= items.length) return
  ;[items[index], items[target]] = [items[target], items[index]]
  items.forEach((item, order) => {
    item.sortOrder = order * 10
  })
}
function addLink() {
  profile.value?.socialLinks.push({
    platform: 'website',
    label: '',
    url: '',
    sortOrder: profile.value.socialLinks.length * 10,
    enabled: true,
  })
}

async function loadProfile() {
  const data: MemberProfile = await memberProfileApi.mine()
  data.modules = (data.modules || []).map(hydrateModule)
  data.socialLinks ||= []
  profile.value = data
  skillsText.value = (data.skills || []).join(', ')
  showDepartment.value = data.showDepartment !== false
  showPosition.value = data.showPosition !== false
}
function payload() {
  return {
    ...profile.value!,
    skills: parseSkills(),
    showDepartment: showDepartment.value,
    showPosition: showPosition.value,
    modules: sortedModules.value.map((module, index) => ({
      key: module.key,
      title: module.title,
      sortOrder: index * 10,
      columnSpan: module.columnSpan,
      enabled: module.enabled,
      visibility: module.visibility,
      config: moduleConfig(module),
    })),
    socialLinks: profile
      .value!.socialLinks.filter((link) => link.url.trim())
      .map((link, index) => ({ ...link, sortOrder: index * 10 })),
  }
}
async function saveDraft(showMessage = true) {
  if (!profile.value) return false
  saving.value = true
  try {
    const saved = await memberProfileApi.save(payload())
    saved.modules = (saved.modules || []).map(hydrateModule)
    profile.value = saved
    skillsText.value = saved.skills.join(', ')
    if (showMessage) ElMessage.success('草稿已保存')
    return true
  } catch {
    return false
  } finally {
    saving.value = false
  }
}
async function publish() {
  if (!(await saveDraft(false))) return
  publishing.value = true
  try {
    profile.value = await memberProfileApi.publish()
    profile.value.modules = profile.value.modules.map(hydrateModule)
    ElMessage.success('主页已发布')
  } finally {
    publishing.value = false
  }
}
async function unpublish() {
  publishing.value = true
  try {
    profile.value = await memberProfileApi.unpublish()
    profile.value.modules = profile.value.modules.map(hydrateModule)
    ElMessage.success('主页已下线')
  } finally {
    publishing.value = false
  }
}
async function uploadImage(kind: 'avatar' | 'banner' | 'card-background', event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !profile.value) return
  try {
    const uploaded = await memberProfileApi.upload(kind, file)
    const fresh: MemberProfile = await memberProfileApi.mine()
    profile.value.version = fresh.version
    if (kind === 'avatar') profile.value.avatarUrl = uploaded.url
    if (kind === 'banner') profile.value.bannerUrl = uploaded.url
    if (kind === 'card-background') profile.value.cardBackgroundUrl = uploaded.url
    ElMessage.success('图片已上传')
  } finally {
    input.value = ''
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-editor {
  min-height: 100vh;
  background: var(--oa-page-soft-bg);
}
.profile-editor__topbar {
  position: sticky;
  z-index: 30;
  top: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 66px;
  padding: 0 22px;
  background: color-mix(in srgb, var(--oa-elevated-bg) 90%, transparent);
  border-bottom: 1px solid var(--oa-border);
  backdrop-filter: blur(18px);
}
.profile-editor__topbar > div {
  display: flex;
  align-items: center;
  gap: 8px;
}
.profile-editor__status {
  padding: 5px 9px;
  color: #92400e;
  background: #fffbeb;
  border-radius: 999px;
  font-size: 12px;
}
.profile-editor__status.is-published {
  color: #166534;
  background: #f0fdf4;
}
.profile-editor__workspace {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  min-height: calc(100vh - 126px);
}
.profile-editor__panel {
  padding: 18px;
  background: var(--oa-elevated-bg);
  border-right: 1px solid var(--oa-border);
}
.profile-editor__panel label {
  display: grid;
  gap: 7px;
  margin: 0 0 17px;
  color: var(--oa-text-soft);
  font-size: 13px;
  font-weight: 700;
}
.profile-editor__panel :deep(.el-select) {
  width: 100%;
}
.slug-input {
  display: flex;
  align-items: center;
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}
.slug-input > span {
  padding: 0 0 0 10px;
  color: var(--oa-muted);
  font-weight: 400;
}
.slug-input :deep(.el-input__wrapper) {
  box-shadow: none;
}
.theme-options {
  display: flex;
}
.image-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
  margin-bottom: 16px;
}
.image-upload {
  display: grid !important;
  place-items: center;
  min-height: 44px;
  margin: 0 !important;
  padding: 8px;
  text-align: center;
  cursor: pointer;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}
.image-upload input {
  display: none;
}
.profile-editor__hint {
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.6;
}
.module-library {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 14px;
}
.module-library button {
  padding: 7px 9px;
  color: var(--oa-text);
  cursor: pointer;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}
.module-editor,
.social-editor {
  display: grid;
  gap: 9px;
  margin-bottom: 10px;
  padding: 12px;
  background: var(--oa-page-soft-bg);
  border: 1px solid var(--oa-divider);
  border-radius: 10px;
}
.module-editor__head,
.module-editor__options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 7px;
}
.module-editor__head > div {
  display: flex;
}
.module-editor__options :deep(.el-select) {
  width: 98px;
}
.profile-editor__preview {
  min-width: 0;
  padding: 28px;
  overflow: auto;
}
.profile-editor__preview-label {
  display: flex;
  justify-content: space-between;
  margin: 0 auto 14px;
  max-width: 1120px;
  color: var(--oa-muted);
}
.profile-editor__preview-label span {
  color: var(--oa-text);
  font-weight: 800;
}
.profile-editor__preview :deep(.profile-renderer) {
  max-width: 1120px;
  margin: 0 auto;
}
.profile-editor__loading {
  max-width: 1000px;
  margin: 100px auto;
}
@media (max-width: 980px) {
  .profile-editor__workspace {
    grid-template-columns: 1fr;
  }
  .profile-editor__panel {
    border-right: 0;
    border-bottom: 1px solid var(--oa-border);
  }
}
@media (max-width: 640px) {
  .profile-editor__topbar {
    top: 60px;
    height: auto;
    padding: 10px;
    flex-wrap: wrap;
  }
  .profile-editor__preview {
    padding: 14px;
  }
  .image-actions {
    grid-template-columns: 1fr;
  }
  .profile-editor__preview-label small {
    display: none;
  }
}
</style>
