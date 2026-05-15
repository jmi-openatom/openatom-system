<script lang="ts" setup>
import {
  AppleCard,
  AppleCardCarousel,
  AppleCarouselItem,
} from "@/components/ui/apple-card-carousel";

defineProps<{
  club: Record<string, any>
  focusAreas: any[]
  loading: boolean
}>()

const data = [
  {
    category: "项目部 - 项目实践",
    title: "参与真实项目积累开发经验",
    src: "https://picture-api.jmi-openatom.cn/uploads/f0af197b-fbe7-4293-9e1b-dd74eb1a17b5.png",
    description: "从需求分析到上线部署，全程参与实际项目的设计与开发，在团队协作中快速成长。",
  },
  {
    category: "活动部 - 技术活动",
    title: "技术竞赛与社团活动",
    src: "https://picture-api.jmi-openatom.cn/uploads/16977dd4-c695-4767-892f-e2b66133616d.png",
    description: "定期组织技术分享、Hackathon 和各类赛事，激发创意，展示你的技术实力。",
  },
  {
    category: "宣传部 - 宣传设计",
    title: "用设计传递社团声音",
    src: "https://picture-api.jmi-openatom.cn/uploads/d1a0e369-7084-48e4-9871-5165e681c436.png",
    description: "负责社团品牌视觉、海报、推文等宣传物料的设计与制作，提升社团影响力。",
  },
  {
    category: "外联部 - 对外合作",
    title: "连接校内外的技术与资源",
    src: "https://picture-api.jmi-openatom.cn/uploads/b05af3e0-3aec-4330-8507-cd51d9dcd1b6.png",
    description: "对接企业、兄弟社团和开源社区，争取赞助与合作机会，拓宽成员视野。",
  },
]
</script>

<template>
  <section class="container section club-brief">
    <div class="section-heading reveal-block">
      <span>社团概览</span>
      <h2>{{ club.name || "社团" }}的主要方向</h2>
      <p>左右滑动浏览社团各大方向，点击卡片查看详细介绍。</p>
    </div>

    <el-empty v-if="!focusAreas.length && !loading" description="暂无社团部门数据" />

    <div class="w-full overflow-hidden">
      <AppleCardCarousel>
        <AppleCarouselItem
          v-for="(card, index) in data"
          :key="index"
          :index="index"
        >
          <AppleCard
            :card="card"
            :index="index"
            :layout="true"
          >
            <div
              :key="`dummy-content${index}`"
              class="mb-4 rounded-3xl bg-[#F5F5F7] p-8 md:p-14 dark:bg-neutral-800 "
            >
              <p class="mx-auto max-w-3xl font-sans text-base text-neutral-600 md:text-2xl dark:text-neutral-400">
                {{ card.description }}
              </p>
              <img
                :src="card.src"
                :alt="card.title"
                height="500"
                width="500"
                class="mx-auto mt-8 size-full rounded-2xl object-cover md:size-2/3"
              />
            </div>
          </AppleCard>
        </AppleCarouselItem>
      </AppleCardCarousel>
    </div>
  </section>
</template>
