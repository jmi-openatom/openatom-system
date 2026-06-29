# 配置说明

## 配置文件结构

后端配置文件位于 `backend/src/main/resources/` 目录下：

```
resources/
├── application.yml          # 主配置（公共配置）
├── application-dev.yaml     # 开发环境配置
├── application-prod.yaml    # 生产环境配置
└── db/migration/            # Flyway 迁移脚本
```

## 主配置（application.yml）

```yaml
spring:
  profiles:
    active: dev              # 默认开发环境，生产环境通过 -Dspring.profiles.active=prod 切换
  jackson:
    time-zone: ${JACKSON_TIME_ZONE:Asia/Shanghai}

app:
  cache:
    redis:
      enabled: ${APP_REDIS_CACHE_ENABLED:true}
      key-prefix: ${APP_REDIS_CACHE_KEY_PREFIX:openatom:cache}
      default-ttl: ${APP_REDIS_CACHE_DEFAULT_TTL:10m}
      scan-batch-size: ${APP_REDIS_CACHE_SCAN_BATCH_SIZE:500}
```

## 开发环境配置（application-dev.yaml）

### 数据源

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/openatom_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: ab321818         # 本地开发密码，请修改为你的密码
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      pool-name: openatom-dev-hikari
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Flyway

```yaml
spring:
  flyway:
    enabled: false            # 开发环境禁用，需手动执行 SQL
```

### Redis

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password:               # 本地开发无密码
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 8
          min-idle: 1
          max-wait: 3000ms
```

### 服务端口

```yaml
server:
  port: 8921
  servlet:
    context-path: /api/v1
```

### Sa-Token

```yaml
sa-token:
  token-name: jmiopenatom
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET_KEY:openatom-development-jwt-secret-change-me}
  timeout: 2592000            # 30 天
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: true                # 开发环境开启日志
```

### 业务配置

```yaml
app:
  miniapp:
    app-id: ${APP_MINIAPP_APP_ID:wx8c6b52ab95da0938}
    app-secret: ${APP_MINIAPP_APP_SECRET:d15542dcaa23b92191962235dacef7b9}
  cors:
    allowed-origin-patterns: '*'
  rate-limit:
    application-submit:
      max-per-second: 5       # 报名提交限流：5次/秒
  avatar:
    storage-dir: ./uploads/avatars
  image-hosting:
    storage-dir: ./uploads/images
  leave-attachment:
    storage-dir: ./uploads/leave-attachments
  document-template:
    storage-dir: ./uploads/document-templates
  generated-document:
    storage-dir: ./uploads/generated-documents
  ai:
    deepseek:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY:}
      model: deepseek-chat
      timeout-seconds: 30
  bot:
    callback-token: ${APP_BOT_CALLBACK_TOKEN:}
    leave-review-callback-url: http://astrbot:6198/openatom/leave-review
  napcat:
    base-url: http://napcat:3000
    access-token: sRtnBQJy-lqPTWjj
```

## 生产环境配置（application-prod.yaml）

### 数据源

```yaml
spring:
  datasource:
    # 生产环境使用 host 网络，直连宿主机宝塔 MySQL
    url: jdbc:mysql://127.0.0.1:3306/openatom-db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: openatom-db
    password: 7jcAS4e8eBSbBFBt  # 生产密码，请通过环境变量覆盖
    hikari:
      pool-name: openatom-prod-hikari
      maximum-pool-size: 20
      minimum-idle: 5
```

### Flyway

```yaml
spring:
  flyway:
    enabled: true             # 生产环境启用
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
```

### Redis

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 3000ms
```

### Tomcat 增强

```yaml
server:
  port: 8921
  servlet:
    context-path: /api/v1
  compression:
    enabled: true
    min-response-size: 1024
  forward-headers-strategy: framework
  tomcat:
    basedir: /tmp/openatom-system
    accesslog:
      enabled: true
      directory: logs
      pattern: '%h %l %u %t "%r" %s %b %D'
```

### 日志

```yaml
logging:
  level:
    root: INFO
    edu.jmi.openatom.server.openatomsystem: INFO
    cn.dev33.satoken: false   # 生产环境关闭 Sa-Token 日志
  file:
    name: ./logs/openatom-system.log
```

## 环境变量

生产环境通过环境变量覆盖默认值，详见 [环境变量文档](../deploy/env.md)。

## 文件存储路径

系统有多个文件存储目录，开发环境和 Docker 环境的路径不同：

| 配置项 | 开发环境 | Docker 环境 |
|--------|----------|-------------|
| `app.avatar.storage-dir` | `./uploads/avatars` | `/app/uploads/avatars` |
| `app.image-hosting.storage-dir` | `./uploads/images` | `/app/uploads/images` |
| `app.leave-attachment.storage-dir` | `./uploads/leave-attachments` | `/app/uploads/leave-attachments` |
| `app.document-template.storage-dir` | `./uploads/document-templates` | `/app/uploads/document-templates` |
| `app.generated-document.storage-dir` | `./uploads/generated-documents` | `/app/uploads/generated-documents` |

Docker 环境中这些目录统一挂载到 `avatar_data` 持久卷下的 `/app/uploads/`。

## 多 Profile 切换

### 命令行切换

```bash
# 开发环境（默认）
java -jar app.jar

# 生产环境
java -Dspring.profiles.active=prod -jar app.jar
```

### Docker 环境

Dockerfile 中已固定生产环境：

```dockerfile
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

### IDE 环境

在 IntelliJ IDEA 的运行配置中，设置 VM Options：
```
-Dspring.profiles.active=dev
```
