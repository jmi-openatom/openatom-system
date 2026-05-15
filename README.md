# OpenAtom System

OpenAtom System 是一个基于 Spring Boot 3 + Vue 3 Option 的社团/组织管理系统，旨在提供高效的成员管理、招新流程、活动组织及文书审批功能。

## 🚀 技术栈

### 后端

- **核心框架**: Spring Boot 3.3.12
- **Java 版本**: JDK 21
- **权限校验**: Sa-Token
- **持久层**: MyBatis Plus
- **数据库**: MySQL 8.0
- **工具类**: Lombok, POI (Excel 导出), Jackson

### 前端

- **核心框架**: Vue 3 + Vite
- **UI 组件库**: Element Plus
- **网络请求**: Axios
- **动画库**: GSAP
- **路由管理**: Vue Router

## 🛠️ 快速开始 (本地开发)

### 环境要求

- JDK 21
- Node.js 20+
- MySQL 8.0

### 后端启动

1. 本地开发如使用独立 MySQL，可执行 `backend/db/create_table.sql` 初始化。
2. 修改配置：在 `backend/src/main/resources/application-dev.yaml` 中配置数据库连接。
3. 运行 `OpenatomSystemApplication`。

### 前端启动

```bash
cd frontend/web_pc
pnpm install
pnpm run dev
```

## 🐳 Docker 部署 (推荐)

项目已完整支持 Docker 化部署。

### 一键启动

在项目根目录下执行：

```bash
docker-compose up -d --build
```

该命令将启动：

- **MySQL**: 宿主机暴露端口 3307（容器内仍为 3306）
- **Backend**: 暴露端口 8921
- **Frontend**: 暴露端口 80 (集成 Nginx 反向代理)

Docker / 生产环境不再依赖 MySQL 容器首次启动时导入 SQL。后端启动时会通过 Flyway 自动执行 `backend/src/main/resources/db/migration/` 下的版本化迁移脚本。

当前接入方式采用保守切换：已有生产库首次接入 Flyway 时会先 baseline 到版本 `1`，现存历史兼容逻辑仍由应用内的 `SchemaCompatibilityInitializer` 兜底；从后续新增数据库变更开始，再按 `V2__...sql`、`V3__...sql` 的方式持续演进。

## 🔄 持续集成与部署 (CI/CD)

项目使用 GitHub Actions 实现自动化部署。每当代码推送到 `main` 分支时，会自动触发服务器同步。

### 配置步骤

1. 在 GitHub Repo 中配置以下 Secrets:
    - `SERVER_HOST`: 服务器 IP
    - `SERVER_USER`: SSH 用户名
    - `SERVER_SSH_KEY`: SSH 私钥
2. 修改 `.github/workflows/deploy.yml` 中的项目路径。

## 📂 项目结构

- `src/`: 后端 Java 源代码
- `frontend/`: 前端 Vue 源代码
- `backend/db/`: 手工初始化脚本
- `backend/src/main/resources/db/migration/`: 生产环境自动执行的 Flyway 迁移脚本
- `docs/`: PRD 及 API 文档
- `Dockerfile`: 后端容器化配置
- `docker-compose.yml`: 全栈编排配置

## 📝 核心功能

- **社团管理**: 基础信息、部门架构、岗位设置。
- **招新系统**: 自定义报名表单、面试安排、多级审批。
- **活动管理**: 活动发布、在线报名、获奖记录。
- **办公自动化**: 文书生成与导出、通知推送。
- **系统监控**: 操作日志、登录日志记录。
