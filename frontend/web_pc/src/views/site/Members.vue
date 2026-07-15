<template>
  <ViewPage :loading="loading" class="members-page">
    <section class="members-hero">
      <div class="container members-hero__inner">
        <div>
          <span>MEMBER DIRECTORY</span>
          <h1>认识一起创造的人</h1>
          <p>
            成员主页只对社团内部开放。浏览大家的方向、项目和技术文章，也把自己的经历整理成一张长期名片。
          </p>
        </div>
        <el-button size="large" type="primary" @click="$router.push('/profile/edit')"
          >定制我的主页</el-button
        >
      </div>
    </section>

    <section class="container members-content">
      <div class="members-toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索成员、方向或简介"
          size="large"
          @keyup.enter="search"
        >
          <template #prefix
            ><el-icon><Search /></el-icon
          ></template>
        </el-input>
        <el-button size="large" @click="search">搜索</el-button>
        <el-select
          v-model="departmentId"
          clearable
          placeholder="全部部门"
          size="large"
          @change="search"
        >
          <el-option
            v-for="item in filters.departments"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-model="skill"
          clearable
          filterable
          placeholder="全部方向"
          size="large"
          @change="search"
        >
          <el-option v-for="item in filters.skills" :key="item" :label="item" :value="item" />
        </el-select>
        <span>{{ pageData.total }} 位成员</span>
      </div>

      <el-alert
        v-if="loadFailed"
        :closable="false"
        show-icon
        title="成员列表加载失败，请检查网络后重试"
        type="error"
      >
        <template #default>
          <el-button link type="primary" @click="fetchMembers">重新加载</el-button>
        </template>
      </el-alert>

      <div v-if="loading" class="members-grid" aria-label="成员列表加载中">
        <el-skeleton v-for="item in 6" :key="item" :rows="4" animated class="members-skeleton" />
      </div>
      <div v-else-if="pageData.list.length" class="members-grid">
        <MemberBannerCard v-for="member in pageData.list" :key="member.slug" :member="member" />
      </div>
      <el-empty v-else description="没有找到符合条件的成员" />

      <el-pagination
        v-if="pageData.total > pageData.pageSize"
        v-model:current-page="pageData.page"
        :page-size="pageData.pageSize"
        :total="pageData.total"
        background
        layout="prev, pager, next"
        @current-change="fetchMembers"
      />
    </section>
  </ViewPage>
</template>

<script lang="ts" setup>
import ViewPage from '@/components/common/ViewPage.vue'
import MemberBannerCard from '@/components/site/member/MemberBannerCard.vue'
import { memberProfileApi } from '@/api'
import type { MemberFilters, MemberPage } from '@/types/member-profile'
import { Search } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'

const keyword = ref('')
const departmentId = ref<number | undefined>()
const skill = ref('')
const loading = ref(false)
const loadFailed = ref(false)
const pageData = reactive<MemberPage>({ list: [], page: 1, pageSize: 8, total: 0 })
const filters = reactive<MemberFilters>({ departments: [], skills: [] })

async function fetchMembers() {
  loading.value = true
  loadFailed.value = false
  try {
    const data = await memberProfileApi.members({
      keyword: keyword.value || undefined,
      departmentId: departmentId.value,
      skill: skill.value || undefined,
      page: pageData.page,
      pageSize: pageData.pageSize,
    })
    Object.assign(pageData, data)
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function search() {
  pageData.page = 1
  fetchMembers()
}
onMounted(async () => {
  try {
    Object.assign(filters, await memberProfileApi.filters())
  } catch {
    // 筛选项失败不影响成员列表主流程。
  }
  fetchMembers()
})
</script>

<style scoped>
.members-page {
  min-height: 100vh;
  background: var(--oa-page-soft-bg);
}
.members-hero {
  padding: 86px 0 58px;
  background: linear-gradient(150deg, var(--oa-elevated-bg), var(--oa-page-soft-bg));
  border-bottom: 1px solid var(--oa-divider);
}
.members-hero__inner {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 40px;
}
.members-hero span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
}
.members-hero h1 {
  margin: 12px 0 16px;
  font-size: clamp(38px, 6vw, 74px);
  line-height: 0.98;
  letter-spacing: -0.055em;
}
.members-hero p {
  max-width: 680px;
  margin: 0;
  color: var(--oa-muted);
  font-size: 17px;
  line-height: 1.75;
}
.members-content {
  padding-top: 34px;
  padding-bottom: 80px;
}
.members-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto 180px 180px auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}
.members-toolbar > span {
  justify-self: end;
  color: var(--oa-muted);
}
.members-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.members-skeleton {
  min-height: 210px;
  padding: 28px;
  background: var(--oa-elevated-bg);
  border: 1px solid var(--oa-border);
  border-radius: 18px;
}
.el-pagination {
  justify-content: center;
  margin-top: 36px;
}
@media (max-width: 920px) {
  .members-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 680px) {
  .members-hero__inner {
    align-items: flex-start;
    flex-direction: column;
  }
  .members-toolbar {
    grid-template-columns: 1fr 1fr;
  }
  .members-toolbar > :first-child {
    grid-column: 1 / -1;
  }
  .members-toolbar > span {
    grid-column: 1 / -1;
    justify-self: start;
  }
}
</style>
