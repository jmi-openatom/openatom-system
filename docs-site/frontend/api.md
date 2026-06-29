# API 请求层

## Axios 实例

前端 API 请求基于 Axios 封装，核心配置位于 `src/api/request.ts`：

```typescript
const service: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
    timeout: 20000,
    withCredentials: true,
})
```

## 请求拦截器

自动添加认证 Token 到请求头：

```typescript
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
        config.headers.jmiopenatom = token           // Sa-Token 请求头
        config.headers.Authorization = `Bearer ${token}` // 兼容 Bearer 鉴权
    }
    return config
})
```

## 响应拦截器

### 成功响应处理

后端统一返回 `Result<T>` 结构，响应拦截器自动解包：

```typescript
service.interceptors.response.use((response: AxiosResponse) => {
    const body = response.data
    // code !== 0 表示业务失败
    if (body && typeof body.code !== 'undefined' && body.code !== 0) {
        if (UNAUTHORIZED_CODES.has(Number(body.code))) {
            redirectToLogin(body.message)  // 401 / 40100 → 跳转登录
            return Promise.reject(body)
        }
        if (!config.silent) showError(body.message || '操作失败')
        return Promise.reject(body)
    }
    // 自动解包：返回 data 字段
    return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
})
```

### 错误响应处理

- **401 未授权**：清除会话，跳转登录页
- **GET 请求可重试**：对 408/425/429/500/502/503/504 和网络错误自动重试 1 次
- **网络错误**：智能提示（断网、超时、服务不可用）
- **消息去重**：1.8 秒内相同错误消息不重复弹窗

### 限流响应处理

- **429**：提示"请求过于频繁，请稍后重试"
- GET 请求自动重试

## 特殊配置项

| 配置项 | 类型 | 说明 |
|--------|------|------|
| `retry` | number | GET 请求最大重试次数，默认 1 |
| `silent` | boolean | 静默模式，不弹出错误提示 |

使用示例：

```typescript
// 静默请求（不弹错误提示）
api.get('/site/register-enabled', { silent: true })

// 自定义重试次数
api.get('/large-data', { retry: 3 })
```

## API 方法定义

API 方法定义在 `src/api/index.ts` 中，按模块组织：

```typescript
export const authApi = {
    login: (data: RequestLoginDTO) => service.post('/auth/login', data),
    logout: () => service.post('/auth/logout'),
    me: () => service.get('/auth/me'),
    refreshToken: (data: RequestRefreshTokenDTO) => service.post('/auth/refresh-token', data),
}

export const clubApi = {
    list: (params?: ClubQuery) => service.get('/clubs', { params }),
    detail: (clubId: number) => service.get(`/clubs/${clubId}`),
    create: (data: RequestCreateClubDTO) => service.post('/clubs', data),
    update: (clubId: number, data: RequestUpdateClubDTO) => service.patch(`/clubs/${clubId}`, data),
}
```

## 全局请求状态

通过 `useAppStatus` 组合式函数管理全局请求状态：

```typescript
// 请求开始
const requestToken = beginRequest()

// 请求结束
endRequest(requestToken)
```

可用于显示全局加载指示器。

## 网络错误消息

智能网络错误提示：

| 场景 | 消息 |
|------|------|
| 离线 | "网络连接已断开，请检查网络后重试" |
| 超时 | "网络响应超时，请稍后重试" |
| 429 | "请求过于频繁，请稍后重试" |
| 5xx | "服务暂时不可用，请稍后重试" |
| 其他 | 后端返回的 message 或 "网络请求失败" |

## 重试机制

GET 请求在以下情况自动重试：

- 网络错误（`ECONNABORTED`, `ETIMEDOUT`, `ERR_NETWORK`）
- 状态码 408, 425, 429, 500, 502, 503, 504
- 无响应（服务不可达）

重试延迟使用指数退避 + 随机抖动：

```typescript
function retryDelay(attempt: number): Promise<void> {
    const delay = Math.min(2400, 500 * 2 ** Math.max(0, attempt - 1)) + Math.random() * 250
    return new Promise((resolve) => window.setTimeout(resolve, delay))
}
```

重试前会等待网络恢复（最长 8 秒）。
