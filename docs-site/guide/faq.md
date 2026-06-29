# 常见问题

## 后端

### 启动报 `Unknown column` SQL 语法错误

**原因**：实体类添加了新字段，但数据库未执行对应的迁移脚本。

**解决**：
- 开发环境（Flyway 已禁用）：手动执行 `backend/src/main/resources/db/migration/` 下对应的 SQL 文件
- 生产环境（Flyway 已启用）：确认 Flyway 迁移脚本放在正确目录 `backend/src/main/resources/db/migration/`，命名格式为 `V{版本号}__{描述}.sql`

::: tip Flyway 迁移规则
- 迁移文件必须放在 `backend/src/main/resources/db/migration/` 目录下
- 命名格式：`V{版本号}__{描述}.sql`（如 `V30__add_new_feature.sql`）
- 版本号必须递增，不能与已有版本号重复
- 放在 `backend/db/` 等其他目录的 SQL 文件不会被 Flyway 执行
:::

### MyBatis-Plus `selectOne` 返回多条记录异常

**原因**：查询条件不够精确，`selectOne` 期望最多返回一条记录但实际返回了多条。

**解决**：检查查询条件，确保唯一性约束；或改用 `selectList` + 取第一条。

### `HikariPool-1 - Starting...` 反复出现

**原因**：后端无法连接 MySQL 数据库。

**排查**：
```bash
# 检查 MySQL 是否在监听
ss -lntp | grep ':3306'

# 查看后端日志
docker compose logs --tail=200 backend
```

- `Access denied`：检查数据库用户名和密码
- `Connection refused`：检查 MySQL 是否启动、端口是否正确
- Docker 环境：确认 Backend 使用 host 网络模式，可以直接连接宿主机 `127.0.0.1:3306`

### Sa-Token 登录后 Token 不生效

**检查**：
1. 请求头中是否携带了 `jmiopenatom` Token（前端 Axios 拦截器会自动添加）
2. JWT 密钥是否一致（`sa-token.jwt-secret-key`）
3. Token 是否已过期（默认超时 30 天）

## 前端

### 页面白屏 / 路由加载失败

**原因**：动态导入模块加载失败（通常是网络问题或部署路径变化）。

**解决**：前端路由已实现重试机制（`resilientView`），会自动重试 2 次。如果持续失败，检查网络连接和部署路径。

### API 请求返回 401

**原因**：Token 过期或未登录。

**行为**：前端拦截器会自动跳转到登录页，并携带 `redirect` 参数记录原始路径。OIDC 模式下会重定向到认证中心。

### Element Plus 样式丢失

**检查**：Vite 配置中是否正确使用了 `ElementPlusResolver` 自动导入。确认 `vite.config.ts` 中的 `unplugin-vue-components` 配置：

```ts
Components({
  resolvers: [ElementPlusResolver({ directives: false, importStyle: 'css' })],
})
```

### 构建后 chunk 过大

Vite 配置了 `manualChunks` 手动分包：
- `mapbox-gl` → 地图库单独分包
- `markdown-it` → Markdown 渲染
- `vendor-motion` → GSAP + motion-v
- `vendor-vue` → Vue + Vue Router

如仍有大包警告，可在 `vite.config.ts` 的 `chunkSizeWarningLimit` 调整阈值。

## 部署

### Docker 部署后页面显示 Nginx 错误页

**排查步骤**：

```bash
# 1. 检查 Frontend 容器是否运行
curl -i http://127.0.0.1:18080/

# 2. 检查 Backend 是否可访问
curl -i http://127.0.0.1:8921/api/v1/site/register-enabled

# 3. 检查宝塔 Nginx 反向代理配置
# 确保代理目标是 127.0.0.1 而非服务器公网 IP

# 4. 查看 Nginx 错误日志
tail -n 100 /www/wwwlogs/www.jmi-openatom.cn.error.log
```

### 头像上传后丢失

**原因**：头像保存在容器内 `/app/uploads/avatars`，由 `avatar_data` 持久卷承载。如果旧版本曾将头像写在容器临时文件系统中，容器重建后文件会丢失。

**解决**：确保 Docker Compose 配置中 `avatar_data` volume 正确挂载。已丢失的头像需要用户重新上传。

### `api.jmi-openatom.cn` 访问失败但本地 Backend 正常

**原因**：宝塔站点或 DNS 配置有误。

**检查**：
1. DNS 是否正确解析到服务器 IP
2. 宝塔中是否为 `api.jmi-openatom.cn` 创建了独立站点
3. SSL 证书是否正确配置
4. 反向代理目标是否为 `127.0.0.1:8921`

## 数据库

### Flyway 迁移失败

**排查**：
```bash
# 查看后端启动日志中的 Flyway 输出
docker compose logs --tail=200 backend | grep -i flyway
```

常见原因：
- 迁移脚本校验失败（已有数据库与脚本不一致）→ 使用 `baseline-on-migrate: true` 和 `baseline-version: 0`
- 脚本命名不规范 → 确保格式为 `V{数字}__{描述}.sql`，双下划线
- 版本号冲突 → 检查是否与已有版本号重复

### 往届成员查询不到数据

**原因**：查询往届成员时需要 `leftAt` 字段不为空（即成员已退出）。

**解决**：确保查询条件包含 `left_at IS NOT NULL`。参考 `development_practice_specification` 中的"往届管理人员添加取消成员状态限制"。

## 开发

### 新增实体字段后的完整流程

1. 在 `entity` 包下修改实体类，添加新字段
2. 在 `backend/src/main/resources/db/migration/` 下创建新的 Flyway 迁移脚本（如 `V30__add_new_field.sql`）
3. 开发环境手动执行该 SQL
4. 如有 DTO 变更，同步修改 `dto` 包下的请求对象
5. 如有查询变更，同步修改 `mapper` 和 `service`
6. 前端如需展示新字段，修改对应的 Vue 组件和 API 类型定义
