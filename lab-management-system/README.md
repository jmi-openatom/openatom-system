# Lab Management System

这是独立于 CMS 的实验室管理系统实现骨架。CMS 只承担 OAuth 认证中心、实验室成员资格分发和社团积分 API 提供方；LMS 自己运行前端、后端和本地业务数据。

## 目录结构

```text
lab-management-system/
├── backend/              # 独立 Spring Boot API
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile       # 后端 Docker 镜像
├── lab-ui-web/          # 独立实验室网站 (Vue3 + Element Plus)
│   ├── src/
│   ├── package.json
│   ├── Dockerfile       # 前端 Docker 镜像
│   └── nginx.conf       # Nginx 配置
├── docker-compose.yml   # Docker Compose 编排
└── README.md
```

## 技术栈

### 后端
- Java 21 + Spring Boot 3.3
- Redis 7
- Maven 3.9

### 前端
- Vue 3.5 + TypeScript
- Element Plus 2.9 (UI 组件库，与 CMS 保持一致)
- TailwindCSS 3.4 (样式框架)
- Vite 6 (构建工具)
- Pinia (状态管理)

## 系统边界

- 登录：LMS 跳转到 CMS OAuth，回调后由 LMS 后端读取 CMS 用户信息中的 `isLabMember`。
- 准入：非实验室成员被 LMS 拦截，不发放 LMS token。
- 数据：题目、提交、签到、信誉分和通知属于 LMS 本地数据。
- 对接：社团积分同步只记录待同步事件，真实环境接 CMS `add_club_score` API 或 MQ。
- 判题：当前后端保留 ACM 状态机和安全模式评测，真实环境把 `LabStore#judge` 替换为 Docker/Isolate 沙箱客户端。

## 本地开发

### 前端开发
```bash
cd lab-management-system/lab-ui-web
pnpm install
pnpm dev  # http://localhost:5174
```

### 后端开发
```bash
cd lab-management-system/backend
./mvnw spring-boot:run  # http://localhost:8922
```

## Docker 部署

### 启动所有服务
```bash
cd lab-management-system
docker-compose up -d --build
```

### 服务端口
- LMS Frontend: `http://localhost:18081`
- LMS Backend: `http://localhost:8922`
- LMS Redis: `127.0.0.1:6380`

### 查看服务状态
```bash
docker-compose ps
docker-compose logs -f lms-backend
docker-compose logs -f lms-frontend
```

### 停止服务
```bash
docker-compose down
```

## 环境变量配置

创建 `.env` 文件：

```bash
# Redis
LMS_REDIS_PASSWORD=lms123

# JWT
LMS_SA_TOKEN_JWT_SECRET_KEY=your-secret-key

# CMS 对接
APP_CMS_OAUTH_ISSUER=https://oauth.jmi-openatom.cn/api/v1
APP_CMS_API_BASE_URL=https://api.jmi-openatom.cn/api/v1

# AI 接口
APP_AI_API_KEY=your-ai-api-key
APP_AI_API_BASE_URL=https://api.openai.com/v1

# 前端构建参数
VITE_APP_VERSION=v1.0.0
VITE_LMS_API_BASE_URL=https://lms-api.jmi-openatom.cn/api/v1
VITE_OIDC_AUTHORITY=https://oauth.jmi-openatom.cn/api/v1
VITE_LMS_OIDC_CLIENT_ID=lms-web
```

## GitHub Actions CI/CD

### 工作流文件
`.github/workflows/deploy-lms.yml`

### 触发条件
- Push 到任意分支（仅检查 `lab-management-system/**` 路径）
- Pull Request
- 手动触发 (workflow_dispatch)

### CI 阶段
1. **前端检查**: TypeScript 类型检查 + Vite 构建
2. **后端检查**: Maven 编译检查

### CD 阶段 (仅 main 分支)
1. SSH 连接服务器
2. 拉取最新代码
3. 停止旧容器
4. 构建并启动新容器
5. 健康检查（等待后端就绪）
6. 验证所有服务状态

### 配置 GitHub Secrets
```
SERVER_HOST=your-server-ip
SERVER_USER=your-ssh-user
SERVER_PORT=22
SERVER_PASSWORD=your-ssh-password
```

## 与 CMS 的区别

### 端口配置
- CMS Backend: 8921 → LMS Backend: 8922
- CMS Frontend: 18080 → LMS Frontend: 18081
- CMS Redis: 6379 → LMS Redis: 6380

### 独立部署
- 独立的 `docker-compose.yml`
- 独立的 CI/CD workflow
- 相同的技术栈（Vue3 + Element Plus + Spring Boot）

### 数据隔离
- 独立的 Redis 实例
- 独立的数据库
- 通过 OAuth2.0 和 API 与 CMS 互通

## 生产环境部署清单

- [ ] 配置生产环境变量
- [ ] 配置 GitHub Secrets
- [ ] 在 CMS 中注册 OAuth Client (`lms-web`)
- [ ] 配置反向代理和 SSL 证书
- [ ] 配置数据库连接
- [ ] 配置 AI API 密钥
- [ ] 配置监控告警

## 后续开发计划

根据 `LMS.md` 需求文档：
1. ✅ Docker 部署配置
2. ✅ GitHub Actions CI/CD
3. ✅ 前端 UI 框架（Element Plus + TailwindCSS）
4. ✅ OAuth2.0 认证模块
5. ✅ AI 每日一练生成
6. ✅ ACM 判题沙箱
7. ✅ 签到考勤系统
8. ✅ 数据库集成（MySQL + Redis + JPA）
9. ✅ 完整前后端页面

## 功能清单

### 后端（Spring Boot）
- ✅ **实体层**：LabUser, Problem, Submission, CheckinScoreLog, Notification
- ✅ **Repository 层**：JPA 数据访问
- ✅ **服务层**：
  - AuthService - OAuth2.0 认证、用户管理
  - AIService - 定时生成每日算法题（每天 00:00）
  - JudgeService - 代码判题（AC/WA/TLE/MLE/RE/CE）
  - CheckinService - 签到考勤（刷题自动签到、现场签到）
  - ScoreService - 积分管理（信誉分扣除）
- ✅ **控制器层**：
  - AuthController - 登录/登出/获取用户信息
  - ProblemController - 获取今日题目
  - SubmissionController - 提交代码、查看提交记录
  - CheckinController - 手动签到
  - NotificationController - 通知列表

### 前端（Vue 3 + Element Plus）
- ✅ **首页**：统计卡片（签到、AC数、信誉分）
- ✅ **每日一练页**：题目展示、代码编辑器、提交记录
- ✅ **API 集成**：axios 封装、token 认证
- ✅ **响应式设计**：适配移动端和桌面端

### 数据库
- ✅ MySQL 8.0（主数据库）
- ✅ Redis 7（会话缓存）
- ✅ JPA 自动建表
- ✅ 5 张核心表：用户、题目、提交、签到、通知

### 部署
- ✅ Docker Compose 一键部署
- ✅ GitHub Actions 自动 CI/CD
- ✅ 健康检查与服务监控
- ✅ 独立端口配置（MySQL:3307, Redis:6380, Backend:8922, Frontend:18081）

## 核心功能说明

### 1. OAuth2.0 认证流程
1. 用户从 CMS 获取 token
2. LMS 调用 CMS `/auth/me` 接口验证
3. 检查 `isLabMember` 字段，非实验室成员拒绝登录
4. 创建或更新本地用户记录
5. 生成 LMS token（sa-token）

### 2. AI 每日一练
- 每天 00:00 定时任务触发
- 调用 AI API 生成算法题（题目、测试用例、标准答案）
- 自动发送群发通知
- 前端展示题目和代码编辑器

### 3. ACM 判题机制
- 支持 C++/Java/Python
- 模拟沙箱执行（生产环境可替换为 Docker/Isolate）
- 返回判题状态：AC/WA/TLE/MLE/RE/CE
- AC 后自动触发签到

### 4. 签到考勤
- **刷题自动签到**：当天 AC 题目自动标记签到
- **现场签到**：点击签到按钮
- **迟到判定**：09:00 为标准时间，09:01-09:30 算迟到，扣 5 分
- **旷课判定**：每天 23:00 检查未签到用户，扣 10 分

### 5. 积分体系
- **实验室信誉分**：默认 100 分，迟到/旷课扣分
- **社团积分同步**：正常签到异步同步到 CMS（每 5 分钟）

## 待扩展功能

- [ ] 完善 AI API 对接（当前为模拟返回）
- [ ] Docker/Isolate 沙箱替换（当前为安全模式模拟）
- [ ] 邮件/微信通知推送
- [ ] 后台管理页面（题目审核、考勤统计）
- [ ] 失焦收卷功能
- [ ] 排行榜功能

