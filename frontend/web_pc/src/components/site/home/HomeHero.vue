<template>

    <section id="overview" class="hero">
        <div aria-hidden="true" class="hero__mesh"></div>
        <div class="container hero__inner">
            <div class="hero__content">
                <MorphingText
                    class="home-hero__morph"
                    :cool-down-time="coolDownTime"
                    :morph-time="morphTime"
                    :texts="texts"
                    style="width: 50%"
                />

            </div>
        </div>
    </section>




</template><script setup lang="ts">
import { MorphingText } from '@/components/ui/morphing-text'
import EncryptedText from "../../ui/encrypted-text/EncryptedText.vue";
import type { BaseProps } from "../../ui/pattern-background";
import { PATTERN_BACKGROUND_SPEED } from "../../ui/pattern-background";
import { Motion } from "motion-v";

// 1. Define a single interface for all props
interface HeroProps extends BaseProps {
    text?: string;
    revealDelayMs?: number;
    coolDownTime?: number;
    morphTime?: number;
    texts: string[];
}

// 2. Use withDefaults to handle your default values cleanly
const props = withDefaults(defineProps<HeroProps>(), {
    text: "技术分享 · 项目实践 · 竞赛训练 · 开源协作",
    revealDelayMs: 100,
    animate: true,
    direction: "bottom",
    variant: "big-dot",
    size: "md",
    mask: "ellipse-top",
    speed: () => PATTERN_BACKGROUND_SPEED.Slow, // Use a factory function for imported constants/objects
});
</script>