<template>
  <div class="scan-page">
    <section class="container scan-shell">
      <div class="scan-panel">
        <div class="scan-heading">
          <el-tag effect="plain" type="success">现场签到</el-tag>
          <h1>扫码签到</h1>
          <p>将摄像头对准签到二维码，识别后会自动提交。也可以粘贴签到码手动提交。</p>
        </div>

        <div class="scanner-box" :class="{ 'scanner-box--active': cameraActive }">
          <video ref="videoRef" autoplay muted playsinline class="scanner-video"></video>
          <div v-if="!cameraActive" class="scanner-placeholder">
            <el-icon><Camera /></el-icon>
            <span>{{ cameraStatus }}</span>
          </div>
          <div v-else class="scanner-frame" aria-hidden="true"></div>
        </div>

        <div class="scan-actions">
          <el-button v-if="!cameraActive" :icon="Camera" type="primary" @click="startCamera">
            打开摄像头
          </el-button>
          <el-button v-else :icon="Close" @click="stopCamera">关闭摄像头</el-button>
          <el-button :icon="Refresh" :loading="submitting" @click="submitToken(lastToken)">
            重新提交
          </el-button>
        </div>

        <el-alert
          v-if="!detectorSupported"
          :closable="false"
          class="scan-alert"
          show-icon
          title="当前浏览器不支持直接识别二维码，请使用下方手动签到。"
          type="warning"
        />

        <el-form class="manual-form" label-position="top" @submit.prevent>
          <el-form-item label="签到码">
            <el-input
              v-model="manualToken"
              clearable
              placeholder="可粘贴 openatom-checkin: 开头的签到码"
              @keyup.enter="submitToken(manualToken)"
            />
          </el-form-item>
          <el-button
            :disabled="!manualToken.trim()"
            :loading="submitting"
            class="manual-submit"
            type="primary"
            @click="submitToken(manualToken)"
          >
            提交签到
          </el-button>
        </el-form>
      </div>

      <aside class="scan-result">
        <div class="result-icon" :class="result ? 'result-icon--success' : ''">
          <el-icon><Select /></el-icon>
        </div>
        <h2>{{ result ? '签到完成' : '等待签到' }}</h2>
        <p v-if="result">
          {{ result.realName || result.userName || '当前账号' }} 已完成本次签到。
        </p>
        <p v-else>请保持登录状态，并确认自己在本次签到发放名单中。</p>
        <dl v-if="result" class="result-detail">
          <div>
            <dt>状态</dt>
            <dd>{{ result.status === 'checked' ? '已签到' : result.status }}</dd>
          </div>
          <div>
            <dt>签到时间</dt>
            <dd>{{ formatDateTime(result.checkinAt) || '-' }}</dd>
          </div>
        </dl>
      </aside>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Camera, Close, Refresh, Select } from '@element-plus/icons-vue'
import { checkInApi } from '@/api'
import { formatDateTime } from '@/utils/format.ts'

export default {
  name: 'SiteCheckInScan',
  data() {
    return {
      Camera,
      Close,
      Refresh,
      Select,
      cameraActive: false,
      cameraStatus: '摄像头未开启',
      detectorSupported: 'BarcodeDetector' in window,
      detector: null,
      stream: null,
      scanTimer: null,
      lastToken: '',
      manualToken: '',
      submitting: false,
      result: null,
    }
  },
  beforeUnmount() {
    this.stopCamera()
  },
  methods: {
    formatDateTime,
    async startCamera() {
      if (!this.detectorSupported) {
        this.cameraStatus = '浏览器不支持二维码识别'
        return
      }
      try {
        this.cameraStatus = '正在请求摄像头权限'
        this.detector = new window.BarcodeDetector({ formats: ['qr_code'] })
        this.stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' } },
          audio: false,
        })
        const video = this.$refs.videoRef
        video.srcObject = this.stream
        await video.play()
        this.cameraActive = true
        this.cameraStatus = '正在识别二维码'
        this.scanLoop()
      } catch (error) {
        this.cameraActive = false
        this.cameraStatus = '无法打开摄像头，请检查浏览器权限'
        ElMessage.error('无法打开摄像头，请检查浏览器权限或使用手动签到')
      }
    },
    stopCamera() {
      if (this.scanTimer) {
        window.cancelAnimationFrame(this.scanTimer)
        this.scanTimer = null
      }
      if (this.stream) {
        this.stream.getTracks().forEach((track) => track.stop())
        this.stream = null
      }
      this.cameraActive = false
      this.cameraStatus = '摄像头未开启'
    },
    async scanLoop() {
      if (!this.cameraActive) return
      if (!this.detector || this.submitting) {
        this.scanTimer = window.requestAnimationFrame(this.scanLoop)
        return
      }
      try {
        const codes = await this.detector.detect(this.$refs.videoRef)
        const token = codes?.[0]?.rawValue || ''
        if (token && token !== this.lastToken) {
          this.lastToken = token
          this.manualToken = token
          await this.submitToken(token)
        }
      } catch (error) {
        // Video frames can be temporarily unavailable while the stream is warming up.
      } finally {
        if (this.cameraActive) {
          this.scanTimer = window.requestAnimationFrame(this.scanLoop)
        }
      }
    },
    async submitToken(value) {
      const token = String(value || '').trim()
      if (!token || this.submitting) return
      this.submitting = true
      try {
        this.result = await checkInApi.scan({ token })
        ElMessage.success('签到成功')
        this.stopCamera()
      } finally {
        this.submitting = false
      }
    },
  },
}
</script>

<style scoped>
.scan-page {
  padding: 42px 0 72px;
}

.scan-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 22px;
  align-items: start;
}

.scan-panel,
.scan-result {
  border: 1px solid rgba(219, 230, 245, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--oa-shadow);
  backdrop-filter: blur(16px);
}

.scan-panel {
  padding: 24px;
}

.scan-heading h1 {
  margin: 14px 0 10px;
  font-size: 34px;
}

.scan-heading p,
.scan-result p {
  margin: 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.scanner-box {
  position: relative;
  display: grid;
  min-height: 420px;
  margin-top: 22px;
  overflow: hidden;
  place-items: center;
  border: 1px solid rgba(37, 99, 235, 0.14);
  border-radius: 18px;
  background: #0f172a;
}

.scanner-video {
  width: 100%;
  height: 100%;
  min-height: 420px;
  object-fit: cover;
}

.scanner-placeholder {
  position: absolute;
  display: grid;
  gap: 10px;
  justify-items: center;
  color: #cbd5e1;
}

.scanner-placeholder .el-icon {
  font-size: 42px;
}

.scanner-frame {
  position: absolute;
  width: min(68vw, 300px);
  aspect-ratio: 1;
  border: 3px solid rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  box-shadow: 0 0 0 999px rgba(15, 23, 42, 0.36);
}

.scan-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.scan-alert {
  margin-top: 16px;
}

.manual-form {
  margin-top: 18px;
}

.manual-submit {
  width: 100%;
}

.scan-result {
  position: sticky;
  top: 104px;
  padding: 24px;
}

.result-icon {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 16px;
  font-size: 28px;
}

.result-icon--success {
  color: #16a34a;
  background: #dcfce7;
}

.scan-result h2 {
  margin: 18px 0 8px;
}

.result-detail {
  display: grid;
  gap: 12px;
  margin: 20px 0 0;
}

.result-detail div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.result-detail dt {
  color: var(--oa-muted);
}

.result-detail dd {
  margin: 0;
  font-weight: 700;
}

@media (max-width: 860px) {
  .scan-page {
    padding: 20px 0 44px;
  }

  .scan-shell {
    grid-template-columns: 1fr;
  }

  .scan-panel,
  .scan-result {
    padding: 18px;
  }

  .scanner-box,
  .scanner-video {
    min-height: 62vh;
  }

  .scan-result {
    position: static;
  }
}
</style>
