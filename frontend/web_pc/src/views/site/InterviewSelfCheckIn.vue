<template>
  <main class="self-check-in">
    <section class="check-in-card" aria-labelledby="check-in-title">
      <p class="eyebrow">OPENATOM INTERVIEW</p>
      <template v-if="!success">
        <h1 id="check-in-title">面试签到</h1>
        <p class="intro">请在到达候场区后输入报名时填写的学号，完成现场签到。</p>

        <el-form class="check-in-form" @submit.prevent="submit">
          <label for="student-id">学号</label>
          <el-input
            id="student-id"
            v-model.trim="studentId"
            size="large"
            autocomplete="off"
            maxlength="32"
            placeholder="请输入学号"
            :disabled="submitting"
            @blur="validateStudentId"
          />
          <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>
          <el-button class="submit-button" type="primary" native-type="submit" size="large" :loading="submitting">
            {{ submitting ? '正在签到' : '确认签到' }}
          </el-button>
        </el-form>
        <p class="hint">如签到失败，请核对学号或联系现场工作人员。</p>
      </template>

      <section v-else class="success-state" aria-live="polite">
        <div class="success-mark" aria-hidden="true">✓</div>
        <h1 id="check-in-title">签到成功</h1>
        <p>你已进入候场队列，请在候场区等候并留意叫号大屏。</p>
        <el-button plain size="large" @click="reset">为其他候选人签到</el-button>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { interviewSessionApi } from '@/api'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const studentId = ref('')
const errorMessage = ref('')
const submitting = ref(false)
const success = ref(false)

function validateStudentId() {
  errorMessage.value = studentId.value.trim() ? '' : '请输入学号'
  return !errorMessage.value
}

async function submit() {
  if (!validateStudentId() || submitting.value) return
  submitting.value = true
  try {
    await interviewSessionApi.selfCheckIn(String(route.params.sessionId), studentId.value.trim())
    success.value = true
    ElMessage.success('签到成功')
  } catch (error: any) {
    errorMessage.value = error?.message || '签到失败，请稍后重试或联系现场工作人员'
  } finally {
    submitting.value = false
  }
}

function reset() {
  studentId.value = ''
  errorMessage.value = ''
  success.value = false
}
</script>

<style scoped>
.self-check-in {
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 8% 8%, rgba(37, 99, 235, 0.13), transparent 30%),
    linear-gradient(145deg, #f7f9ff 0%, #fff 52%, #f7f4ff 100%);
  color: #0f172a;
}
.check-in-card {
  width: min(100%, 520px);
  padding: clamp(32px, 7vw, 56px);
  border: 1px solid #e4ecfc;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.1);
}
.eyebrow {
  margin: 0 0 16px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}
h1 { margin: 0; font-size: clamp(36px, 8vw, 52px); line-height: 1; letter-spacing: -0.06em; }
.intro, .hint, .success-state p { color: #64748b; line-height: 1.7; }
.intro { margin: 18px 0 30px; }
.check-in-form { display: grid; gap: 10px; }
.check-in-form label { color: #334155; font-size: 15px; font-weight: 700; }
.check-in-form :deep(.el-input__wrapper) { min-height: 52px; border-radius: 12px; }
.submit-button { width: 100%; min-height: 52px; margin-top: 14px; border-radius: 12px; font-weight: 700; }
.field-error { margin: 0; color: #b42318; font-size: 14px; }
.hint { margin: 20px 0 0; font-size: 14px; }
.success-state { display: grid; gap: 18px; }
.success-state p { margin: 0; }
.success-state :deep(.el-button) { justify-self: start; min-height: 44px; border-radius: 10px; }
.success-mark { display: grid; width: 52px; height: 52px; place-items: center; border-radius: 50%; background: #dcfce7; color: #15803d; font-size: 30px; font-weight: 800; }
@media (max-width: 480px) { .self-check-in { padding: 16px; } .check-in-card { padding: 32px 24px; border-radius: 22px; } }
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto !important; transition: none !important; } }
</style>
