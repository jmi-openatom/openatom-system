<template>
  <div class="page-container setup-page">
    <header class="page-heading">
      <p class="eyebrow">MEMBER PROFILE</p>
      <h1>先认识一下你</h1>
      <p>基础学籍资料由 LMS 自动同步，其他信息用于推荐成长路线和安排合适的导师。</p>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="setup-form">
      <section class="content-card form-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">BASIC</p>
            <h2>基本资料</h2>
          </div>
          <span>学籍资料由 LMS 同步</span>
        </div>
        <div class="form-grid">
          <el-form-item label="姓名或社团昵称" prop="nickname"
            ><el-input v-model="form.nickname" maxlength="64" show-word-limit
          /></el-form-item>
          <el-form-item label="头像地址"
            ><el-input v-model="form.avatarUrl" placeholder="https://..."
          /></el-form-item>
          <el-form-item v-for="item in educationFields" :key="item.key" :label="item.label">
            <div class="synced-field">
              <span>{{ form[item.key] || "LMS 暂无数据" }}</span>
              <small>由 LMS 同步</small>
            </div>
          </el-form-item>
        </div>
      </section>

      <section class="content-card form-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">DIRECTION</p>
            <h2>学习方向</h2>
          </div>
        </div>
        <el-form-item label="感兴趣的技术方向" prop="directionIds">
          <el-select
            v-model="form.directionIds"
            multiple
            placeholder="至少选择一个方向"
            style="width: 100%"
          >
            <el-option
              v-for="item in directions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="已掌握的技能">
          <el-select
            v-model="form.skills"
            multiple
            allow-create
            filterable
            default-first-option
            placeholder="选择技能，也可输入后回车添加"
            style="width: 100%"
          >
            <el-option v-for="skill in skillOptions" :key="skill" :label="skill" :value="skill" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="GitHub 或 Gitee 主页"
            ><el-input v-model="form.codeProfileUrl" placeholder="https://github.com/..."
          /></el-form-item>
          <el-form-item label="每周可投入时间" prop="weeklyHours"
            ><el-input-number v-model="form.weeklyHours" :min="0" :max="168" /><span
              class="field-suffix"
              >小时</span
            ></el-form-item
          >
        </div>
        <el-form-item label="个人简介"
          ><el-input v-model="form.bio" type="textarea" :rows="4" maxlength="1000" show-word-limit
        /></el-form-item>
        <el-form-item prop="conductAgreed"
          ><el-checkbox v-model="form.conductAgreed"
            >我已阅读并同意社团成员行为准则</el-checkbox
          ></el-form-item
        >
      </section>

      <div class="form-actions">
        <el-button type="primary" size="large" :loading="saving" @click="submit"
          >保存并开始新人引导</el-button
        >
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { http, type ApiResponse } from "@/api/http";
import { useAuthStore } from "@/stores/auth";

interface Direction {
  id: number;
  name: string;
}
type EducationField = "school" | "college" | "major" | "grade";
const router = useRouter();
const auth = useAuthStore();
const formRef = ref<FormInstance>();
const saving = ref(false);
const directions = ref<Direction[]>([]);
const educationFields: Array<{ key: EducationField; label: string }> = [
  { key: "school", label: "学校" },
  { key: "college", label: "学院" },
  { key: "major", label: "专业" },
  { key: "grade", label: "年级" },
];
const skillOptions = [
  "HTML / CSS",
  "JavaScript",
  "TypeScript",
  "Vue",
  "React",
  "Node.js",
  "Java",
  "Spring Boot",
  "Python",
  "FastAPI",
  "MySQL",
  "Redis",
  "Git",
  "Linux",
  "Docker",
  "GitHub Actions",
  "REST API",
  "OpenHarmony",
  "ArkTS",
  "人工智能",
  "UI/UX",
  "Figma",
  "技术写作",
];
const draftKey = computedDraftKey();
const form = reactive({
  nickname: auth.member?.nickname || "",
  avatarUrl: auth.member?.avatarUrl || "",
  school: "",
  college: "",
  major: "",
  grade: "",
  directionIds: [] as number[],
  skills: [] as string[],
  codeProfileUrl: "",
  weeklyHours: 4,
  bio: "",
  conductAgreed: false,
});
const rules: FormRules = {
  nickname: [{ required: true, message: "请填写姓名或社团昵称", trigger: "blur" }],
  directionIds: [
    { type: "array", required: true, min: 1, message: "至少选择一个技术方向", trigger: "change" },
  ],
  conductAgreed: [
    {
      validator: (_rule, value, callback) =>
        value ? callback() : callback(new Error("请先同意成员行为准则")),
      trigger: "change",
    },
  ],
};

onMounted(async () => {
  const [directionResponse, profileResponse] = await Promise.all([
    http.get<ApiResponse<Direction[]>>("/directions"),
    http.get<ApiResponse<Record<string, unknown>>>("/members/me/profile"),
  ]);
  directions.value = directionResponse.data.data;
  Object.assign(form, profileResponse.data.data);
  const draft = localStorage.getItem(draftKey);
  if (draft) {
    try {
      Object.assign(form, JSON.parse(draft), pickEducation(profileResponse.data.data));
      ElMessage.info("已恢复未提交的资料草稿");
    } catch {
      localStorage.removeItem(draftKey);
    }
  }
});

watch(form, () => localStorage.setItem(draftKey, JSON.stringify(editableProfile())), {
  deep: true,
});

function computedDraftKey() {
  return `quest-profile-draft:${auth.member?.id || "current"}`;
}

function pickEducation(profileData: Record<string, unknown>) {
  return Object.fromEntries(educationFields.map(({ key }) => [key, profileData[key] || ""]));
}

function editableProfile() {
  return {
    nickname: form.nickname,
    avatarUrl: form.avatarUrl,
    directionIds: form.directionIds,
    skills: form.skills,
    codeProfileUrl: form.codeProfileUrl,
    weeklyHours: form.weeklyHours,
    bio: form.bio,
    conductAgreed: form.conductAgreed,
  };
}

async function submit() {
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    await http.put("/members/me/profile", editableProfile());
    localStorage.removeItem(draftKey);
    await auth.resolve(true);
    ElMessage.success("资料已保存");
    await router.push("/onboarding");
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.synced-field {
  width: 100%;
  min-height: 44px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
}

.synced-field span {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.synced-field small {
  flex: 0 0 auto;
  color: var(--color-text-tertiary);
  font-size: 11px;
}
</style>
