# CI/CD 持续集成与部署

## 概述

项目使用 GitHub Actions 实现自动化构建检查和部署。主工作流定义在 `.github/workflows/deploy.yml` 中，另有多达 6 个独立子系统的部署工作流：

| 工作流 | 部署对象 | 说明 |
|--------|----------|------|
| `deploy.yml` | 主系统 | 全栈主流程（本文档） |
| `docs-system.yml` | 文档中心 | ONLYOFFICE 文档子系统 |
| `mail-system.yml` | 邮件系统 | 自托管邮件服务 |
| `lab-lms.yml` | 实验室管理系统 | LMS 前后端 |
| `seafile.yml` | Seafile | 网盘服务 |
| `oauth-proxy.yml` | OAuth 代理 | OAuth 反代配置 |
| `oauth-proxy-diagnostic.yml` | OAuth 代理 | 诊断辅助 |

## 工作流触发

```yaml
on:
  workflow_dispatch:        # 手动触发
  push:
    branches: ['**']        # 所有分支推送
  pull_request:
    branches: ['**']        # 所有分支 PR
```

## 构建检查任务

推送到任何分支或提交 PR 时，会并行执行三个检查任务：

### 1. 前端检查（frontend-check）

```yaml
steps:
  - uses: actions/checkout@v4
  - name: Install pnpm
    uses: pnpm/action-setup@v4
    with:
      version: 9
  - name: Set up Node.js
    uses: actions/setup-node@v4
    with:
      node-version: 20
      cache: 'pnpm'
  - name: Frontend Build Check
    run: |
      cd frontend/web_pc
      pnpm install --frozen-lockfile
      pnpm run typecheck
      pnpm run build
  - name: UniApp Type Check
    run: |
      cd frontend/uni_app
      pnpm install --frozen-lockfile
      pnpm run typecheck
```

### 2. 后端检查（backend-check）

```yaml
steps:
  - uses: actions/checkout@v4
  - name: Set up JDK 21
    uses: actions/setup-java@v4
    with:
      java-version: '21'
      distribution: 'temurin'
      cache: 'maven'
  - name: Backend Build Check
    run: |
      cd backend
      ./mvnw -q -DskipTests compile
```

### 3. 机器人检查（bot-check）

```yaml
steps:
  - uses: actions/checkout@v4
  - name: Set up Python
    uses: actions/setup-python@v5
    with:
      python-version: '3.11'
  - name: AstrBot Plugin Syntax Check
    run: |
      python -m py_compile astrbot/data/plugins/astrbot_plugin_openatom_api/main.py
```

## 部署任务

当所有检查通过，且代码推送到 `main` 分支时，自动触发部署：

```yaml
deploy:
  needs: [frontend-check, backend-check, bot-check]
  if: |
    (github.event_name == 'push' || github.event_name == 'workflow_dispatch') &&
    github.ref == 'refs/heads/main'
  runs-on: ubuntu-latest
  environment: SERVER
```

### 部署流程

1. **检查服务器架构和镜像缓存**：SSH 查询 `linux/amd64` 或 `linux/arm64`，记录缺失的 Redis、AstrBot、NapCat 镜像。
2. **在 Actions 构建生产镜像**：后端两个副本共用同一镜像，主站前端和 `docs-site` 分别构建。所有镜像以本次提交 SHA 标记，前端同时注入版本号和生产 API/OIDC 地址。
3. **打包上传**：Actions 下载缺失的运行镜像，将 Docker 镜像压缩并生成 SHA256 清单，通过 SCP 上传到本次运行独立的 `.deploy-images/` 目录。
4. **校验并导入**：服务器同步本次提交，生成 `.env`，校验镜像清单和提交 SHA，再执行 `docker load`。校验、导入或镜像检查失败时会中止，现有容器继续运行。
5. **启动容器**：服务器使用已导入的镜像，不需要访问 Docker Hub：
   ```sh
   OPENATOM_IMAGE_TAG=<本次完整提交 SHA> \
     docker compose up -d --no-build --pull never --remove-orphans
   ```
6. **验证服务**：分别等待 `8921`、`8922` 的 OIDC discovery 返回 HTTP 200，验证 Seafile OAuth 授权入口、Nginx 配置和各服务运行状态，通过后清理悬空镜像。

`docs.jmi-openatom.cn` 对应主工作流中的 `docs-site` 容器，Logo 和文档内容随主站部署一起更新；`docs-system.yml` 部署的是独立的在线文档中心。

## GitHub Secrets 配置

在 GitHub Repo 的 Settings → Secrets and variables → Actions 中配置：

| Secret | 说明 |
|--------|------|
| `SERVER_HOST` | 服务器 IP 地址 |
| `SERVER_USER` | SSH 用户名 |
| `SERVER_PORT` | SSH 端口（默认 22） |
| `SERVER_PASSWORD` | SSH 密码 |

::: tip 注意
也可使用 SSH 密钥认证，将 `SERVER_PASSWORD` 替换为 `SERVER_SSH_KEY`（SSH 私钥）。
:::

## 版本号规则

部署版本号格式：`v{packageVersion}.{buildNumber}-{shortHash}`

示例：`v1.0.0.42-a1b2c3d`

- `1.0.0` — `package.json` 中的版本号
- `42` — GitHub Actions 运行编号
- `a1b2c3d` — Git commit 短哈希

## 部署失败排查

```bash
# 查看所有容器状态
docker compose ps -a

# 查看后端日志
docker compose logs --tail=200 backend

# 检查后端是否监听端口
docker compose exec backend bash -c 'exec 3<>/dev/tcp/127.0.0.1/8921'

# 验证 API
curl -i http://127.0.0.1:8921/api/v1/site/register-enabled
```
