# CI/CD 持续集成与部署

## 概述

项目使用 GitHub Actions 实现自动化构建检查和部署。工作流定义在 `.github/workflows/deploy.yml` 中。

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

1. **Checkout 代码**

2. **SSH 连接服务器**，执行部署脚本：
   ```bash
   cd /www/wwwroot/openatom-system
   git fetch --all
   git reset --hard origin/main
   ```

3. **停止旧容器**：
   ```bash
   $COMPOSE_CMD down --remove-orphans
   ```

4. **计算版本号**：
   ```bash
   BUILD_NUMBER="${{ github.run_number }}"
   SHORT_SHA=$(git rev-parse --short HEAD)
   PKG_VER=$(grep -oP '"version"\s*:\s*"\K[^"]+' frontend/web_pc/package.json)
   APP_VER="v$PKG_VER.$BUILD_NUMBER-$SHORT_SHA"
   ```

5. **构建并启动**：
   ```bash
   $COMPOSE_CMD pull astrbot napcat || true
   VITE_APP_VERSION=$APP_VER \
     VITE_API_BASE_URL=https://api.jmi-openatom.cn/api/v1 \
     $COMPOSE_CMD up -d --build
   ```

6. **等待后端就绪**（最长 180 秒）：
   ```bash
   for attempt in $(seq 1 36); do
     if $COMPOSE_CMD exec -T backend bash -c 'exec 3<>/dev/tcp/127.0.0.1/8921'; then
       BACKEND_READY=true
       break
     fi
     sleep 5
   done
   ```

7. **验证所有服务运行**：
   ```bash
   for service in redis backend frontend astrbot napcat; do
     if ! $COMPOSE_CMD ps "$service" | grep -iq "Up"; then
       echo "Critical Error: Service $service is not running!"
       exit 1
     fi
   done
   ```

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
