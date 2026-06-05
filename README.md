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

- **External MySQL**: 不启动 MySQL 容器，Backend 使用 host 网络直接连接宿主机 `127.0.0.1:3306`
- **Backend**: 暴露端口 8921
- **Frontend**: 暴露端口 80 (集成 Nginx 反向代理)
- **Avatar Storage**: 用户头像写入 Docker 持久卷 `avatar_data`，容器重建后仍会保留

Docker 部署前，请先在宿主机 MySQL 中创建所需数据库，并在 `backend/src/main/resources/application-prod.yaml` 中直接填写数据库地址、用户名和密码。生产 Backend 使用 Linux host 网络，可直接连接只监听 `127.0.0.1:3306` 的宝塔 MySQL，不需要开放 MySQL 到 Docker 网段或公网。后端启动时会通过 Flyway 自动执行 `backend/src/main/resources/db/migration/` 下的版本化迁移脚本。

修改数据库配置后可重启并检查后端：

```bash
docker compose down --remove-orphans
docker compose up -d --build
docker compose logs --tail=200 backend
curl -i http://127.0.0.1:8921/api/v1/site/register-enabled
```

### 宝塔 MySQL 配置

宝塔面板中显示的“本地数据库”表示 MySQL 安装在服务器本机。Backend 使用 host 网络后，可以直接连接它。配置步骤：

1. 在“数据库 > MySQL”页面确认目标数据库、用户名和密码。
2. 保持 MySQL 监听 `127.0.0.1:3306` 即可，不需要把权限设置为“所有人”。
3. 在服务器防火墙和云安全组中不要向公网开放 `3306`。
4. 确认 `backend/src/main/resources/application-prod.yaml` 中的数据库名、用户名和密码与宝塔数据库列表一致。

如果页面仍显示 Nginx 错误页，请在服务器终端执行以下命令定位，不要只看宝塔数据库列表：

```bash
# 宝塔 MySQL 可以只监听 127.0.0.1:3306
ss -lntp | grep ':3306'

# 查看后端真实错误
docker compose ps -a
docker compose logs --tail=200 backend
```

反复出现 `HikariPool-1 - Starting...` 表示 Backend 仍未成功连接 MySQL；出现 `Access denied` 表示需要修改宝塔数据库用户权限或密码。

### 宝塔 Nginx 反向代理

生产环境使用两个独立域名：

- `www.jmi-openatom.cn` 只代理 Frontend：`127.0.0.1:18080`
- `api.jmi-openatom.cn` 只代理 Backend：`127.0.0.1:8921`

如果宝塔 Nginx 和 Docker Compose 运行在同一台服务器，反向代理目标应使用本机地址，不要使用服务器公网 IP。`www.jmi-openatom.cn` 的反向代理配置：

```nginx
location / {
    proxy_pass http://127.0.0.1:18080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
}
```

`api.jmi-openatom.cn` 需要在宝塔中创建独立站点并配置 SSL，然后将全部请求代理到 Backend：

```nginx
location / {
    proxy_pass http://127.0.0.1:8921;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Frontend Docker 镜像默认使用 `VITE_API_BASE_URL=https://api.jmi-openatom.cn/api/v1`，浏览器 API 请求不应发送到 `www.jmi-openatom.cn/api/v1`。修改该构建参数后必须重新构建 Frontend 容器。

出现 Nginx 错误页时，在服务器终端依次检查：

```bash
# 宝塔 Nginx能否连接 Frontend 容器
curl -i http://127.0.0.1:18080/

# 宿主机能否直接访问 Backend
curl -i http://127.0.0.1:8921/api/v1/site/register-enabled

# 验证公开 API 域名
curl -i https://api.jmi-openatom.cn/api/v1/site/register-enabled

# 查看两个宝塔站点的 Nginx 上游错误
tail -n 100 /www/wwwlogs/www.jmi-openatom.cn.error.log
tail -n 100 /www/wwwlogs/api.jmi-openatom.cn.error.log
```

Frontend 请求失败表示 Frontend 容器未运行或 `18080` 未监听；Backend 请求失败表示 Backend 未运行；本机 Backend 请求成功但公开 API 域名失败，表示 `api.jmi-openatom.cn` 的宝塔站点或 DNS 配置有误。错误日志中如果上游仍为服务器公网 IP，需要先改为对应的 `127.0.0.1` 端口。

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
