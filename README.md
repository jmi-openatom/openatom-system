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
2. 在 `backend/src/main/resources/application-dev.yaml` 中直接填写本地 MySQL 的用户名和密码；默认连接 `127.0.0.1:3306/openatom-system`。
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

- **External MySQL**: 不启动 MySQL 容器，Backend 通过宿主机网关连接宿主机 `3306` 端口
- **Backend**: 暴露端口 8921
- **Frontend**: 暴露端口 80 (集成 Nginx 反向代理)
- **Avatar Storage**: 用户头像写入 Docker 持久卷 `avatar_data`，容器重建后仍会保留

Docker 部署前，请先在宿主机 MySQL 中创建所需数据库，并在 `backend/src/main/resources/application-prod.yaml` 中直接填写数据库地址、用户名和密码。配置中的 `host.docker.internal` 指向宿主机网关，但 Docker 容器无法连接只监听 `127.0.0.1` 的 MySQL。宿主机 MySQL 必须监听 Docker 网桥可访问的地址，并允许配置文件中的数据库用户从 Docker 网段连接。后端启动时会通过 Flyway 自动执行 `backend/src/main/resources/db/migration/` 下的版本化迁移脚本。

在 Linux 部署服务器上可先检查 MySQL 监听地址和 Docker 网段：

```bash
sudo ss -lntp | grep ':3306'
docker network inspect openatom-system_openatom-network
```

如果 MySQL 仅监听 `127.0.0.1:3306`，需要在 MySQL 配置中调整 `bind-address`，重启 MySQL，并为 Docker 网段授权。不要把 `3306` 端口直接开放到公网。完成后可重启并检查后端：

```bash
docker compose up -d --build backend frontend
docker compose logs --tail=200 backend
```

### 宝塔 MySQL 配置

宝塔面板中显示的“本地数据库”表示 MySQL 安装在服务器本机，但 Docker Backend 连接它时仍属于外部连接。配置步骤：

1. 在“数据库 > MySQL”页面找到目标数据库，点击“权限”，将访问权限设置为 Docker 网段。可先通过 `docker network inspect openatom-system_openatom-network` 查看网段；不建议把权限永久设置为“所有人”。
2. 在“MySQL > 设置 > 配置修改”中检查 `bind-address`。如果当前为 `127.0.0.1`，改为 Docker 网桥可访问的地址或 `0.0.0.0`，保存并重启 MySQL。
3. 在服务器防火墙和云安全组中不要向公网开放 `3306`，只允许服务器本机及 Docker 网段访问。
4. 确认 `backend/src/main/resources/application-prod.yaml` 中的数据库名、用户名和密码与宝塔数据库列表一致。

> 头像上传后会保存到容器内 `/app/uploads/avatars`，该目录由 `avatar_data` 持久卷承载。  
> 如果旧版本曾把头像直接写在容器临时文件系统中，那么在容器被重建后，数据库中的头像 URL 可能仍然存在，但原始图片文件已经丢失，需要用户重新上传一次。

当前接入方式采用保守切换：已有生产库首次接入 Flyway 时会先 baseline 到版本 `0`，随后执行幂等的 `V1__init_schema.sql` 以补齐基础表；现存历史兼容逻辑仍由应用内的 `SchemaCompatibilityInitializer` 兜底；从后续新增数据库变更开始，再按 `V2__...sql`、`V3__...sql` 的方式持续演进。

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
