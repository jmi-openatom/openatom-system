<template>
  <div class="onboard" :class="{ 'onboard--loaded': loaded }">
    <!-- ============ Background Layers ============ -->
    <div class="onboard__bg" aria-hidden="true">
      <div class="onboard__bg-gradient"></div>
      <div class="onboard__bg-orb onboard__bg-orb--1" ref="orb1Ref"></div>
      <div class="onboard__bg-orb onboard__bg-orb--2" ref="orb2Ref"></div>
      <div class="onboard__bg-orb onboard__bg-orb--3" ref="orb3Ref"></div>
      <div class="onboard__bg-grid"></div>
      <div class="onboard__bg-particles">
        <span v-for="n in 18" :key="n" class="onboard__particle" :style="particleStyle(n)"></span>
      </div>
    </div>

   <!-- ============ Open-Source Floating Decoration ============ -->
    <div class="ob-os-deco" aria-hidden="true">
      <span class="ob-os-deco__symbol ob-os-deco__symbol--1">&lt;/&gt;</span>
      <span class="ob-os-deco__symbol ob-os-deco__symbol--2">{ }</span>
      <span class="ob-os-deco__symbol ob-os-deco__symbol--3">git push</span>
      <span class="ob-os-deco__symbol ob-os-deco__symbol--4">open source</span>
      <span class="ob-os-deco__symbol ob-os-deco__symbol--5">PR #{{ String(step).padStart(2, '0') }}</span>
    </div>

    <!-- ============ Progress Indicator ============ -->
    <div class="onboard__progress" aria-hidden="true">
      <div class="onboard__progress-track">
        <div class="onboard__progress-fill" :style="{ width: `${(step / TOTAL_STEPS) * 100}%` }"></div>
      </div>
      <div class="onboard__progress-info">
        <span class="onboard__progress-current">{{ String(step).padStart(2, '0') }}</span>
        <span class="onboard__progress-sep">/</span>
        <span class="onboard__progress-total">{{ String(TOTAL_STEPS).padStart(2, '0') }}</span>
      </div>
    </div>

    <!-- ============ Step Container ============ -->
    <Transition :name="transitionName" mode="out-in" @after-enter="onStepEntered">
      <component :is="'div'" :key="step" class="onboard__step">
       <!-- ====== Step 1: Welcome ====== -->
        <section v-if="step === 1" class="ob-step ob-step--welcome">
          <div class="ob-step__inner">
            <div class="ob-logo" :class="{ 'ob-logo--in': stepInAnim }">
              <img src="../../../public/logo.png" width="100" alt="">
            </div>
            <p class="ob-eyebrow" :style="{ '--delay': '0.1s' }">开放原子开源社团 · JMI-OPENATOM</p>
            <h1 class="ob-title" :style="{ '--delay': '0.25s' }">
              欢迎加入
            </h1>

            <div class="ob-welcome-name" :style="{ '--delay': '0.4s' }">
              <h1 style="font-style: italic;font-weight: bold;">{{ displayName }} · {{ info?.membership?.departmentName || '尚未分配部门' }}</h1>
            </div>

            <p class="ob-subtitle" :style="{ '--delay': '0.55s' }">
              这里不只是一个社团，而是你技术旅程的全新起点。<br>让开源，在校园里真正发生。
            </p>

            <div class="ob-os-badge" :style="{ '--delay': '0.65s' }">
              <svg class="ob-os-badge__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M8 3H5a2 2 0 00-2 2v3m18 0V5a2 2 0 00-2-2h-3m0 18h3a2 2 0 002-2v-3M3 16v3a2 2 0 002 2h3" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M12 8l-3 4 3 4m0-8l3 4-3 4" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>Open Source · Open Mind · Open Future</span>
            </div>

            <button class="ob-btn ob-btn--primary ob-btn--glow" :style="{ '--delay': '0.8s' }" @click="nextStep">
              <svg class="ob-btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M16 12h5M12 16v5M8 12H3M12 8V3"/></svg>
              Fork 未来
              <span class="ob-btn__arrow">→</span>
            </button>
          </div>
        </section>

        <!-- ====== Step 2: Manifesto ====== -->
        <section v-else-if="step === 2" class="ob-step ob-step--manifesto">
          <div class="ob-step__inner ob-step__inner--narrow">
            <p class="ob-eyebrow" :style="{ '--delay': '0.05s' }">我们的使命 · MANIFESTO</p>
            <h2 class="ob-title ob-title--md" :style="{ '--delay': '0.15s' }">
              让开源在校园里真正发生
            </h2>
            <p class="ob-desc" :style="{ '--delay': '0.3s' }">
              我们相信开源不仅是一种协作方式，更是一种学习姿态。<br>每一行代码都被认真审视，每一次提交都值得被讨论。<br>我们不追求一蹴而就的成果，而是在持续的实践中，把「<span style="font-style: italic">做出有用的东西</span>」变成日常。
            </p>
            <div class="ob-pillars" :style="{ '--delay': '0.45s' }">
            <div v-for="(p, i) in manifestoPillars" :key="p.title" class="ob-pillar">
                <div class="ob-pillar__icon">
                  <svg v-if="i === 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" stroke-linecap="round"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" stroke-linecap="round"/></svg>
                  <svg v-else-if="i === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 18l6-6-6-6M8 6l-6 6 6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10M18 20V4M6 20v-4" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </div>
                <strong class="ob-pillar__title">{{ p.title }}</strong>
                <p class="ob-pillar__desc">{{ p.desc }}</p>
              </div>
            </div>
            <button class="ob-btn ob-btn--primary" :style="{ '--delay': '0.6s' }" @click="nextStep">
              Git Init 使命
              <span class="ob-btn__arrow">→</span>
            </button>
          </div>
        </section>

        <!-- ====== Step 3: What We Do ====== -->
        <section v-else-if="step === 3" class="ob-step ob-step--what">
          <div class="ob-step__inner ob-step__inner--narrow">
            <p class="ob-eyebrow" :style="{ '--delay': '0.05s' }">日常方向 · WHAT WE DO</p>
            <h2 class="ob-title ob-title--md" :style="{ '--delay': '0.15s' }">
              我们做什么
            </h2>
            <p class="ob-desc" :style="{ '--delay': '0.3s' }">
              四个方向，构成社团日常的全部内容。每一个方向，都从真实需求出发，而非纸上谈兵。
            </p>
            <div class="ob-pillars" :style="{ '--delay': '0.45s' }">
              <div v-for="(item, idx) in whatWeDo" :key="item.title" class="ob-pillar">
                <div class="ob-pillar__icon">
                  <svg v-if="idx === 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  <svg v-else-if="idx === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4" stroke-linecap="round"/></svg>
                  <svg v-else-if="idx === 2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M6 9l6-6 6 6M6 15l6 6 6-6" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z" stroke-linecap="round"/></svg>
                </div>
                <strong class="ob-pillar__title">{{ item.title }}</strong>
                <p class="ob-pillar__desc">{{ item.desc }}</p>
              </div>
            </div>
            <button class="ob-btn ob-btn--primary" :style="{ '--delay': '0.6s' }" @click="nextStep">
              Explore 方向
              <span class="ob-btn__arrow">→</span>
            </button>
          </div>
        </section>

        <!-- ====== Step 4: Department ====== -->
        <section v-else-if="step === 4" class="ob-step ob-step--department">
          <div class="ob-step__inner ob-step__inner--narrow">
            <p class="ob-eyebrow" :style="{ '--delay': '0.05s' }">你的位置 · YOUR DEPARTMENT</p>

            <!-- Department Hero -->
            <div class="ob-dept-hero" :style="{ '--delay': '0.15s' }">
              <div class="ob-dept-hero__icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 21h18M3 7v1a3 3 0 006 0V7m0 1a3 3 0 006 0V7m0 1a3 3 0 006 0V7H3l2-4h14l2 4M5 21V10.87M19 21V10.87" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
              <h2 class="ob-dept-hero__name">
                {{ info?.membership?.departmentName || '尚未分配部门' }}
              </h2>
              <div v-if="info?.membership?.positionName" class="ob-dept-hero__position">
                <svg class="ob-dept-hero__position-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 7h-4V3H8v4H4c-1.1 0-2 .9-2 2v2h20V9c0-1.1-.9-2-2-2zM4 13v6c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2v-6H4z" stroke-linecap="round" stroke-linejoin="round"/></svg>
                {{ info.membership.positionName }}
              </div>
            </div>

            <p class="ob-desc" :style="{ '--delay': '0.3s' }">
              {{ info?.membership?.departmentDescription || '部门介绍即将上线，如有疑问请联系社长或管理员。' }}
            </p>

            <!-- Department Heads -->
            <div class="ob-dept-heads" :style="{ '--delay': '0.4s' }">
              <div v-if="info?.departmentHead" class="ob-dept-head ob-dept-head--primary">
                <div class="ob-dept-head__badge">部长</div>
                <div class="ob-dept-head__avatar">
                  <img v-if="info.departmentHead.avatar || info.departmentHead.qqAvatar" :src="info.departmentHead.avatar || info.departmentHead.qqAvatar" alt="" />
                  <span v-else>{{ info.departmentHead.initial || info.departmentHead.name?.charAt(0) }}</span>
                </div>
                <strong class="ob-dept-head__name">{{ info.departmentHead.name }}</strong>
              </div>
              <div v-if="info?.viceDepartmentHead" class="ob-dept-head">
                <div class="ob-dept-head__badge ob-dept-head__badge--vice">副部长</div>
                <div class="ob-dept-head__avatar">
                  <img v-if="info.viceDepartmentHead.qqAvatar || info.viceDepartmentHead.avatar" :src="info.viceDepartmentHead.qqAvatar || info.viceDepartmentHead.avatar" alt="" />
                  <span v-else>{{ info.viceDepartmentHead.initial || info.viceDepartmentHead.name?.charAt(0) }}</span>
                </div>
                <strong class="ob-dept-head__name">{{ info.viceDepartmentHead.name }}</strong>
              </div>
            </div>

            <div v-if="info?.membership?.departmentWechatQrcode" class="ob-qrcode" :style="{ '--delay': '0.5s' }">
              <span class="ob-mono-label">部门微信群</span>
              <img :src="info.membership.departmentWechatQrcode" alt="部门微信群二维码" class="ob-qrcode__img" />
              <p>扫码加入部门微信群</p>
            </div>

            <div class="ob-step__actions" :style="{ '--delay': '0.6s' }">
              <button class="ob-btn ob-btn--ghost" @click="prevStep">返回</button>
              <button class="ob-btn ob-btn--primary" @click="nextStep">
                Merge 入列
                <span class="ob-btn__arrow">→</span>
              </button>
            </div>
          </div>
        </section>

        <!-- ====== Step 5: Leadership ====== -->
        <section v-else-if="step === 5" class="ob-step ob-step--leaders">
          <div class="ob-step__inner ob-step__inner--narrow">
            <p class="ob-eyebrow" :style="{ '--delay': '0.05s' }">核心团队 · LEADERSHIP</p>
            <h2 class="ob-title ob-title--md" :style="{ '--delay': '0.15s' }">
              与你同行的人
            </h2>
            <p class="ob-desc" :style="{ '--delay': '0.3s' }">
              社团的核心管理团队，将带你一起探索开源世界。
            </p>

            <!-- Leaders Grid (all equal) -->
            <div class="ob-leaders-grid" :style="{ '--delay': '0.4s' }">
              <div v-if="info?.president" class="ob-leader-card">
                <div class="ob-leader-card__avatar">
                  <img v-if="info.president.qqAvatar || info.president.avatar" :src="info.president.qqAvatar || info.president.avatar" alt="" />
                  <span v-else>{{ info.president.initial || info.president.name?.charAt(0) }}</span>
                </div>
                <div class="ob-leader-card__info">
                  <span class="ob-leader-card__name">{{ info.president.name }}</span>
                  <span class="ob-leader-card__tag">社长</span>
                </div>
              </div>
              <div v-if="info?.leagueSecretary" class="ob-leader-card">
                <div class="ob-leader-card__avatar">
                  <img v-if="info.leagueSecretary.qqAvatar || info.leagueSecretary.avatar" :src="info.leagueSecretary.qqAvatar || info.leagueSecretary.avatar" alt="" />
                  <span v-else>{{ info.leagueSecretary.initial || info.leagueSecretary.name?.charAt(0) }}</span>
                </div>
                <div class="ob-leader-card__info">
                  <span class="ob-leader-card__name">{{ info.leagueSecretary.name }}</span>
                  <span class="ob-leader-card__tag">团支书</span>
                </div>
              </div>
              <div
                v-for="vp in info?.vicePresidents || []"
                :key="vp.userId"
                class="ob-leader-card"
              >
                <div class="ob-leader-card__avatar">
                  <img v-if="vp.qqAvatar || vp.avatar" :src="vp.qqAvatar || vp.avatar" alt="" />
                  <span v-else>{{ vp.initial || vp.name?.charAt(0) }}</span>
                </div>
                <div class="ob-leader-card__info">
                  <span class="ob-leader-card__name">{{ vp.name }}</span>
                  <span class="ob-leader-card__tag">副社长</span>
                </div>
              </div>
              <div
                v-if="!info?.president && !info?.leagueSecretary && (!info?.vicePresidents || !info.vicePresidents.length)"
                class="ob-leader--empty"
              >
                <span class="ob-mono-label">NOT SET</span>
                <p>暂未设置管理团队信息，请联系管理员完善。</p>
              </div>
            </div>

            <div class="ob-step__actions" :style="{ '--delay': '0.65s' }">
              <button class="ob-btn ob-btn--ghost" @click="prevStep">返回</button>
              <button class="ob-btn ob-btn--primary" @click="nextStep">
                Connect 团队
                <span class="ob-btn__arrow">→</span>
              </button>
            </div>
          </div>
        </section>

        <!-- ====== Step 6: Join Groups ====== -->
        <section v-else-if="step === 6" class="ob-step ob-step--groups">
          <div class="ob-step__inner ob-step__inner--narrow">
            <p class="ob-eyebrow" :style="{ '--delay': '0.05s' }">加入群聊 · JOIN US</p>
            <h2 class="ob-title ob-title--md" :style="{ '--delay': '0.15s' }">
              加入社团群聊
            </h2>
            <p class="ob-desc" :style="{ '--delay': '0.3s' }">
              扫码加入微信群，获取社团最新动态。加入QQ群是激活账号的前置条件，请生成验证码后申请入群。
            </p>

            <div v-if="info?.club?.wechatGroupQrcode" class="ob-qrcode" :style="{ '--delay': '0.4s' }">
              <span class="ob-mono-label">社团微信群</span>
              <img :src="info.club.wechatGroupQrcode" alt="社团微信群二维码" class="ob-qrcode__img" />
              <p>打开微信扫码加入社团总群</p>
            </div>

            <div v-if="info?.club?.qqGroupNumber" class="ob-group-card" :style="{ '--delay': '0.45s' }">
              <div class="ob-group-card__label">社团QQ群号</div>
              <div class="ob-group-card__number">{{ info.club.qqGroupNumber }}</div>
            </div>

            <div class="ob-token-area" :style="{ '--delay': '0.5s' }">
              <template v-if="groupJoinToken">
                <div class="ob-token-card">
                  <span class="ob-token-card__label">入群验证码</span>
                  <code class="ob-token-card__code">{{ groupJoinToken }}</code>
                </div>
                <button class="ob-btn ob-btn--glass" @click="copyToken">
                  复制验证码
                </button>
              </template>
              <button v-else class="ob-btn ob-btn--glass" :loading="tokenLoading" @click="generateJoinToken">
                生成入群验证码
              </button>
            </div>

            <div v-if="info?.qqGroupJoined" class="ob-joined-badge" :style="{ '--delay': '0.55s' }">
              <span class="ob-check-icon">✓</span> 已加入QQ群
            </div>

            <div v-else-if="groupJoinToken" class="ob-polling-hint" :style="{ '--delay': '0.55s' }">
              <span class="ob-polling-hint__dot" :class="{ 'ob-polling-hint__dot--checking': checkingStatus }"></span>
              {{ checkingStatus ? '正在检测入群状态…' : '已复制验证码，请在QQ群中提交入群申请，系统将自动检测' }}
            </div>

            <div class="ob-step__actions" :style="{ '--delay': '0.65s' }">
              <button class="ob-btn ob-btn--ghost" @click="prevStep">返回</button>
              <button
                class="ob-btn ob-btn--primary"
                :disabled="!info?.qqGroupJoined"
                @click="nextStep"
              >
                {{ info?.qqGroupJoined ? 'Link 社群' : '请先加入QQ群' }}
                <span v-if="info?.qqGroupJoined" class="ob-btn__arrow">→</span>
              </button>
            </div>
          </div>
        </section>

        <!-- ====== Step 7: Password ====== -->
        <section v-else-if="step === 7" class="ob-step ob-step--password">
          <div class="ob-step__inner ob-step__inner--narrow">
            <p class="ob-eyebrow" :style="{ '--delay': '0.05s' }">安全设置 · SECURITY</p>
            <h2 class="ob-title ob-title--md" :style="{ '--delay': '0.15s' }">
              设置你的密码
            </h2>
            <p class="ob-desc" :style="{ '--delay': '0.3s' }">
              这是激活流程的最后一步。设置新密码后，你的账号将正式激活，可使用所有功能。
            </p>

            <form class="ob-form" :style="{ '--delay': '0.4s' }" @submit.prevent="handleActivate">
              <div class="ob-input-group">
                <input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  class="ob-input"
                  placeholder="当前密码"
                  autocomplete="current-password"
                />
              </div>
              <div class="ob-input-group">
                <input
                  v-model="passwordForm.newPassword"
                  type="password"
                  class="ob-input"
                  placeholder="新密码（至少4位）"
                  autocomplete="new-password"
                />
              </div>
              <div class="ob-step__actions">
                <button type="button" class="ob-btn ob-btn--ghost" @click="prevStep">返回</button>
                <button
                  type="submit"
                  class="ob-btn ob-btn--primary"
                  :disabled="!passwordForm.oldPassword || !passwordForm.newPassword || passwordForm.newPassword.length < 4"
                >
                  Deploy 激活
                  <span class="ob-btn__arrow">→</span>
                </button>
              </div>
            </form>
          </div>
        </section>

        <!-- ====== Step 8: Activating ====== -->
        <section v-else-if="step === 8" class="ob-step ob-step--loading">
          <div class="ob-step__inner">
            <div class="ob-loader">
              <svg class="ob-loader__ring" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="46" stroke="currentColor" stroke-width="2" fill="none" opacity="0.15"/>
                <circle cx="50" cy="50" r="46" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" class="ob-loader__arc"/>
              </svg>
              <p class="ob-loader__text">{{ loadingText }}</p>
            </div>
          </div>
        </section>

        <!-- ====== Step 9: Success ====== -->
        <section v-else-if="step === 9" class="ob-step ob-step--success">
          <div class="ob-step__inner">

            <!-- Success Animation -->
            <div class="ob-success-check" :class="{ 'ob-success-check--in': stepInAnim }">
              <svg viewBox="0 0 64 64" class="ob-success-check__svg">
                <circle
                    cx="32"
                    cy="32"
                    r="28"
                    stroke="currentColor"
                    stroke-width="2"
                    fill="none"
                    class="ob-success-check__circle"
                />
                <path
                    d="M20 33 L28 41 L44 23"
                    stroke="currentColor"
                    stroke-width="3"
                    fill="none"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    class="ob-success-check__path"
                />
              </svg>
            </div>

            <p class="ob-eyebrow" :style="{ '--delay': '0.15s' }">
              Welcome to JMI-OPENATOM
            </p>

            <h1 class="ob-title" :style="{ '--delay': '0.3s' }">
              欢迎加入
            </h1>

            <h2 class="ob-name" :style="{ '--delay': '0.45s' }">
              {{ displayName }}
            </h2>

            <p class="ob-subtitle" :style="{ '--delay': '0.65s' }">
              你的身份已完成激活。
              <br />
              欢迎成为
              <strong>{{ info?.club?.name || '开放原子开源社团' }}</strong>
              的正式成员。
            </p>

            <div class="ob-success-terminal" :style="{ '--delay': '0.8s' }">
              <div class="ob-success-terminal__bar">
                <span class="ob-success-terminal__dot ob-success-terminal__dot--red"></span>
                <span class="ob-success-terminal__dot ob-success-terminal__dot--yellow"></span>
                <span class="ob-success-terminal__dot ob-success-terminal__dot--green"></span>
                <span class="ob-success-terminal__title">~/openatom</span>
              </div>
              <div class="ob-success-terminal__body">
                <p><span class="ob-term-prompt">$</span> git status</p>
                <p class="ob-term-success">✓ Account activated successfully</p>
                <p><span class="ob-term-prompt">$</span> echo "Code. Share. Inspire."</p>
                <p class="ob-term-output">Code. Share. Inspire.<span class="ob-term-cursor">█</span></p>
              </div>
            </div>

            <button
                class="ob-btn ob-btn--primary ob-btn--glow"
                :style="{ '--delay': '1s' }"
                @click="enterHome"
            >
              <svg class="ob-btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>
              Launch
              <span class="ob-btn__arrow">→</span>
            </button>

          </div>
        </section>
      </component>
    </Transition>
  </div>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus/es/components/message/index'
import { computed, onBeforeUnmount, onMounted, ref, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, siteApi } from '@/api'
import { setSession, getToken, getCurrentUser } from '@/utils/auth.ts'

interface Leader {
  userId: number
  name: string
  initial: string
  role: string
  avatar?: string
  qqAvatar?: string
}

interface MembershipInfo {
  clubId: number
  departmentId: number | null
  departmentName: string | null
  departmentDescription: string | null
  positionId: number | null
  positionName: string | null
  status: string
  joinedAt: string
  departmentWechatQrcode?: string
}

interface ClubInfo {
  id: number
  name: string
  code: string
  category?: string
  description?: string
  logoUrl?: string
  focusAreas: Array<{ title: string; description: string }>
  qqGroupNumber?: string
  wechatGroupQrcode?: string
}

interface ActivationInfo {
  userId: number
  userName: string
  realName: string
  avatar?: string
  qqOpenid?: string
  activated: boolean
  activatedAt?: string
  qqGroupJoined?: boolean
  membership?: MembershipInfo | null
  departmentHead?: Leader | null
  viceDepartmentHead?: Leader | null
  president?: Leader | null
  vicePresidents?: Leader[]
  leagueSecretary?: Leader | null
  club?: ClubInfo
}

const TOTAL_STEPS = 9
const router = useRouter()

// ============ State ============
const loaded = ref(false)
const step = ref(1)
const stepInAnim = ref(false)
const transitionName = ref('ob-slide-next')
const info = ref<ActivationInfo | null>(null)
const groupJoinToken = ref('')
const tokenLoading = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '' })
const loadingText = ref('正在验证身份…')
const orb1Ref = ref<HTMLElement>()
const orb2Ref = ref<HTMLElement>()
const orb3Ref = ref<HTMLElement>()
const checkingStatus = ref(false)

let parallaxFrame = 0
let parallaxY = 0
let groupPollTimer: ReturnType<typeof setInterval> | undefined

const manifestoPillars = [
  { title: '开放协作', desc: '代码、文档、思路全部公开，让每个人都能参与和讨论。' },
  { title: '工程实践', desc: '用真实项目训练工程能力，从需求到上线全流程参与。' },
  { title: '持续成长', desc: '不与他人比较，只与昨天的自己比较，保持长期主义。' },
]

const whatWeDo = [
  { title: '技术分享', desc: '定期内部分享会，从基础工具到前沿话题，由成员轮流主讲。每一次分享都是对自己知识的整理，也是对同伴的馈赠。' },
  { title: '项目实践', desc: '我们维护若干长期项目，覆盖 Web、Bot、工具链等方向。新成员从一个小 feature 起步，逐步承担更完整的模块。' },
  { title: '竞赛训练', desc: '组队参加各类程序设计、开源相关比赛，把日常积累转化为赛场发挥。赛后的复盘与沉淀，比奖牌本身更重要。' },
  { title: '开源协作', desc: '鼓励成员向上游开源项目提交 PR、参与社区讨论，把校园里的练习延伸到更广阔的开源世界。' },
]

const displayName = computed(() => info.value?.realName || info.value?.userName || '新成员')

// ============ Step Navigation ============
function nextStep() {
  transitionName.value = 'ob-slide-next'
  stepInAnim.value = false
  step.value = Math.min(step.value + 1, TOTAL_STEPS)
}

function prevStep() {
  transitionName.value = 'ob-slide-prev'
  stepInAnim.value = false
  step.value = Math.max(step.value - 1, 1)
}

function goToStep(n: number) {
  if (n === step.value) return
  transitionName.value = n > step.value ? 'ob-slide-next' : 'ob-slide-prev'
  stepInAnim.value = false
  step.value = n
}

function onStepEntered() {
  nextTick(() => {
    stepInAnim.value = true
  })
}

function enterHome() {
  router.replace('/')
}

// ============ Particle Background ============
function particleStyle(n: number) {
  const seed = n * 37
  const size = 2 + (seed % 3)
  const left = (seed * 7) % 100
  const top = (seed * 11) % 100
  const duration = 15 + (seed % 10)
  const delay = -(seed % 8)
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
  } as Record<string, string>
}

// ============ Parallax ============
function onScroll() {
  parallaxY = window.scrollY || window.pageYOffset
  if (parallaxFrame) return
  parallaxFrame = requestAnimationFrame(() => {
    parallaxFrame = 0
    const orbs = [orb1Ref.value, orb2Ref.value, orb3Ref.value].filter(Boolean) as HTMLElement[]
    orbs.forEach((orb, i) => {
      orb.style.transform = `translate3d(0, ${parallaxY * (0.02 + i * 0.012)}px, 0)`
    })
  })
}

// ============ Business Logic ============
async function fetchActivationInfo() {
  try {
    const result = await siteApi.activation()
    const wasJoined = info.value?.qqGroupJoined
    info.value = result as ActivationInfo
    // Sync server-side activation status to localStorage
    setSession({
      user: { ...getCurrentUser(), activatedAt: info.value?.activatedAt || null },
    })
    // If already activated, skip to success step
    if (info.value?.activated) {
      step.value = 9
      stepInAnim.value = true
    }
    // If QQ group join status changed to true during polling, notify the user
    if (!wasJoined && info.value?.qqGroupJoined && step.value === 6) {
      ElMessage.success('检测到你已加入QQ群，可以继续下一步')
    }
  } catch (error) {
    console.error('加载激活信息失败', error)
    ElMessage.error('激活信息加载失败，请刷新重试')
  }
}

// ============ QQ Group Join Status Polling ============
function startGroupPolling() {
  stopGroupPolling()
  // Only poll if not yet joined
  if (info.value?.qqGroupJoined) return
  groupPollTimer = setInterval(async () => {
    if (info.value?.qqGroupJoined) {
      stopGroupPolling()
      return
    }
    checkingStatus.value = true
    try {
      await fetchActivationInfo()
    } finally {
      checkingStatus.value = false
    }
  }, 5000)
}

function stopGroupPolling() {
  if (groupPollTimer) {
    clearInterval(groupPollTimer)
    groupPollTimer = undefined
  }
}

// Start/stop polling based on current step (Step 6 = Join Groups)
watch(step, (newStep) => {
  if (newStep === 6 && !info.value?.qqGroupJoined) {
    startGroupPolling()
  } else {
    stopGroupPolling()
  }
}, { immediate: false })

async function generateJoinToken() {
  tokenLoading.value = true
  try {
    const result = await authApi.createGroupJoinToken()
    groupJoinToken.value = result.token || ''
    ElMessage.success(`入群验证码已生成，有效期为 ${Math.floor((result.expiresIn || 1800) / 60)} 分钟`)
  } catch {
    ElMessage.error('生成验证码失败，请重试')
  } finally {
    tokenLoading.value = false
  }
}

async function copyToken() {
  if (!groupJoinToken.value) return
  try {
    await navigator.clipboard.writeText(groupJoinToken.value)
    ElMessage.success('验证码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

async function handleActivate() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword || passwordForm.value.newPassword.length < 4) {
    ElMessage.warning('请填写正确的密码信息')
    return
  }
  // Move to loading step
  transitionName.value = 'ob-slide-next'
  stepInAnim.value = false
  step.value = 8
  loadingText.value = '正在验证身份…'

  try {
    // Phase 1: Activate
    loadingText.value = '正在激活账号…'
    await new Promise(resolve => setTimeout(resolve, 600))
    const result = await authApi.activate()
    if (result) setSession({ accessToken: getToken() || undefined, user: result })

    // Phase 2: Update password
    loadingText.value = '正在设置密码…'
    await new Promise(resolve => setTimeout(resolve, 400))
    await authApi.updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })

    // Phase 3: Success
    loadingText.value = '即将完成…'
    await new Promise(resolve => setTimeout(resolve, 600))

    // Sync activation status
    if (info.value) {
      info.value.activated = true
      info.value.activatedAt = new Date().toISOString()
    }

    // Move to success step
    transitionName.value = 'ob-slide-next'
    stepInAnim.value = false
    step.value = 9
    ElMessage.success('账号已激活，请使用新密码重新登录')
  } catch (error: any) {
    const msg = error?.message || error?.msg || '激活失败，请重试'
    ElMessage.error(msg)
    // Go back to password step
    transitionName.value = 'ob-slide-prev'
    stepInAnim.value = false
    step.value = 7
  }
}

// ============ Lifecycle ============
onMounted(async () => {
  await fetchActivationInfo()
  requestAnimationFrame(() => {
    loaded.value = true
    stepInAnim.value = true
    window.addEventListener('scroll', onScroll, { passive: true })
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (parallaxFrame) cancelAnimationFrame(parallaxFrame)
  stopGroupPolling()
})
</script>

<style scoped>
/* ============ Base ============ */
.onboard {
  position: fixed;
  inset: 0;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--oa-page-bg);
  color: var(--oa-text);
  font-family: 'SF Pro Display', -apple-system, system-ui, BlinkMacSystemFont, sans-serif;
  opacity: 0;
  transition: opacity 1.2s cubic-bezier(0.22, 1, 0.36, 1);
  isolation: isolate;
  user-select: none;
  -webkit-user-select: none;
}

.onboard--loaded {
  opacity: 1;
}

/* ============ Background Layers ============ */
.onboard__bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.onboard__bg-gradient {
  position: absolute;
  inset: 0;
  background: var(--oa-hero-gradient);
}

.onboard__bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  will-change: transform;
  opacity: 0.6;
}

.onboard__bg-orb--1 {
  top: -10%;
  left: -5%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, color-mix(in srgb, var(--oa-active-bg) 12%, transparent), transparent 70%);
  animation: orbFloat1 24s ease-in-out infinite;
}

.onboard__bg-orb--2 {
  bottom: -15%;
  right: -8%;
  width: 440px;
  height: 440px;
  background: radial-gradient(circle, color-mix(in srgb, #5b8def 14%, transparent), transparent 70%);
  animation: orbFloat2 30s ease-in-out infinite;
}

.onboard__bg-orb--3 {
  top: 40%;
  left: 50%;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, color-mix(in srgb, var(--oa-text) 6%, transparent), transparent 70%);
  animation: orbFloat3 20s ease-in-out infinite;
}

@keyframes orbFloat1 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(30px, 40px, 0) scale(1.08); }
}

@keyframes orbFloat2 {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-40px, -30px, 0) scale(0.95); }
}

@keyframes orbFloat3 {
  0%, 100% { transform: translate3d(-50%, 0, 0) scale(1); }
  50% { transform: translate3d(-50%, 20px, 0) scale(1.1); }
}

.onboard__bg-grid {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(color-mix(in srgb, var(--oa-text) 1.5%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--oa-text) 1.5%, transparent) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at 50% 35%, rgba(0,0,0,0.35), transparent 68%);
  -webkit-mask-image: radial-gradient(ellipse at 50% 35%, rgba(0,0,0,0.35), transparent 68%);
}

.onboard__bg-grid::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 49.5%, color-mix(in srgb, var(--oa-active-bg) 3%, transparent) 49.5%, color-mix(in srgb, var(--oa-active-bg) 3%, transparent) 50.5%, transparent 50.5%);
  background-size: 200px 200px;
  mask-image: radial-gradient(ellipse at 50% 40%, rgba(0,0,0,0.3), transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at 50% 40%, rgba(0,0,0,0.3), transparent 70%);
}

/* Particle */
.onboard__bg-particles {
  position: absolute;
  inset: 0;
}

.onboard__particle {
  position: absolute;
  border-radius: 50%;
  background: color-mix(in srgb, var(--oa-text) 20%, transparent);
  animation: particleDrift 20s linear infinite;
  opacity: 0.4;
}

@keyframes particleDrift {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.4; }
  90% { opacity: 0.3; }
  100% { transform: translateY(-120px) translateX(20px); opacity: 0; }
}

/* ============ Progress Bar ============ */
.onboard__progress {
  position: fixed;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 14px;
}

.onboard__progress-track {
  width: 200px;
  height: 2px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-text) 8%, transparent);
  overflow: hidden;
  position: relative;
}

.onboard__progress-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--oa-active-bg);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.onboard__progress-info {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  letter-spacing: 0.08em;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.onboard__progress-current {
  font-weight: 600;
  color: var(--oa-text);
  font-size: 14px;
}

.onboard__progress-sep {
  color: var(--oa-faint);
  margin: 0 2px;
}

.onboard__progress-total {
  color: var(--oa-muted);
}

/* ============ Step Container ============ */
.onboard__step {
  position: relative;
  z-index: 10;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px 80px;
}

.ob-step {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ob-step__inner {
  max-width: 1000px;
  width: 100%;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.ob-step__inner--narrow {
  max-width: 920px;
}

/* ============ Step Transition Animations ============ */
.ob-slide-next-enter-active,
.ob-slide-next-leave-active,
.ob-slide-prev-enter-active,
.ob-slide-prev-leave-active {
  transition:
    opacity 0.6s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.6s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.ob-slide-next-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.97);
  filter: blur(8px);
}

.ob-slide-next-leave-to {
  opacity: 0;
  transform: translateY(-30px) scale(0.97);
  filter: blur(8px);
}

.ob-slide-prev-enter-from {
  opacity: 0;
  transform: translateY(-30px) scale(0.97);
  filter: blur(8px);
}

.ob-slide-prev-leave-to {
  opacity: 0;
  transform: translateY(30px) scale(0.97);
  filter: blur(8px);
}

/* ============ Entrance Animation for Elements ============ */
.ob-eyebrow,
.ob-title,
.ob-subtitle,
.ob-desc,
.ob-btn,
.ob-pillars,
.ob-group-card,
.ob-token-area,
.ob-joined-badge,
.ob-polling-hint,
.ob-form,
.ob-logo,
.ob-position-tag,
.ob-dept-heads,
.ob-qrcode,
.ob-leader-featured,
.ob-leaders-grid,
.ob-success-terminal,
.ob-os-badge,
.ob-welcome-name {
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.7s ease;
}

.ob-step--welcome .ob-logo,
.ob-step--success .ob-success-check {
  opacity: 0;
  transform: scale(0.8);
  filter: blur(10px);
  transition:
    opacity 0.9s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.9s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.9s ease;
}

.ob-logo--in,
.ob-success-check--in {
  opacity: 1 !important;
  transform: scale(1) !important;
  filter: blur(0) !important;
}

.onboard--loaded .ob-eyebrow,
.onboard--loaded .ob-title,
.onboard--loaded .ob-subtitle,
.onboard--loaded .ob-desc,
.onboard--loaded .ob-btn,
.onboard--loaded .ob-pillars,
.onboard--loaded .ob-group-card,
.onboard--loaded .ob-token-area,
.onboard--loaded .ob-joined-badge,
.onboard--loaded .ob-polling-hint,
.onboard--loaded .ob-form,
.onboard--loaded .ob-position-tag,
.onboard--loaded .ob-dept-heads,
.onboard--loaded .ob-qrcode,
.onboard--loaded .ob-leader-featured,
.onboard--loaded .ob-leaders-grid,
.onboard--loaded .ob-success-terminal,
.onboard--loaded .ob-os-badge,
.onboard--loaded .ob-welcome-name {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

/* ============ Typography ============ */
.ob-eyebrow {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: var(--oa-muted);
  text-transform: uppercase;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.ob-eyebrow::before {
  content: '';
  width: 24px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--oa-muted));
  flex-shrink: 0;
}

.ob-eyebrow::after {
  content: '';
  width: 24px;
  height: 1px;
  background: linear-gradient(90deg, var(--oa-muted), transparent);
  flex-shrink: 0;
}

.ob-mono-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.2em;
  color: var(--oa-muted);
  text-transform: uppercase;
  display: block;
  margin-bottom: 8px;
}

.ob-title {
  margin: 0;
  font-size: clamp(40px, 6vw, 56px);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--oa-text);
}

.ob-title--md {
  font-size: clamp(28px, 4vw, 38px);
}

.ob-subtitle {
  margin: 0;
  max-width: 620px;
  font-size: clamp(17px, 2vw, 21px);
  font-weight: 400;
  line-height: 1.75;
  color: var(--oa-text-soft);
}

.ob-subtitle strong {
  font-weight: 600;
  color: var(--oa-text);
}

.ob-desc {
  margin: 0;
  max-width: 820px;
  font-size: clamp(15px, 1.6vw, 18px);
  line-height: 1.8;
  color: var(--oa-text-soft);
}

/* ============ Logo ============ */
.ob-logo {
  margin-bottom: 8px;
}

.ob-logo__svg {
  width: 80px;
  height: 80px;
  color: var(--oa-text);
}

/* ============ Buttons ============ */
.ob-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 54px;
  padding: 0 32px;
  border: none;
  border-radius: 999px;
  font-size: 17px;
  font-weight: 500;
  font-family: inherit;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition:
    transform 0.25s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.25s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.2s;
  position: relative;
  overflow: hidden;
  -webkit-tap-highlight-color: transparent;
}

.ob-btn:active {
  transform: scale(0.97);
}

.ob-btn--primary {
  background: var(--oa-active-bg);
  color: var(--oa-active-text);
  box-shadow: 0 4px 20px color-mix(in srgb, var(--oa-active-bg) 25%, transparent);
}

.ob-btn--primary:hover {
  transform: scale(1.03);
  box-shadow: 0 8px 32px color-mix(in srgb, var(--oa-active-bg) 35%, transparent);
}

.ob-btn--glass {
  background: color-mix(in srgb, var(--oa-surface) 80%, transparent);
  color: var(--oa-text);
  border: 1px solid var(--oa-border);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.ob-btn--glass:hover {
  transform: scale(1.02);
  border-color: var(--oa-border-strong);
  box-shadow: 0 8px 28px color-mix(in srgb, var(--oa-text) 12%, transparent);
}

.ob-btn--ghost {
  background: transparent;
  color: var(--oa-muted);
}

.ob-btn--ghost:hover {
  color: var(--oa-text);
  background: color-mix(in srgb, var(--oa-text) 6%, transparent);
}

.ob-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

.ob-btn__arrow {
  font-size: 20px;
  transition: transform 0.25s ease;
}

.ob-btn:hover .ob-btn__arrow {
  transform: translateX(4px);
}

/* Ripple effect */
.ob-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.4s ease, height 0.4s ease;
}

.ob-btn:active::after {
  width: 300px;
  height: 300px;
}

/* ============ Pillars (grid layout) ============ */
.ob-pillars {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 10px;
  width: 100%;
  margin-top: 8px;
}

.ob-pillar__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--oa-text);
}

.ob-pillar__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--oa-text-soft);
}

/* ============ Position Tag (Step 4) ============ */
.ob-position-tag {
  display: inline-block;
  padding: 5px 16px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-active-bg) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--oa-active-bg) 12%, transparent);
  color: color-mix(in srgb, var(--oa-active-bg) 70%, var(--oa-text));
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

/* ============ Department Hero (Step 4) ============ */
.ob-dept-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition: opacity 0.7s cubic-bezier(0.22,1,0.36,1), transform 0.7s cubic-bezier(0.22,1,0.36,1), filter 0.7s ease;
}

.onboard--loaded .ob-dept-hero {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

.ob-dept-hero__icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--oa-active-bg) 12%, transparent), color-mix(in srgb, var(--oa-active-bg) 4%, transparent));
  border: 1px solid color-mix(in srgb, var(--oa-active-bg) 15%, transparent);
}

.ob-dept-hero__icon svg {
  width: 28px;
  height: 28px;
  color: color-mix(in srgb, var(--oa-active-bg) 75%, var(--oa-text));
}

.ob-dept-hero__name {
  margin: 0;
  font-size: clamp(30px, 4.5vw, 42px);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
  color: var(--oa-text);
}

.ob-dept-hero__position {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 16px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-active-bg) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--oa-active-bg) 14%, transparent);
  color: color-mix(in srgb, var(--oa-active-bg) 70%, var(--oa-text));
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.03em;
}

.ob-dept-hero__position-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* ============ Department Heads (Step 4) - vertical cards ============ */
.ob-dept-heads {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 16px;
  width: 100%;
}

.ob-dept-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 26px 36px 22px;
  border-radius: 20px;
  background: color-mix(in srgb, var(--oa-surface) 35%, transparent);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
  transition: transform 0.35s cubic-bezier(0.22,1,0.36,1), border-color 0.35s ease, box-shadow 0.35s ease;
  position: relative;
  min-width: 155px;
}

.ob-dept-head:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--oa-active-bg) 20%, var(--oa-border));
  box-shadow: 0 16px 48px color-mix(in srgb, var(--oa-text) 8%, transparent);
}

.ob-dept-head--primary {
  background: linear-gradient(160deg, color-mix(in srgb, var(--oa-active-bg) 6%, var(--oa-surface) 40%), color-mix(in srgb, var(--oa-surface) 30%, transparent));
  border-color: color-mix(in srgb, var(--oa-active-bg) 12%, var(--oa-border));
}

.ob-dept-head--primary:hover {
  border-color: color-mix(in srgb, var(--oa-active-bg) 28%, var(--oa-border));
  box-shadow: 0 16px 48px color-mix(in srgb, var(--oa-active-bg) 12%, transparent);
}

.ob-dept-head__badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--oa-active-bg) 70%, var(--oa-text));
  padding: 3px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-active-bg) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--oa-active-bg) 10%, transparent);
}

.ob-dept-head__badge--vice {
  color: var(--oa-muted);
  background: color-mix(in srgb, var(--oa-text) 4%, transparent);
  border-color: color-mix(in srgb, var(--oa-border) 50%, transparent);
}

.ob-dept-head__avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid color-mix(in srgb, var(--oa-border) 60%, transparent);
  background: color-mix(in srgb, var(--oa-text) 4%, transparent);
  font-size: 18px;
  font-weight: 600;
  color: var(--oa-muted);
  flex-shrink: 0;
  transition: border-color 0.3s ease;
}

.ob-dept-head--primary .ob-dept-head__avatar {
  border-color: color-mix(in srgb, var(--oa-active-bg) 35%, var(--oa-border));
}

.ob-dept-head__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ob-dept-head__name {
  font-size: 17px;
  font-weight: 700;
  color: var(--oa-text);
  letter-spacing: -0.01em;
}

/* ============ QR Code - no card ============ */
.ob-qrcode {
  text-align: center;
  padding: 18px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--oa-surface) 30%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
}

.ob-qrcode__img {
  display: block;
  width: 160px;
  height: 160px;
  margin: 8px auto;
  border-radius: 8px;
  object-fit: contain;
}

.ob-qrcode p {
  font-size: 12px;
  color: var(--oa-muted);
}

/* ============ Leaders (Step 5) - Featured + Grid ============ */
.ob-leader-featured {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 36px 48px 28px;
  border-radius: 24px;
  background: linear-gradient(160deg, color-mix(in srgb, var(--oa-active-bg) 8%, var(--oa-surface) 40%), color-mix(in srgb, var(--oa-surface) 30%, transparent));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid color-mix(in srgb, var(--oa-active-bg) 18%, var(--oa-border));
  overflow: hidden;
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition: opacity 0.7s cubic-bezier(0.22,1,0.36,1), transform 0.7s cubic-bezier(0.22,1,0.36,1), filter 0.7s ease, border-color 0.35s ease, box-shadow 0.35s ease;
}

.onboard--loaded .ob-leader-featured {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

.ob-leader-featured:hover {
  border-color: color-mix(in srgb, var(--oa-active-bg) 30%, var(--oa-border));
  box-shadow: 0 20px 60px color-mix(in srgb, var(--oa-active-bg) 14%, transparent);
}

.ob-leader-featured__glow {
  position: absolute;
  top: -40%;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--oa-active-bg) 18%, transparent), transparent 70%);
  filter: blur(40px);
  pointer-events: none;
}

.ob-leader-featured__avatar {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: visible;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 1;
}

.ob-leader-featured__avatar::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    color-mix(in srgb, var(--oa-active-bg) 40%, transparent),
    color-mix(in srgb, var(--oa-active-bg) 10%, transparent),
    color-mix(in srgb, var(--oa-active-bg) 40%, transparent)
  );
  animation: leaderRingSpin 12s linear infinite;
  z-index: -1;
}

.ob-leader-featured__avatar::after {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  border: 2px solid color-mix(in srgb, var(--oa-active-bg) 30%, transparent);
  z-index: 0;
}

@keyframes leaderRingSpin {
  to { transform: rotate(360deg); }
}

.ob-leader-featured__avatar img,
.ob-leader-featured__avatar span {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--oa-text) 6%, transparent);
  font-size: 24px;
  font-weight: 600;
  color: var(--oa-muted);
  position: relative;
  z-index: 1;
}

.ob-leader-featured__info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  z-index: 1;
}

.ob-leader-featured__tag {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--oa-active-bg) 65%, var(--oa-text));
}

.ob-leader-featured__name {
  font-size: 24px;
  font-weight: 700;
  color: var(--oa-text);
  letter-spacing: -0.02em;
}

/* Leaders Grid */
.ob-leaders-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100%;
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition: opacity 0.7s cubic-bezier(0.22,1,0.36,1), transform 0.7s cubic-bezier(0.22,1,0.36,1), filter 0.7s ease;
}

.onboard--loaded .ob-leaders-grid {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

.ob-leader-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 26px 36px 22px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--oa-surface) 35%, transparent);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
  transition: transform 0.35s cubic-bezier(0.22,1,0.36,1), border-color 0.35s ease, box-shadow 0.35s ease;
  min-width: 155px;
}

.ob-leader-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--oa-active-bg) 20%, var(--oa-border));
  box-shadow: 0 14px 44px color-mix(in srgb, var(--oa-text) 8%, transparent);
}

.ob-leader-card__avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid color-mix(in srgb, var(--oa-border) 50%, transparent);
  background: color-mix(in srgb, var(--oa-text) 4%, transparent);
  font-size: 17px;
  font-weight: 600;
  color: var(--oa-muted);
  flex-shrink: 0;
}

.ob-leader-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ob-leader-card__info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.ob-leader-card__name {
  font-size: 17px;
  font-weight: 700;
  color: var(--oa-text);
  letter-spacing: -0.01em;
}

.ob-leader-card__tag {
  font-size: 12px;
  font-weight: 500;
  color: var(--oa-muted);
  letter-spacing: 0.05em;
}

.ob-leader--empty {
  text-align: center;
  color: var(--oa-muted);
}

.ob-leader--empty p {
  font-size: 14px;
  margin-top: 8px;
}

/* ============ Group Info (Step 6) - no card ============ */
.ob-group-card {
  text-align: center;
  padding: 14px 28px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--oa-surface) 30%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
}

.ob-group-card__label {
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--oa-muted);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.ob-group-card__number {
  font-size: 28px;
  font-weight: 700;
  color: var(--oa-text);
  letter-spacing: 0.06em;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

/* ============ Token Area - no card ============ */
.ob-token-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.ob-token-card {
  text-align: center;
  padding: 14px 28px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--oa-surface) 30%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
}

.ob-token-card__label {
  display: block;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: var(--oa-muted);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.ob-token-card__code {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--oa-text);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

/* ============ Joined Badge & Polling Hint ============ */
.ob-joined-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: 999px;
  background: color-mix(in srgb, #22c55e 12%, transparent);
  color: #22c55e;
  font-size: 14px;
  font-weight: 500;
}

.ob-check-icon {
  font-size: 16px;
}

.ob-polling-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-text) 6%, transparent);
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.6;
}

.ob-polling-hint__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--oa-muted);
  flex-shrink: 0;
  animation: pollingPulse 2s ease-in-out infinite;
}

.ob-polling-hint__dot--checking {
  background: #3b82f6;
  animation: pollingChecking 0.8s ease-in-out infinite;
}

@keyframes pollingPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}

@keyframes pollingChecking {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.4); opacity: 1; }
}

/* ============ Step Actions ============ */
.ob-step__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 8px;
}

/* ============ Glass Input (Step 7) ============ */
.ob-form {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ob-input-group {
  position: relative;
}

.ob-input {
  width: 100%;
  height: 56px;
  padding: 0 24px;
  border: 1px solid var(--oa-border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--oa-surface) 50%, transparent);
  color: var(--oa-text);
  font-size: 17px;
  font-family: inherit;
  outline: none;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  user-select: text;
  -webkit-user-select: text;
  transition:
    border-color 0.3s ease,
    box-shadow 0.3s ease,
    background 0.3s ease;
}

.ob-input::placeholder {
  color: var(--oa-faint);
}

.ob-input:focus {
  border-color: color-mix(in srgb, var(--oa-active-bg) 50%, transparent);
  box-shadow:
    0 0 0 4px color-mix(in srgb, var(--oa-active-bg) 10%, transparent),
    0 4px 20px color-mix(in srgb, var(--oa-text) 8%, transparent);
  background: color-mix(in srgb, var(--oa-surface) 80%, transparent);
}

/* ============ Loader (Step 8) ============ */
.ob-loader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.ob-loader__ring {
  width: 80px;
  height: 80px;
  color: var(--oa-text);
}

.ob-loader__arc {
  stroke-dasharray: 289;
  stroke-dashoffset: 289;
  animation: loaderArc 1.5s ease-in-out infinite;
  transform-origin: 50% 50%;
}

@keyframes loaderArc {
  0% { stroke-dashoffset: 289; transform: rotate(0deg); }
  50% { stroke-dashoffset: 72; transform: rotate(180deg); }
  100% { stroke-dashoffset: 289; transform: rotate(360deg); }
}

.ob-loader__text {
  font-size: 16px;
  color: var(--oa-muted);
  letter-spacing: 0.04em;
  animation: loaderPulse 1.5s ease-in-out infinite;
}

@keyframes loaderPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

/* ============ Success Check (Step 9) ============ */
.ob-success-check {
  margin-bottom: 8px;
}

.ob-success-check__svg {
  width: 96px;
  height: 96px;
  color: var(--oa-text);
}

.ob-success-check__circle {
  stroke-dasharray: 176;
  stroke-dashoffset: 176;
  animation: checkCircle 0.6s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.ob-success-check__path {
  stroke-dasharray: 40;
  stroke-dashoffset: 40;
  animation: checkPath 0.4s 0.5s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.ob-success-check--in .ob-success-check__circle {
  animation: checkCircle 0.6s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.ob-success-check--in .ob-success-check__path {
  animation: checkPath 0.4s 0.5s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

@keyframes checkCircle {
  to { stroke-dashoffset: 0; }
}

@keyframes checkPath {
  to { stroke-dashoffset: 0; }
}

/* ============ Responsive ============ */
@media (max-width: 640px) {
  .ob-step__inner {
    padding: 0 12px;
  }

  .ob-title {
    font-size: clamp(36px, 8vw, 48px);
  }

  .ob-subtitle {
    font-size: clamp(16px, 4vw, 20px);
  }

  .ob-group-card {
    padding: 20px 24px;
  }

  .ob-group-card__number {
    font-size: 26px;
  }

  .ob-pillar {
    gap: 12px;
  }

  .ob-leaders {
    flex-direction: column;
  }

  .ob-qrcode__img {
    width: 160px;
    height: 160px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .onboard__bg-orb,
  .onboard__particle {
    animation: none !important;
  }

  .ob-slide-next-enter-active,
  .ob-slide-next-leave-active,
  .ob-slide-prev-enter-active,
  .ob-slide-prev-leave-active {
    transition: opacity 0.3s ease;
  }
}

/* ============ Dark Mode Enhancements ============ */
html.dark .onboard__bg-orb--2 {
  background: radial-gradient(circle, color-mix(in srgb, #7c6def 18%, transparent), transparent 70%);
  opacity: 0.5;
}

html.dark .onboard__bg-orb--1 {
  background: radial-gradient(circle, color-mix(in srgb, #5b8def 12%, transparent), transparent 70%);
}

html.dark .ob-token-card {
  background: color-mix(in srgb, #5b8def 10%, transparent);
}

html.dark .ob-token-card__code {
  color: #a0c4ff;
}

html.dark .ob-success-terminal {
  background: color-mix(in srgb, #1a1a2e 70%, transparent);
  border-color: color-mix(in srgb, #5b8def 15%, transparent);
}

html.dark .ob-welcome-name {
  background: color-mix(in srgb, #1a1a2e 60%, transparent);
  border-color: color-mix(in srgb, #5b8def 12%, transparent);
}

html.dark .ob-pillar {
  background: color-mix(in srgb, #1a1a2e 40%, transparent);
  border-color: color-mix(in srgb, #5b8def 8%, transparent);
}

html.dark .ob-leader {
  background: color-mix(in srgb, #1a1a2e 45%, transparent);
  border-color: color-mix(in srgb, #5b8def 10%, transparent);
}

html.dark .ob-leader-featured {
  background: linear-gradient(160deg, color-mix(in srgb, #5b8def 8%, #1a1a2e 50%), color-mix(in srgb, #1a1a2e 40%, transparent));
  border-color: color-mix(in srgb, #5b8def 15%, transparent);
}

html.dark .ob-leader-featured__glow {
  background: radial-gradient(circle, color-mix(in srgb, #5b8def 22%, transparent), transparent 70%);
}

html.dark .ob-leader-card {
  background: color-mix(in srgb, #1a1a2e 45%, transparent);
  border-color: color-mix(in srgb, #5b8def 8%, transparent);
}

html.dark .ob-dept-head {
  background: color-mix(in srgb, #1a1a2e 45%, transparent);
  border-color: color-mix(in srgb, #5b8def 8%, transparent);
}

html.dark .ob-dept-head--primary {
  background: linear-gradient(160deg, color-mix(in srgb, #5b8def 8%, #1a1a2e 50%), color-mix(in srgb, #1a1a2e 40%, transparent));
  border-color: color-mix(in srgb, #5b8def 12%, transparent);
}

.ob-name{
  margin-top:12px;
  margin-bottom:20px;
  font-size:34px;
  font-weight:700;
  letter-spacing:-0.02em;
}

.ob-quote{
  margin-top:36px;
  margin-bottom:48px;
  font-size:15px;
  opacity:.55;
  letter-spacing:.25em;
  text-transform:uppercase;
}

/* ============ Open-Source Floating Decorations ============ */
.ob-os-deco {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.ob-os-deco__symbol {
  position: absolute;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: color-mix(in srgb, var(--oa-text) 5%, transparent);
  white-space: nowrap;
  animation: osSymbolFloat 30s ease-in-out infinite;
}

.ob-os-deco__symbol--1 { top: 12%; left: 6%; animation-duration: 26s; font-size: 16px; }
.ob-os-deco__symbol--2 { top: 72%; right: 8%; animation-duration: 32s; animation-delay: -8s; }
.ob-os-deco__symbol--3 { top: 28%; right: 12%; animation-duration: 28s; animation-delay: -4s; font-size: 11px; }
.ob-os-deco__symbol--4 { bottom: 18%; left: 10%; animation-duration: 34s; animation-delay: -12s; }
.ob-os-deco__symbol--5 { top: 55%; left: 80%; animation-duration: 22s; animation-delay: -6s; font-size: 11px; }

@keyframes osSymbolFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); opacity: 0.4; }
  25% { transform: translateY(-12px) rotate(1deg); opacity: 0.7; }
  50% { transform: translateY(-6px) rotate(-0.5deg); opacity: 0.5; }
  75% { transform: translateY(-15px) rotate(0.5deg); opacity: 0.65; }
}

html.dark .ob-os-deco__symbol {
  color: color-mix(in srgb, var(--oa-active-bg) 8%, transparent);
}

/* ============ Welcome Name (Terminal Style) ============ */
.ob-welcome-name {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 28px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--oa-surface) 40%, transparent);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 60%, transparent);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.7s ease;
}

.onboard--loaded .ob-welcome-name {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

.ob-welcome-name__prompt {
  font-size: 15px;
  color: var(--oa-active-bg);
  font-weight: 600;
}

.ob-welcome-name__text {
  font-size: 28px;
  font-weight: 700;
  color: var(--oa-text);
  letter-spacing: -0.01em;
}

.ob-welcome-name__cursor {
  animation: termCursorBlink 1s step-end infinite;
  color: var(--oa-active-bg);
  font-weight: 300;
}

@keyframes termCursorBlink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.ob-welcome-name__dept {
  font-size: 13px;
  color: var(--oa-muted);
  letter-spacing: 0.02em;
}

/* ============ Open-Source Badge ============ */
.ob-os-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--oa-active-bg) 6%, transparent);
  border: 1px solid color-mix(in srgb, var(--oa-active-bg) 15%, transparent);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: color-mix(in srgb, var(--oa-active-bg) 70%, var(--oa-text));
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.7s ease;
}

.onboard--loaded .ob-os-badge {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

.ob-os-badge__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* ============ Button Icon ============ */
.ob-btn__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.ob-btn--glow {
  animation: btnGlow 3s ease-in-out infinite;
}

@keyframes btnGlow {
  0%, 100% { box-shadow: 0 4px 20px color-mix(in srgb, var(--oa-active-bg) 25%, transparent); }
  50% { box-shadow: 0 8px 40px color-mix(in srgb, var(--oa-active-bg) 40%, transparent), 0 0 60px color-mix(in srgb, var(--oa-active-bg) 15%, transparent); }
}

/* ============ Pillar Icons ============ */
.ob-pillar__icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: color-mix(in srgb, var(--oa-active-bg) 8%, transparent);
  flex-shrink: 0;
}

.ob-pillar__icon svg {
  width: 20px;
  height: 20px;
  color: color-mix(in srgb, var(--oa-active-bg) 80%, var(--oa-text));
}

/* ============ Success Terminal ============ */
.ob-success-terminal {
  width: 100%;
  max-width: 460px;
  border-radius: 12px;
  overflow: hidden;
  background: color-mix(in srgb, var(--oa-surface) 50%, transparent);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 50%, transparent);
  text-align: left;
  opacity: 0;
  transform: translateY(20px);
  filter: blur(6px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.7s ease;
}

.onboard--loaded .ob-success-terminal {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: var(--delay, 0s);
}

.ob-success-terminal__bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: color-mix(in srgb, var(--oa-text) 4%, transparent);
  border-bottom: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
}

.ob-success-terminal__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.ob-success-terminal__dot--red { background: #ff5f57; }
.ob-success-terminal__dot--yellow { background: #febc28; }
.ob-success-terminal__dot--green { background: #28c840; }

.ob-success-terminal__title {
  margin-left: auto;
  font-size: 11px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--oa-muted);
  letter-spacing: 0.04em;
}

.ob-success-terminal__body {
  padding: 14px 16px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  line-height: 1.8;
  color: var(--oa-text-soft);
}

.ob-success-terminal__body p {
  margin: 0;
}

.ob-term-prompt {
  color: var(--oa-active-bg);
  font-weight: 700;
  margin-right: 6px;
}

.ob-term-success {
  color: #22c55e;
}

.ob-term-output {
  color: var(--oa-text);
}

.ob-term-cursor {
  animation: termCursorBlink 1s step-end infinite;
  color: var(--oa-active-bg);
  font-size: 12px;
}

/* ============ Pillars refined (no num/body) ============ */
.ob-pillar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 20px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--oa-surface) 30%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid color-mix(in srgb, var(--oa-border) 40%, transparent);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
}

.ob-pillar:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--oa-active-bg) 20%, var(--oa-border));
  box-shadow: 0 12px 40px color-mix(in srgb, var(--oa-text) 8%, transparent);
}
</style>
