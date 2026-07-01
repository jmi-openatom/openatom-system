# OpenAtom 微服务 API 契约与接口规约文档

> 版本：v1.0  
> 适用范围：各微服务 REST API 定义、Feign 内部调用接口、事件消息格式  
> 配套文档：《微服务重构开发文档》《AI 搭建基础框架文档》  
> 编写日期：2026-07

---

## 一、通用规约

### 1.1 统一响应体

所有 REST API 返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 200=成功，401=未认证，403=无权限，500=服务错误 |
| message | string | 提示信息 |
| data | any | 业务数据 |

### 1.2 分页响应

```json
{
  "code": 200,
  "data": {
    "list": [],
    "total": 100,
    "current": 1,
    "size": 20
  }
}
```

### 1.3 路径规范

- 统一前缀：`/api/{服务名简写}/`（网关层剥离 `/api` 前缀后路由）
- RESTful 风格：`GET /list`、`POST /create`、`PATCH /{id}`、`DELETE /{id}`
- 路径参数：kebab-case（`/recruitment-campaigns/{campaignId}`）
- 查询参数：camelCase（`?pageNum=1&pageSize=20`）

### 1.4 认证头

```
jmiopenatom: {token}
Authorization: Bearer {token}
```

网关校验 Token 后，将 `userId` 注入下游请求头 `X-User-Id`。

### 1.5 错误码表

| 错误码 | 含义 | HTTP Status |
|--------|------|-------------|
| 200 | 成功 | 200 |
| 400 | 参数错误 | 400 |
| 401 | 未认证/Token失效 | 401 |
| 403 | 无权限 | 403 |
| 404 | 资源不存在 | 404 |
| 409 | 状态冲突 | 409 |
| 500 | 服务内部错误 | 500 |

---

## 二、auth-service 接口定义

### 2.1 对外 REST API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| POST | `/auth/login` | 公开 | 用户登录 |
| POST | `/auth/register` | 公开 | 用户注册（需开启注册） |
| POST | `/auth/logout` | `auth:logout` | 退出登录 |
| GET | `/auth/me` | `auth:me` | 获取当前用户信息 |
| PATCH | `/auth/password` | 登录 | 修改密码 |
| POST | `/auth/avatar` | 登录 | 上传头像 |
| DELETE | `/auth/avatar` | 登录 | 删除头像 |
| POST | `/auth/qq-bind-token` | 登录 | 创建QQ绑定Token |
| DELETE | `/auth/qq-bind` | 登录 | 解绑QQ |
| PATCH | `/auth/register-enabled` | `auth:register:manage` | 切换注册开关 |
| POST | `/oauth/token` | 公开 | OAuth 2.0 Token 端点 |
| GET | `/oauth/admin/clients` | `oauth-client:list` | OAuth客户端列表 |
| POST | `/oauth/admin/clients` | `oauth-client:create` | 创建OAuth客户端 |
| PATCH | `/oauth/admin/clients/{id}` | `oauth-client:update` | 更新OAuth客户端 |
| DELETE | `/oauth/admin/clients/{id}` | `oauth-client:delete` | 删除OAuth客户端 |
| GET | `/roles` | `role:list` | 角色列表 |
| POST | `/roles` | `role:create` | 创建角色 |
| GET | `/roles/{id}` | `role:detail` | 角色详情 |
| PATCH | `/roles/{id}` | `role:update` | 更新角色 |
| DELETE | `/roles/{id}` | `role:delete` | 删除角色 |
| POST | `/roles/{id}/permissions` | `role:permission:assign` | 分配角色权限 |
| GET | `/permissions` | `permission:list` | 权限点列表 |
| POST | `/permissions` | `permission:create` | 创建权限点 |

### 2.2 内部 Feign 接口

```java
@FeignClient(name = "auth-service")
public interface AuthFeignClient {

    // 校验用户权限
    @GetMapping("/internal/auth/check-permission")
    Result<Boolean> checkPermission(@RequestParam Long userId,
                                    @RequestParam String permissionCode);

    // 获取用户角色列表
    @GetMapping("/internal/auth/user-roles")
    Result<List<String>> getUserRoles(@RequestParam Long userId);

    // 获取用户权限列表
    @GetMapping("/internal/auth/user-permissions")
    Result<List<String>> getUserPermissions(@RequestParam Long userId);
}
```

---

## 三、user-service 接口定义

### 3.1 对外 REST API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/users` | `user:list` | 用户分页列表 |
| POST | `/users` | `user:create` | 创建用户 |
| GET | `/users/{id}` | `user:info` | 用户详情 |
| PATCH | `/users/{id}` | `user:update` | 更新用户 |
| DELETE | `/users/{id}` | `user:delete` | 删除用户 |
| PATCH | `/users/{id}/status` | `user:status:update` | 更新用户状态 |
| POST | `/users/{id}/reset-password` | `user:password:reset` | 重置密码 |
| GET | `/users/{id}/roles` | `user:role:assign` | 用户角色列表 |
| POST | `/users/{id}/roles` | `user:role:assign` | 分配用户角色 |
| POST | `/users/import` | `user:import` | Excel导入用户 |
| GET | `/users/import/template` | `user:import` | 下载导入模板 |
| GET | `/users/export` | `user:export` | 导出用户Excel |
| GET | `/users/avatars/health` | `user:info` | 头像健康检查 |
| POST | `/users/avatars/cleanup` | `user:update` | 清理无效头像 |
| GET | `/users/{id}/memberships` | `user:membership:list` | 用户社团关系 |
| GET | `/operation-logs` | `log:operation:list` | 操作日志 |
| GET | `/login-logs` | `log:login:list` | 登录日志 |

### 3.2 内部 Feign 接口

```java
@FeignClient(name = "user-service")
public interface UserFeignClient {

    // 批量查用户基本信息
    @GetMapping("/internal/users/batch")
    Result<Map<Long, UserBriefVO>> batchGetUsers(@RequestParam List<Long> userIds);

    // 单个用户详情
    @GetMapping("/internal/users/{userId}")
    Result<UserBriefVO> getUserBrief(@PathVariable Long userId);
}
```

**UserBriefVO（内部调用返回）：**
```json
{
  "userId": 1,
  "userName": "admin",
  "realName": "管理员",
  "avatar": "/uploads/avatar.jpg",
  "studentId": "2023001"
}
```

---

## 四、club-service 接口定义

### 4.1 对外 REST API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/clubs` | `club:list` | 社团列表 |
| POST | `/clubs` | `club:create` | 创建社团 |
| GET | `/clubs/{id}` | `club:detail` | 社团详情 |
| PATCH | `/clubs/{id}` | `club:update` | 更新社团 |
| PATCH | `/clubs/{id}/status` | `club:status:update` | 更新社团状态 |
| PATCH | `/clubs/{id}/recruitment-status` | `club:recruitment-status:update` | 更新招新状态 |
| GET | `/clubs/{clubId}/departments` | `department:list` | 部门列表 |
| POST | `/clubs/{clubId}/departments` | `department:create` | 创建部门 |
| GET | `/departments/{id}` | `department:detail` | 部门详情 |
| PATCH | `/departments/{id}` | `department:update` | 更新部门 |
| DELETE | `/departments/{id}` | `department:delete` | 删除部门 |
| GET | `/clubs/{clubId}/positions` | `position:list` | 岗位列表 |
| POST | `/clubs/{clubId}/positions` | `position:create` | 创建岗位 |
| PATCH | `/positions/{id}` | `position:update` | 更新岗位 |
| DELETE | `/positions/{id}` | `position:delete` | 删除岗位 |
| GET | `/memberships` | `membership:list` | 成员列表 |
| POST | `/memberships` | `membership:create` | 新增成员 |
| PATCH | `/memberships/{id}` | `membership:update` | 更新成员 |
| POST | `/memberships/{id}/assign-position` | `membership:position:assign` | 分配岗位 |
| POST | `/memberships/{id}/change-status` | `membership:status:change` | 修改成员状态 |
| POST | `/memberships/batch-change-status` | `membership:status:change` | 批量修改状态 |
| POST | `/memberships/batch-create` | `membership:create` | 批量新增成员 |
| POST | `/memberships/{id}/force-exit` | `membership:force-exit` | 强制退社 |
| GET | `/awards` | `award:list` | 获奖列表 |
| POST | `/awards` | `award:create` | 创建获奖 |
| PATCH | `/awards/{id}` | `award:update` | 更新获奖 |
| DELETE | `/awards/{id}` | `award:delete` | 删除获奖 |
| GET | `/clubs/{clubId}/alumni-groups` | `membership:list` | 往届分组列表 |
| POST | `/clubs/{clubId}/alumni-groups` | `membership:create` | 创建往届分组 |
| PATCH | `/alumni-groups/{id}` | `membership:update` | 更新往届分组 |
| DELETE | `/alumni-groups/{id}` | `membership:update` | 删除往届分组 |

### 4.2 内部 Feign 接口

```java
@FeignClient(name = "club-service")
public interface ClubFeignClient {

    // 创建成员（招新审批通过后调用）
    @PostMapping("/internal/memberships")
    Result<Long> createMembership(@RequestBody CreateMembershipDTO dto);

    // 查社团基本信息
    @GetMapping("/internal/clubs/{clubId}")
    Result<ClubBriefVO> getClubBrief(@PathVariable Long clubId);
}
```

---

## 五、recruitment-service 接口定义

### 5.1 对外 REST API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/clubs/{clubId}/recruitment-campaigns` | `recruitment-campaign:list` | 招新计划列表 |
| POST | `/clubs/{clubId}/recruitment-campaigns` | `recruitment-campaign:create` | 创建招新计划 |
| GET | `/recruitment-campaigns/{id}` | `recruitment-campaign:detail` | 招新计划详情 |
| PATCH | `/recruitment-campaigns/{id}` | `recruitment-campaign:update` | 更新招新计划 |
| POST | `/recruitment-campaigns/{id}/publish` | `recruitment-campaign:publish` | 发布招新计划 |
| POST | `/recruitment-campaigns/{id}/close` | `recruitment-campaign:close` | 关闭招新计划 |
| GET | `/site-forms` | `site-form:list` | 信息表单列表 |
| POST | `/site-forms` | `site-form:create` | 创建信息表单 |
| PATCH | `/site-forms/{id}` | `site-form:update` | 更新信息表单 |
| GET | `/site-forms/{id}` | `site-form:detail` | 表单详情 |
| POST | `/site-forms/{id}/publish` | `site-form:update` | 发布表单 |
| POST | `/site-forms/{id}/close` | `site-form:update` | 关闭表单 |
| GET | `/site-forms/{id}/share-info` | `site-form:detail` | 表单分享信息 |
| GET | `/site-forms/{formId}/submissions` | `site-form:detail` | 表单提交列表 |
| GET | `/site-forms/{formId}/submissions/export` | `site-form:detail` | 导出提交记录 |
| GET | `/applications` | `application:list` | 入会申请列表 |
| POST | `/applications` | `application:create` | 提交入会申请 |
| PATCH | `/applications/{id}` | `application:update` | 更新申请 |
| POST | `/applications/{id}/submit` | `application:submit` | 提交草稿 |
| POST | `/applications/{id}/withdraw` | `application:withdraw` | 撤回申请 |
| GET | `/applications/{id}` | `application:detail` | 申请详情 |
| GET | `/applications/{id}/approval-records` | `approval-record:list` | 审批记录 |
| POST | `/applications/{id}/approvals` | `application:approve` | 审批 |
| POST | `/applications/batch-approvals` | `application:batch-approve` | 批量审批 |
| POST | `/applications/{id}/final-decisions` | `application:final-decision` | 终审 |
| GET | `/applications/export` | `application:list` | 导出申请 |
| GET | `/interviews` | `interview:list` | 面试列表 |
| POST | `/interviews` | `interview:create` | 创建面试 |
| PATCH | `/interviews/{id}` | `interview:update` | 更新面试 |
| POST | `/interviews/{id}/confirm` | `interview:confirm` | 确认面试 |
| POST | `/interviews/{id}/feedback` | `interview:feedback` | 提交反馈 |
| POST | `/interviews/{id}/complete` | `interview:complete` | 完成面试 |
| GET | `/interviews/{id}/feedbacks` | `interview:detail` | 面试反馈列表 |

---

## 六、activity-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/activities` | `activity:list` | 活动列表 |
| POST | `/activities` | `activity:create` | 创建活动 |
| GET | `/activities/{id}` | `activity:detail` | 活动详情 |
| PATCH | `/activities/{id}` | `activity:update` | 更新活动 |
| DELETE | `/activities/{id}` | `activity:delete` | 删除活动 |
| POST | `/activities/{id}/registrations` | 登录 | 活动报名 |
| GET | `/activities/{id}/registrations` | `activity-registration:list` | 报名列表 |
| GET | `/site/club-home` | 公开 | 社团首页 |
| GET | `/site/activities` | 公开 | 前台活动列表 |
| GET | `/site/activities/{id}` | 公开 | 前台活动详情 |
| GET | `/site/recruitment` | 公开 | 前台招新信息 |
| GET | `/site/progress` | 登录 | 报名进度 |

---

## 七、blog-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| POST | `/blog/articles` | 登录 | 创建文章 |
| PATCH | `/blog/articles/{id}` | 登录 | 更新文章 |
| POST | `/blog/articles/{id}/publish` | 登录 | 发布文章 |
| DELETE | `/blog/articles/{id}` | 登录 | 删除文章 |
| GET | `/blog/my/articles` | 登录 | 我的文章 |
| GET | `/blog/admin/articles` | `blog:list` | 后台文章列表 |
| POST | `/blog/admin/articles/{id}/review` | `blog:review` | 审核文章 |
| DELETE | `/blog/admin/articles/{id}` | `blog:delete` | 后台删除 |
| GET | `/blog/admin/articles/{id}/comments` | `blog:list` | 文章评论 |
| PATCH | `/blog/admin/comments/{id}/status` | `blog:comment:moderate` | 评论状态 |
| GET | `/blog/admin/interactions` | `blog:list` | 互动记录 |
| GET | `/site/blog/articles` | 公开 | 前台文章列表 |
| GET | `/site/blog/articles/{id}` | 公开 | 前台文章详情 |
| GET | `/site/blog/categories` | 公开 | 分类列表 |
| GET | `/site/blog/articles/{id}/comments` | 公开 | 前台评论 |
| POST | `/site/blog/articles/{id}/comments` | 登录 | 发表评论 |
| POST | `/site/blog/articles/{id}/like` | 登录 | 点赞 |
| POST | `/site/blog/articles/{id}/favorite` | 登录 | 收藏 |
| POST | `/site/blog/articles/{id}/share` | 登录 | 分享 |

---

## 八、checkin-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/check-ins` | `check-in:list` | 签到场次列表 |
| POST | `/check-ins` | `check-in:create` | 创建签到场次 |
| GET | `/check-ins/{id}` | `check-in:list` | 场次详情 |
| PATCH | `/check-ins/{id}` | `check-in:update` | 更新场次 |
| POST | `/check-ins/{id}/close` | `check-in:update` | 关闭场次 |
| DELETE | `/check-ins/{id}` | `check-in:delete` | 删除场次 |
| POST | `/check-ins/{id}/targets` | `check-in:update` | 添加签到人员 |
| GET | `/check-ins/{id}/records` | `check-in:list` | 签到记录 |
| PATCH | `/check-ins/{id}/records/{userId}` | `check-in:update` | 更新记录状态 |
| GET | `/check-in-groups` | `check-in:list` | 签到分组列表 |
| POST | `/check-in-groups` | `check-in:create` | 创建分组 |
| PUT | `/check-in-groups/{id}` | `check-in:update` | 更新分组 |
| DELETE | `/check-in-groups/{id}` | `check-in:delete` | 删除分组 |
| DELETE | `/check-in-groups/{id}/members/{userId}` | `check-in:update` | 移除分组成员 |
| POST | `/site/check-ins/scan` | 登录 | 扫码签到 |

---

## 九、point-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/points/admin/accounts` | `point:account:list` | 积分账户列表 |
| GET | `/points/admin/transactions` | `point:account:list` | 积分流水 |
| POST | `/points/admin/adjustments` | `point:adjust` | 积分调整 |
| GET | `/points/admin/rules` | `point:rule:list` | 积分规则 |
| PATCH | `/points/admin/rules` | `point:rule:update` | 更新规则 |
| GET | `/points/admin/items` | `point:item:list` | 兑换项列表 |
| POST | `/points/admin/items` | `point:item:create` | 创建兑换项 |
| PATCH | `/points/admin/items/{id}` | `point:item:update` | 更新兑换项 |
| DELETE | `/points/admin/items/{id}` | `point:item:delete` | 删除兑换项 |
| GET | `/points/admin/redemptions` | `point:redemption:list` | 兑换记录 |
| PATCH | `/points/admin/redemptions/{id}/status` | `point:redemption:review` | 审核兑换 |
| GET | `/site/points/leaderboard` | 公开 | 积分排行榜 |
| GET | `/site/points/me` | 登录 | 我的积分 |
| GET | `/site/points/items` | 公开 | 兑换商城 |
| POST | `/site/points/items/{id}/redeem` | 登录 | 兑换 |
| GET | `/site/points/redemptions` | 登录 | 我的兑换 |

### 内部接口（事件消费）

point-service 不暴露 Feign 接口，通过消费 RabbitMQ 事件实现积分发放。

---

## 十、notification-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/notifications` | 登录 | 我的通知 |
| GET | `/notifications/unread-count` | 登录 | 未读数 |
| POST | `/notifications/{id}/read` | 登录 | 标记已读 |
| GET | `/notifications/admin` | `notification:list` | 后台通知列表 |
| POST | `/notifications/admin` | `notification:create` | 发送通知 |
| DELETE | `/notifications/admin/{id}` | `notification:delete` | 删除通知 |

---

## 十一、office-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/office-documents` | `document:list` | 文书列表 |
| POST | `/office-documents` | `document:create` | 创建文书 |
| PATCH | `/office-documents/{id}` | `document:update` | 更新文书 |
| GET | `/office-documents/{id}/export` | `document:export` | 导出文书 |
| GET | `/office-documents/user-options` | `document:list` | 用户选项 |
| GET | `/document-templates` | `document:list` | 模板列表 |
| POST | `/document-templates` | `document:create` | 上传模板 |
| GET | `/document-templates/{id}/variables` | `document:list` | 模板变量 |
| PUT | `/document-templates/{id}/variables` | `document:update` | 保存变量 |
| GET | `/generated-documents/{id}/download` | `document:export` | 下载生成文书 |

---

## 十二、file-service 接口定义

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| POST | `/image-hosting/images` | 登录 | 上传图片 |
| GET | `/image-hosting/images/my` | 登录 | 我的图片 |
| DELETE | `/image-hosting/images/{id}` | 登录 | 删除图片 |
| GET | `/image-hosting/admin/images` | `image:list` | 后台图片列表 |
| DELETE | `/image-hosting/admin/images/{id}` | `image:delete` | 后台删除图片 |

---

## 十三、事件消息契约

### 13.1 事件信封格式

所有 RabbitMQ 事件统一信封：

```json
{
  "eventType": "point.award",
  "userId": 123,
  "sourceType": "checkin",
  "sourceId": 456,
  "sourceKey": "checkin:456:user:123",
  "payload": {
    "delta": 2,
    "description": "扫码签到奖励"
  },
  "timestamp": "2026-07-01T10:00:00Z"
}
```

### 13.2 事件清单

| 事件名 | Exchange | Routing Key | 发布者 | 消费者 | 说明 |
|--------|----------|-------------|--------|--------|------|
| `point.award` | `openatom.point` | `point.award` | checkin-service | point-service | 签到积分 |
| `point.award` | `openatom.point` | `point.award` | activity-service | point-service | 活动参与积分 |
| `point.award` | `openatom.point` | `point.award` | blog-service | point-service | 博客发布积分 |
| `point.award` | `openatom.point` | `point.award` | auth-service | point-service | 每日登录积分 |
| `notification.send` | `openatom.notification` | `notification.send` | 各业务服务 | notification-service | 统一通知 |
| `member.created` | `openatom.club` | `club.member.created` | recruitment-service | notification-service | 成员创建通知 |

### 13.3 事件详细定义

**point.award（积分发放）：**
```json
{
  "eventType": "point.award",
  "userId": 123,
  "sourceType": "checkin",
  "sourceId": 456,
  "sourceKey": "checkin:456:user:123",
  "payload": {
    "delta": 2,
    "description": "扫码签到奖励",
    "operatorId": null
  },
  "timestamp": "2026-07-01T10:00:00Z"
}
```
> `sourceKey` 作为幂等键，point-service 消费时校验防重。

**notification.send（通知发送）：**
```json
{
  "eventType": "notification.send",
  "payload": {
    "title": "招新结果通知",
    "content": "您的入会申请已通过审批",
    "type": "recruitment",
    "receiverUserIds": [123, 456]
  },
  "timestamp": "2026-07-01T10:00:00Z"
}
```

**member.created（成员创建）：**
```json
{
  "eventType": "member.created",
  "userId": 123,
  "payload": {
    "clubId": 1,
    "membershipId": 789,
    "departmentId": 3,
    "positionId": 5
  },
  "timestamp": "2026-07-01T10:00:00Z"
}
```

### 13.4 消费者幂等规范

所有事件消费者必须实现幂等处理：

```java
@RabbitListener(queues = "point.award")
public void handlePointAward(PointAwardEvent event) {
    // 幂等校验：sourceKey 唯一
    if (pointTransactionMapper.existsBySourceKey(event.getSourceKey())) {
        return; // 已处理，跳过
    }
    // 业务处理...
}
```

---

## 十四、Feign 接口汇总

| Feign 客户端 | 被调服务 | 调用方 | 用途 |
|-------------|----------|--------|------|
| AuthFeignClient | auth-service | 各服务 | 权限校验 |
| UserFeignClient | user-service | club, recruitment, office, activity | 查用户信息 |
| ClubFeignClient | club-service | recruitment, activity, office | 查社团/创建成员 |
| FileFeignClient | file-service | blog, activity, office | 文件URL获取 |

### Feign 接口包规范

各服务在 `feign/` 包下定义 Feign 客户端接口：

```
club-service/src/main/java/edu/jmi/openatom/club/feign/
├── UserFeignClient.java      ← 调用 user-service
└── AuthFeignClient.java      ← 调用 auth-service
```

内部接口路径统一 `/internal/` 前缀，不经过网关鉴权：

```java
// user-service 内部接口（不对外暴露）
@RestController
@RequestMapping("/internal/users")
public class UserInternalController {
    @GetMapping("/batch")
    public Result<Map<Long, UserBriefVO>> batchGetUsers(@RequestParam List<Long> userIds) { ... }
}
```

> 网关路由规则排除 `/internal/**` 路径，仅服务间可通过 Nacos 直连调用。

---

## 附录：权限码完整清单

| 模块 | 权限码 | 说明 |
|------|--------|------|
| 认证 | auth:logout, auth:me | 退出/查看个人信息 |
| 用户 | user:list, user:create, user:info, user:update, user:status:update, user:password:reset, user:role:assign, user:import, user:membership:list, user:export | 用户管理 |
| 社团 | club:list, club:create, club:detail, club:update, club:status:update, club:recruitment-status:update | 社团管理 |
| 部门 | department:list, department:create, department:detail, department:update, department:delete | 部门管理 |
| 岗位 | position:list, position:create, position:detail, position:update, position:delete | 岗位管理 |
| 招新 | recruitment-campaign:list/create/detail/update/publish/close | 招新计划 |
| 表单 | site-form:list/create/update/detail | 信息表单 |
| 申请 | application:list/create/detail/update/submit/withdraw/approve/batch-approve/final-decision | 入会申请 |
| 审批 | approval-record:list | 审批记录 |
| 面试 | interview:list/create/detail/update/confirm/feedback/complete | 面试管理 |
| 成员 | membership:list/create/detail/update/position:assign/status:change/force-exit | 成员管理 |
| 退社 | exit-application:list/create/detail/approve/reject | 退社申请 |
| 活动 | activity:list/create/detail/update/delete, activity-registration:list | 活动管理 |
| 博客 | blog:list/review/delete, blog:comment:moderate | 博客管理 |
| 签到 | check-in:list/create/update/delete | 签到管理 |
| 积分 | point:account:list, point:adjust, point:rule:list/update, point:item:list/create/update/delete, point:redemption:list/review | 积分管理 |
| 通知 | notification:list/create/read/delete | 通知管理 |
| 文书 | document:list/create/update/export | 办公文书 |
| 图床 | image:list/delete | 图床管理 |
| 日志 | log:operation:list, log:login:list | 系统日志 |
| OAuth | oauth-client:list/create/update/delete | OAuth客户端 |
| 获奖 | award:list/create/update/delete | 获奖管理 |

---

> 本文档为微服务 API 契约规约，开发时各服务按此定义实现接口。架构设计请参考《微服务重构开发文档》。
