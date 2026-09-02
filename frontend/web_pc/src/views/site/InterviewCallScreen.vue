<template>
  <main class="call-screen">
    <header class="screen-header">
      <div>
        <span>OPENATOM INTERVIEW</span>
        <h1>{{ screen.sessionName || '面试叫号' }}</h1>
      </div>
      <div class="screen-tools">
        <span :class="{ online: online }" class="connection-state">{{
          online ? '实时同步中' : '连接中断'
        }}</span>
        <button type="button" :aria-pressed="voiceEnabled" @click="toggleVoice">
          {{ voiceEnabled ? '语音已开启' : '开启语音' }}
        </button>
        <button type="button" @click="toggleFullscreen">全屏显示</button>
      </div>
    </header>

    <section aria-live="polite" class="room-stage" :class="`rooms-${Math.min(rooms.length, 4)}`">
      <article
        v-for="room in rooms"
        :key="room.roomId"
        class="room-call-card"
        :class="{ active: room.current }"
      >
        <div class="room-label">
          <span>面试间</span><strong>{{ room.name }}</strong>
        </div>
        <template v-if="room.current">
          <div class="candidate-number">{{ numberText(room.current) }}</div>
          <h2>{{ room.current.applicantName }}</h2>
          <p>
            请前往 {{ room.name }}<template v-if="room.location"> · {{ room.location }}</template>
          </p>
        </template>
        <template v-else
          ><div class="standby"><strong>请稍候</strong><span>等待叫号</span></div></template
        >
      </article>
    </section>

    <footer class="screen-footer">
      <span>请留意大屏并保持现场安静</span><time>{{ clock }}</time>
    </footer>

    <div
      v-if="announcement"
      class="announcement-curtain"
      :class="{ revealing: announcementPhase === 'revealing' }"
    >
      <div class="curtain-panel curtain-panel--top"></div>
      <div class="curtain-panel curtain-panel--bottom"></div>
      <section class="announcement-copy">
        <span>请下一位候选人前往面试</span>
        <strong>{{ announcement.applicantName }}</strong>
        <div>{{ numberText(announcement) }} · {{ announcement.roomName }}</div>
      </section>
    </div>

    <button v-if="showAudioGuide" type="button" class="audio-guide" @click="enableExperience">
      <strong>点击开启语音叫号</strong><span>建议同时进入全屏显示</span>
    </button>
  </main>
</template>

<script setup lang="ts">
import { interviewSessionApi } from '@/api'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute(),
  screen = ref<any>({}),
  online = ref(true),
  voiceEnabled = ref(true),
  showAudioGuide = ref(true)
const announcement = ref<any>(null),
  announcementPhase = ref<'showing' | 'revealing'>('showing'),
  clock = ref('')
const seen = new Map<number, string>(),
  pending: any[] = []
let refreshTimer: number | null = null,
  clockTimer: number | null = null,
  revealTimer: number | null = null,
  closeTimer: number | null = null
const rooms = computed<any[]>(() => screen.value.rooms || [])

async function loadScreen() {
  try {
    const data = await interviewSessionApi.callScreen(String(route.params.sessionId))
    online.value = true
    const firstLoad = !screen.value.sessionId
    screen.value = data
    const newCalls: any[] = []
    for (const room of data.rooms || []) {
      if (!room.current) continue
      const key = `${room.current.interviewId}-${room.current.callCount}`
      if (!firstLoad && seen.get(room.roomId) !== key)
        newCalls.push({ ...room.current, roomName: room.name })
      seen.set(room.roomId, key)
    }
    newCalls.sort((a, b) => new Date(a.calledAt).getTime() - new Date(b.calledAt).getTime())
    pending.push(...newCalls)
    playNext()
  } catch {
    online.value = false
  }
}
function playNext() {
  if (announcement.value || !pending.length) return
  const item = pending.shift()
  announcement.value = item
  announcementPhase.value = 'showing'
  speak(item)
  revealTimer = window.setTimeout(() => {
    announcementPhase.value = 'revealing'
    closeTimer = window.setTimeout(() => {
      announcement.value = null
      playNext()
    }, 900)
  }, 2600)
}
function speak(item: any) {
  if (!voiceEnabled.value || !('speechSynthesis' in window)) return
  window.speechSynthesis.cancel()
  const text = `请 ${item.applicantName}，编号 ${String(item.queueNumber || item.interviewId)}，前往 ${item.roomName} 面试`
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.rate = 0.86
  utterance.pitch = 1
  utterance.volume = 1
  const voice = window.speechSynthesis
    .getVoices()
    .find((v) => v.lang.toLowerCase().startsWith('zh'))
  if (voice) utterance.voice = voice
  window.speechSynthesis.speak(utterance)
}
function toggleVoice() {
  voiceEnabled.value = !voiceEnabled.value
  showAudioGuide.value = false
  if (!voiceEnabled.value) window.speechSynthesis?.cancel()
}
async function enableExperience() {
  voiceEnabled.value = true
  showAudioGuide.value = false
  const probe = new SpeechSynthesisUtterance('语音叫号已开启')
  probe.lang = 'zh-CN'
  probe.rate = 0.9
  window.speechSynthesis?.speak(probe)
  await document.documentElement.requestFullscreen?.().catch(() => undefined)
}
async function toggleFullscreen() {
  if (document.fullscreenElement) await document.exitFullscreen()
  else await document.documentElement.requestFullscreen?.()
}
function numberText(item: any) {
  return item.queueNumber ? `#${String(item.queueNumber).padStart(3, '0')}` : `#${item.interviewId}`
}
function updateClock() {
  clock.value = new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date())
}
onMounted(async () => {
  updateClock()
  await loadScreen()
  refreshTimer = window.setInterval(loadScreen, 1800)
  clockTimer = window.setInterval(updateClock, 1000)
})
onBeforeUnmount(() => {
  if (refreshTimer !== null) window.clearInterval(refreshTimer)
  if (clockTimer !== null) window.clearInterval(clockTimer)
  if (revealTimer !== null) window.clearTimeout(revealTimer)
  if (closeTimer !== null) window.clearTimeout(closeTimer)
  window.speechSynthesis?.cancel()
})
</script>

<style scoped>
.call-screen {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  background: #fff;
  color: #000;
  padding: clamp(20px, 3vw, 48px);
  font-family: inherit;
}
.screen-header,
.screen-tools,
.screen-footer,
.room-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.screen-header span {
  font-size: clamp(11px, 1vw, 15px);
  font-weight: 800;
  letter-spacing: 0.16em;
  color: #66666b;
}
.screen-header h1 {
  margin: 5px 0 0;
  font-size: clamp(22px, 2.8vw, 46px);
  line-height: 1;
}
.screen-tools button {
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid #d2d2d7;
  border-radius: 999px;
  background: #fff;
  color: #1d1d1f;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}
.screen-tools button:hover,
.screen-tools button:focus-visible {
  background: #f5f5f7;
  border-color: #86868b;
  outline: 3px solid rgba(0, 113, 227, 0.28);
  outline-offset: 2px;
}
.connection-state {
  display: flex;
  align-items: center;
  gap: 8px;
}
.connection-state::before {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #d70015;
  content: '';
}
.connection-state.online::before {
  background: #248a3d;
}
.room-stage {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(360px, 100%), 1fr));
  align-content: center;
  gap: clamp(14px, 2vw, 28px);
  padding: clamp(22px, 4vh, 56px) 0;
}
.room-call-card {
  display: grid;
  min-height: clamp(260px, 48vh, 540px);
  grid-template-rows: auto 1fr auto auto;
  align-items: center;
  padding: clamp(24px, 3vw, 48px);
  border: 1px solid #e5e5e7;
  border-radius: clamp(22px, 3vw, 40px);
  background: #fbfbfd;
  text-align: center;
}
.room-call-card.active {
  background: #fff;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.08);
}
.room-label {
  align-self: start;
  text-align: left;
}
.room-label span {
  color: #66666b;
  font-size: clamp(13px, 1.2vw, 18px);
}
.room-label strong {
  font-size: clamp(19px, 2vw, 32px);
}
.candidate-number {
  align-self: end;
  color: #66666b;
  font-size: clamp(24px, 3vw, 48px);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.room-call-card h2 {
  max-width: 100%;
  margin: 8px 0;
  font-size: clamp(48px, 7vw, 118px);
  font-weight: 850;
  letter-spacing: -0.04em;
  line-height: 0.95;
  overflow-wrap: anywhere;
}
.room-call-card p {
  margin: 10px 0 0;
  color: #66666b;
  font-size: clamp(16px, 1.7vw, 26px);
  font-weight: 650;
}
.standby {
  display: grid;
  align-self: center;
  gap: 10px;
  color: #86868b;
}
.standby strong {
  font-size: clamp(38px, 5vw, 76px);
}
.standby span {
  font-size: clamp(16px, 1.6vw, 24px);
}
.screen-footer {
  color: #66666b;
  font-size: clamp(13px, 1.3vw, 20px);
  font-weight: 650;
}
.screen-footer time {
  color: #000;
  font-size: clamp(18px, 2vw, 32px);
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}
.announcement-curtain {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  overflow: hidden;
  pointer-events: none;
}
.curtain-panel {
  position: absolute;
  left: 0;
  right: 0;
  height: 50%;
  background: #fff;
  transition: transform 0.9s cubic-bezier(0.76, 0, 0.24, 1);
  will-change: transform;
}
.curtain-panel--top {
  top: 0;
  box-shadow: 0 1px 0 rgba(29, 29, 31, 0.08);
  animation: topIn 0.48s cubic-bezier(0.76, 0, 0.24, 1) both;
}
.curtain-panel--bottom {
  bottom: 0;
  box-shadow: 0 -1px 0 rgba(29, 29, 31, 0.08);
  animation: bottomIn 0.48s cubic-bezier(0.76, 0, 0.24, 1) both;
}
.announcement-curtain.revealing .curtain-panel--top {
  transform: translateY(-100%);
}
.announcement-curtain.revealing .curtain-panel--bottom {
  transform: translateY(100%);
}
.announcement-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 18px;
  max-width: calc(100vw - 48px);
  text-align: center;
  transition:
    opacity 0.35s ease,
    transform 0.35s ease;
  animation: copyIn 0.36s 0.34s ease both;
}
.announcement-copy span {
  color: #66666b;
  font-size: clamp(20px, 3vw, 46px);
  font-weight: 750;
}
.announcement-copy strong {
  font-size: clamp(70px, 13vw, 210px);
  font-weight: 850;
  letter-spacing: -0.05em;
  line-height: 0.9;
  white-space: nowrap;
}
.announcement-copy div {
  font-size: clamp(28px, 4.5vw, 70px);
  font-weight: 800;
}
.announcement-curtain.revealing .announcement-copy {
  opacity: 0;
  transform: scale(0.97);
}
.audio-guide {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-content: center;
  gap: 12px;
  border: 0;
  background: rgba(255, 255, 255, 0.96);
  color: #000;
  text-align: center;
  cursor: pointer;
}
.audio-guide strong {
  font-size: clamp(42px, 7vw, 100px);
}
.audio-guide span {
  color: #66666b;
  font-size: clamp(18px, 2.5vw, 36px);
}
@keyframes topIn {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(0);
  }
}
@keyframes bottomIn {
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
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
@media (max-width: 760px) {
  .call-screen {
    padding: 18px;
  }
  .screen-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .screen-tools {
    width: 100%;
    flex-wrap: wrap;
  }
  .connection-state {
    margin-right: auto;
  }
  .room-stage {
    align-content: start;
    overflow: auto;
  }
  .room-call-card {
    min-height: 280px;
  }
  .screen-footer span {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .curtain-panel,
  .announcement-copy,
  .screen-tools button {
    animation: none;
    transition: none;
  }
}
</style>
