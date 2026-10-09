<template>
  <div ref="root" class="brand-film">
    <div class="brand-film__frame">
      <video
        ref="video"
        :src="source || undefined"
        poster="/about/brand/film-poster.webp"
        preload="none"
        muted
        loop
        playsinline
        :controls="userStarted"
        aria-label="JMI-OPENATOM 品牌宣传片"
        @play="handlePlay"
        @pause="handlePause"
        @volumechange="syncVolume"
        @timeupdate="updateProgress"
        @error="failed = true"
      ></video>
      <button
        v-if="!playing && !userStarted && !failed"
        class="brand-film__play"
        type="button"
        aria-label="播放 JMI-OPENATOM 宣传片"
        @click="startWithSound"
      >
        <span><Play :size="25" fill="currentColor" aria-hidden="true" /></span>
        <strong>走进我们的故事</strong>
      </button>
      <div v-if="failed" class="brand-film__error" role="status">
        <p>视频暂时无法播放</p>
        <a :href="source" target="_blank" rel="noopener"
          >在新窗口打开宣传片 <ArrowUpRight :size="16"
        /></a>
      </div>
    </div>
    <div class="brand-film__toolbar">
      <div class="brand-film__caption">
        <span class="film-dot" :class="{ 'is-playing': playing }"></span> JMI-OPENATOM
        <span class="film-runtime">/ 01:23</span>
      </div>
      <div class="brand-film__actions">
        <button
          type="button"
          :aria-label="playing ? '暂停宣传片' : '播放宣传片'"
          @click="togglePlayback"
        >
          <Pause v-if="playing" :size="16" aria-hidden="true" /><Play
            v-else
            :size="16"
            aria-hidden="true"
          />
          <span>{{ playing ? '暂停' : '播放' }}</span>
        </button>
        <button
          type="button"
          :aria-label="muted ? '开启宣传片声音' : '静音宣传片'"
          :aria-pressed="!muted"
          @click="toggleSound"
        >
          <VolumeX v-if="muted" :size="16" aria-hidden="true" /><Volume2
            v-else
            :size="16"
            aria-hidden="true"
          />
          <span>{{ muted ? '开启声音' : '静音' }}</span>
        </button>
        <button type="button" aria-label="全屏观看宣传片" @click="enterFullscreen">
          <Maximize :size="16" aria-hidden="true" />
        </button>
      </div>
    </div>
    <div class="brand-film__progress" aria-hidden="true">
      <span :style="{ transform: `scaleX(${progress})` }"></span>
    </div>
    <p class="brand-film__hint">
      {{
        playing && !userStarted
          ? '正在静音播放 · 开启声音，完整感受这段故事'
          : '一起创造，让每一次贡献被看见。'
      }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowUpRight, Maximize, Pause, Play, Volume2, VolumeX } from 'lucide-vue-next'

const root = ref<HTMLElement>()
const video = ref<HTMLVideoElement>()
const source = ref('')
const playing = ref(false)
const muted = ref(true)
const userStarted = ref(false)
const failed = ref(false)
const progress = ref(0)
let nearObserver: IntersectionObserver | undefined
let visibilityObserver: IntersectionObserver | undefined
let motionQuery: MediaQueryList | undefined
let desktopQuery: MediaQueryList | undefined
let visible = false
let automaticPlayback = false
let manuallyPaused = false
let disposed = false
let playAttempt = 0
let expectedPause = false

type Connection = { saveData?: boolean; effectiveType?: string }

async function loadSource() {
  if (source.value || disposed) return
  const connection = (navigator as Navigator & { connection?: Connection }).connection
  const compact =
    window.matchMedia('(max-width: 767px)').matches ||
    connection?.saveData ||
    /2g/.test(connection?.effectiveType || '')
  source.value = compact
    ? '/about/brand/community-film-mobile.mp4'
    : '/about/brand/community-film.mp4'
  await nextTick()
  if (!disposed) video.value?.load()
}

async function playVideo() {
  const attempt = ++playAttempt
  await loadSource()
  if (disposed || !video.value || document.hidden) return
  try {
    await video.value.play()
    // A pending play() can resolve after the page/film has left the viewport.
    if (disposed || attempt !== playAttempt || !visible || document.hidden) pauseVideo()
  } catch {
    // Autoplay rejection leaves an accessible, explicit play control in place.
    playing.value = false
  }
}

function syncPlayback() {
  if (!video.value) return
  if (!visible || document.hidden || manuallyPaused || (!automaticPlayback && !userStarted.value)) {
    playAttempt += 1
    pauseVideo()
  } else {
    void playVideo()
  }
}

async function startWithSound() {
  userStarted.value = true
  manuallyPaused = false
  muted.value = false
  if (video.value) video.value.muted = false
  await playVideo()
}

function pauseVideo() {
  if (video.value && !video.value.paused) {
    expectedPause = true
    video.value.pause()
  }
}

function handlePlay() {
  playing.value = true
  manuallyPaused = false
}

function handlePause() {
  playing.value = false
  if (!expectedPause) manuallyPaused = true
  expectedPause = false
}

function syncVolume() {
  if (video.value) muted.value = video.value.muted
}

function togglePlayback() {
  if (playing.value) {
    manuallyPaused = true
    playAttempt += 1
    pauseVideo()
  } else {
    userStarted.value = true
    manuallyPaused = false
    void playVideo()
  }
}

function toggleSound() {
  muted.value = !muted.value
  userStarted.value = true
  if (video.value) video.value.muted = muted.value
  if (!playing.value) {
    manuallyPaused = false
    void playVideo()
  }
}

async function enterFullscreen() {
  const element = video.value as
    | (HTMLVideoElement & { webkitEnterFullscreen?: () => void })
    | undefined
  if (!element) return
  userStarted.value = true
  await loadSource()
  try {
    if (element.requestFullscreen) await element.requestFullscreen()
    else element.webkitEnterFullscreen?.()
  } catch {
    // Inline and native video controls remain available when fullscreen is unavailable.
  }
}

function updateProgress() {
  if (video.value?.duration && Number.isFinite(video.value.duration))
    progress.value = video.value.currentTime / video.value.duration
}

function updateMotionPreference() {
  const connection = (navigator as Navigator & { connection?: Connection }).connection
  automaticPlayback =
    !motionQuery?.matches &&
    desktopQuery?.matches &&
    !connection?.saveData &&
    !/2g/.test(connection?.effectiveType || '')
  syncPlayback()
}

onMounted(() => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  desktopQuery = window.matchMedia('(min-width: 768px) and (hover: hover)')
  motionQuery.addEventListener('change', updateMotionPreference)
  desktopQuery.addEventListener('change', updateMotionPreference)
  document.addEventListener('visibilitychange', syncPlayback)
  updateMotionPreference()
  if (!root.value) return
  nearObserver = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        // Mobile/reduced-motion/save-data modes do not download video before a tap.
        if (automaticPlayback) void loadSource()
        nearObserver?.disconnect()
      }
    },
    { rootMargin: '450px 0px' },
  )
  visibilityObserver = new IntersectionObserver(
    ([entry]) => {
      visible = Boolean(entry?.isIntersecting)
      syncPlayback()
    },
    { threshold: 0.2 },
  )
  nearObserver.observe(root.value)
  visibilityObserver.observe(root.value)
})

onBeforeUnmount(() => {
  disposed = true
  playAttempt += 1
  nearObserver?.disconnect()
  visibilityObserver?.disconnect()
  motionQuery?.removeEventListener('change', updateMotionPreference)
  desktopQuery?.removeEventListener('change', updateMotionPreference)
  document.removeEventListener('visibilitychange', syncPlayback)
  video.value?.pause()
  video.value?.removeAttribute('src')
  video.value?.load()
})
</script>

<style scoped>
.brand-film {
  position: relative;
  color: var(--apple-ink, var(--color-text-primary));
}
.brand-film__frame {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  border-radius: 24px;
  background: #050506;
  border: 1px solid var(--apple-line, var(--color-border));
}
video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.brand-film__play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 18px;
  color: #fff;
  background: linear-gradient(transparent, rgb(0 0 0 / 16%));
}
.brand-film__play > span {
  display: grid;
  place-items: center;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: rgb(255 255 255 / 15%);
  border: 1px solid rgb(255 255 255 / 48%);
  backdrop-filter: blur(12px);
  transition:
    transform 0.3s,
    background 0.3s;
}
.brand-film__play:hover > span {
  transform: scale(1.08);
  background: rgb(255 255 255 / 24%);
}
.brand-film__play strong {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.08em;
}
.brand-film__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 2px 18px;
  font-size: 12px;
}
.brand-film__caption,
.brand-film__actions,
.brand-film__actions button {
  display: flex;
  align-items: center;
  gap: 12px;
}
.brand-film__caption {
  font-family: var(--font-family-mono);
  letter-spacing: 0.07em;
}
.film-runtime {
  color: var(--apple-faint, var(--color-text-tertiary));
}
.film-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--apple-faint, var(--color-text-tertiary));
}
.film-dot.is-playing {
  background: var(--color-primary);
}
.brand-film__actions {
  gap: 14px;
}
.brand-film__actions button {
  min-height: 44px;
  padding: 0 8px;
  color: var(--apple-muted, var(--color-text-secondary));
  gap: 7px;
  transition: color 0.2s;
}
.brand-film__actions button:hover {
  color: var(--apple-ink, var(--color-text-primary));
}
.brand-film__progress {
  overflow: hidden;
  height: 1px;
  background: var(--apple-line, var(--color-border));
}
.brand-film__progress span {
  display: block;
  height: 100%;
  background: var(--color-primary);
  transform-origin: left;
}
.brand-film__hint {
  margin: 16px 0 0;
  color: var(--apple-muted, var(--color-text-secondary));
  font-size: 12px;
}
.brand-film__error {
  position: absolute;
  inset: 0;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 16px;
  color: #fff;
  background: rgb(5 12 32 / 85%);
}
.brand-film__error a {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: #fff;
}
button {
  border: 0;
  font: inherit;
  cursor: pointer;
  background: none;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 4px;
}
@media (max-width: 600px) {
  .brand-film__frame {
    border-radius: 14px;
  }
  .brand-film__play > span {
    width: 52px;
    height: 52px;
  }
  .brand-film__play {
    gap: 10px;
  }
  .brand-film__play strong {
    font-size: 12px;
  }
  .brand-film__caption {
    font-size: 10px;
    gap: 6px;
  }
  .film-runtime {
    display: none;
  }
  .brand-film__toolbar {
    padding: 10px 0;
    gap: 6px;
  }
  .brand-film__actions {
    gap: 2px;
  }
  .brand-film__actions button {
    padding: 0 7px;
    font-size: 11px;
  }
  .brand-film__hint {
    line-height: 1.7;
  }
}
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    transition: none !important;
  }
}
</style>
