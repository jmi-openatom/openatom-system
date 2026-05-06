<template>
    <div v-loading="loading" class="home-page">
        <section id="overview" class="hero">
            <div aria-hidden="true" class="hero__mesh"></div>
            <div class="container hero__inner">
                <div class="hero__content">
                    <el-tag class="hero__tag" effect="plain">开放原子计算机社团</el-tag>
                    <h1>{{ club.name || '正在读取社团数据' }}</h1>
                    <p>
                        {{ club.description || '正在从数据库加载社团介绍、活动、成员和比赛获奖信息。' }}
                    </p>
                    <div class="hero__actions">
                        <el-button :icon="DataAnalysis" size="large" type="primary" @click="scrollTo('activities')">
                            查看活动
                        </el-button>
                        <el-button :icon="UserFilled" size="large" @click="scrollTo('people')">了解成员</el-button>
                    </div>
                </div>

                <div class="command-panel">
                    <div class="panel-topline">
                        <span>社团概况</span>
                        <strong>持续更新</strong>
                    </div>
                    <div class="signal-grid">
                        <div v-for="metric in metrics" :key="metric.label" class="signal-card">
                            <span>{{ metric.label }}</span>
                            <strong>{{ metric.value }}</strong>
                            <small>{{ metric.note }}</small>
                        </div>
                    </div>
                    <el-empty v-if="!metrics.length" :image-size="72" description="暂无社团统计数据"/>
                    <div v-if="techStack.length" class="terminal-strip">
                        <span v-for="item in stack" :key="item">{{ item }}</span>
                    </div>
                </div>
            </div>
        </section>

        <section class="container section club-brief">
            <div class="section-heading reveal-block">
                <span>社团概览</span>
                <h2>{{ club.name || '社团' }}的主要方向</h2>
                <p>从部门分工到日常活动，页面内容尽量保持清晰、真实，方便快速了解社团正在做什么。</p>
            </div>
            <div class="brief-grid">
                <article v-for="item in focusAreas" :key="item.title" class="brief-card reveal-card">
                    <el-icon>
                        <component :is="focusIcon(item.icon)"/>
                    </el-icon>
                    <h3>{{ item.title }}</h3>
                    <p>{{ item.description }}</p>
                </article>
            </div>
            <el-empty v-if="!focusAreas.length && !loading" description="暂无社团部门数据"/>
        </section>

        <section id="activities" class="activity-band">
            <div class="container section">
                <div class="section-heading reveal-block">
                    <span>近期活动</span>
                    <h2>最新活动</h2>
                </div>
                <div class="activity-timeline">
                    <article v-for="activity in activities" :key="activity.title" class="activity-item reveal-card"
                             @click="$router.push(`/activities/${activity.id}`)">
                        <time>{{ activity.date }}</time>
                        <div>
                            <h3>{{ activity.title }}</h3>
                            <p>{{ activity.description }}</p>
                        </div>
                        <el-tag :type="statusType(activity.status)" effect="plain">{{
                                statusText(activity.status)
                            }}
                        </el-tag>
                    </article>
                </div>
                <el-empty v-if="!activities.length && !loading" description="暂无活动数据"/>
            </div>
        </section>

        <section id="people" class="container section people-section">
            <div class="section-heading reveal-block">
                <span>成员信息</span>
                <h2>主要人员</h2>
                <p>{{ people.length ? `当前展示 ${people.length} 位成员` : '暂无成员信息' }}</p>
            </div>
            <div class="people-grid">
                <article v-for="person in people" :key="person.userId || person.name" class="person-card reveal-card">
                    <div class="avatar">{{ person.initial }}</div>
                    <div>
                        <h3>{{ person.name }}</h3>
                        <p>{{ person.role }}</p>
                    </div>
                    <small>{{ person.focus }}</small>
                </article>
            </div>
            <el-empty v-if="!people.length && !loading" description="暂无主要人员数据"/>
        </section>

        <section id="achievements" class="container section achievements-section">
            <div class="section-heading reveal-block">
                <span>成果展示</span>
                <h2>比赛获奖展示</h2>
                <p>{{ awards.length ? `当前展示 ${awards.length} 项获奖记录` : '暂无获奖记录' }}</p>
            </div>
            <div class="award-grid">
                <article v-for="award in awards" :key="award.id || `${award.year}-${award.title}`"
                         class="award-card reveal-card">
                    <div class="award-card__year">{{ award.year }}</div>
                    <h3>{{ award.title }}</h3>
                    <p>{{ award.competitionName }}</p>
                    <strong>{{ award.awardLevel }}</strong><br><br>
                    <small>{{ award.teamName }}</small>
                </article>
            </div>
            <el-empty v-if="!awards.length && !loading" description="暂无比赛获奖数据"/>
        </section>
    </div>
</template>

<script>
import gsap from 'gsap'
import {ScrollTrigger} from 'gsap/ScrollTrigger'
import {markRaw, nextTick} from 'vue'
import {siteApi} from '../../api'
import {Cpu, DataAnalysis, Lightning, Monitor, UserFilled} from '@element-plus/icons-vue'

gsap.registerPlugin(ScrollTrigger)

const icons = {
    Cpu: markRaw(Cpu),
    DataAnalysis: markRaw(DataAnalysis),
    Lightning: markRaw(Lightning),
    Monitor: markRaw(Monitor),
    UserFilled: markRaw(UserFilled)
}

export default {
    name: 'SiteHome',
    data() {
        return {
            DataAnalysis: icons.DataAnalysis,
            UserFilled: icons.UserFilled,
            loading: false,
            club: {},
            metrics: [],
            techStack: [],
            focusAreas: [],
            activities: [],
            people: [],
            awards: []
        }
    },
    computed: {
        stack() {
            return this.techStack
        }
    },
    async mounted() {
        await this.loadClubHome()
        await nextTick()
        this.animationContext = gsap.context(() => {
            gsap.from('.hero__content > *', {
                y: 28,
                opacity: 0,
                duration: 0.8,
                ease: 'power3.out',
                stagger: 0.12
            })
            gsap.from('.command-panel', {
                y: 34,
                opacity: 0,
                duration: 0.9,
                delay: 0.2,
                ease: 'power3.out'
            })
            gsap.to('.hero__mesh', {
                backgroundPosition: '120px 80px',
                duration: 10,
                repeat: -1,
                yoyo: true,
                ease: 'sine.inOut'
            })
            gsap.utils.toArray('.reveal-block, .reveal-card').forEach((element) => {
                gsap.from(element, {
                    y: 34,
                    opacity: 0,
                    duration: 0.7,
                    ease: 'power2.out',
                    scrollTrigger: {
                        trigger: element,
                        start: 'top 84%'
                    }
                })
            })
        }, this.$el)
    },
    beforeUnmount() {
        this.animationContext?.revert()
    },
    methods: {
        async loadClubHome() {
            this.loading = true
            try {
                const data = await siteApi.clubHome()
                this.club = data.club || {}
                this.metrics = data.metrics || []
                this.techStack = data.techStack || []
                this.focusAreas = data.focusAreas || []
                this.activities = data.activities || []
                this.people = data.people || []
                this.awards = data.awards || []
            } catch (error) {
                this.club = {}
                this.metrics = []
                this.techStack = []
                this.focusAreas = []
                this.activities = []
                this.people = []
                this.awards = []
            } finally {
                this.loading = false
            }
        },
        scrollTo(id) {
            document.getElementById(id)?.scrollIntoView({behavior: 'smooth', block: 'start'})
        },
        focusIcon(name) {
            const iconMap = {
                monitor: icons.Monitor,
                cpu: icons.Cpu,
                lightning: icons.Lightning
            }
            return iconMap[name] || icons.Monitor
        },
        statusType(status) {
            if (status === 'published') {
                return 'success'
            }
            if (status === 'draft') {
                return 'warning'
            }
            return 'info'
        },
        statusText(status) {
            return {draft: '草稿', published: '已发布', closed: '已关闭', cancelled: '已取消'}[status] || status || '-'
        }
    }
}
</script>

<style scoped>
.home-page {
    overflow: hidden;
    background: transparent;
}

.hero {
    position: relative;
    min-height: 560px;
    display: flex;
    align-items: center;
    color: var(--oa-text);
    background: radial-gradient(circle at top left, rgba(147, 197, 253, 0.3), transparent 34%),
    radial-gradient(circle at right center, rgba(191, 219, 254, 0.54), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(240, 247, 255, 0.92));
}

.hero__mesh {
    position: absolute;
    inset: 0;
    opacity: 0.5;
    background-image: linear-gradient(rgba(148, 163, 184, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.08) 1px, transparent 1px);
    background-size: 54px 54px;
    mask-image: radial-gradient(circle at center, #000 48%, transparent 100%);
}

.hero__inner {
    position: relative;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 430px;
    gap: 44px;
    align-items: center;
    padding: 72px 0;
}

.hero__tag {
    height: 34px;
    padding: 0 14px;
    border: 1px solid rgba(37, 99, 235, 0.12);
    color: var(--oa-primary-dark);
    background: rgba(255, 255, 255, 0.76);
}

.hero h1 {
    margin: 18px 0 16px;
    max-width: 720px;
    font-size: 56px;
    line-height: 1.12;
    letter-spacing: -0.03em;
}

.hero p {
    max-width: 650px;
    margin: 0;
    color: #475569;
    font-size: 18px;
    line-height: 1.9;
}

.hero__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    margin-top: 30px;
}

.command-panel {
    padding: 22px;
    border: 1px solid rgba(219, 230, 245, 0.95);
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.82);
    box-shadow: 0 22px 60px rgba(37, 99, 235, 0.12);
    backdrop-filter: blur(18px);
}

.panel-topline,
.terminal-strip {
    display: flex;
    align-items: center;
}

.panel-topline {
    justify-content: space-between;
    margin-bottom: 18px;
    color: #64748b;
    font-size: 13px;
}

.panel-topline strong {
    color: var(--oa-primary-dark);
}

.signal-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
}

.signal-card {
    min-height: 122px;
    padding: 16px;
    border: 1px solid rgba(219, 230, 245, 0.95);
    border-radius: 18px;
    background: linear-gradient(180deg, #ffffff, #f7fbff);
}

.signal-card span,
.signal-card small {
    display: block;
    color: #64748b;
}

.signal-card strong {
    display: block;
    margin: 10px 0 8px;
    color: #0f172a;
    font-size: 34px;
}

.terminal-strip {
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 16px;
}

.terminal-strip span {
    padding: 6px 10px;
    border: 1px solid rgba(191, 219, 254, 0.9);
    border-radius: 999px;
    color: var(--oa-primary-dark);
    background: #eff6ff;
    font-size: 12px;
}

.section {
    padding: 72px 0;
}

.section-heading {
    max-width: 660px;
}

.section-heading span {
    color: var(--oa-primary-dark);
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.section-heading h2 {
    margin: 10px 0;
    color: #0f172a;
    font-size: 32px;
    line-height: 1.25;
    letter-spacing: -0.02em;
}

.section-heading p {
    margin: 0;
    color: #64748b;
    line-height: 1.8;
}

.brief-grid,
.people-grid,
.award-grid {
    display: grid;
    gap: 16px;
    margin-top: 28px;
}

.brief-grid {
    grid-template-columns: repeat(3, 1fr);
}

.brief-card,
.person-card,
.award-card,
.activity-item {
    border: 1px solid rgba(219, 230, 245, 0.95);
    border-radius: 22px;
    background: rgba(255, 255, 255, 0.88);
    box-shadow: 0 18px 42px rgba(37, 99, 235, 0.08);
    backdrop-filter: blur(16px);
}

.activity-item {
    cursor: pointer;
    transition: transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.activity-item:hover,
.brief-card:hover,
.person-card:hover,
.award-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 20px 44px rgba(37, 99, 235, 0.12);
    border-color: rgba(147, 197, 253, 0.95);
}

.brief-card {
    padding: 24px;
}

.brief-card .el-icon {
    width: 42px;
    height: 42px;
    border-radius: 14px;
    color: var(--oa-primary-dark);
    background: #eff6ff;
    font-size: 24px;
}

.brief-card h3,
.activity-item h3,
.person-card h3,
.award-card h3 {
    margin: 16px 0 8px;
    color: #111827;
}

.brief-card p,
.activity-item p,
.person-card p,
.award-card p {
    margin: 0;
    color: #64748b;
    line-height: 1.7;
}

.activity-band {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0)),
    linear-gradient(180deg, #eff6ff, #f7fbff);
}

.activity-timeline {
    display: grid;
    gap: 14px;
    margin-top: 28px;
}

.activity-item {
    display: grid;
    grid-template-columns: 86px minmax(0, 1fr) auto;
    gap: 18px;
    align-items: center;
    padding: 18px;
}

.activity-item time {
    color: var(--oa-primary-dark);
    font-size: 26px;
    font-weight: 800;
}

.activity-item h3 {
    margin-top: 0;
}

.people-grid {
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.person-card {
    min-height: 220px;
    display: flex;
    flex-direction: column;
    gap: 18px;
    padding: 22px;
}

.avatar {
    display: grid;
    width: 52px;
    height: 52px;
    place-items: center;
    border-radius: 16px;
    color: #ffffff;
    background: linear-gradient(135deg, #60a5fa, #2563eb);
    font-size: 22px;
    font-weight: 800;
}

.person-card small {
    margin-top: auto;
    color: #475569;
    line-height: 1.6;
}

.award-grid {
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.award-card {
    position: relative;
    min-height: 230px;
    padding: 24px;
    overflow: hidden;
}

.award-card::after {
    position: absolute;
    right: -36px;
    bottom: -42px;
    width: 120px;
    height: 120px;
    content: "";
    border: 1px solid rgba(147, 197, 253, 0.6);
    border-radius: 50%;
}

.award-card__year {
    color: #3b82f6;
    font-size: 30px;
    font-weight: 900;
}

.award-card strong {
    display: inline-block;
    margin-top: 18px;
    color: var(--oa-primary-dark);
}

@media (max-width: 1040px) {
    .hero__inner,
    .brief-grid,
    .people-grid,
    .award-grid {
        grid-template-columns: 1fr 1fr;
    }

    .hero__content {
        grid-column: 1 / -1;
    }
}

@media (max-width: 720px) {
    .hero {
        min-height: auto;
    }

    .hero__inner,
    .brief-grid,
    .people-grid,
    .award-grid,
    .signal-grid,
    .activity-item {
        grid-template-columns: 1fr;
    }

    .hero h1 {
        font-size: 38px;
    }

    .hero p {
        font-size: 16px;
    }

    .section {
        padding: 52px 0;
    }

    .section-heading h2 {
        font-size: 26px;
    }

    .activity-item {
        align-items: start;
    }
}
</style>
