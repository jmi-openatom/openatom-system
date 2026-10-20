# 开放原子开源社团 · 成员使用指南

独立运行的静态文档网站，面向新成员，介绍社团账号激活、社团邮箱、Quest、新人主站探索任务、社团活动以及成果提交。

与仓库中面向开发者的 `docs-site` 分开，不需要主站后端、数据库、登录会话或第三方前端依赖。系统入口仅通过普通链接打开。本指南不会收集账号、密码或个人标记。

## 本地启动

需要 Node.js 18 或更新版本，无需安装依赖。

```sh
cd member-guide
npm run dev
```

默认访问 <http://127.0.0.1:4180/>，开发服务器仅监听本机。切换端口：

```sh
npm run dev -- --port 4181
```

## 检查与构建

```sh
npm run check
npm run build
npm start
```

构建结果在 `dist/`，可单独上传到 Nginx 等静态站点托管服务；Node 预览服务器不是上线所必需。网站使用 hash 路由，无需后端 API 或 SPA 路由回退，可放在独立域名或子目录。部署前按实际域名更新 `content.js` 的 `links` 与 `index.html` 中的页头系统入口。

`npm start` 使用构建产物，默认端口同样为 4180；先停止开发服务或使用不同端口。独立 Actions、首次域名配置与发布机制见 [部署说明](deploy/README.md)。

## 文档内容与维护

- `content.js`：7 篇文章、分节、逐步图文渲染、目录、外部入口与新成员清单。
- `walkthroughs.js`：94 个操作步骤，每步包含操作说明、对应页面、操作位置与预期结果；每个步骤拥有独立的图片。
- `app.js`：首页、文档路由、全文搜索、目录、主题、图示放大与浏览器本地进度。
- `styles.css`：响应式文档布局、深浅主题、键盘焦点、减少动态效果与打印样式。
- `assets/tokens.css`：从主站 `frontend/web_pc/src/styles/tokens.css` 复制的品牌设计变量。
- `assets/logo.svg`：主站品牌标志。
- `assets/fonts/`：与主站一致的本地拉丁字体及其授权文件；中文使用系统字体，阅读时不请求外部字体服务。
- `assets/steps/`：94 张独立的 SVG 界面演示图，蓝框标注当步操作区域。支持放大、原尺寸横向滚动、新标签页查看。
- `scripts/figures.mjs`、`scripts/render-step.mjs`：从步骤数据生成图片；运行 `npm run figures`，或通过 `npm run build` 自动生成。

图片数量：账号激活 22、邮箱 19、Quest 入门 17、认识社团任务 14、社团活动 6、成果提交与反馈 15、求助模板 1。概念解释和常见问题不作为操作步骤计数。

构建检查会验证每个步骤恰好对应一张独立图片、图片文件存在、预期结果完整，以及所有文章、分节和步骤深链接有效。概览图不再替代逐步配图。

全文搜索、勾选清单、主题切换均在浏览器本地运行，无统计上报。进度代表成员自行勾选的完成情况，不读取业务系统的真实激活或任务状态。关闭本地存储时，页面仍可阅读，进度可能无法保留。

## 内容依据

本版于 2026-10-20 对照仓库当前源码整理，未验证线上部署和账号状态。所有配图是依照现有流程与页面文案绘制的**简化界面演示**，使用演示数据，并明确标记“非线上截图”；没有渲染或访问线上账号。QQ 入群申请只演示字段关系，实际客户端界面可能因版本不同而变化。若替换为真实截图，需使用专门演示账号并遮挡个人信息、验证码和授权信息。

| 文章 | 主要依据 |
| --- | --- |
| 激活社团账号 | `frontend/web_pc/src/views/site/Activation.vue`、`frontend/web_pc/src/views/admin/Login.vue` |
| 社团邮箱 | `mail-system/mail-web/src/App.vue`、`views/LoginView.vue`、`components/mail/ActivationWizard.vue`、`components/layout/FolderSidebar.vue`、`components/mail/MessageReader.vue`、`components/mail/ComposeDialog.vue`、`stores/compose.ts` |
| Quest 入门 | `openatom-quest/frontend/src/views/LoginView.vue`、`OnboardingView.vue`、`TaskCenterView.vue`、`TaskDetailView.vue`、`router/index.ts` |
| 认识社团任务 | `openatom-quest/frontend/src/constants/siteExploration.ts`、`views/AssignmentDetailView.vue`、`openatom-quest/backend/src/main/resources/db/migration/V6__site_exploration_l0.sql` |
| 参与社团活动 | `frontend/web_pc/src/views/site/Activities.vue`、`ActivityDetail.vue` |
| 成果提交与反馈 | `openatom-quest/frontend/src/views/AssignmentDetailView.vue`、`openatom-quest/README.md` |

系统流程变化时应同步更新正文、示意图和此处核对日期。不要把邮箱地址激活与成员账号激活混为一谈；主站探索使用个人标记自动核验，普通任务使用成果版本与审核流程。
