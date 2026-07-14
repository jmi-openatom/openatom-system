<template>
  <div class="next-page">
    <!-- 动态粒子背景 -->
    <div class="particles">
      <span v-for="i in 50" :key="i" class="particle" :style="particleStyle(i)" />
    </div>

    <!-- 网格背景 -->
    <div class="grid-bg" />

    <!-- 返回官网按钮 -->
    <div class="back-bar">
      <router-link to="/" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
        返回官网
      </router-link>
    </div>

    <!-- 主内容区 -->
    <div class="next-content">
      <!-- 顶部徽章 -->
      <div class="badge-row">
        <span class="badge">
          <span class="badge-dot" />
          IN DEVELOPMENT
        </span>
      </div>

      <!-- 主标题 -->
      <h1 class="hero-title">
        <span class="title-openatom">openatom</span><span class="title-system">-system</span>
        <br />
        <span class="title-next">next</span>
      </h1>

      <!-- 描述 -->
      <p class="hero-desc">
        新一代开放原子开源社团管理系统，正在紧锣密鼓地开发中。<br />
        更强大的功能、更流畅的体验、更开放的架构。
      </p>

      <!-- 统计数据 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
              <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7Z" />
            </svg>
          </div>
          <div class="stat-value">{{ animatedViews }}</div>
          <div class="stat-label">页面访问</div>
        </div>
        <div class="stat-divider" />
        <div class="stat-card">
          <div class="stat-icon stat-icon--purple">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
            </svg>
          </div>
          <div class="stat-value">{{ animatedLikes }}</div>
          <div class="stat-label">点赞支持</div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions-row">
        <button class="btn-join" @click="handleJoin">
          <span class="btn-join__glow" />
          <span class="btn-join__content">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14" />
            </svg>
            加入开发
          </span>
        </button>
        <button class="btn-like" :class="{ 'btn-like--active': liked }" @click="handleLike">
          <svg viewBox="0 0 24 24" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
          </svg>
          {{ liked ? '已点赞' : '点赞' }}
        </button>
      </div>

      <!-- 项目介绍：微服务架构 -->
      <section class="arch-section">
        <h2 class="arch-title">项目架构</h2>
        <p class="arch-subtitle">从单体到微服务的全面演进</p>

        <!-- 架构概览 -->
        <div class="arch-overview">
          <div class="arch-badge">Spring Cloud 2023</div>
          <div class="arch-badge">JDK 21</div>
          <div class="arch-badge">Nacos</div>
          <div class="arch-badge">Gateway</div>
          <div class="arch-badge">RabbitMQ</div>
          <div class="arch-badge">Redis</div>
        </div>

        <!-- 服务拓扑图 -->
        <div class="topo-map">
          <div class="topo-center">
            <div class="topo-node topo-node--gateway">
              <span class="topo-label">Gateway</span>
              <span class="topo-port">:8080</span>
            </div>
          </div>
          <div class="topo-services">
            <div v-for="svc in services" :key="svc.name" class="topo-node topo-node--service">
              <span class="topo-label">{{ svc.name }}</span>
              <span class="topo-port">:{{ svc.port }}</span>
            </div>
          </div>
          <div class="topo-infra">
            <div class="topo-node topo-node--infra">
              <span class="topo-label">Nacos</span>
              <span class="topo-sub">注册 / 配置中心</span>
            </div>
            <div class="topo-node topo-node--infra">
              <span class="topo-label">RabbitMQ</span>
              <span class="topo-sub">事件驱动</span>
            </div>
            <div class="topo-node topo-node--infra">
              <span class="topo-label">Redis</span>
              <span class="topo-sub">共享会话</span>
            </div>
            <div class="topo-node topo-node--infra">
              <span class="topo-label">MySQL 8</span>
              <span class="topo-sub">独立数据源</span>
            </div>
          </div>
        </div>

        <!-- 服务列表 -->
        <div class="services-grid">
          <div v-for="svc in services" :key="svc.name" class="service-card">
            <div class="service-header">
              <span class="service-name">{{ svc.name }}</span>
              <span class="service-port">:{{ svc.port }}</span>
            </div>
            <p class="service-desc">{{ svc.desc }}</p>
            <div class="service-db" v-if="svc.db">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="12" height="12">
                <ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.66 4.03 3 9 3s9-1.34 9-3V5"/><path d="M3 12c0 1.66 4.03 3 9 3s9-1.34 9-3"/>
              </svg>
              {{ svc.db }}
            </div>
          </div>
        </div>

        <!-- 通信模式 -->
        <div class="comm-section">
          <h3 class="comm-title">通信模式</h3>
          <div class="comm-grid">
            <div class="comm-card">
              <div class="comm-tag comm-tag--sync">同步</div>
              <h4>OpenFeign</h4>
              <p>服务间实时调用，如社团服务查询用户信息</p>
              <code>club-service → user-service</code>
            </div>
            <div class="comm-card">
              <div class="comm-tag comm-tag--async">异步事件</div>
              <h4>RabbitMQ</h4>
              <p>事件驱动解耦，积分发放、通知推送全异步</p>
              <code>point.award · notification.send</code>
            </div>
          </div>
        </div>

        <!-- 认证方案 -->
        <div class="auth-section">
          <h3 class="comm-title">认证方案</h3>
          <div class="auth-flow">
            <div class="auth-step">
              <div class="auth-step-num">1</div>
              <div class="auth-step-text">
                <strong>网关前置校验</strong>
                <p>Gateway 统一拦截，Sa-Token 验证</p>
              </div>
            </div>
            <div class="auth-arrow">→</div>
            <div class="auth-step">
              <div class="auth-step-num">2</div>
              <div class="auth-step-text">
                <strong>Redis 共享会话</strong>
                <p>所有服务读取同一 Redis 实例获取登录态</p>
              </div>
            </div>
            <div class="auth-arrow">→</div>
            <div class="auth-step">
              <div class="auth-step-num">3</div>
              <div class="auth-step-text">
                <strong>零侵入透传</strong>
                <p>各服务无需调用 auth-service</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 迁移路线 -->
        <div class="roadmap-section">
          <h3 class="comm-title">迁移路线</h3>
          <div class="roadmap-timeline">
            <div v-for="(phase, idx) in roadmap" :key="idx" class="roadmap-item">
              <div class="roadmap-dot" :class="{ 'roadmap-dot--done': phase.done, 'roadmap-dot--active': phase.active }" />
              <div class="roadmap-content">
                <div class="roadmap-phase">Phase {{ idx }}{{ phase.done ? ' ✓' : phase.active ? ' ◉' : '' }}</div>
                <div class="roadmap-name">{{ phase.name }}</div>
                <div class="roadmap-desc">{{ phase.desc }}</div>
                <div class="roadmap-time">{{ phase.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 版本信息 -->
      <div class="version-info">
        <span class="version-tag">v2.0.0-alpha</span>
        <span class="version-sep">·</span>
        <span>预计 2026年 9月发布</span>
      </div>
    </div>

    <!-- 弹窗 -->
    <Teleport to="body">
      <transition name="modal">
        <div v-if="showJoinModal" class="modal-overlay" @click.self="showJoinModal = false">
          <div class="modal-box">
            <button class="modal-close" @click="showJoinModal = false">&times;</button>
            <h2>加入 openatom-system-next 开发</h2>
            <p class="modal-desc">填写以下信息，我们会尽快与你联系！</p>

            <form class="join-form" @submit.prevent="handleSubmitJoin">
              <div class="form-group">
                <label>姓名 <span class="required">*</span></label>
                <input v-model="joinForm.name" type="text" placeholder="请输入你的姓名" class="form-input" />
              </div>
              <div class="form-group">
                <label>联系方式 <span class="required">*</span></label>
                <input v-model="joinForm.contact" type="text" placeholder="QQ / 微信 / 邮箱" class="form-input" />
              </div>
              <div class="form-group">
                <label>感兴趣方向 <span class="required">*</span></label>
                <div class="direction-options">
                  <label class="direction-option" :class="{ active: joinForm.direction === 'frontend' }">
                    <input type="radio" v-model="joinForm.direction" value="frontend" />
                    <span class="direction-icon">🎨</span>
                    <span>前端开发</span>
                  </label>
                  <label class="direction-option" :class="{ active: joinForm.direction === 'backend' }">
                    <input type="radio" v-model="joinForm.direction" value="backend" />
                    <span class="direction-icon">⚙️</span>
                    <span>后端开发</span>
                  </label>
                  <label class="direction-option" :class="{ active: joinForm.direction === 'devops' }">
                    <input type="radio" v-model="joinForm.direction" value="devops" />
                    <span class="direction-icon">🧪</span>
                    <span>测试 & DevOps</span>
                  </label>
                </div>
              </div>
              <div class="form-group">
                <label>技能栈</label>
                <input v-model="joinForm.skills" type="text" placeholder="如：Vue3, Spring Boot, Docker..." class="form-input" />
              </div>
              <div class="form-group">
                <label>留言</label>
                <textarea v-model="joinForm.message" placeholder="想说的话..." class="form-textarea" rows="3" />
              </div>
              <button type="submit" class="form-submit" :disabled="submitting">
                {{ submitting ? '提交中...' : '提交申请' }}
              </button>
            </form>

            <div v-if="submitSuccess" class="submit-success">
              <span class="success-icon">✓</span>
              <p>申请已提交，我们会尽快联系你！</p>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { nextPageApi } from '@/api'

const views = ref(0)
const likes = ref(0)
const liked = ref(false)
const showJoinModal = ref(false)
const animatedViews = ref('0')
const animatedLikes = ref('0')

const joinForm = ref({
  name: '',
  contact: '',
  direction: '',
  skills: '',
  message: '',
})
const submitting = ref(false)
const submitSuccess = ref(false)

const services = [
  { name: 'auth-service', port: 8101, db: 'db_auth', desc: '登录 / 注册 / Sa-Token / RBAC / OAuth' },
  { name: 'user-service', port: 8102, db: 'db_user', desc: '用户 CRUD / Excel 导入导出 / 头像' },
  { name: 'club-service', port: 8103, db: 'db_club', desc: '社团 / 部门 / 岗位 / 成员管理' },
  { name: 'recruitment-service', port: 8104, db: 'db_recruitment', desc: '招新计划 / 申请 / 审批 / 面试' },
  { name: 'activity-service', port: 8105, db: 'db_activity', desc: '活动发布 / 报名 / 签到关联' },
  { name: 'blog-service', port: 8106, db: 'db_blog', desc: '博客文章 / 评论 / 互动' },
  { name: 'checkin-service', port: 8107, db: 'db_checkin', desc: '签到场次 / 分组 / 高并发独立部署' },
  { name: 'point-service', port: 8108, db: 'db_point', desc: '积分账户 / 流水 / 兑换' },
  { name: 'notification-service', port: 8109, db: 'db_notification', desc: '站内通知 / 已读管理' },
  { name: 'office-service', port: 8110, db: 'db_office', desc: '办公文书生成与导出' },
  { name: 'file-service', port: 8111, db: 'db_file_storage', desc: '图床 / 文件上传存储' },
]

const roadmap = [
  { name: '基础设施搭建', desc: 'Nacos + openatom-common + Gateway 网关', time: '1-2 周', done: true, active: false },
  { name: '边缘服务拆分', desc: 'file-service / notification-service / blog-service', time: '2-3 周', done: false, active: true },
  { name: '核心域拆分', desc: 'auth / user / club / point 四大核心服务', time: '3-4 周', done: false, active: false },
  { name: '业务域拆分', desc: 'recruitment / activity / checkin / office', time: '2-3 周', done: false, active: false },
  { name: '全量切换', desc: '下线单体应用，全部流量走网关', time: '1 周', done: false, active: false },
]

function particleStyle(i: number) {
  const size = 2 + Math.random() * 3
  return {
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${Math.random() * 8}s`,
    animationDuration: `${6 + Math.random() * 10}s`,
    opacity: 0.15 + Math.random() * 0.35,
  }
}

function animateNumber(target: number, setter: (v: string) => void, duration = 1600) {
  const start = performance.now()
  function tick(now: number) {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    setter(Math.floor(eased * target).toLocaleString())
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

function handleLike() {
  if (liked.value) return
  liked.value = true
  // 调用真实 API 点赞
  nextPageApi.like().then((res) => {
    if (res && typeof res.likeCount === 'number') {
      likes.value = res.likeCount
      animateNumber(likes.value, (v) => (animatedLikes.value = v))
    }
  }).catch(() => {
    // 失败时仍然本地递增
    likes.value++
    animateNumber(likes.value, (v) => (animatedLikes.value = v))
  })
}

function handleJoin() {
  // 跳转到社团管理系统的表单页面
  window.location.href = 'https://www.jmi-openatom.cn/forms/9'
}

async function handleSubmitJoin() {
  if (!joinForm.value.name.trim()) return alert('请填写姓名')
  if (!joinForm.value.contact.trim()) return alert('请填写联系方式')
  if (!joinForm.value.direction) return alert('请选择感兴趣的方向')

  submitting.value = true
  try {
    await nextPageApi.join({
      name: joinForm.value.name.trim(),
      contact: joinForm.value.contact.trim(),
      direction: joinForm.value.direction,
      skills: joinForm.value.skills.trim(),
      message: joinForm.value.message.trim(),
    })
    submitSuccess.value = true
    // 重置表单
    joinForm.value = { name: '', contact: '', direction: '', skills: '', message: '' }
    setTimeout(() => {
      showJoinModal.value = false
      submitSuccess.value = false
    }, 2500)
  } catch (e) {
    alert('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 调用真实 API 获取统计数据（同时递增访问量）
  try {
    const data = await nextPageApi.getStats()
    if (data) {
      views.value = data.viewCount ?? 0
      likes.value = data.likeCount ?? 0
      animateNumber(views.value, (v) => (animatedViews.value = v))
      animateNumber(likes.value, (v) => (animatedLikes.value = v))
    }
  } catch (e) {
    // API 失败时使用默认值
    views.value = 0
    likes.value = 0
    animatedViews.value = '0'
    animatedLikes.value = '0'
  }
})
</script>

<style scoped>
.next-page {
  position: relative;
  min-height: 100vh;
  background: #09090b;
  color: #e4e4e7;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px 60px;
}

/* ===== 返回官网按钮 ===== */
.back-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 16px 24px;
  background: rgba(9, 9, 11, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(168, 85, 247, 0.15);
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  color: #c084fc;
  background: rgba(168, 85, 247, 0.08);
  border: 1px solid rgba(168, 85, 247, 0.25);
  text-decoration: none;
  transition: all 0.25s;
}
.back-btn:hover {
  background: rgba(168, 85, 247, 0.15);
  border-color: rgba(168, 85, 247, 0.4);
  color: #d8b4fe;
  transform: translateX(-2px);
}
.back-btn svg {
  transition: transform 0.25s;
}
.back-btn:hover svg {
  transform: translateX(-3px);
}

/* ===== 粒子背景 ===== */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.particle {
  position: absolute;
  border-radius: 50%;
  background: #a855f7;
  animation: particleFloat linear infinite;
}
@keyframes particleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: var(--particle-opacity, 0.3); }
  90% { opacity: var(--particle-opacity, 0.3); }
  50% { transform: translateY(-60px) scale(1.4); }
}

/* ===== 网格背景 ===== */
.grid-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(rgba(168, 85, 247, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(168, 85, 247, 0.04) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 50%, black 30%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 70% 60% at 50% 50%, black 30%, transparent 100%);
}

/* ===== 主内容 ===== */
.next-content {
  position: relative;
  z-index: 1;
  max-width: 720px;
  width: 100%;
  text-align: center;
}

/* ===== 徽章 ===== */
.badge-row {
  margin-bottom: 28px;
}
.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 18px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #c084fc;
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.25);
  text-transform: uppercase;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a855f7;
  animation: dotPulse 2s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(168, 85, 247, 0.5); }
  50% { opacity: 0.6; box-shadow: 0 0 0 6px rgba(168, 85, 247, 0); }
}

/* ===== 标题 ===== */
.hero-title {
  font-size: clamp(42px, 8vw, 80px);
  font-weight: 800;
  line-height: 1.05;
  margin: 0 0 24px;
  letter-spacing: -0.03em;
}
.title-openatom {
  color: #f4f4f5;
}
.title-system {
  color: #71717a;
}
.title-next {
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 40%, #c084fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 32px rgba(168, 85, 247, 0.35));
}

/* ===== 描述 ===== */
.hero-desc {
  font-size: 16px;
  line-height: 1.7;
  color: #a1a1aa;
  margin: 0 auto 40px;
  max-width: 520px;
}

/* ===== 统计卡片 ===== */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  margin-bottom: 40px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 32px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  min-width: 160px;
}
.stat-icon {
  width: 28px;
  height: 28px;
  color: #a855f7;
}
.stat-icon svg {
  width: 100%;
  height: 100%;
}
.stat-icon--purple {
  color: #c084fc;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  background: linear-gradient(180deg, #f4f4f5, #a1a1aa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.stat-label {
  font-size: 12px;
  color: #71717a;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.stat-divider {
  width: 1px;
  height: 48px;
  background: rgba(255, 255, 255, 0.08);
}

/* ===== 按钮 ===== */
.actions-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 56px;
}
.btn-join {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 14px 36px;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #7c3aed, #a855f7);
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.3s;
  box-shadow: 0 0 24px rgba(168, 85, 247, 0.3), 0 4px 12px rgba(0, 0, 0, 0.4);
}
.btn-join:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 40px rgba(168, 85, 247, 0.45), 0 8px 24px rgba(0, 0, 0, 0.5);
}
.btn-join:active {
  transform: translateY(0);
}
.btn-join__glow {
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  background: linear-gradient(135deg, #a855f7, #7c3aed, #c084fc, #7c3aed);
  background-size: 300% 300%;
  animation: glowShift 4s ease infinite;
  z-index: 0;
  filter: blur(8px);
  opacity: 0.5;
}
@keyframes glowShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
.btn-join__content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-join__content svg {
  width: 18px;
  height: 18px;
}

.btn-like {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  font-size: 14px;
  font-weight: 500;
  color: #a1a1aa;
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: all 0.25s;
}
.btn-like svg {
  width: 18px;
  height: 18px;
  transition: all 0.25s;
}
.btn-like:hover {
  border-color: rgba(168, 85, 247, 0.4);
  color: #c084fc;
  background: rgba(168, 85, 247, 0.08);
}
.btn-like--active {
  border-color: rgba(168, 85, 247, 0.5);
  color: #a855f7;
  background: rgba(168, 85, 247, 0.12);
}

/* ===== 架构区域 ===== */
.arch-section {
  margin-bottom: 48px;
}
.arch-title {
  font-size: 24px;
  font-weight: 700;
  color: #f4f4f5;
  margin: 0 0 6px;
  text-align: center;
}
.arch-subtitle {
  font-size: 14px;
  color: #71717a;
  margin: 0 0 24px;
  text-align: center;
}
.arch-overview {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-bottom: 32px;
}
.arch-badge {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #c084fc;
  background: rgba(168, 85, 247, 0.08);
  border: 1px solid rgba(168, 85, 247, 0.2);
}

/* ===== 拓扑图 ===== */
.topo-map {
  position: relative;
  padding: 32px 0;
  margin-bottom: 32px;
}
.topo-center {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}
.topo-services {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
}
.topo-infra {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}
.topo-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.3s;
}
.topo-node:hover {
  border-color: rgba(168, 85, 247, 0.3);
  background: rgba(168, 85, 247, 0.04);
}
.topo-node--gateway {
  background: rgba(168, 85, 247, 0.1);
  border-color: rgba(168, 85, 247, 0.3);
  padding: 16px 32px;
}
.topo-node--infra {
  background: rgba(34, 197, 94, 0.05);
  border-color: rgba(34, 197, 94, 0.15);
}
.topo-node--infra:hover {
  border-color: rgba(34, 197, 94, 0.3);
  background: rgba(34, 197, 94, 0.08);
}
.topo-label {
  font-size: 13px;
  font-weight: 600;
  color: #e4e4e7;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.topo-port {
  font-size: 12px;
  color: #71717a;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.topo-sub {
  font-size: 12px;
  color: #52525b;
}

/* ===== 服务列表 ===== */
.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 40px;
}
.service-card {
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.3s;
}
.service-card:hover {
  border-color: rgba(168, 85, 247, 0.3);
  background: rgba(168, 85, 247, 0.04);
  transform: translateY(-2px);
}
.service-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.service-name {
  font-size: 13px;
  font-weight: 600;
  color: #c084fc;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.service-port {
  font-size: 12px;
  color: #52525b;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.service-desc {
  font-size: 12px;
  color: #a1a1aa;
  margin: 0 0 8px;
  line-height: 1.5;
}
.service-db {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #71717a;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

/* ===== 通信模式 ===== */
.comm-section,
.auth-section,
.roadmap-section {
  margin-bottom: 40px;
}
.comm-title {
  font-size: 18px;
  font-weight: 600;
  color: #f4f4f5;
  margin: 0 0 20px;
  text-align: center;
}
.comm-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.comm-card {
  padding: 20px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.comm-card h4 {
  font-size: 15px;
  font-weight: 600;
  color: #f4f4f5;
  margin: 12px 0 8px;
}
.comm-card p {
  font-size: 13px;
  color: #a1a1aa;
  margin: 0 0 12px;
  line-height: 1.5;
}
.comm-card code {
  font-size: 12px;
  color: #c084fc;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  background: rgba(168, 85, 247, 0.08);
  padding: 4px 10px;
  border-radius: 6px;
}
.comm-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
}
.comm-tag--sync {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
}
.comm-tag--async {
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.2);
}

/* ===== 认证流程 ===== */
.auth-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}
.auth-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  max-width: 220px;
}
.auth-step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(168, 85, 247, 0.15);
  color: #c084fc;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.auth-step-text strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #f4f4f5;
  margin-bottom: 4px;
}
.auth-step-text p {
  font-size: 12px;
  color: #71717a;
  margin: 0;
  line-height: 1.4;
}
.auth-arrow {
  font-size: 20px;
  color: #52525b;
}

/* ===== 迁移路线 ===== */
.roadmap-timeline {
  position: relative;
  padding-left: 24px;
}
.roadmap-timeline::before {
  position: absolute;
  left: 7px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: rgba(255, 255, 255, 0.06);
  content: '';
}
.roadmap-item {
  position: relative;
  padding-bottom: 24px;
  padding-left: 20px;
}
.roadmap-item:last-child {
  padding-bottom: 0;
}
.roadmap-dot {
  position: absolute;
  left: -20px;
  top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #27272a;
  border: 2px solid #52525b;
}
.roadmap-dot--done {
  background: #22c55e;
  border-color: #22c55e;
}
.roadmap-dot--active {
  background: #a855f7;
  border-color: #a855f7;
  box-shadow: 0 0 0 4px rgba(168, 85, 247, 0.2);
}
.roadmap-phase {
  font-size: 12px;
  font-weight: 600;
  color: #71717a;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  margin-bottom: 4px;
}
.roadmap-name {
  font-size: 14px;
  font-weight: 600;
  color: #f4f4f5;
  margin-bottom: 4px;
}
.roadmap-desc {
  font-size: 12px;
  color: #a1a1aa;
  margin-bottom: 4px;
  line-height: 1.5;
}
.roadmap-time {
  font-size: 12px;
  color: #52525b;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

/* ===== 版本信息 ===== */
.version-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 13px;
  color: #52525b;
}
.version-tag {
  padding: 3px 10px;
  border-radius: 999px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.2);
}
.version-sep {
  color: #3f3f46;
}

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  padding: 20px;
}
.modal-box {
  position: relative;
  max-width: 520px;
  width: 100%;
  padding: 36px;
  border-radius: 20px;
  background: #18181b;
  border: 1px solid rgba(168, 85, 247, 0.2);
  box-shadow: 0 0 60px rgba(168, 85, 247, 0.12), 0 24px 48px rgba(0, 0, 0, 0.5);
}
.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: #71717a;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.modal-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #f4f4f5;
}
.modal-box h2 {
  font-size: 20px;
  font-weight: 700;
  color: #f4f4f5;
  margin: 0 0 8px;
}
.modal-desc {
  font-size: 14px;
  color: #a1a1aa;
  margin: 0 0 24px;
  line-height: 1.6;
}
.modal-directions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

/* ===== 表单样式 ===== */
.join-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #d4d4d8;
}
.required {
  color: #ef4444;
}
.form-input {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: #f4f4f5;
  font-size: 14px;
  outline: none;
  transition: border-color 0.25s, background 0.25s;
}
.form-input:focus {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(168, 85, 247, 0.05);
}
.form-input::placeholder,
.form-textarea::placeholder {
  color: #52525b;
}
.form-textarea {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: #f4f4f5;
  font-size: 14px;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.25s, background 0.25s;
}
.form-textarea:focus {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(168, 85, 247, 0.05);
}
.direction-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.direction-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: all 0.25s;
  font-size: 12px;
  color: #a1a1aa;
}
.direction-option input {
  display: none;
}
.direction-option:hover {
  border-color: rgba(168, 85, 247, 0.3);
}
.direction-option.active {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(168, 85, 247, 0.1);
  color: #c084fc;
}
.direction-option .direction-icon {
  font-size: 24px;
}
.form-submit {
  padding: 12px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #7c3aed, #a855f7);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
  margin-top: 4px;
}
.form-submit:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}
.form-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.submit-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  margin-top: 16px;
  border-radius: 12px;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.2);
}
.success-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.submit-success p {
  font-size: 14px;
  color: #22c55e;
  margin: 0;
}

/* ===== 弹窗动画 ===== */
.modal-enter-active { transition: opacity 0.3s; }
.modal-enter-active .modal-box { transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.3s; }
.modal-leave-active { transition: opacity 0.2s; }
.modal-leave-active .modal-box { transition: transform 0.2s, opacity 0.2s; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .modal-box { transform: scale(0.95) translateY(12px); opacity: 0; }
.modal-leave-to { opacity: 0; }
.modal-leave-to .modal-box { transform: scale(0.97); opacity: 0; }

/* ===== 响应式 ===== */
@media (max-width: 640px) {
  .next-page { padding: 60px 16px 40px; }
  .stats-row { flex-direction: column; gap: 16px; }
  .stat-divider { width: 48px; height: 1px; }
  .stat-card { min-width: 200px; }
  .services-grid { grid-template-columns: 1fr; }
  .comm-grid { grid-template-columns: 1fr; }
  .actions-row { flex-direction: column; }
  .btn-join, .btn-like { width: 100%; justify-content: center; }
  .modal-directions { grid-template-columns: 1fr; }
  .direction-options { grid-template-columns: 1fr; }
  .auth-flow { flex-direction: column; }
  .auth-arrow { transform: rotate(90deg); }
}
</style>
