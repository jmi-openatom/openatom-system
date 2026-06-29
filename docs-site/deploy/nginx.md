# Nginx 反向代理

## 双域名架构

生产环境使用两个独立域名：

| 域名 | 代理目标 | 说明 |
|------|----------|------|
| `www.jmi-openatom.cn` | `127.0.0.1:18080` | Frontend 前端 |
| `api.jmi-openatom.cn` | `127.0.0.1:8921` | Backend 后端 API |

::: warning 重要
如果宝塔 Nginx 和 Docker Compose 运行在同一台服务器，反向代理目标应使用本机地址 `127.0.0.1`，不要使用服务器公网 IP。
:::

## Frontend 反向代理

`www.jmi-openatom.cn` 站点配置：

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

### WebSocket 支持

前端如使用 WebSocket，需配置 Upgrade 头：

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection $connection_upgrade;
```

需要在 `http` 块中定义 `$connection_upgrade`：

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}
```

## Backend 反向代理

`api.jmi-openatom.cn` 站点配置：

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

### SSL 配置

`api.jmi-openatom.cn` 需要在宝塔中创建独立站点并配置 SSL 证书（Let's Encrypt）。

## API 域名分离

::: danger 关键说明
Frontend Docker 镜像默认使用 `VITE_API_BASE_URL=https://api.jmi-openatom.cn/api/v1`。

浏览器 API 请求**不应**发送到 `www.jmi-openatom.cn/api/v1`，而应直接发送到 `api.jmi-openatom.cn/api/v1`。

修改该构建参数后**必须重新构建** Frontend 容器。
:::

## 排查 Nginx 错误

出现 Nginx 错误页时，在服务器终端依次检查：

```bash
# 1. 宝塔 Nginx 能否连接 Frontend 容器
curl -i http://127.0.0.1:18080/

# 2. 宿主机能否直接访问 Backend
curl -i http://127.0.0.1:8921/api/v1/site/register-enabled

# 3. 验证公开 API 域名
curl -i https://api.jmi-openatom.cn/api/v1/site/register-enabled

# 4. 查看两个宝塔站点的 Nginx 上游错误
tail -n 100 /www/wwwlogs/www.jmi-openatom.cn.error.log
tail -n 100 /www/wwwlogs/api.jmi-openatom.cn.error.log
```

### 错误场景判断

| 现象 | 可能原因 |
|------|----------|
| `curl 127.0.0.1:18080` 失败 | Frontend 容器未运行或 18080 未监听 |
| `curl 127.0.0.1:8921` 失败 | Backend 未运行 |
| 本机 Backend 成功但公开 API 失败 | `api.jmi-openatom.cn` 宝塔站点或 DNS 配置有误 |
| 错误日志中上游为服务器公网 IP | 需改为对应的 `127.0.0.1` 端口 |

## CORS 配置

后端已配置 CORS 支持：

```yaml
app:
  cors:
    allowed-origin-patterns: '*'
```

生产环境建议限制为具体域名：

```yaml
app:
  cors:
    allowed-origin-patterns: 'https://www.jmi-openatom.cn,https://api.jmi-openatom.cn'
```

## 压缩配置

后端已启用 Gzip 压缩：

```yaml
server:
  compression:
    enabled: true
    min-response-size: 1024
```

Nginx 也可配置额外的压缩，但注意不要重复压缩已压缩的响应。
