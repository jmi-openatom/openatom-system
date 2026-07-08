<script lang="ts" setup>
import {
  AppleCard,
  AppleCardCarousel,
  AppleCarouselItem,
} from "@/components/ui/apple-card-carousel";
import HomeInteractiveBackdrop from './HomeInteractiveBackdrop.vue'

defineProps<{
  club: Record<string, any>
  focusAreas: any[]
  loading: boolean
}>()

const data = [
  {
    category: "项目部 - 项目实践",
    title: "参与真实项目积累开发经验",
    src: "/project.png",
    description: "从需求分析到上线部署，全程参与实际项目的设计与开发，在团队协作中快速成长。",
  },
  {
    category: "活动部 - 技术活动",
    title: "技术竞赛与社团活动",
    src: "/activity.png",
    description: "定期组织技术分享、Hackathon 和各类赛事，激发创意，展示你的技术实力。",
  },
  {
    category: "宣传部 - 宣传设计",
    title: "用设计传递社团声音",
    src: "/propaganda.png",
    description: "负责社团品牌视觉、海报、推文等宣传物料的设计与制作，提升社团影响力。",
  },
  {
    category: "外联部 - 对外合作",
    title: "连接校内外的技术与资源",
    src: "/external.png",
    description: "对接企业、兄弟社团和开源社区，争取赞助与合作机会，拓宽成员视野。",
  },
  {
    category: "社区部 - 社区运营",
    title: "共建开源社区生态",
    src: "/community.png",
    description: "负责开源社区运营、内容输出与社群维护，搭建校园与开源生态的桥梁，让成员融入更大的技术共同体。",
  },
]
</script>

<template>
  <section class="container section club-brief home-interactive-section">
    <HomeInteractiveBackdrop :radius="210" :spacing="62" :strength="18" />
    <div class="section-heading reveal-block">
      <span>社团概览</span>
      <h2>{{ club.name || "社团" }}的主要方向</h2>
      <p>左右滑动浏览社团各大方向，点击卡片查看详细介绍。</p>
    </div>

    <div class="focus-carousel w-full overflow-hidden">
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
