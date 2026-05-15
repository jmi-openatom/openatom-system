<template>
  <section id="people" class="container section people-section">
    <div class="section-heading reveal-block">
      <span>成员信息</span>
      <h2>主要人员</h2>
      <p>{{ people.length ? `当前展示 ${people.length} 位成员` : '暂无成员信息' }}</p>
    </div>
    <el-empty v-if="!people.length && !loading" description="暂无主要人员数据" />
    <div v-if="people.length" class="people-marquee-wrapper">
      <Marquee pause-on-hover class="[--duration:20s] [--gap:6rem]">
        <ReviewCard
          v-for="person in firstRow"
          :key="person.userId || person.name"
          :img="person.img"
          :name="person.name"
          :username="person.username"
          :body="person.body"
          style="margin-left: 10px;"
        />
      </Marquee>
      <Marquee reverse pause-on-hover class="[--duration:20s] [--gap:6rem]">
        <ReviewCard
          v-for="person in secondRow"
          :key="person.userId || person.name"
          :img="person.img"
          :name="person.name"
          :username="person.username"
          :body="person.body"
          style="margin-left: 10px;"
        />
      </Marquee>
      <div class="people-marquee-gradient people-marquee-gradient--left" />
      <div class="people-marquee-gradient people-marquee-gradient--right" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Marquee, ReviewCard } from '@/components/ui/marquee'

const props = defineProps<{
  people: any[]
  loading: boolean
}>()

const mappedPeople = computed(() =>
  props.people.map((person) => ({
    userId: person.userId,
    name: person.name,
    img: `https://avatar.vercel.sh/${encodeURIComponent(person.name || 'user')}`,
    username: person.role || '',
    body: person.focus || '',
  })),
)

const firstRow = computed(() =>
  mappedPeople.value.slice(0, mappedPeople.value.length / 2),
)
const secondRow = computed(() =>
  mappedPeople.value.slice(mappedPeople.value.length / 2),
)
</script>

<style scoped>
.people-marquee-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  width: 100%;
  margin-top: 32px;
}

.people-marquee-gradient {
  pointer-events: none;
  position: absolute;
  inset-y: 0;
  width: 33.333%;
  z-index: 1;
}

.people-marquee-gradient--left {
  left: 0;
  background: linear-gradient(to right, #ffffff, transparent);
}

.people-marquee-gradient--right {
  right: 0;
  background: linear-gradient(to left, #ffffff, transparent);
}
</style>
