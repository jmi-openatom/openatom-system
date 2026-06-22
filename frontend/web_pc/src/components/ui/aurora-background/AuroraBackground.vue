<script setup lang="ts">
import { cn } from '@inspira-ui/plugins'
import { computed } from 'vue'

interface AuroraBackgroundProps {
  radialGradient?: boolean
  class?: string
}

const props = withDefaults(defineProps<AuroraBackgroundProps>(), {
  radialGradient: true,
})

const styles = computed(() => {
  return {
    '--aurora':
      'repeating-linear-gradient(100deg,#1d1d1f_10%,#6e6e73_15%,#a1a1a6_20%,#d2d2d7_25%,#8e8e93_30%)',
    '--dark-gradient':
      'repeating-linear-gradient(100deg,#000_0%,#000_7%,transparent_10%,transparent_12%,#000_16%)',
    '--white-gradient':
      'repeating-linear-gradient(100deg,#fff_0%,#fff_7%,transparent_10%,transparent_12%,#fff_16%)',

    '--blue-300': '#a1a1a6',
    '--blue-400': '#8e8e93',
    '--blue-500': '#1d1d1f',
    '--indigo-300': '#6e6e73',
    '--violet-200': '#ddd6fe',
    '--black': '#000',
    '--white': '#fff',
    '--transparent': 'transparent',
    '--animate-aurora': 'aurora 60s linear infinite',
  }
})
</script>

<template>
  <main>
    <div
      v-bind="props"
      :class="
        cn(
          `transition-bg relative flex h-screen flex-col items-center justify-center bg-zinc-50 text-slate-950 dark:bg-zinc-900`,
          props.class,
        )
      "
    >
      <div :style="styles" class="absolute inset-0 overflow-hidden">
        <div
          :class="
            cn(
              `after:animate-aurora pointer-events-none absolute -inset-2.5 [background-image:var(--white-gradient),var(--aurora)] bg-size-[300%,200%] bg-position-[50%_50%,50%_50%] opacity-50 blur-[10px] invert filter will-change-transform [--aurora:repeating-linear-gradient(100deg,var(--blue-500)_10%,var(--indigo-300)_15%,var(--blue-300)_20%,var(--violet-200)_25%,var(--blue-400)_30%)] [--dark-gradient:repeating-linear-gradient(100deg,var(--black)_0%,var(--black)_7%,var(--transparent)_10%,var(--transparent)_12%,var(--black)_16%)] [--white-gradient:repeating-linear-gradient(100deg,var(--white)_0%,var(--white)_7%,var(--transparent)_10%,var(--transparent)_12%,var(--white)_16%)] after:absolute after:inset-0 after:[background-image:var(--white-gradient),var(--aurora)] after:bg-size-[200%,100%] after:bg-fixed after:mix-blend-difference after:content-[''] dark:[background-image:var(--dark-gradient),var(--aurora)] dark:invert-0 after:dark:[background-image:var(--dark-gradient),var(--aurora)]`,
              props.radialGradient &&
                `mask-[radial-gradient(ellipse_at_100%_0%,black_10%,var(--transparent)_70%)]`,
            )
          "
        />
      </div>
      <slot />
    </div>
  </main>
</template>
