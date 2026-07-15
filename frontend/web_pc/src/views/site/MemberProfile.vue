<template>
  <ViewPage :loading="loading" class="member-profile-page">
    <div class="container member-profile-page__toolbar">
      <el-button text @click="$router.push('/members')">← 返回成员列表</el-button>
      <div v-if="profile?.owner">
        <el-tag v-if="profile.status !== 'published'" effect="plain">{{
          profile.status === 'default' ? '默认主页' : '草稿'
        }}</el-tag>
        <el-button @click="$router.push('/settings/account')">账号与安全</el-button>
        <el-button type="primary" @click="$router.push('/profile/edit')">编辑主页</el-button>
      </div>
    </div>
    <div v-if="profile" class="container member-profile-page__content">
      <MemberProfileRenderer :profile="profile" />
      <p v-if="profile.owner && profile.status !== 'published'" class="member-profile-page__notice">
        当前展示的是你的草稿预览；其他成员暂时看到系统默认主页。
      </p>
    </div>
    <el-result
      v-else-if="!loading"
      icon="warning"
      sub-title="该成员主页不存在或不可访问"
      title="无法打开主页"
    >
      <template #extra
        ><el-button type="primary" @click="$router.push('/members')"
          >查看成员列表</el-button
        ></template
      >
    </el-result>
  </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import MemberProfileRenderer from '@/components/site/member/MemberProfileRenderer.vue'
import { memberProfileApi } from '@/api'
import type { MemberProfile } from '@/types/member-profile'
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const loading = ref(false)
const profile = ref<MemberProfile | null>(null)

async function fetchProfile() {
  loading.value = true
  profile.value = null
  try {
    profile.value =
      route.path === '/profile'
        ? await memberProfileApi.mine()
        : await memberProfileApi.detail(String(route.params.slug || ''))
  } finally {
    loading.value = false
  }
}

onMounted(fetchProfile)
watch(() => route.fullPath, fetchProfile)
</script>

<style scoped>
.member-profile-page {
  min-height: 100vh;
  padding: 26px 0 80px;
  background: var(--oa-page-soft-bg);
}
.member-profile-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}
.member-profile-page__toolbar > div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.member-profile-page__notice {
  margin: 18px 0 0;
  padding: 14px 18px;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
}
@media (max-width: 620px) {
  .member-profile-page__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
