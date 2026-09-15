# OpenAtom Quest

面向开放原子开源社团新成员的成长路线与任务实践平台。

当前按 [PRD.md](./PRD.md) 和 [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) 实现第一阶段 MVP。

当前可用版已包含 OAuth 登录、首次资料完善、成长路线、任务领取与管理员分配、成果草稿与多版本提交、导师审核、积分等级、站内通知和管理后台。附件上传暂不纳入本版，成果可先通过仓库、Pull Request、在线演示或视频地址提交。

## 技术结构

- `frontend/`：Vue 3 + TypeScript + Vite + Element Plus
- `backend/`：Spring Boot 3 + Java 21 + MyBatis Plus + Flyway
- `docker-compose.yml`：MySQL、Redis、后端和前端完整运行栈

## 本地开发

```bash
docker compose up -d mysql redis
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && pnpm install && pnpm dev
```

开发环境可访问 `http://localhost:5174/dev-login`，使用仅在 `dev` Profile 注册的本地验收身份；生产构建不存在该入口。

## 生产运行

1. 按 [ACTIONS_CONFIGURATION_CHECKLIST.md](./ACTIONS_CONFIGURATION_CHECKLIST.md) 在 GitHub Environment `SERVER` 配置部署 Secrets 和 Variables。
2. 在 OpenAtom OAuth 后台手动登记回调地址 `https://quest.jmi-openatom.cn/api/auth/callback`。
3. 运行 GitHub Actions 的 `OpenAtom Quest CI/CD`；Action 会生成服务器 `.env`、构建并启动容器，再执行健康检查。
4. 由服务器入口网关把 `quest.jmi-openatom.cn` 的 HTTPS 流量转发到 `127.0.0.1:${QUEST_HTTP_PORT:-18084}`。

生产部署必须由外层网关终止 HTTPS；应用会使用 Secure、HttpOnly、SameSite=Lax 会话 Cookie。
