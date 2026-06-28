<template>
  <ViewPage :loading="loading" class="alumni-page">
    <section class="alumni-hero home-interactive-section">
      <div class="container alumni-hero__inner">
        <div class="section-heading reveal-block">
          <span>Alumni</span>
          <h1>往届管理人员</h1>
          <p>感谢曾经为社团发展做出贡献的每一位管理人员。</p>
        </div>
      </div>
    </section>

    <section class="alumni-list-section home-interactive-section">
      <div class="container alumni-list">
        <div v-if="!loading && !alumniManagers.length" class="alumni-empty">
          <el-empty description="暂无往届管理人员数据" />
        </div>

        <template v-else>
          <div v-for="group in groupedAlumni" :key="group.name" class="alumni-group">
            <div class="alumni-group__header reveal-block">
              <h2>{{ group.name }}</h2>
              <span>{{ group.members.length }} 人</span>
            </div>
            <div class="alumni-grid">
              <article
                v-for="(person, index) in group.members"
                :key="person.userId || person.name"
                class="alumni-card reveal-card"
              >
                <span class="alumni-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
                <div class="alumni-card__portrait">
                  <UserAvatar
                    :fallback-src="person.qqAvatar"
                    :name="person.name"
                    :size="80"
                    :src="person.avatar"
                  />
                </div>
                <div class="alumni-card__meta">
                  <strong>{{ person.name }}</strong>
                  <span class="alumni-card__role">{{ person.role || '管理人员' }}</span>
                  <small class="alumni-card__dept">{{ person.focus || '' }}</small>
                </div>
              </article>
            </div>
          </div>
        </template>
      </div>
    </section>
  </ViewPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { siteApi } from '@/api'
import ViewPage from '@/components/common/ViewPage.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

const loading = ref(false)
const alumniManagers = ref<any[]>([])

const groupedAlumni = computed(() => {
  const map = new Map<string, any[]>()
  for (const person of alumniManagers.value) {
    const group = person.alumniGroup || '其他'
    if (!map.has(group)) map.set(group, [])
    map.get(group)!.push(person)
  }
  return [...map.entries()].map(([name, members]) => ({ name, members }))
})

async function loadAlumniManagers() {
  loading.value = true
  try {
    const data = await siteApi.clubHome()
    alumniManagers.value = data.alumniManagers || []
  } catch {
    alumniManagers.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAlumniManagers()
})
</script>

<style scoped>
.alumni-page {
  animation: none;
  overflow: hidden;
  background: var(--oa-elevated-bg);
}

.alumni-hero {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  background: var(--oa-page-soft-bg);
  padding: 100px 0 60px;
}

.alumni-hero__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.alumni-list-section {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  padding: 60px 0 100px;
  background: var(--oa-elevated-bg);
}

.alumni-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 64px;
}

.alumni-empty {
  width: min(440px, 100%);
  margin: 40px auto 0;
}

.alumni-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.alumni-group__header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--oa-border);
  width: min(1180px, 100%);
}

.alumni-group__header h2 {
  color: var(--oa-text);
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  margin: 0;
}

.alumni-group__header span {
  color: var(--oa-muted);
  font-size: 14px;
}

.alumni-grid {
  display: grid;
  width: min(1180px, 100%);
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  margin: 0 auto;
}

.alumni-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 24px;
  border: 1px solid var(--oa-border);
  border-radius: 20px;
  background: var(--oa-elevated-bg);
  text-align: center;
  transition:
    transform 0.26s ease,
    border-color 0.26s ease,
    box-shadow 0.26s ease;
}

.alumni-card:hover {
  transform: translateY(-6px);
  border-color: var(--oa-text);
  box-shadow: 0 20px 48px rgba(29, 29, 31, 0.12);
}

.alumni-card__index {
  color: var(--oa-faint);
  font-size: 12px;
  letter-spacing: 0.24em;
}

.alumni-card__portrait {
  display: grid;
  width: 80px;
  height: 80px;
  place-items: center;
  overflow: hidden;
  border-radius: 999px;
  background: var(--oa-elevated-bg);
  filter: saturate(0.92);
  transition:
    filter 0.28s ease,
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.alumni-card:hover .alumni-card__portrait {
  filter: saturate(1.06);
  transform: scale(1.08);
}

.alumni-card__meta {
  display: grid;
  gap: 6px;
}

.alumni-card__meta strong {
  color: var(--oa-text);
  font-family: 'SF Pro Display', system-ui, sans-serif;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
}

.alumni-card__role {
  color: var(--oa-muted-strong);
  font-size: 14px;
}

.alumni-card__dept {
  color: var(--oa-muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  .alumni-hero {
    padding: 80px 0 40px;
  }

  .alumni-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }

  .alumni-card {
    padding: 22px 16px;
  }

  .alumni-card__portrait {
    width: 64px;
    height: 64px;
  }

  .alumni-card__meta strong {
    font-size: 16px;
  }
}
</style>
