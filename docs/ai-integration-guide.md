# 开放原子社团管理系统 · AI 项目接入文档

> 本文档面向需要接入「开放原子社团管理系统」的 AI 及第三方项目，覆盖全部 HTTP 接口、认证方式、数据模型与调用约定。

---

## 1. 服务信息

| 项目 | 值 |
| --- | --- |
| 服务名称 | openatom-system |
| 技术栈 | Java 17 + Spring Boot + MyBatis-Plus + MySQL + Redis |
| 鉴权框架 | Sa-Token（内部会话）+ OAuth 2.0 / OIDC（第三方接入） |
| HTTP 端口 | 8921 |
| 全局前缀（context-path） | `/api/v1` |
| 生产 Issuer | `https://oauth.jmi-openatom.cn/api/v1` |
| 本地开发地址 | `http://localhost:8921/api/v1` |
| 请求/响应格式 | `application/json`（文件上传/导出除外） |
| 时间格式 | ISO 8601 字符串，如 `2026-06-24T14:00:00+08:00` |
| 字段命名 | camelCase |

下文所有路径均省略公共前缀 `/api/v1`，完整地址 = `基础地址 + 路径`。

---

## 2. 认证方式

系统提供两套认证体系，第三方 AI 项目推荐使用 **OAuth 2.0 / OIDC**。

### 2.1 OAuth 2.0 / OIDC（推荐第三方使用）

支持授权码模式、PKCE、Refresh Token、UserInfo、Token Introspection。详细流程见 `docs/oauth-usage.md`，核心端点：

| 用途 | 方法 | 路径 |
| --- | --- | --- |
| OIDC Discovery | GET | `/.well-known/openid-configuration` |
| JWKS | GET | `/oauth/jwks` |
| 发起授权 | GET | `/oauth/authorize` |
| 换取/刷新令牌 | POST | `/oauth/token` |
| 用户信息 | GET | `/oauth/userinfo` |
| 令牌内省 | POST | `/oauth/introspect` |

**授权码流程**：

1. 管理员在后台「认证应用」注册 OAuth 客户端，获得 `client_id` / `client_secret`（公开客户端 secret 留空，必须用 PKCE）。
2. 跳转 `GET /oauth/authorize?response_type=code&client_id=xxx&redirect_uri=xxx&scope=openid profile&state=xxx&code_challenge=xxx&code_challenge_method=S256`。
3. 回调收到 `code`，调用 `POST /oauth/token`（`Content-Type: application/x-www-form-urlencoded`）换取令牌。
4. 使用 `Authorization: Bearer <access_token>` 调用业务接口或 `GET /oauth/userinfo`。

**令牌响应示例**：

```json
{
  "access_token": "ACCESS_TOKEN",
  "id_token": "ID_TOKEN",
  "refresh_token": "REFRESH_TOKEN",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "openid profile email roles permissions",
  "user": {
    "sub": "1",
    "club_user_id": 1,
    "username": "username",
    "name": "张三",
    "email": "user@example.com",
    "phone": "13800000000",
    "student_id": "20260001",
    "avatar": "https://example.com/avatar.png",
    "roles": ["formal_member"],
    "permissions": ["activity:list"]
  },
  "issuer": "https://oauth.jmi-openatom.cn/api/v1"
}
```

- Access Token 有效期 1 小时，Refresh Token 7 天，Refresh Token 轮换使用。
- 推荐 `sub` 作为用户稳定唯一标识。
- 支持 Scope：`openid` `profile` `email` `roles` `permissions`。

### 2.2 Sa-Token 内部会话

Web 前端与小程序使用账号密码登录后获取 Sa-Token，通过请求头传递：

- Token 名称（Header / Cookie key）：`jmiopenatom`
- 用法：`jmiopenatom: <tokenValue>`

### 2.3 机器人/API Key

- QQ 机器人事件回调使用 `X-OpenAtom-Bot-Token` 头校验。
- 数据开放公开接口使用 `X-Openatom-Data-Key`（或 `X-Data-Open-Key` / `openatom-data-key`）头。

---

## 3. 通用约定

### 3.1 统一响应格式

所有 JSON 接口返回 `Result<T>` 结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "defaultTraceId"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | int | 0 成功，非 0 失败 |
| message | string | 提示消息 |
| data | T / null | 业务数据，可为 null |
| traceId | string | 链路追踪 ID |

### 3.2 分页响应

分页接口 `data` 为 `PageDataVO<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "page": 1,
    "pageSize": 10,
    "total": 120
  }
}
```

通用分页参数：`page`（默认 1）、`pageSize`（默认 10）。

### 3.3 通用错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 400 | 参数/请求错误 |
| 401 | 未认证或令牌无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 50000 | 服务器内部错误（默认错误码） |

### 3.4 权限校验

后台管理接口使用 `@SaCheckPermission("权限码")` 校验。各接口所需权限码在下文标注。完整权限码列表见第 8 节。未标注权限码的接口为前台公开接口或仅需登录。

---

## 4. 接口清单

### 4.1 认证模块（AuthController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/auth/register` | - | 用户注册 |
| 2 | POST | `/auth/login` | - | 账号密码登录 |
| 3 | POST | `/auth/miniapp-login` | - | 微信小程序登录 |
| 4 | POST | `/auth/miniapp-bind` | - | 绑定微信小程序 |
| 5 | POST | `/auth/qq-bind-token` | - | 生成 QQ 绑定令牌 |
| 6 | POST | `/auth/qq-bind/confirm` | - | 确认 QQ 绑定 |
| 7 | GET | `/auth/qq-bind/status` | - | 查询 QQ 绑定状态 |
| 8 | POST | `/auth/qq-bind` | - | 确认 QQ 绑定（兼容） |
| 9 | DELETE | `/auth/qq-bind` | - | 解绑 QQ |
| 10 | POST | `/auth/refresh-token` | - | 刷新令牌 |
| 11 | POST | `/auth/introspect` | - | 令牌内省 |
| 12 | POST | `/auth/logout` | `auth:logout` | 退出登录 |
| 13 | GET | `/auth/me` | `auth:me` | 获取当前用户信息与权限 |
| 14 | PATCH | `/auth/register-enabled` | - | 更新注册开关 |
| 15 | PATCH | `/auth/password` | - | 修改密码 |
| 16 | POST | `/auth/avatar` | `auth:me` | 上传头像（multipart） |
| 17 | DELETE | `/auth/avatar` | `auth:me` | 删除头像 |

**POST `/auth/register`** 请求体：

```json
{
  "username": "zhangsan",
  "password": "123456Abc",
  "realName": "张三",
  "phone": "13800000000",
  "email": "zhangsan@example.com",
  "studentId": "20260001"
}
```
校验：username/password 必填；密码 8-72 位；phone 正则 `^1\d{10}$`；email 格式校验。

**POST `/auth/login`** 请求体：

```json
{ "username": "zhangsan", "password": "123456Abc" }
```

响应 `data`（`ResponseLoginVO`）：

```json
{
  "accessToken": "xxx",
  "refreshToken": "xxx",
  "expiresIn": 2592000,
  "user": { "id": 1, "username": "zhangsan", "realName": "张三", "...": "..." },
  "roles": ["formal_member"],
  "permissions": ["activity:list", "club:list"]
}
```

**GET `/auth/me`** 响应 `data`（`ResponseCurrentUserVO`）：

```json
{
  "user": { "id": 1, "username": "zhangsan", "...": "..." },
  "roles": ["formal_member"],
  "permissions": ["activity:list"]
}
```

**PATCH `/auth/register-enabled`**：query 参数 `enabled`（Boolean）。
**PATCH `/auth/password`** 请求体：`RequestChangePasswordDTO`（含旧/新密码）。
**POST `/auth/avatar`**：multipart 表单，字段 `file`。

---

### 4.2 用户模块（UserController · `/users`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/users` | `user:list` | 分页查询用户 |
| 2 | POST | `/users` | `user:create` | 创建用户 |
| 3 | POST | `/users/import` | `user:import` | 批量导入用户（multipart） |
| 4 | GET | `/users/import/template` | `user:import` | 下载导入模板（xlsx） |
| 5 | POST | `/users/{userId}/roles` | `user:role:assign` | 分配用户角色 |
| 6 | GET | `/users/{userId}/roles` | `user:role:assign` | 获取用户角色 |
| 7 | GET | `/users/{userId}` | `user:info` | 用户详情 |
| 8 | PATCH | `/users/{userId}` | `user:update` | 更新用户 |
| 9 | DELETE | `/users/{userId}` | `user:delete` | 删除用户 |
| 10 | PATCH | `/users/{userId}/status` | `user:status:update` | 更新用户状态 |
| 11 | POST | `/users/{userId}/reset-password` | `user:password:reset` | 重置密码 |
| 12 | GET | `/users/{userId}/memberships` | `user:membership:list` | 用户社团关系 |
| 13 | GET | `/users/avatars/health` | `user:list` | 头像健康检查 |
| 14 | POST | `/users/avatars/cleanup` | `user:update` | 清理无效头像 |

**GET `/users`** 查询参数：`keyword`、`status`（active/disabled/locked）、`clubId`、`page`、`pageSize`。

**POST `/users`** 请求体（`RequestCreateUserDTO`）：

```json
{
  "username": "lisi",
  "password": "123456Abc",
  "realName": "李四",
  "gender": "male",
  "phone": "13800000001",
  "email": "lisi@example.com",
  "studentNo": "20260002",
  "college": "计算机学院",
  "major": "软件工程",
  "grade": "2026",
  "className": "软工1班",
  "status": "active"
}
```

**POST `/users/{userId}/roles`** 请求体：`{ "roleIds": [2, 5] }`。
**PATCH `/users/{userId}/status`** 请求体：`{ "status": "disabled" }`。
**POST `/users/{userId}/reset-password`** 请求体：`{ "newPassword": "NewPass123" }`。

---

### 4.3 社团模块（ClubController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/clubs` | `club:list` | 社团列表 |
| 2 | POST | `/clubs` | `club:create` | 创建社团 |
| 3 | GET | `/clubs/{clubId}` | `club:detail` | 社团详情 |
| 4 | PATCH | `/clubs/{clubId}` | `club:update` | 更新社团 |
| 5 | PATCH | `/clubs/{clubId}/status` | `club:status:update` | 更新社团状态 |
| 6 | PATCH | `/clubs/{clubId}/recruitment-status` | `club:recruitment-status:update` | 更新招新状态 |

**GET `/clubs`** 参数：`keyword`、`category`、`status`、`recruitmentStatus`。

**POST `/clubs`** 请求体（`RequestCreateClubDTO`）：

```json
{
  "name": "计算机协会",
  "code": "csa",
  "category": "academic",
  "description": "面向编程与技术交流",
  "logoUrl": "",
  "presidentUserId": 12
}
```
校验：name/code 必填；code 正则 `^[a-z][a-z0-9_-]{1,63}$`。

**PATCH `/clubs/{clubId}/status`** 请求体：`{ "status": "active" }`。
**PATCH `/clubs/{clubId}/recruitment-status`** 请求体：`{ "recruitmentStatus": "open" }`。

---

### 4.4 部门模块（DepartmentController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/clubs/{clubId}/departments` | - | 社团部门列表 |
| 2 | POST | `/clubs/{clubId}/departments` | `department:create` | 创建部门 |
| 3 | GET | `/departments/{departmentId}` | `department:detail` | 部门详情 |
| 4 | PATCH | `/departments/{departmentId}` | `department:update` | 更新部门 |
| 5 | DELETE | `/departments/{departmentId}` | `department:delete` | 删除部门 |

**POST `/clubs/{clubId}/departments`** 请求体（`RequestCreateDepartmentDTO`）：

```json
{ "name": "技术部", "description": "负责技术培训", "managerUserId": 18 }
```

---

### 4.5 岗位模块（PositionController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/clubs/{clubId}/positions` | `position:list` | 岗位列表 |
| 2 | POST | `/clubs/{clubId}/positions` | `position:create` | 创建岗位 |
| 3 | GET | `/positions/{positionId}` | `position:detail` | 岗位详情 |
| 4 | PATCH | `/positions/{positionId}` | `position:update` | 更新岗位 |
| 5 | DELETE | `/positions/{positionId}` | `position:delete` | 删除岗位 |

**POST `/clubs/{clubId}/positions`** 请求体（`RequestCreatePositionDTO`）：

```json
{ "name": "部长", "departmentId": 3, "maxCount": 1, "roleIds": [5, 6] }
```

---

### 4.6 招新计划模块（RecruitmentCampaignController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/clubs/{clubId}/recruitment-campaigns` | `recruitment-campaign:list` | 招新计划列表 |
| 2 | POST | `/clubs/{clubId}/recruitment-campaigns` | `recruitment-campaign:create` | 创建招新计划 |
| 3 | GET | `/recruitment-campaigns/{campaignId}` | `recruitment-campaign:detail` | 招新计划详情 |
| 4 | PATCH | `/recruitment-campaigns/{campaignId}` | `recruitment-campaign:update` | 更新招新计划 |
| 5 | POST | `/recruitment-campaigns/{campaignId}/publish` | `recruitment-campaign:publish` | 发布招新 |
| 6 | POST | `/recruitment-campaigns/{campaignId}/close` | `recruitment-campaign:close` | 关闭招新 |

**POST `/clubs/{clubId}/recruitment-campaigns`** 请求体（`RequestCreateRecruitmentCampaignDTO`）：

```json
{
  "name": "2026 春季招新",
  "applyStartAt": "2026-05-01T00:00:00+08:00",
  "applyEndAt": "2026-05-15T23:59:59+08:00",
  "interviewStartAt": "2026-05-18T09:00:00+08:00",
  "interviewEndAt": "2026-05-22T18:00:00+08:00",
  "resultPublishAt": "2026-05-25T12:00:00+08:00",
  "targetGrades": ["2026", "2025"],
  "maxApplicants": 500,
  "loginRequired": false,
  "formSchema": [],
  "status": "draft"
}
```

---

### 4.7 入会申请模块（ApplicationController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/applications` | `application:list` | 分页查询申请 |
| 2 | POST | `/applications` | - | 提交入会申请（支持匿名，IP 限流 5次/秒） |
| 3 | GET | `/applications/{applicationId}` | `application:detail` | 申请详情 |
| 4 | PATCH | `/applications/{applicationId}` | `application:update` | 修改申请 |
| 5 | POST | `/applications/{applicationId}/submit` | `application:submit` | 提交草稿 |
| 6 | POST | `/applications/{applicationId}/withdraw` | `application:withdraw` | 撤回申请 |
| 7 | GET | `/applications/export` | `application:export` | 导出 Excel |

**GET `/applications`** 参数：`campaignId`、`clubId`、`status`、`departmentId`、`keyword`、`page`、`pageSize`。

**POST `/applications`** 请求体（`RequestCreateApplicationDTO`）：

```json
{
  "campaignId": 10,
  "clubId": 2,
  "firstChoiceDepartmentId": 3,
  "secondChoiceDepartmentId": 4,
  "profile": {
    "selfIntroduction": "热爱技术",
    "skills": ["Java", "Spring Boot"],
    "availableTime": "每周二、四晚上"
  }
}
```

---

### 4.8 审核模块（ApprovalController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/applications/{applicationId}/approval-records` | `approval-record:list` | 审批记录 |
| 2 | POST | `/applications/{applicationId}/approvals` | `application:approve` | 执行审批 |
| 3 | POST | `/applications/batch-approvals` | `application:batch-approve` | 批量审批 |

**POST `/applications/{applicationId}/approvals`** 请求体（`RequestApprovalActionDTO`）：

```json
{
  "action": "approve",
  "node": "pre_screen",
  "comment": "符合条件",
  "nextInterviewerIds": [21, 22]
}
```
`action` 可选：`approve` / `reject` / `transfer` / `request_more_info`。

---

### 4.9 面试模块（InterviewController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/interviews` | `interview:list` | 面试列表 |
| 2 | POST | `/interviews` | `interview:create` | 创建面试 |
| 3 | GET | `/interviews/{interviewId}` | `interview:detail` | 面试详情 |
| 4 | PATCH | `/interviews/{interviewId}` | `interview:update` | 更新面试 |
| 5 | POST | `/interviews/{interviewId}/confirm` | `interview:confirm` | 候选人确认 |
| 6 | POST | `/interviews/{interviewId}/feedback` | `interview:feedback` | 提交反馈 |
| 7 | POST | `/interviews/{interviewId}/complete` | `interview:complete` | 标记完成 |
| 8 | GET | `/interviews/{interviewId}/feedbacks` | `interview:detail` | 反馈列表 |

**GET `/interviews`** 参数：`campaignId`、`applicationId`、`interviewerId`、`status`。

**POST `/interviews`** 请求体（`RequestCreateInterviewDTO`）：

```json
{
  "applicationId": 1001,
  "round": 1,
  "scheduledStartAt": "2026-05-18T14:00:00+08:00",
  "scheduledEndAt": "2026-05-18T14:30:00+08:00",
  "location": "教学楼 A203",
  "mode": "offline",
  "interviewerIds": [21, 22]
}
```

**POST `/interviews/{interviewId}/feedback`** 请求体（`RequestInterviewFeedbackDTO`）：含评分 scores、建议 suggestion、评语 comment。

---

### 4.10 成员模块（MembershipController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/applications/{applicationId}/final-decisions` | `application:final-decision` | 终审决策 |
| 2 | GET | `/memberships` | `membership:list` | 成员列表 |
| 3 | POST | `/memberships` | `membership:create` | 新增成员 |
| 4 | GET | `/memberships/{membershipId}` | `membership:detail` | 成员详情 |
| 5 | PATCH | `/memberships/{membershipId}` | `membership:update` | 更新成员 |
| 6 | POST | `/memberships/batch-change-status` | `membership:batch-change-status` | 批量变更状态 |
| 7 | POST | `/memberships/batch-create` | `membership:batch-create` | 批量创建 |
| 8 | POST | `/memberships/{membershipId}/assign-position` | `membership:position:assign` | 分配岗位 |
| 9 | POST | `/memberships/{membershipId}/change-status` | `membership:status:change` | 变更状态 |

**GET `/memberships`** 参数：`clubId`、`departmentId`、`positionId`、`status`、`keyword`。

**POST `/memberships`** 请求体（`RequestCreateMembershipDTO`）：

```json
{
  "userId": 100,
  "clubId": 2,
  "departmentId": 3,
  "positionId": 8,
  "status": "probation",
  "featured": false,
  "sortOrder": 0
}
```

**POST `/applications/{applicationId}/final-decisions`** 请求体（`RequestFinalDecisionDTO`）：含 decision（approved/waitlisted/rejected）、departmentId、positionId、comment。

---

### 4.11 退社模块（ExitApplicationController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/exit-applications` | `exit-application:list` | 退社申请列表 |
| 2 | POST | `/exit-applications` | `exit-application:create` | 发起退社 |
| 3 | GET | `/exit-applications/{exitApplicationId}` | `exit-application:detail` | 退社详情 |
| 4 | POST | `/exit-applications/{exitApplicationId}/approve` | `exit-application:approve` | 通过退社 |
| 5 | POST | `/exit-applications/{exitApplicationId}/reject` | `exit-application:reject` | 驳回退社 |
| 6 | POST | `/memberships/{membershipId}/force-exit` | `membership:force-exit` | 强制退社 |

**POST `/exit-applications`** 请求体（`RequestCreateExitApplicationDTO`）：

```json
{ "membershipId": 3001, "reason": "study_conflict", "description": "课程时间冲突" }
```

---

### 4.12 活动模块（ActivityController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/activities` | `activity:list` | 活动列表 |
| 2 | GET | `/activities/{activityId}` | `activity:detail` | 活动详情 |
| 3 | POST | `/activities` | `activity:create` | 创建活动 |
| 4 | PATCH | `/activities/{activityId}` | `activity:update` | 更新活动 |
| 5 | DELETE | `/activities/{activityId}` | `activity:delete` | 删除活动 |
| 6 | POST | `/activities/{activityId}/registrations` | - | 报名活动 |
| 7 | GET | `/activities/{activityId}/registrations` | `activity-registration:list` | 报名列表 |

**POST `/activities`** 请求体（`RequestCreateActivityDTO`）：

```json
{
  "title": "开源沙龙",
  "summary": "活动摘要",
  "descriptionMarkdown": "活动详情...",
  "activityAt": "2026-06-01T14:00:00+08:00",
  "endAt": "2026-06-01T17:00:00+08:00",
  "location": "图书馆报告厅",
  "status": "draft",
  "coverUrl": "",
  "registrationRequired": true,
  "registrationStartAt": "2026-05-20T00:00:00+08:00",
  "registrationEndAt": "2026-05-30T23:59:59+08:00",
  "registrationFields": [],
  "participationPoints": 10
}
```

---

### 4.13 投票模块（VoteController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/votes` | `vote:list` | 投票列表 |
| 2 | GET | `/votes/{voteId}` | `vote:detail` | 投票详情 |
| 3 | POST | `/clubs/{clubId}/votes` | `vote:create` | 创建投票 |
| 4 | PATCH | `/votes/{voteId}` | `vote:update` | 更新投票 |
| 5 | POST | `/votes/{voteId}/publish` | `vote:update` | 发布投票 |
| 6 | POST | `/votes/{voteId}/close` | `vote:update` | 关闭投票 |
| 7 | POST | `/votes/{voteId}/reset` | `vote:manage-records` | 重置投票 |
| 8 | GET | `/site/votes` | - | 前台投票列表 |
| 9 | GET | `/site/votes/{voteId}` | - | 前台投票详情 |
| 10 | POST | `/site/votes/{voteId}/records` | - | 前台提交投票 |

**POST `/clubs/{clubId}/votes`** 请求体（`RequestCreateVoteDTO`）：

```json
{
  "title": "最佳项目评选",
  "description": "请投票",
  "voteType": "single",
  "maxChoices": 1,
  "anonymousAllowed": false,
  "resultVisibility": "public",
  "startAt": "2026-06-01T00:00:00+08:00",
  "endAt": "2026-06-07T23:59:59+08:00",
  "options": [{ "text": "项目A" }, { "text": "项目B" }]
}
```
`resultVisibility` 可选：`public` / `after_vote` / `private`。

---

### 4.14 抽奖模块（LotteryController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/lotteries` | `lottery:list` | 抽奖列表 |
| 2 | GET | `/lotteries/{lotteryId}` | `lottery:detail` | 抽奖详情 |
| 3 | POST | `/clubs/{clubId}/lotteries` | `lottery:create` | 创建抽奖 |
| 4 | PATCH | `/lotteries/{lotteryId}` | `lottery:update` | 更新抽奖 |
| 5 | POST | `/lotteries/{lotteryId}/publish` | `lottery:update` | 发布抽奖 |
| 6 | POST | `/lotteries/{lotteryId}/close` | `lottery:update` | 关闭抽奖 |
| 7 | POST | `/lotteries/{lotteryId}/draw` | `lottery:draw` | 执行抽奖 |
| 8 | POST | `/lotteries/{lotteryId}/reset` | `lottery:draw` | 重置抽奖 |
| 9 | GET | `/site/lotteries/{lotteryId}/screen` | - | 大屏展示 |

**POST `/clubs/{clubId}/lotteries`** 请求体（`RequestCreateLotteryDTO`）：含 `formId`（关联表单）、`title`、`description`、`prizes`（奖品列表）。

---

### 4.15 签到模块（CheckInController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/check-ins` | `check-in:list` | 签到场次列表 |
| 2 | GET | `/check-ins/user-options` | `check-in:create` | 可选用户 |
| 3 | GET | `/check-in-groups` | `check-in:create` | 签到分组列表 |
| 4 | POST | `/check-in-groups` | `check-in:create` | 创建签到分组 |
| 5 | PUT | `/check-in-groups/{groupId}` | `check-in:create` | 更新签到分组 |
| 6 | DELETE | `/check-in-groups/{groupId}` | `check-in:create` | 删除签到分组 |
| 7 | DELETE | `/check-in-groups/{groupId}/members/{userId}` | `check-in:group-member-delete` | 移除分组成员 |
| 8 | GET | `/check-ins/{sessionId}` | `check-in:detail` | 签到详情 |
| 9 | POST | `/check-ins` | `check-in:create` | 创建签到场次 |
| 10 | PATCH | `/check-ins/{sessionId}` | `check-in:update` | 更新签到场次 |
| 11 | POST | `/check-ins/{sessionId}/close` | `check-in:update` | 关闭签到 |
| 12 | DELETE | `/check-ins/{sessionId}` | `check-in:delete` | 删除签到场次 |
| 13 | POST | `/check-ins/{sessionId}/targets` | `check-in:update` | 添加签到对象 |
| 14 | GET | `/check-ins/{sessionId}/records` | `check-in:records` | 签到记录 |
| 15 | PATCH | `/check-ins/{sessionId}/records/{userId}` | `check-in:update` | 更新记录状态 |
| 16 | POST | `/site/check-ins/scan` | - | 扫码签到 |
| 17 | GET | `/evening-study/schedules` | `check-in:list` | 晚自习排班 |
| 18 | POST | `/evening-study/schedules` | `check-in:create` | 创建排班 |
| 19 | PUT | `/evening-study/schedules/{scheduleId}` | `check-in:update` | 更新排班 |
| 20 | DELETE | `/evening-study/schedules/{scheduleId}` | `check-in:delete` | 删除排班 |
| 21 | POST | `/evening-study/sessions/generate` | `check-in:create` | 生成晚自习场次 |
| 22 | GET | `/evening-study/today` | `check-in:list` | 今日晚自习 |
| 23 | GET | `/site/evening-study/today` | - | 前台今日晚自习 |
| 24 | GET | `/bot/evening-study/today` | - | 机器人今日晚自习 |

**POST `/check-ins`** 请求体（`RequestCreateCheckInSessionDTO`）：含 `activityId`、`groupId`、`title`（必填）、`location`、`startAt`、`endAt`、`checkinPoints`、`targetUserIds`。
**POST `/site/check-ins/scan`** 请求体（`RequestCheckInScanDTO`）：扫码签到信息。

---

### 4.16 积分模块（PointController）

前台接口（`/site/points/*`）仅需登录，后台接口（`/points/admin/*`）需权限。

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/site/points/leaderboard` | - | 积分排行榜 |
| 2 | GET | `/site/points/me` | - | 我的积分概况 |
| 3 | GET | `/site/points/items` | - | 兑换商品列表 |
| 4 | POST | `/site/points/items/{itemId}/redeem` | - | 积分兑换 |
| 5 | GET | `/site/points/redemptions` | - | 我的兑换记录 |
| 6 | GET | `/points/admin/accounts` | `point:account:list` | 积分账户列表 |
| 7 | GET | `/points/admin/transactions` | `point:transaction:list` | 积分流水 |
| 8 | POST | `/points/admin/adjustments` | `point:adjust` | 手动调整积分 |
| 9 | GET | `/points/admin/rules` | `point:account:list` | 积分规则 |
| 10 | PATCH | `/points/admin/rules` | `point:adjust` | 更新积分规则 |
| 11 | GET | `/points/admin/items` | `point:item:list` | 兑换项列表 |
| 12 | POST | `/points/admin/items` | `point:item:manage` | 创建兑换项 |
| 13 | PATCH | `/points/admin/items/{itemId}` | `point:item:manage` | 更新兑换项 |
| 14 | DELETE | `/points/admin/items/{itemId}` | `point:item:manage` | 删除兑换项 |
| 15 | GET | `/points/admin/redemptions` | `point:redemption:list` | 兑换记录列表 |
| 16 | PATCH | `/points/admin/redemptions/{redemptionId}/status` | `point:redemption:manage` | 处理兑换 |

**POST `/points/admin/adjustments`** 请求体（`RequestPointAdjustmentDTO`）：

```json
{ "userId": 100, "delta": 50, "description": "活动奖励" }
```

---

### 4.17 请假模块（LeaveApplicationController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/leave-applications` | `leave-application:list` | 请假列表 |
| 2 | GET | `/site/leave-applications` | - | 我的请假 |
| 3 | GET | `/leave-applications/{leaveApplicationId}` | `leave-application:detail` | 请假详情 |
| 4 | GET | `/site/leave-applications/{leaveApplicationId}` | - | 前台请假详情 |
| 5 | POST | `/site/leave-applications` | - | 提交请假 |
| 6 | POST | `/bot/leave-applications` | - | QQ 机器人提交请假 |
| 7 | GET | `/bot/leave-applications/{leaveApplicationId}/status` | - | 机器人查询请假状态 |
| 8 | POST | `/leave-applications/{leaveApplicationId}/review` | `leave-application:review` | 审批请假 |
| 9 | DELETE | `/leave-applications/{leaveApplicationId}` | `leave-application:delete` | 删除请假 |
| 10 | DELETE | `/site/leave-applications/{leaveApplicationId}` | - | 前台删除请假 |

**POST `/site/leave-applications`** 请求体（`RequestCreateLeaveApplicationDTO`）：

```json
{
  "title": "事假",
  "reason": "家中有事",
  "startAt": "2026-06-24T08:00:00+08:00",
  "endAt": "2026-06-24T18:00:00+08:00",
  "attachments": [{ "fileName": "proof.jpg", "url": "..." }]
}
```

**POST `/leave-applications/{leaveApplicationId}/review`** 请求体（`RequestReviewLeaveApplicationDTO`）：含审批意见与状态。

---

### 4.18 奖项模块（AwardController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/awards` | `award:list` | 奖项列表 |
| 2 | POST | `/awards` | `award:create` | 创建奖项 |
| 3 | PATCH | `/awards/{awardId}` | `award:update` | 更新奖项 |
| 4 | DELETE | `/awards/{awardId}` | `award:delete` | 删除奖项 |

**POST `/awards`** 请求体（`RequestCreateAwardDTO`）：

```json
{
  "title": "蓝桥杯一等奖",
  "competitionName": "蓝桥杯",
  "awardLevel": "国家级一等奖",
  "awardYear": 2026,
  "teamName": "开源队",
  "description": "团队协作获奖",
  "sortOrder": 0
}
```

---

### 4.19 通知模块（NotificationController · `/notifications`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/notifications` | - | 当前用户通知 |
| 2 | GET | `/notifications/unread-count` | - | 未读数量 |
| 3 | POST | `/notifications/admin` | `notification:create` | 发送通知 |
| 4 | POST | `/notifications/{notificationId}/read` | - | 标记已读 |
| 5 | GET | `/notifications/admin` | `notification:list` | 全部通知列表 |
| 6 | DELETE | `/notifications/admin/{notificationId}` | `notification:delete` | 删除通知 |

**POST `/notifications/admin`** 请求体（`RequestCreateNotificationDTO`）：

```json
{
  "title": "活动通知",
  "content": "请准时参加",
  "type": "activity",
  "isAll": false,
  "receiverUserIds": [1, 2, 3]
}
```

---

### 4.20 角色与权限模块

**RoleController（`/roles`）**

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/roles` | `role:list` | 角色列表 |
| 2 | POST | `/roles` | `role:create` | 创建角色 |
| 3 | GET | `/roles/{roleId}` | `role:detail` | 角色详情 |
| 4 | PATCH | `/roles/{roleId}` | `role:update` | 更新角色 |
| 5 | DELETE | `/roles/{roleId}` | `role:delete` | 删除角色 |
| 6 | POST | `/roles/{roleId}/permissions` | `role:permission:assign` | 分配角色权限 |

**POST `/roles/{roleId}/permissions`** 请求体：`{ "permissionIds": [101, 102] }`。

**PermissionController（`/permissions`）**

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/permissions` | `permission:list` | 权限列表 |
| 2 | POST | `/permissions` | `permission:create` | 创建权限 |

---

### 4.21 日志模块（LogController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/operation-logs` | `log:operation:list` | 操作日志 |
| 2 | GET | `/login-logs` | `log:login:list` | 登录日志 |

**GET `/operation-logs`** 参数：`operatorId`、`module`、`action`、`startAt`、`endAt`、`keyword`、`page`、`pageSize`。
**GET `/login-logs`** 参数：`keyword`、`page`、`pageSize`。

---

### 4.22 站点表单模块（SiteFormController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/clubs/{clubId}/site-forms` | `site-form:list` | 表单列表 |
| 2 | POST | `/clubs/{clubId}/site-forms` | `site-form:create` | 创建表单 |
| 3 | GET | `/site-forms/{formId}` | `site-form:detail` | 表单详情 |
| 4 | PATCH | `/site-forms/{formId}` | `site-form:update` | 更新表单 |
| 5 | POST | `/site-forms/{formId}/publish` | `site-form:update` | 发布表单 |
| 6 | POST | `/site-forms/{formId}/close` | `site-form:update` | 关闭表单 |
| 7 | GET | `/site-forms/{formId}/share-info` | `site-form:detail` | 分享信息（小程序路径） |

**POST `/clubs/{clubId}/site-forms`** 请求体（`RequestCreateSiteFormDTO`）：含表单名称、字段结构等。

---

### 4.23 表单提交模块（FormSubmissionController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/site/forms/{formId}/submissions` | - | 提交表单 |
| 2 | GET | `/site-forms/{formId}/submissions` | `site-form:detail` | 提交列表 |
| 3 | GET | `/site-forms/{formId}/submissions/export` | `site-form:export` | 导出 Excel |

**POST `/site/forms/{formId}/submissions`** 请求体（`RequestCreateFormSubmissionDTO`）：含表单字段数据。

---

### 4.24 博客模块（BlogController）

公开浏览（`/site/blog/*`，无需登录）、成员创作（`/blog/*`，需登录）、管理后台（`/blog/admin/*`，需权限）。

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/site/blog/articles` | - | 公开文章列表（分页） |
| 2 | GET | `/site/blog/articles/{articleId}` | - | 公开文章详情 |
| 3 | GET | `/site/blog/categories` | - | 分类列表 |
| 4 | GET | `/site/blog/articles/{articleId}/comments` | - | 公开评论 |
| 5 | POST | `/site/blog/articles/{articleId}/comments` | - | 发表评论 |
| 6 | POST | `/site/blog/articles/{articleId}/like` | - | 点赞 |
| 7 | POST | `/site/blog/articles/{articleId}/favorite` | - | 收藏 |
| 8 | POST | `/site/blog/articles/{articleId}/share` | - | 分享 |
| 9 | GET | `/blog/my/articles` | - | 我的文章 |
| 10 | POST | `/blog/articles` | - | 创建文章 |
| 11 | PATCH | `/blog/articles/{articleId}` | - | 更新文章 |
| 12 | POST | `/blog/articles/{articleId}/publish` | - | 提交审核 |
| 13 | DELETE | `/blog/articles/{articleId}` | - | 删除文章 |
| 14 | GET | `/blog/admin/articles` | `blog:list` | 管理文章列表 |
| 15 | POST | `/blog/admin/articles/{articleId}/review` | `blog:review` | 审核文章 |
| 16 | DELETE | `/blog/admin/articles/{articleId}` | `blog:delete` | 删除文章 |
| 17 | GET | `/blog/admin/articles/{articleId}/comments` | `blog-comment:list` | 评论列表 |
| 18 | PATCH | `/blog/admin/comments/{commentId}/status` | `blog-comment:manage` | 更新评论状态 |
| 19 | GET | `/blog/admin/interactions` | `blog-interaction:list` | 互动记录 |

**POST `/blog/articles`** 请求体（`RequestCreateBlogArticleDTO`）：

```json
{
  "title": "文章标题",
  "contentMarkdown": "正文...",
  "summary": "摘要",
  "coverUrl": "",
  "category": "技术",
  "tags": ["Java", "Spring"],
  "status": "draft"
}
```

---

### 4.25 公文模块（OfficeDocumentController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/office-documents` | `document:list` | 公文列表 |
| 2 | GET | `/office-documents/user-options` | `document:list` | 用户选项 |
| 3 | POST | `/office-documents` | `document:create` | 创建公文 |
| 4 | PATCH | `/office-documents/{documentId}` | `document:update` | 更新公文 |
| 5 | GET | `/office-documents/{documentId}/export` | `document:export` | 导出 Word |

**POST `/office-documents`** 请求体（`RequestSaveOfficeDocumentDTO`）：

```json
{ "docType": "notice", "title": "关于举办活动的通知", "payload": { "body": "..." } }
```

---

### 4.26 文档模板模块（DocumentTemplateController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/document-templates` | `document:create` | 上传模板（multipart） |
| 2 | GET | `/document-templates` | `document:list` | 模板列表 |
| 3 | GET | `/document-templates/{templateId}/variables` | `document:list` | 模板变量 |
| 4 | PUT | `/document-templates/{templateId}/variables` | `document:update` | 保存变量 |
| 5 | GET | `/generated-documents/{documentId}/download` | `document:export` | 下载生成文档 |

**POST `/document-templates`**：multipart，参数 `templateType`、`templateName`、`file`。

---

### 4.27 校历模块（SchoolCalendarController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/school-calendar` | `school-calendar:manage` | 校历详情 |
| 2 | GET | `/site/school-calendar` | - | 前台校历 |
| 3 | POST | `/school-calendar` | `school-calendar:manage` | 保存校历 |
| 4 | POST | `/school-calendar/adjustments` | `school-calendar:manage` | 保存调课 |
| 5 | DELETE | `/school-calendar/adjustments/{adjustmentId}` | `school-calendar:manage` | 删除调课 |

---

### 4.28 规章制度模块（ClubRegulationController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/regulations` | `regulation:list` | 制度列表 |
| 2 | GET | `/regulations/{regulationId}` | `regulation:list` | 制度详情 |
| 3 | POST | `/clubs/{clubId}/regulations` | `regulation:create` | 创建制度 |
| 4 | PATCH | `/regulations/{regulationId}` | `regulation:update` | 更新制度 |
| 5 | DELETE | `/regulations/{regulationId}` | `regulation:delete` | 删除制度 |
| 6 | GET | `/site/regulations` | - | 前台制度列表 |
| 7 | GET | `/site/regulations/{regulationId}` | - | 前台制度详情 |

**GET `/regulations`** 参数：`clubId`、`status`、`keyword`。

---

### 4.29 应用展示模块（ShowcaseAppController）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/site/apps` | - | 公开应用列表 |
| 2 | GET | `/site/apps/{appId}` | - | 公开应用详情 |
| 3 | GET | `/showcase-apps` | `showcase-app:list` | 管理列表（分页） |
| 4 | POST | `/showcase-apps` | `showcase-app:manage` | 创建应用 |
| 5 | PATCH | `/showcase-apps/{appId}` | `showcase-app:manage` | 更新应用 |
| 6 | PATCH | `/showcase-apps/{appId}/status` | `showcase-app:manage` | 更新状态 |
| 7 | DELETE | `/showcase-apps/{appId}` | `showcase-app:delete` | 删除应用 |

**GET `/site/apps`** 参数：`keyword`、`openSource`。
**PATCH `/showcase-apps/{appId}/status`**：query 参数 `status`。

---

### 4.30 图床模块（ImageHostingController · `/image-hosting`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/image-hosting/images` | `image:upload` | 上传图片（multipart） |
| 2 | GET | `/image-hosting/images/my` | `image:upload` | 我的图片（分页） |
| 3 | DELETE | `/image-hosting/images/{imageId}` | `image:upload` | 删除自己的图片 |
| 4 | GET | `/image-hosting/admin/images` | `image:list` | 管理图片（分页） |
| 5 | DELETE | `/image-hosting/admin/images/{imageId}` | `image:delete` | 管理删除图片 |

**POST `/image-hosting/images`**：multipart，字段 `file`。图片读取通过 `GET /files/images/{fileName}` 公开访问。

---

### 4.31 文件模块（FileController · `/files`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/files/avatars/{fileName}` | - | 读取头像（公开） |
| 2 | GET | `/files/images/{fileName}` | - | 读取图床图片（公开） |

---

### 4.32 请假附件模块（LeaveAttachmentController · `/leave-attachments`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/leave-attachments/{fileName}` | - | 读取请假附件（公开） |

---

### 4.33 前台站点模块（SiteController）

均为公开或仅需登录的接口，用于小程序/官网前台。

| # | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 1 | GET | `/site/club-home` | 社团首页（参数 `clubCode`） |
| 2 | GET | `/site/activities` | 公开活动列表 |
| 3 | GET | `/site/activities/{activityId}` | 公开活动详情 |
| 4 | GET | `/site/clubs` | 公开社团列表 |
| 5 | GET | `/site/recruitment` | 招新信息（参数 `clubId`） |
| 6 | GET | `/site/recruitment/{campaignId}` | 招新详情 |
| 7 | GET | `/site/forms/{formId}` | 报名表单详情 |
| 8 | GET | `/site/progress` | 我的报名进度 |
| 9 | GET | `/site/register-enabled` | 注册是否开启 |

---

### 4.34 公开接口模块（PublicController · `/public`）

| # | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 1 | POST | `/public/login` | API Key 登录（头 `X-Openatom-Data-Key`） |
| 2 | POST | `/public/data-open/applications` | 提交数据开放申请 |
| 3 | GET | `/public/data-open/applications/{applicationId}` | 查询申请详情（参数 `applicantContact`） |

**POST `/public/data-open/applications`** 请求体（`RequestDataOpenApplicationDTO`）：

```json
{
  "appName": "我的AI助手",
  "applicantName": "张三",
  "applicantContact": "zhangsan@example.com",
  "organization": "某某实验室",
  "purpose": "用于社团活动数据分析"
}
```

---

### 4.35 数据开放后台模块（DataOpenController · `/data-open`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/data-open/admin/applications` | `data-open:list` | 申请列表（分页） |
| 2 | POST | `/data-open/admin/applications/{applicationId}/review` | `data-open:review` | 审核申请 |

**GET `/data-open/admin/applications`** 参数：`keyword`、`status`、`page`、`pageSize`。

---

### 4.36 OAuth 客户端管理（OauthClientController · `/oauth/admin/clients`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/oauth/admin/clients` | `oauth-client:list` | 客户端列表 |
| 2 | POST | `/oauth/admin/clients` | `oauth-client:manage` | 创建客户端 |
| 3 | PATCH | `/oauth/admin/clients/{id}` | `oauth-client:manage` | 更新客户端 |
| 4 | DELETE | `/oauth/admin/clients/{id}` | `oauth-client:manage` | 删除客户端 |

**POST `/oauth/admin/clients`** 请求体（`RequestOauthClientDTO`）：

```json
{
  "clientId": "my-ai-app",
  "clientSecret": "随机强密钥",
  "clientName": "AI 助手",
  "redirectUris": "https://example.com/auth/callback",
  "scopes": "openid profile email roles permissions",
  "grantTypes": "authorization_code refresh_token",
  "enabled": true
}
```

---

### 4.37 AI 活动自动化模块（AiActivityAutomationController）

基于 DeepSeek 的活动策划自动化，支持 SSE 流式输出。

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/ai/activity/sessions` | `activity:create` | 创建会话 |
| 2 | POST | `/ai/activity/sessions/stream` | `activity:create` | 创建会话（SSE 流） |
| 3 | GET | `/ai/activity/sessions` | `activity:list` | 会话列表 |
| 4 | GET | `/ai/activity/sessions/{sessionId}` | `activity:list` | 会话详情 |
| 5 | DELETE | `/ai/activity/sessions/{sessionId}` | `activity:create` | 删除会话 |
| 6 | POST | `/ai/activity/sessions/{sessionId}/messages` | `activity:create` | 发送消息 |
| 7 | POST | `/ai/activity/sessions/{sessionId}/messages/stream` | `activity:create` | 发送消息（SSE 流） |
| 8 | POST | `/ai/activity/sessions/{sessionId}/confirm-requirement` | `activity:create` | 确认需求 |
| 9 | POST | `/ai/activity/sessions/{sessionId}/generate-plan` | `activity:create` | 生成方案 |
| 10 | POST | `/ai/activity/sessions/{sessionId}/generate-plan/stream` | `activity:create` | 生成方案（SSE 流） |
| 11 | POST | `/ai/activity/sessions/{sessionId}/revise-plan` | `activity:create` | 修订方案 |
| 12 | POST | `/ai/activity/sessions/{sessionId}/revise-plan/stream` | `activity:create` | 修订方案（SSE 流） |
| 13 | POST | `/ai/activity/sessions/{sessionId}/plans` | `activity:create` | 保存方案草稿 |
| 14 | POST | `/ai/activity/sessions/{sessionId}/confirm-plan` | `activity:create` | 确认方案 |
| 15 | POST | `/ai/activity/sessions/{sessionId}/create-activity` | `activity:create` | 创建活动草稿 |
| 16 | POST | `/ai/activity/sessions/{sessionId}/documents/generate` | `document:create` | 生成活动文档 |

SSE 流式端点 `produces = text/event-stream`，响应为 `SseEmitter`。`sessionId` 为 Long 类型。

---

### 4.38 AI 设置模块（AiSettingsController · `/ai/settings`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/ai/settings` | `activity:create` | 获取 AI 设置 |
| 2 | PUT | `/ai/settings` | `activity:create` | 更新 AI 设置 |
| 3 | POST | `/ai/settings/test` | `activity:create` | 测试 DeepSeek 连接 |

---

### 4.39 QQ 机器人管理模块（BotManagementController）

QQ 群管理后台，参数多为 `Map<String, Object>` 动态结构。

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/bot/qq-events` | - | OneBot 事件回调（头 `X-OpenAtom-Bot-Token`） |
| 2 | GET | `/bot-management/overview` | `bot-management:list` | 总览 |
| 3 | GET | `/bot-management/accounts` | `bot-management:list` | 机器人账号列表 |
| 4 | POST | `/bot-management/accounts` | `bot-management:config` | 保存账号 |
| 5 | GET | `/bot-management/groups` | `bot-management:list` | 群列表（参数 keyword/botEnabled/mode） |
| 6 | POST | `/bot-management/groups/sync` | `bot-management:sync` | 同步群列表 |
| 7 | POST | `/bot-management/groups/batch-config` | `bot-management:config` | 批量配置 |
| 8 | GET | `/bot-management/groups/{groupId}` | `bot-management:detail` | 群详情 |
| 9 | GET | `/bot-management/groups/{groupId}/members` | `bot-management:members` | 群成员（参数 keyword/role/muteStatus） |
| 10 | POST | `/bot-management/groups/{groupId}/members/sync` | `bot-management:sync` | 同步成员 |
| 11 | PATCH | `/bot-management/groups/{groupId}/config` | `bot-management:config` | 群配置 |
| 12 | POST | `/bot-management/groups/{groupId}/members/{userId}/mute` | `bot-management:mute` | 禁言成员 |
| 13 | POST | `/bot-management/groups/{groupId}/mute-all` | `bot-management:mute` | 全员禁言 |
| 14 | GET | `/bot-management/groups/{groupId}/messages` | `bot-management:messages` | 消息列表 |
| 15 | POST | `/bot-management/groups/{groupId}/messages` | `bot-management:messages` | 发送消息 |
| 16 | POST | `/bot-management/groups/{groupId}/messages/{messageId}/send-now` | `bot-management:messages` | 立即发送定时消息 |
| 17 | DELETE | `/bot-management/groups/{groupId}/messages/{messageId}` | `bot-management:messages` | 删除消息 |
| 18 | GET | `/bot-management/groups/{groupId}/announcements` | `bot-management:announcements` | 公告列表 |
| 19 | POST | `/bot-management/groups/{groupId}/announcements` | `bot-management:announcements` | 发布公告 |
| 20 | POST | `/bot-management/groups/{groupId}/announcements/{announcementId}/republish` | `bot-management:announcements` | 重新发布公告 |
| 21 | DELETE | `/bot-management/groups/{groupId}/announcements/{announcementId}` | `bot-management:announcements` | 删除公告 |
| 22 | GET | `/bot-management/groups/{groupId}/join-requests` | `bot-management:join-requests` | 入群申请（参数 status） |
| 23 | POST | `/bot-management/groups/{groupId}/join-requests` | `bot-management:join-requests` | 保存入群申请 |
| 24 | POST | `/bot-management/groups/{groupId}/join-requests/{requestId}/handle` | `bot-management:join-requests` | 处理入群申请 |
| 25 | GET | `/bot-management/sensitive-words` | `bot-management:sensitive-words` | 敏感词列表 |
| 26 | POST | `/bot-management/sensitive-words` | `bot-management:sensitive-words` | 保存敏感词 |
| 27 | PATCH | `/bot-management/sensitive-words/{wordId}` | `bot-management:sensitive-words` | 更新敏感词 |
| 28 | DELETE | `/bot-management/sensitive-words/{wordId}` | `bot-management:sensitive-words` | 删除敏感词 |
| 29 | GET | `/bot-management/auto-review-rules` | `bot-management:auto-review` | 自动审核规则（参数 groupId） |
| 30 | POST | `/bot-management/auto-review-rules` | `bot-management:auto-review` | 保存规则 |
| 31 | PATCH | `/bot-management/auto-review-rules/{ruleId}` | `bot-management:auto-review` | 更新规则 |
| 32 | DELETE | `/bot-management/auto-review-rules/{ruleId}` | `bot-management:auto-review` | 删除规则 |
| 33 | GET | `/bot-management/statistics` | `bot-management:statistics` | 统计（参数 groupId/startDate/endDate） |
| 34 | GET | `/bot-management/statistics/active-groups` | `bot-management:statistics` | 活跃群统计 |
| 35 | POST | `/bot-management/message-stats` | `bot-management:statistics` | 记录消息统计 |

---

### 4.40 机器人查人模块（BotUserLookupController · `/bot/users`）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | `/bot/users/lookup` | - | 查找用户（参数 keyword/qqOpenid/limit） |

---

## 5. 数据模型

### 5.1 User（用户）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 用户 ID |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| gender | String | 性别 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| studentNo | String | 学号 |
| college | String | 学院 |
| major | String | 专业 |
| grade | String | 年级 |
| className | String | 班级 |
| avatar | String | 头像 URL |
| status | String | 状态：active / disabled / locked |
| lastLoginAt | DateTime | 最后登录时间 |
| createdAt | DateTime | 创建时间 |

### 5.2 Club（社团）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Integer | 社团 ID |
| name | String | 名称 |
| code | String | 编码 |
| category | String | 分类 |
| description | String | 描述 |
| logoUrl | String | Logo |
| presidentUserId | Integer | 社长用户 ID |
| status | String | 状态：active / inactive |
| recruitmentStatus | String | 招新状态 |

### 5.3 状态枚举

| 枚举 | 可选值 |
| --- | --- |
| UserStatus | `active` / `disabled` / `locked` |
| ClubStatus | `active` / `inactive` |
| RecruitmentStatus | `draft` / `published` / `open` / `closed` / `archived` |
| ApplicationStatus | `draft` / `submitted` / `pre_screen_passed` / `pre_screen_rejected` / `interview_scheduled` / `interviewed` / `final_approved` / `waitlisted` / `rejected` / `cancelled` |
| InterviewStatus | `pending` / `confirmed` / `completed` / `missed` / `cancelled` |
| MembershipStatus | `probation` / `active` / `suspended` / `left` / `graduated` |
| VoteResultVisibility | `public` / `after_vote` / `private` |

---

## 6. 完整权限码列表

以下为系统定义的全部权限码（`SystemPermission` 枚举），用于 `@SaCheckPermission` 校验：

| 权限码 | 说明 |
| --- | --- |
| `auth:logout` | 退出登录 |
| `auth:me` | 查看当前用户信息 |
| `role:list` / `role:create` / `role:detail` / `role:update` / `role:delete` / `role:permission:assign` | 角色管理 |
| `permission:list` / `permission:create` | 权限管理 |
| `club:list` / `club:create` / `club:detail` / `club:update` / `club:status:update` / `club:recruitment-status:update` | 社团管理 |
| `department:list` / `department:create` / `department:detail` / `department:update` / `department:delete` | 部门管理 |
| `position:list` / `position:create` / `position:detail` / `position:update` / `position:delete` | 岗位管理 |
| `user:list` / `user:create` / `user:info` / `user:update` / `user:delete` / `user:status:update` / `user:password:reset` / `user:import` / `user:role:assign` / `user:membership:list` | 用户管理 |
| `recruitment-campaign:list` / `:create` / `:detail` / `:update` / `:publish` / `:close` | 招新计划 |
| `activity:list` / `activity:create` / `activity:detail` / `activity:update` / `activity:delete` / `activity-registration:list` | 活动管理 |
| `check-in:list` / `:detail` / `:create` / `:update` / `:delete` / `:records` / `:group-member-delete` | 签到管理 |
| `leave-application:list` / `:detail` / `:review` / `:delete` | 请假管理 |
| `school-calendar:manage` | 校历管理 |
| `regulation:list` / `:create` / `:update` / `:delete` | 规章制度 |
| `award:list` / `:create` / `:update` / `:delete` | 奖项管理 |
| `application:list` / `:create` / `:export` / `:detail` / `:update` / `:submit` / `:withdraw` / `:approve` / `:batch-approve` / `:final-decision` | 入会申请 |
| `approval-record:list` | 审批记录 |
| `interview:list` / `:create` / `:detail` / `:update` / `:confirm` / `:feedback` / `:complete` | 面试管理 |
| `site-form:list` / `:create` / `:detail` / `:update` / `:export` | 站点表单 |
| `lottery:list` / `:create` / `:detail` / `:update` / `:draw` | 抽奖管理 |
| `vote:list` / `:create` / `:detail` / `:update` / `:manage-records` | 投票管理 |
| `point:account:list` / `point:transaction:list` / `point:adjust` / `point:item:list` / `point:item:manage` / `point:redemption:list` / `point:redemption:manage` | 积分管理 |
| `membership:list` / `:create` / `:detail` / `:update` / `:position:assign` / `:status:change` / `:batch-change-status` / `:batch-create` / `:force-exit` | 成员管理 |
| `exit-application:list` / `:create` / `:detail` / `:approve` / `:reject` | 退社管理 |
| `notification:list` / `:create` / `:read` / `:delete` | 通知管理 |
| `document:list` / `:create` / `:update` / `:export` | 公文管理 |
| `image:upload` / `image:list` / `image:delete` | 图床管理 |
| `showcase-app:list` / `:manage` / `:delete` | 应用展示 |
| `data-open:list` / `:review` | 数据开放 |
| `oauth-client:list` / `:manage` | OAuth 客户端 |
| `blog:list` / `blog:review` / `blog:delete` / `blog-comment:list` / `blog-comment:manage` / `blog-interaction:list` | 博客管理 |
| `bot-management:list` / `:detail` / `:members` / `:sync` / `:config` / `:mute` / `:messages` / `:announcements` / `:join-requests` / `:sensitive-words` / `:auto-review` / `:statistics` | 机器人管理 |
| `log:operation:list` / `log:login:list` | 日志查询 |

---

## 7. AI 项目接入示例

### 7.1 OAuth 接入获取用户身份

```python
import requests

ISSUER = "https://oauth.jmi-openatom.cn/api/v1"
CLIENT_ID = "my-ai-app"
CLIENT_SECRET = "your-secret"
REDIRECT_URI = "https://your-ai-app.com/callback"

# 1. 引导用户授权
authorize_url = (
    f"{ISSUER}/oauth/authorize"
    f"?response_type=code&client_id={CLIENT_ID}"
    f"&redirect_uri={REDIRECT_URI}&scope=openid profile email roles permissions"
    f"&state=random_state"
)

# 2. 回调换取令牌
def exchange_token(code):
    resp = requests.post(
        f"{ISSUER}/oauth/token",
        data={
            "grant_type": "authorization_code",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "code": code,
            "redirect_uri": REDIRECT_URI,
        },
    )
    return resp.json()

# 3. 调用业务接口
def call_api(access_token, path, params=None):
    return requests.get(
        f"{ISSUER}{path}",
        headers={"Authorization": f"Bearer {access_token}"},
        params=params,
    ).json()
```

### 7.2 Sa-Token 账号登录调用

```python
def login(username, password):
    resp = requests.post(
        f"{ISSUER}/auth/login",
        json={"username": username, "password": password},
    )
    data = resp.json()["data"]
    token = data["accessToken"]
    return token

def call_with_satoken(token, path, params=None):
    return requests.get(
        f"{ISSUER}{path}",
        headers={"jmiopenatom": token},
        params=params,
    ).json()
```

### 7.3 调用 AI 活动自动化（SSE 流）

```python
def generate_plan_stream(session_id, token):
    resp = requests.post(
        f"{ISSUER}/ai/activity/sessions/{session_id}/generate-plan/stream",
        headers={"jmiopenatom": token},
        stream=True,
    )
    for line in resp.iter_lines():
        if line:
            print(line.decode())
```

---

## 8. 注意事项

1. **令牌安全**：`client_secret` 只能存放在可信后端，禁止写入前端或公开仓库；公开客户端必须使用 PKCE。
2. **用户标识**：第三方系统应以 OAuth 返回的 `sub` 作为用户唯一标识，不要使用用户名/手机号/邮箱作为关联主键。
3. **限流**：`POST /applications` 按 IP 限制 5 次/秒；分页接口 `pageSize` 有最大值限制。
4. **逻辑删除**：删除接口优先逻辑删除，不物理删除。
5. **RBAC + 数据权限**：接口除权限码校验外，还有基于社团/部门的数据范围隔离。
6. **审计日志**：所有敏感写操作自动记录操作日志。
7. **文件上传**：头像最大 20MB（`multipart.max-file-size`），图片通过 `/files/images/{fileName}` 公开读取。
8. **SSE 流式接口**：AI 活动自动化模块的 `/stream` 端点返回 `text/event-stream`，需按 SSE 协议逐行解析。
9. **OAuth 令牌有效期**：Access Token 1 小时、Refresh Token 7 天且轮换，旧 Refresh Token 用后即失效。
10. **生产 JWKS 安全**：当前 JWKS 使用 HS256 对称算法，生产环境建议改用 RS256/ES256 并限制访问。
