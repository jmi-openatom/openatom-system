# 社团管理系统 RESTful API 文档

## 1. 设计约定

### 基础路径

- Base URL: `/api/v1`

### 通用约定

- 协议：`HTTPS`
- 数据格式：`application/json`
- 认证方式：`Bearer Token`
- 时间格式：`ISO 8601`
- 字段命名：`camelCase`
- 删除策略：逻辑删除优先

### 通用响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "a1b2c3d4"
}
```

### 分页响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "page": 1,
    "pageSize": 20,
    "total": 120
  },
  "traceId": "a1b2c3d4"
}
```

### 通用错误码建议

| code  | 含义            |
| ----- | ------------- |
| 0     | 成功            |
| 40000 | 参数错误          |
| 40100 | 未登录或 token 无效 |
| 40300 | 无权限           |
| 40400 | 资源不存在         |
| 40900 | 资源冲突          |
| 42200 | 业务校验失败        |
| 50000 | 服务器内部错误       |

## 2. 认证模块

### POST `/auth/register`

用户注册

请求体：

```json
{
  "username": "zhangsan",
  "password": "123456Abc",
  "realName": "张三",
  "phone": "13800000000",
  "email": "zhangsan@example.com"
}
```

### POST `/auth/login`

账号登录

请求体：

```json
{
  "username": "zhangsan",
  "password": "123456Abc"
}
```

响应体：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "jwt-token",
    "refreshToken": "refresh-token",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "zhangsan",
      "realName": "张三"
    }
  }
}
```

### POST `/auth/refresh-token`

刷新 token

### POST `/auth/logout`

退出登录

### GET `/auth/me`

获取当前登录用户信息与权限

## 3. 用户模块

### GET `/users`

分页查询用户列表

查询参数：

- `keyword`
- `status`
- `clubId`
- `page`
- `pageSize`

### POST `/users`

后台创建用户

### GET `/users/{userId}`

查询用户详情

### PATCH `/users/{userId}`

更新用户基础信息

### PATCH `/users/{userId}/status`

启用/禁用用户

请求体：

```json
{
  "status": "disabled"
}
```

### POST `/users/{userId}/reset-password`

重置用户密码

### GET `/users/{userId}/memberships`

查看用户的社团成员关系

## 4. 社团模块

### GET `/clubs`

查询社团列表

参数：

- `keyword`
- `category`
- `status`
- `recruitmentStatus`

### POST `/clubs`

创建社团

请求体：

```json
{
  "name": "计算机协会",
  "code": "CSA",
  "category": "academic",
  "description": "面向编程与技术交流",
  "presidentUserId": 12
}
```

### GET `/clubs/{clubId}`

查询社团详情

### PATCH `/clubs/{clubId}`

更新社团信息

### PATCH `/clubs/{clubId}/status`

更新社团状态

### PATCH `/clubs/{clubId}/recruitment-status`

更新招新状态

## 5. 部门模块

### GET `/clubs/{clubId}/departments`

查询某社团的部门列表

### POST `/clubs/{clubId}/departments`

创建部门

请求体：

```json
{
  "name": "技术部",
  "description": "负责技术培训与项目实践",
  "managerUserId": 18
}
```

### GET `/departments/{departmentId}`

部门详情

### PATCH `/departments/{departmentId}`

更新部门

### DELETE `/departments/{departmentId}`

删除部门

## 6. 岗位模块

### GET `/clubs/{clubId}/positions`

查询岗位列表

### POST `/clubs/{clubId}/positions`

创建岗位

请求体：

```json
{
  "name": "部长",
  "departmentId": 3,
  "maxCount": 1,
  "roleIds": [5, 6]
}
```

### GET `/positions/{positionId}`

岗位详情

### PATCH `/positions/{positionId}`

更新岗位

### DELETE `/positions/{positionId}`

删除岗位

## 7. 招新计划模块

### GET `/clubs/{clubId}/recruitment-campaigns`

查询招新批次

### POST `/clubs/{clubId}/recruitment-campaigns`

创建招新计划

请求体：

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
  "status": "draft"
}
```

### GET `/recruitment-campaigns/{campaignId}`

查询招新计划详情

### PATCH `/recruitment-campaigns/{campaignId}`

更新招新计划

### POST `/recruitment-campaigns/{campaignId}/publish`

发布招新计划

### POST `/recruitment-campaigns/{campaignId}/close`

关闭招新计划

## 8. 入会申请模块

### GET `/applications`

分页查询申请列表

参数：

- `campaignId`
- `clubId`
- `status`
- `departmentId`
- `keyword`
- `page`
- `pageSize`

### POST `/applications`

提交入会申请

说明：

- 该接口支持未登录用户匿名提交。
- 未登录提交时，`profile` 中必须包含 `applicantName` 与 `contact`。
- 后端按客户端 IP 限制 1 秒内最多 5 次访问，并将提交快照写入 Redis。

请求体：

```json
{
  "campaignId": 10,
  "clubId": 2,
  "firstChoiceDepartmentId": 3,
  "secondChoiceDepartmentId": 4,
  "profile": {
    "selfIntroduction": "热爱技术和团队协作",
    "skills": ["Java", "Spring Boot"],
    "availableTime": "每周二、四晚上"
  }
}
```

### GET `/applications/{applicationId}`

申请详情

### PATCH `/applications/{applicationId}`

修改申请

限制：仅 `draft` 或 `submitted` 且未进入审核时可修改

### POST `/applications/{applicationId}/submit`

提交草稿申请

### POST `/applications/{applicationId}/withdraw`

撤回申请

## 9. 审核模块

### GET `/applications/{applicationId}/approval-records`

查询审批记录

### POST `/applications/{applicationId}/approvals`

执行审批动作

请求体：

```json
{
  "action": "approve",
  "node": "pre_screen",
  "comment": "符合基础条件",
  "nextInterviewerIds": [21, 22]
}
```

`action` 可选值：

- `approve`
- `reject`
- `transfer`
- `request_more_info`

### POST `/applications/batch-approvals`

批量审批

## 10. 面试模块

### GET `/interviews`

分页查询面试列表

参数：

- `campaignId`
- `applicationId`
- `interviewerId`
- `status`

### POST `/interviews`

创建面试安排

请求体：

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

### GET `/interviews/{interviewId}`

面试详情

### PATCH `/interviews/{interviewId}`

修改面试安排

### POST `/interviews/{interviewId}/confirm`

候选人确认面试

### POST `/interviews/{interviewId}/feedback`

提交面试反馈

请求体：

```json
{
  "scores": {
    "communication": 8,
    "cooperation": 9,
    "stability": 7,
    "skillMatch": 8,
    "motivation": 9
  },
  "suggestion": "pass",
  "comment": "表达清晰，时间投入较稳定"
}
```

### POST `/interviews/{interviewId}/complete`

标记面试完成

## 11. 录用与成员模块

### POST `/applications/{applicationId}/final-decisions`

终审决策

请求体：

```json
{
  "decision": "approved",
  "departmentId": 3,
  "positionId": 8,
  "comment": "录用为技术部干事"
}
```

`decision` 可选值：

- `approved`
- `waitlisted`
- `rejected`

### GET `/memberships`

分页查询成员列表

参数：

- `clubId`
- `departmentId`
- `positionId`
- `status`
- `keyword`

### POST `/memberships`

手动新增成员

### GET `/memberships/{membershipId}`

成员详情

### PATCH `/memberships/{membershipId}`

更新成员信息

### POST `/memberships/{membershipId}/assign-position`

分配岗位

请求体：

```json
{
  "positionId": 8,
  "effectiveAt": "2026-06-01T00:00:00+08:00"
}
```

### POST `/memberships/{membershipId}/change-status`

修改成员状态

## 12. 退社模块

### GET `/exit-applications`

查询退社申请列表

### POST `/exit-applications`

发起退社申请

请求体：

```json
{
  "membershipId": 3001,
  "reason": "study_conflict",
  "description": "课程与社团时间冲突"
}
```

### GET `/exit-applications/{exitApplicationId}`

退社申请详情

### POST `/exit-applications/{exitApplicationId}/approve`

审核通过退社

### POST `/exit-applications/{exitApplicationId}/reject`

驳回退社

### POST `/memberships/{membershipId}/force-exit`

管理员强制退社

## 13. RBAC 模块

### GET `/roles`

查询角色列表

### POST `/roles`

创建角色

请求体：

```json
{
  "name": "社团管理员",
  "code": "club_admin",
  "dataScope": "club_only",
  "description": "负责社团管理和招新审批"
}
```

### GET `/roles/{roleId}`

角色详情

### PATCH `/roles/{roleId}`

更新角色

### DELETE `/roles/{roleId}`

删除角色

### GET `/permissions`

查询权限点列表

### POST `/permissions`

创建权限点

请求体：

```json
{
  "name": "审批入会申请",
  "code": "application:approve",
  "type": "api",
  "path": "/applications/{applicationId}/approvals",
  "method": "POST"
}
```

### POST `/roles/{roleId}/permissions`

给角色分配权限

请求体：

```json
{
  "permissionIds": [101, 102, 103]
}
```

### POST `/users/{userId}/roles`

给用户分配角色

请求体：

```json
{
  "roleIds": [2, 5]
}
```

## 14. 通知模块

### GET `/notifications`

查询当前用户通知列表

### POST `/notifications`

后台发送通知

### POST `/notifications/{notificationId}/read`

标记已读

## 15. 日志审计模块

### GET `/operation-logs`

查询操作日志

参数：

- `operatorId`
- `module`
- `action`
- `startAt`
- `endAt`

### GET `/login-logs`

查询登录日志

## 16. 推荐状态枚举

### 用户状态

- `active`
- `disabled`
- `locked`

### 社团状态

- `active`
- `inactive`

### 招新状态

- `draft`
- `published`
- `open`
- `closed`
- `archived`

### 申请状态

- `draft`
- `submitted`
- `pre_screen_passed`
- `pre_screen_rejected`
- `interview_scheduled`
- `interviewed`
- `final_approved`
- `waitlisted`
- `rejected`
- `cancelled`

### 面试状态

- `pending`
- `confirmed`
- `completed`
- `missed`
- `cancelled`

### 成员状态

- `probation`
- `active`
- `suspended`
- `left`
- `graduated`

## 17. 安全与实现建议

- 所有写接口必须鉴权
- 所有敏感操作写入审计日志
- 列表接口默认分页，限制最大 `pageSize`
- RBAC 与数据权限同时校验
- 删除接口优先逻辑删除
- 关键业务动作建议采用幂等设计

## 18. 推荐实现顺序

1. 认证与用户
2. 社团、部门、岗位
3. RBAC 角色权限
4. 招新计划
5. 入会申请
6. 审核流
7. 面试管理
8. 成员管理
9. 退社流程
10. 通知与日志
