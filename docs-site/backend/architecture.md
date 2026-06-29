# 后端架构概览

## 整体架构

后端采用经典的分层架构，基于 Spring Boot 3.3.12 构建：

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP 请求入口                          │
│                  Context Path: /api/v1                    │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                 Controller 层 (控制器)                     │
│   接收请求 → 参数校验 → 调用 Service → 返回 Result<T>      │
│                                                         │
│   ┌─────────────────────────────────────────┐           │
│   │  AOP 切面: ControllerLogAspect          │           │
│   │  自动记录操作日志                         │           │
│   └─────────────────────────────────────────┘           │
│                                                         │
│   ┌─────────────────────────────────────────┐           │
│   │  拦截器:                                 │           │
│   │  - OperationLogInterceptor (操作日志)    │           │
│   │  - ApplicationSubmitRateLimitInterceptor│           │
│   │  - ClientIpResolver (IP 解析)           │           │
│   └─────────────────────────────────────────┘           │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Service 层 (业务逻辑)                     │
│   ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  │
│   │ AuthService │  │ ClubService  │  │ UserService  │  │
│   │ MembershipS │  │ ActivitySv   │  │ BlogService  │  │
│   │ PointServic │  │ BotService   │  │ VoteService  │  │
│   │ ... (40+)   │  │              │  │              │  │
│   └─────────────┘  └──────────────┘  └──────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                Mapper 层 (MyBatis Plus)                   │
│   BaseMapper 接口 → XML / 注解 SQL                        │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    MySQL 8.0                              │
└─────────────────────────────────────────────────────────┘

         ┌─────────────────┐    ┌──────────────────┐
         │  Redis Cache    │    │  Sa-Token (JWT)  │
         │  限流 / 缓存     │    │  认证 / 权限校验  │
         └─────────────────┘    └──────────────────┘
```

## 分层职责

### Controller 层

- 接收 HTTP 请求，参数校验（`@Valid`）
- 调用 Service 层处理业务逻辑
- 返回统一响应格式 `Result<T>`
- 通过 `@SaCheckPermission` 注解进行接口级权限控制
- 共有 44 个 Controller，覆盖全部业务模块

### Service 层

- 核心业务逻辑处理
- 事务管理（`@Transactional`）
- 调用 Mapper 进行数据持久化
- 缓存操作
- 跨模块协调
- 共有 40+ 个 Service 接口及实现

### Mapper 层

- 基于 MyBatis Plus 的 `BaseMapper` 接口
- 复杂查询使用 XML 或注解 SQL
- 分页查询使用 `PageRequests` 工具类

### Entity 层

- 与数据库表一一对应
- 使用 Lombok `@Data` / `@Getter` 简化代码
- MyBatis Plus `@TableName` / `@TableField` 注解
- 共有 60+ 个实体类

## 统一响应格式

所有 API 接口统一返回 `Result<T>` 结构：

```java
public class Result<T> implements Serializable {
    private final Integer code;    // 状态码，0 = 成功
    private final String message;  // 消息提示
    private final T data;          // 返回数据
    private final String traceId;  // 追踪 ID
}
```

**成功响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "traceId": "defaultTraceId"
}
```

**失败响应**：

```json
{
  "code": 50000,
  "message": "操作失败",
  "data": null,
  "traceId": "defaultTraceId"
}
```

**特殊状态码**：

| 状态码 | 含义 |
|--------|------|
| 0 | 成功 |
| 401 | 未登录 |
| 40100 | Token 过期 |
| 50000 | 通用错误 |

## 全局异常处理

`GlobalExceptionHandlerAdvice` 统一捕获异常并转换为 `Result` 响应：

- `MethodArgumentNotValidException` → 参数校验失败
- `RateLimitExceededException` → 限流超限
- `NotLoginException` → Sa-Token 未登录
- `NotPermissionException` → Sa-Token 权限不足
- 其他未捕获异常 → 500 内部错误

## AOP 切面

### ControllerLogAspect

自动记录 Controller 层操作日志：
- 拦截所有 Controller 方法
- 记录请求参数、响应结果、执行耗时
- 写入 `operation_log` 表

## 拦截器

| 拦截器 | 说明 |
|--------|------|
| `OperationLogInterceptor` | 操作日志记录 |
| `ApplicationSubmitRateLimitInterceptor` | 报名提交限流（基于 Redis，默认 5 次/秒） |
| `ClientIpResolver` | 客户端 IP 解析（支持代理转发） |

## 启动初始化器

`bootstrap` 包下的初始化器在应用启动时自动执行：

| 初始化器 | 说明 |
|----------|------|
| `SystemUserInitializer` | 创建默认管理员账号 |
| `SystemPermissionInitializer` | 初始化权限种子数据 |
| `DefaultClubInitializer` | 创建默认社团 |
| `DefaultDepartmentInitializer` | 创建默认部门 |
| `DefaultPositionInitializer` | 创建默认岗位 |
| `DefaultMembershipInitializer` | 创建默认成员关系 |
| `SchemaCompatibilityInitializer` | 数据库兼容性兜底 |
| `AvatarMaintenanceInitializer` | 头像数据维护 |

## 端口与路径

| 配置项 | 值 |
|--------|-----|
| 服务端口 | 8921 |
| Context Path | /api/v1 |
| Swagger（如有） | /api/v1/swagger-ui.html |

## 相关文档

- [项目结构](./structure.md) - 详细的包结构说明
- [认证与权限](./auth.md) - Sa-Token 与 RBAC 模型
- [数据库迁移](./flyway.md) - Flyway 使用指南
- [配置说明](./config.md) - 应用配置详解
- [开发规范](./conventions.md) - 代码规范与约定
