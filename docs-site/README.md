---
home: true
title:  开发文档
heroText: OpenAtom System
tagline: 开放原子开源社团全栈管理系统 · 从后端到前端的完整开发文档
actions:
  - text: 🚀 快速开始
    link: /guide/getting-started.html
    type: primary
  - text: 📖 项目介绍
    link: /guide/introduction.html
    type: secondary
features:
  - icon: ☕
    title: Spring Boot 3 + JDK 21
    details: 后端基于 Spring Boot 3.3，Sa-Token JWT 鉴权、MyBatis Plus 持久层、Flyway 自动迁移，运行于 JDK 21 LTS。
  - icon: ⚡
    title: Vue 3 + Vite 6
    details: PC 端采用 Vue 3.5 组合式 API + Vite 6 + Element Plus + TailwindCSS，UniApp 支持微信小程序多端。
  - icon: 🐳
    title: Docker 全栈编排
    details: Redis、Backend、Frontend、AstrBot、NapCat、Docs 一键 Docker Compose 编排，支持宝塔面板 Nginx 反代。
  - icon: 🔐
    title: RBAC 权限模型
    details: 基于 Sa-Token 的细粒度 API 权限控制，100+ 权限点覆盖全部业务接口，前端路由级 + 后端接口级双重校验。
  - icon: 🤖
    title: QQ 群机器人
    details: AstrBot + NapCat + OneBot v11 协议栈，实现群消息收发、成员管理、请假审批通知、AI 对话等能力。
  - icon: 🔄
    title: CI/CD 自动化
    details: GitHub Actions 自动构建检查，main 分支推送自动 SSH 部署到生产服务器，前端 typecheck + 后端编译全流程。
  - icon: 🗄️
    title: Flyway 数据库版本控制
    details: 版本化 SQL 迁移脚本管理数据库 Schema 变更，支持 baseline 接入已有生产库，幂等迁移确保数据安全。
  - icon: 🎨
    title: Apple 风格设计系统
    details: 统一的设计语言：单一蓝色 #0066cc 主色、无渐变、仅产品图片有阴影，GSAP 动画 + Mapbox 地图集成。
footer: Copyright © 2026 JMI-OPENATOM · 开放原子开源社团 · 开源精神，技术驱动
---

<div class="homepage-stats">
  <div class="stat-item">
    <div class="stat-value">26+</div>
    <div class="stat-label">文档页面</div>
  </div>
  <div class="stat-item">
    <div class="stat-value">44</div>
    <div class="stat-label">后端接口</div>
  </div>
  <div class="stat-item">
    <div class="stat-value">100+</div>
    <div class="stat-label">权限点</div>
  </div>
  <div class="stat-item">
    <div class="stat-value">60+</div>
    <div class="stat-label">数据表</div>
  </div>
  <div class="stat-item">
    <div class="stat-value">6</div>
    <div class="stat-label">Docker 服务</div>
  </div>
</div>

<div class="homepage-tech-stack">
  <span class="tech-badge">JDK 21</span>
  <span class="tech-badge">Spring Boot 3.3</span>
  <span class="tech-badge">Sa-Token</span>
  <span class="tech-badge">MyBatis Plus</span>
  <span class="tech-badge">Vue 3.5</span>
  <span class="tech-badge">Vite 6</span>
  <span class="tech-badge">TypeScript</span>
  <span class="tech-badge">Element Plus</span>
  <span class="tech-badge">TailwindCSS</span>
  <span class="tech-badge">UniApp</span>
  <span class="tech-badge">Docker</span>
  <span class="tech-badge">Redis 7</span>
  <span class="tech-badge">MySQL 8</span>
  <span class="tech-badge">Flyway</span>
  <span class="tech-badge">GitHub Actions</span>
</div>
