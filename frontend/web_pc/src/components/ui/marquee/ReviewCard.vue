<script lang="ts" setup>
interface Props {
  img: string;
  name: string;
  username: string;
  body: string;
}

defineProps<Props>();
</script>

<template>
  <figure
    class="relative w-64 cursor-pointer overflow-hidden rounded-xl border border-gray-950/10 bg-white/60 p-4 shadow-sm backdrop-blur-sm transition-all duration-300 hover:scale-[1.02] hover:border-blue-400/30 hover:bg-blue-50/40 hover:shadow-md dark:border-gray-50/10 dark:bg-gray-50/5 dark:hover:border-blue-400/20 dark:hover:bg-blue-950/20"
  >
    <div class="flex flex-row items-center gap-3">
      <div class="relative shrink-0">
        <img
          :src="img"
          class="size-10 rounded-full object-cover ring-2 ring-blue-100 dark:ring-blue-900/50"
          width="40"
          height="40"
          alt=""
        />
        <span class="absolute -bottom-0.5 -right-0.5 size-3 rounded-full border-2 border-white bg-emerald-400 dark:border-neutral-800" />
      </div>
      <div class="flex flex-col min-w-0">
        <span class="truncate text-sm font-semibold text-neutral-800 dark:text-white">
          {{ name }}
        </span>
        <p class="truncate text-xs font-medium text-blue-500 dark:text-blue-400">
          {{ username }}
        </p>
      </div>
    </div>
    <blockquote class="mt-3 text-xs leading-relaxed text-neutral-500 dark:text-neutral-400">
      {{ body }}
    </blockquote>
  </figure>
</template>
