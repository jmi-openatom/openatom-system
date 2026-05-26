<template>
  <main :class="`phase-${phase}`" class="lottery-screen">
    <section aria-live="polite" class="winner-stage">
      <strong>{{ displayWinner?.winnerName || '会是谁呢？' }}</strong>
      <div v-if="winnerPrizeText" class="winner-prize">{{ winnerPrizeText }}</div>
    </section>

    <div v-if="introVisible" class="intro-screen">
      <strong>抽奖即将开始</strong>
    </div>

    <div
      v-if="curtainVisible"
      :class="{
        'is-entering': phase === 'entering',
        'is-rolling': phase === 'rolling',
        'is-opening': phase === 'revealing',
      }"
      class="draw-curtain"
    >
      <div class="curtain-panel curtain-panel--top"></div>
      <div class="curtain-panel curtain-panel--bottom"></div>
      <div class="curtain-copy">
        <strong>{{ phase === 'rolling' ? rollingName : displayWinner?.winnerName }}</strong>
      </div>
    </div>
  </main>
</template>

<script lang="ts" setup>
import { lotteryApi } from '@/api'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

type DrawPhase = 'idle' | 'entering' | 'rolling' | 'revealing' | 'revealed'

const CURTAIN_ENTER_DURATION = 900
const DRAW_TEXT_DURATION = 2800
const CURTAIN_OPEN_DURATION = 1100
const INTRO_DURATION = 1200
const NAME_TICK_INTERVAL = 86

const route = useRoute()
const screen = ref<Record<string, any>>({})
const phase = ref<DrawPhase>('idle')
const introVisible = ref(true)
const revealedWinner = ref<Record<string, any> | null>(null)
const pendingWinner = ref<Record<string, any> | null>(null)
const lastWinnerKey = ref('')
const hasSyncedInitialWinner = ref(false)
const highlightIndex = ref(0)

let refreshTimer: number | null = null
let enterTimer: number | null = null
let drawTimer: number | null = null
let curtainTimer: number | null = null
let nameTicker: number | null = null
let introTimer: number | null = null
let rollingSoundTimer: number | null = null
let audioContext: AudioContext | null = null

const winners = computed(() => screen.value.winners || [])
const latestWinner = computed(() => screen.value.latestWinner || winners.value[0] || null)
const latestWinnerKey = computed(() => winnerKey(latestWinner.value))
const curtainVisible = computed(() => ['entering', 'rolling', 'revealing'].includes(phase.value))
const displayWinner = computed<Record<string, any>>(() => revealedWinner.value || {})
const winnerPrizeText = computed(() => {
  const winner = displayWinner.value
  const level = String(winner.prizeLevel || '').trim()
  const name = String(winner.prizeName || '').trim()
  return [level, name].filter(Boolean).join(' · ')
})
const rollingNames = computed(() => {
  const names = Array.isArray(screen.value.participantNames) ? screen.value.participantNames : []
  const cleanedNames = names.map((name: unknown) => String(name || '').trim()).filter(Boolean)
  if (cleanedNames.length) return cleanedNames
  return ['正在抽取']
})
const rollingName = computed(() => {
  const names = rollingNames.value
  return names[highlightIndex.value % names.length] || '正在抽取'
})

async function loadScreen() {
  screen.value = await lotteryApi.screen(String(route.params.id))
}

function startTimer() {
  stopTimer()
  refreshTimer = window.setInterval(() => {
    loadScreen()
  }, 2000)
}

function stopTimer() {
  if (refreshTimer === null) return
  window.clearInterval(refreshTimer)
  refreshTimer = null
}

function startDraw(winner: Record<string, any>) {
  pendingWinner.value = winner
  phase.value = 'entering'
  introVisible.value = false
  stopNameTicker()
  stopRollingSound()
  playDrawStartSound()

  if (enterTimer !== null) window.clearTimeout(enterTimer)
  if (drawTimer !== null) window.clearTimeout(drawTimer)
  if (curtainTimer !== null) window.clearTimeout(curtainTimer)

  enterTimer = window.setTimeout(() => {
    phase.value = 'rolling'
    startNameTicker()
    startRollingSound()
    enterTimer = null

    drawTimer = window.setTimeout(() => {
      stopNameTicker()
      stopRollingSound()
      revealedWinner.value = pendingWinner.value
      pendingWinner.value = null
      playWinnerSound()
      phase.value = 'revealing'
      drawTimer = null

      curtainTimer = window.setTimeout(() => {
        phase.value = 'revealed'
        curtainTimer = null
      }, CURTAIN_OPEN_DURATION)
    }, DRAW_TEXT_DURATION)
  }, CURTAIN_ENTER_DURATION)
}

function resetDrawState() {
  stopNameTicker()
  stopRollingSound()
  if (enterTimer !== null) {
    window.clearTimeout(enterTimer)
    enterTimer = null
  }
  if (drawTimer !== null) {
    window.clearTimeout(drawTimer)
    drawTimer = null
  }
  if (curtainTimer !== null) {
    window.clearTimeout(curtainTimer)
    curtainTimer = null
  }
  pendingWinner.value = null
  revealedWinner.value = null
  phase.value = 'idle'
}

function startNameTicker() {
  stopNameTicker()
  highlightIndex.value = Math.max(0, highlightIndex.value + 1)
  nameTicker = window.setInterval(() => {
    highlightIndex.value = (highlightIndex.value + 1) % Math.max(rollingNames.value.length, 1)
  }, NAME_TICK_INTERVAL)
}

function stopNameTicker() {
  if (nameTicker === null) return
  window.clearInterval(nameTicker)
  nameTicker = null
}

function getAudioContext() {
  if (typeof window === 'undefined') return null
  const AudioContextConstructor =
    window.AudioContext ||
    (window as Window & typeof globalThis & { webkitAudioContext?: typeof AudioContext })
      .webkitAudioContext
  if (!AudioContextConstructor) return null
  if (!audioContext) audioContext = new AudioContextConstructor()
  return audioContext
}

function requestAudioUnlock() {
  void unlockAudio()
}

async function unlockAudio() {
  const context = getAudioContext()
  if (!context || context.state !== 'suspended') return
  await context.resume().catch(() => undefined)
}

function playTone(
  frequency: number,
  duration: number,
  delay = 0,
  type: OscillatorType = 'sine',
  volume = 0.05,
) {
  const context = getAudioContext()
  if (!context) return
  void unlockAudio()

  const startAt = context.currentTime + delay
  const oscillator = context.createOscillator()
  const gainNode = context.createGain()

  oscillator.type = type
  oscillator.frequency.setValueAtTime(frequency, startAt)
  gainNode.gain.setValueAtTime(0.0001, startAt)
  gainNode.gain.exponentialRampToValueAtTime(volume, startAt + 0.02)
  gainNode.gain.exponentialRampToValueAtTime(0.0001, startAt + duration)

  oscillator.connect(gainNode)
  gainNode.connect(context.destination)
  oscillator.start(startAt)
  oscillator.stop(startAt + duration + 0.04)
}

function playDrawStartSound() {
  playTone(392, 0.14, 0, 'triangle', 0.045)
  playTone(523.25, 0.16, 0.1, 'triangle', 0.05)
  playTone(659.25, 0.2, 0.22, 'triangle', 0.04)
}

function playRollingTickSound() {
  const frequency = 720 + (highlightIndex.value % 5) * 28
  playTone(frequency, 0.035, 0, 'square', 0.012)
}

function startRollingSound() {
  stopRollingSound()
  playRollingTickSound()
  rollingSoundTimer = window.setInterval(playRollingTickSound, 170)
}

function stopRollingSound() {
  if (rollingSoundTimer === null) return
  window.clearInterval(rollingSoundTimer)
  rollingSoundTimer = null
}

function playWinnerSound() {
  playTone(523.25, 0.28, 0, 'sine', 0.055)
  playTone(659.25, 0.32, 0.08, 'sine', 0.05)
  playTone(783.99, 0.36, 0.16, 'sine', 0.048)
  playTone(1046.5, 0.48, 0.28, 'triangle', 0.04)
}

function winnerKey(winner: Record<string, any> | null) {
  if (!winner) return ''
  return String(
    winner.id || `${winner.prizeId || ''}-${winner.winnerName || ''}-${winner.wonAt || ''}`,
  )
}

watch(latestWinnerKey, (key) => {
  if (!hasSyncedInitialWinner.value) return

  if (!key) {
    lastWinnerKey.value = ''
    resetDrawState()
    return
  }

  if (key !== lastWinnerKey.value) {
    lastWinnerKey.value = key
    startDraw(latestWinner.value)
  }
})

function syncInitialWinner() {
  if (hasSyncedInitialWinner.value) return
  hasSyncedInitialWinner.value = true
  lastWinnerKey.value = latestWinnerKey.value
  if (latestWinnerKey.value) {
    revealedWinner.value = latestWinner.value
    phase.value = 'revealed'
  } else {
    resetDrawState()
  }
}

onMounted(async () => {
  window.addEventListener('pointerdown', requestAudioUnlock, { passive: true })
  window.addEventListener('keydown', requestAudioUnlock)
  await loadScreen()
  syncInitialWinner()
  startTimer()
  introTimer = window.setTimeout(() => {
    introVisible.value = false
    introTimer = null
  }, INTRO_DURATION)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', requestAudioUnlock)
  window.removeEventListener('keydown', requestAudioUnlock)
  stopTimer()
  resetDrawState()
  if (introTimer !== null) {
    window.clearTimeout(introTimer)
  }
})
</script>

<style scoped>
.lottery-screen {
  position: relative;
  display: grid;
  min-height: 100vh;
  min-height: 100svh;
  place-items: center;
  overflow: hidden;
  background: #ffffff;
  color: #020617;
}

.winner-stage {
  display: grid;
  gap: 22px;
  justify-items: center;
  width: min(100%, 1120px);
  padding: 40px;
  text-align: center;
}

.winner-stage span {
  display: none;
}

.winner-stage strong {
  max-width: 100%;
  overflow-wrap: anywhere;
  color: #020617;
  font-size: clamp(64px, 12vw, 180px);
  font-weight: 850;
  letter-spacing: 0;
  line-height: 0.94;
  white-space: nowrap;
}

.winner-prize {
  max-width: min(100%, 900px);
  margin: 0;
  overflow: hidden;
  color: #475569;
  font-size: clamp(18px, 4.6vw, 36px);
  font-weight: 760;
  letter-spacing: 0;
  line-height: 1.05;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.intro-screen,
.draw-curtain {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.intro-screen {
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 32px;
  background: #ffffff;
  color: #020617;
  text-align: center;
  animation: introExit 1.2s cubic-bezier(0.76, 0, 0.24, 1) forwards;
}

.intro-screen strong {
  max-width: min(1000px, calc(100vw - 48px));
  font-size: clamp(54px, 10vw, 160px);
  font-weight: 850;
  letter-spacing: 0;
  line-height: 0.96;
}

.draw-curtain {
  z-index: 30;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #020617;
}

.curtain-panel {
  position: absolute;
  left: 0;
  right: 0;
  height: 50%;
  background: #ffffff;
  transition: transform 1.1s cubic-bezier(0.76, 0, 0.24, 1);
  will-change: transform;
}

.curtain-panel--top {
  top: 0;
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.08);
}

.curtain-panel--bottom {
  bottom: 0;
  box-shadow: 0 -1px 0 rgba(15, 23, 42, 0.08);
}

.draw-curtain.is-entering .curtain-panel--top {
  animation: curtainTopIn 0.9s cubic-bezier(0.76, 0, 0.24, 1) both;
}

.draw-curtain.is-entering .curtain-panel--bottom {
  animation: curtainBottomIn 0.9s cubic-bezier(0.76, 0, 0.24, 1) both;
}

.draw-curtain.is-opening .curtain-panel--top {
  transform: translateY(-100%);
}

.draw-curtain.is-opening .curtain-panel--bottom {
  transform: translateY(100%);
}

.curtain-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 24px;
  justify-items: center;
  max-width: min(1100px, calc(100vw - 48px));
  text-align: center;
  transition:
    opacity 0.42s ease,
    transform 0.42s ease;
}

.draw-curtain.is-entering .curtain-copy {
  opacity: 0;
  transform: scale(0.98);
}

.draw-curtain.is-rolling .curtain-copy {
  animation: copyIn 0.36s ease both;
}

.curtain-copy strong {
  color: #020617;
  font-size: clamp(64px, 12vw, 180px);
  font-weight: 850;
  letter-spacing: 0;
  line-height: 0.94;
  white-space: nowrap;
}

.curtain-copy p {
  margin: 0;
  color: #475569;
  font-size: clamp(28px, 5vw, 78px);
  font-weight: 760;
  line-height: 1.05;
  white-space: nowrap;
}

.draw-curtain.is-opening .curtain-copy {
  opacity: 0;
  transform: scale(0.96);
}

@keyframes curtainTopIn {
  from {
    transform: translateY(-100%);
  }

  to {
    transform: translateY(0);
  }
}

@keyframes curtainBottomIn {
  from {
    transform: translateY(100%);
  }

  to {
    transform: translateY(0);
  }
}

@keyframes copyIn {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes introExit {
  0%,
  68% {
    opacity: 1;
    transform: translateY(0);
  }

  100% {
    opacity: 0;
    transform: translateY(-18px);
  }
}

@media (max-width: 640px) {
  .winner-stage {
    padding: 24px;
  }

  .winner-stage strong,
  .curtain-copy strong {
    font-size: clamp(46px, 12vw, 76px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-screen,
  .curtain-panel,
  .curtain-copy {
    animation: none;
    transition: none;
  }
}
</style>
