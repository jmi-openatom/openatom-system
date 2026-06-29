# API 权限清单

## 概述

系统采用 RBAC 权限模型，所有 API 权限点在 `SystemPermission` 枚举中定义。共包含 100+ 个权限点，覆盖全部业务接口。

## 认证与用户

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `auth:logout` | 退出登录 | `/auth/logout` | POST |
| `auth:me` | 查看当前用户信息 | `/auth/me` | GET |
| `user:list` | 查询用户列表 | `/users` | GET |
| `user:create` | 后台创建用户 | `/users` | POST |
| `user:info` | 获取用户详情 | `/users/{userId}` | GET |
| `user:update` | 更新用户信息 | `/users/{userId}` | PATCH |
| `user:delete` | 删除用户 | `/users/{userId}` | DELETE |
| `user:status:update` | 更新用户状态 | `/users/{userId}/status` | PATCH |
| `user:password:reset` | 重置用户密码 | `/users/{userId}/reset-password` | POST |
| `user:import` | 导入用户与下载模板 | `/users/import/**` | POST |
| `user:membership:list` | 获取用户社团关系 | `/users/{userId}/memberships` | GET |
| `user:role:assign` | 分配用户角色 | `/users/{userId}/roles` | POST |

## 角色与权限

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `role:list` | 查询角色列表 | `/roles` | GET |
| `role:create` | 创建角色 | `/roles` | POST |
| `role:detail` | 获取角色详情 | `/roles/{roleId}` | GET |
| `role:update` | 更新角色 | `/roles/{roleId}` | PATCH |
| `role:delete` | 删除角色 | `/roles/{roleId}` | DELETE |
| `role:permission:assign` | 分配角色权限 | `/roles/{roleId}/permissions` | POST |
| `permission:list` | 查询权限点列表 | `/permissions` | GET |
| `permission:create` | 创建权限点 | `/permissions` | POST |

## 社团管理

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `club:list` | 查询社团列表 | `/clubs` | GET |
| `club:create` | 创建社团 | `/clubs` | POST |
| `club:detail` | 获取社团详情 | `/clubs/{clubId}` | GET |
| `club:update` | 更新社团信息 | `/clubs/{clubId}` | PATCH |
| `club:status:update` | 更新社团状态 | `/clubs/{clubId}/status` | PATCH |
| `club:recruitment-status:update` | 更新社团招新状态 | `/clubs/{clubId}/recruitment-status` | PATCH |
| `department:list` | 查询部门列表 | `/clubs/{clubId}/departments` | GET |
| `department:create` | 创建部门 | `/clubs/{clubId}/departments` | POST |
| `department:detail` | 获取部门详情 | `/departments/{departmentId}` | GET |
| `department:update` | 更新部门 | `/departments/{departmentId}` | PATCH |
| `department:delete` | 删除部门 | `/departments/{departmentId}` | DELETE |
| `position:list` | 查询岗位列表 | `/clubs/{clubId}/positions` | GET |
| `position:create` | 创建岗位 | `/clubs/{clubId}/positions` | POST |
| `position:detail` | 获取岗位详情 | `/positions/{positionId}` | GET |
| `position:update` | 更新岗位 | `/positions/{positionId}` | PATCH |
| `position:delete` | 删除岗位 | `/positions/{positionId}` | DELETE |

## 成员管理

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `membership:list` | 查询成员列表 | `/memberships` | GET |
| `membership:create` | 新增成员 | `/memberships` | POST |
| `membership:detail` | 获取成员详情 | `/memberships/{membershipId}` | GET |
| `membership:update` | 更新成员 | `/memberships/{membershipId}` | PATCH |
| `membership:position:assign` | 分配成员岗位 | `/memberships/{membershipId}/assign-position` | POST |
| `membership:status:change` | 修改成员状态 | `/memberships/{membershipId}/change-status` | POST |
| `membership:batch-change-status` | 批量修改成员状态 | `/memberships/batch-change-status` | POST |
| `membership:batch-create` | 批量创建成员 | `/memberships/batch-create` | POST |
| `membership:force-exit` | 强制退社 | `/memberships/{membershipId}/force-exit` | POST |

## 招新系统

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `recruitment-campaign:list` | 查询招新计划 | `/clubs/{clubId}/recruitment-campaigns` | GET |
| `recruitment-campaign:create` | 创建招新计划 | `/clubs/{clubId}/recruitment-campaigns` | POST |
| `recruitment-campaign:detail` | 获取招新计划详情 | `/recruitment-campaigns/{campaignId}` | GET |
| `recruitment-campaign:update` | 更新招新计划 | `/recruitment-campaigns/{campaignId}` | PATCH |
| `recruitment-campaign:publish` | 发布招新计划 | `/recruitment-campaigns/{campaignId}/publish` | POST |
| `recruitment-campaign:close` | 关闭招新计划 | `/recruitment-campaigns/{campaignId}/close` | POST |
| `application:list` | 查询入会申请 | `/applications` | GET |
| `application:create` | 提交入会申请 | `/applications` | POST |
| `application:export` | 导出入会申请 | `/applications/export` | GET |
| `application:detail` | 获取入会申请详情 | `/applications/{applicationId}` | GET |
| `application:update` | 更新入会申请 | `/applications/{applicationId}` | PATCH |
| `application:submit` | 提交草稿申请 | `/applications/{applicationId}/submit` | POST |
| `application:withdraw` | 撤回入会申请 | `/applications/{applicationId}/withdraw` | POST |
| `application:approve` | 审批入会申请 | `/applications/{applicationId}/approvals` | POST |
| `application:batch-approve` | 批量审批入会申请 | `/applications/batch-approvals` | POST |
| `application:final-decision` | 终审决策 | `/applications/{applicationId}/final-decisions` | POST |
| `approval-record:list` | 查询审批记录 | `/applications/{applicationId}/approval-records` | GET |
| `exit-application:list` | 查询退社申请 | `/exit-applications` | GET |
| `exit-application:create` | 提交退社申请 | `/exit-applications` | POST |
| `exit-application:detail` | 获取退社申请详情 | `/exit-applications/{exitApplicationId}` | GET |
| `exit-application:approve` | 通过退社申请 | `/exit-applications/{exitApplicationId}/approve` | POST |
| `exit-application:reject` | 驳回退社申请 | `/exit-applications/{exitApplicationId}/reject` | POST |

## 面试管理

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `interview:list` | 查询面试列表 | `/interviews` | GET |
| `interview:create` | 创建面试安排 | `/interviews` | POST |
| `interview:detail` | 获取面试详情 | `/interviews/{interviewId}` | GET |
| `interview:update` | 更新面试安排 | `/interviews/{interviewId}` | PATCH |
| `interview:confirm` | 确认面试 | `/interviews/{interviewId}/confirm` | POST |
| `interview:feedback` | 提交面试反馈 | `/interviews/{interviewId}/feedback` | POST |
| `interview:complete` | 完成面试 | `/interviews/{interviewId}/complete` | POST |

## 活动与签到

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `activity:list` | 查询活动列表 | `/activities` | GET |
| `activity:create` | 创建活动 | `/activities` | POST |
| `activity:detail` | 获取活动详情 | `/activities/{activityId}` | GET |
| `activity:update` | 更新活动 | `/activities/{activityId}` | PATCH |
| `activity:delete` | 删除活动 | `/activities/{activityId}` | DELETE |
| `activity-registration:list` | 查询活动报名 | `/activities/{activityId}/registrations` | GET |
| `check-in:list` | 查询内部签到 | `/check-ins` | GET |
| `check-in:detail` | 获取内部签到详情 | `/check-ins/{sessionId}` | GET |
| `check-in:create` | 发布内部签到 | `/check-ins` | POST |
| `check-in:update` | 更新内部签到 | `/check-ins/{sessionId}` | PATCH |
| `check-in:delete` | 删除内部签到 | `/check-ins/{sessionId}` | DELETE |
| `check-in:records` | 查询内部签到记录 | `/check-ins/{sessionId}/records` | GET |
| `check-in:group-member-delete` | 移除签到分组成员 | `/check-in-groups/{groupId}/members/{userId}` | DELETE |
| `award:list` | 查询获奖经历 | `/awards` | GET |
| `award:create` | 创建获奖经历 | `/awards` | POST |
| `award:update` | 更新获奖经历 | `/awards/{awardId}` | PATCH |
| `award:delete` | 删除获奖经历 | `/awards/{awardId}` | DELETE |
| `school-calendar:manage` | 管理校历 | `/school-calendar` | POST |

## 请假管理

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `leave-application:list` | 查询请假申请 | `/leave-applications` | GET |
| `leave-application:detail` | 获取请假详情 | `/leave-applications/{leaveApplicationId}` | GET |
| `leave-application:review` | 审批请假申请 | `/leave-applications/{leaveApplicationId}/review` | POST |
| `leave-application:delete` | 删除请假申请 | `/leave-applications/{leaveApplicationId}` | DELETE |

## 规章制度

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `regulation:list` | 查询社团规章制度 | `/regulations` | GET |
| `regulation:create` | 创建社团规章制度 | `/clubs/{clubId}/regulations` | POST |
| `regulation:update` | 更新社团规章制度 | `/regulations/{regulationId}` | PATCH |
| `regulation:delete` | 删除社团规章制度 | `/regulations/{regulationId}` | DELETE |

## 表单与文书

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `site-form:list` | 查询信息收集表单 | `/site-forms` | GET |
| `site-form:create` | 新增信息收集表单 | `/site-forms` | POST |
| `site-form:detail` | 获取表单详情 | `/site-forms/{formId}` | GET |
| `site-form:update` | 更新信息收集表单 | `/site-forms/{formId}` | PATCH |
| `site-form:export` | 导出表单提交数据 | `/site-forms/{formId}/submissions/export` | GET |
| `document:list` | 查询文书列表 | `/office-documents` | GET |
| `document:create` | 创建文书 | `/office-documents` | POST |
| `document:update` | 更新文书 | `/office-documents/{documentId}` | PATCH |
| `document:export` | 导出文书 | `/office-documents/{documentId}/export` | GET |

## 互动功能

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `blog:my:list` | 查询我的博客 | `/blog/my/articles` | GET |
| `blog:create` | 发布博客文章 | `/blog/articles` | POST |
| `blog:update-own` | 更新自己的博客文章 | `/blog/articles/{articleId}` | PATCH |
| `blog:delete-own` | 删除自己的博客文章 | `/blog/articles/{articleId}` | DELETE |
| `blog:list` | 管理博客文章 | `/blog/admin/articles` | GET |
| `blog:review` | 审核博客文章 | `/blog/admin/articles/{articleId}/review` | POST |
| `blog:delete` | 删除博客文章 | `/blog/admin/articles/{articleId}` | DELETE |
| `blog-comment:list` | 查询博客评论 | `/blog/admin/articles/{articleId}/comments` | GET |
| `blog-comment:manage` | 管理博客评论 | `/blog/admin/comments/{commentId}/status` | PATCH |
| `blog-interaction:list` | 查询博客互动记录 | `/blog/admin/interactions` | GET |
| `lottery:list` | 查询抽奖活动 | `/lotteries` | GET |
| `lottery:create` | 创建抽奖活动 | `/clubs/{clubId}/lotteries` | POST |
| `lottery:detail` | 获取抽奖详情 | `/lotteries/{lotteryId}` | GET |
| `lottery:update` | 更新抽奖活动 | `/lotteries/{lotteryId}` | PATCH |
| `lottery:draw` | 执行抽奖 | `/lotteries/{lotteryId}/draw` | POST |
| `vote:list` | 查询投票活动 | `/votes` | GET |
| `vote:create` | 创建投票活动 | `/clubs/{clubId}/votes` | POST |
| `vote:detail` | 获取投票详情 | `/votes/{voteId}` | GET |
| `vote:update` | 更新投票活动 | `/votes/{voteId}` | PATCH |
| `vote:manage-records` | 管理投票记录 | `/votes/{voteId}/reset` | POST |

## 积分系统

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `point:account:list` | 查询积分账户 | `/points/admin/accounts` | GET |
| `point:transaction:list` | 查询积分流水 | `/points/admin/transactions` | GET |
| `point:adjust` | 手动调整积分 | `/points/admin/adjustments` | POST |
| `point:item:list` | 查询积分兑换项 | `/points/admin/items` | GET |
| `point:item:manage` | 管理积分兑换项 | `/points/admin/items/**` | POST |
| `point:redemption:list` | 查询积分兑换记录 | `/points/admin/redemptions` | GET |
| `point:redemption:manage` | 处理积分兑换记录 | `/points/admin/redemptions/{redemptionId}/status` | PATCH |

## 通知与图床

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `notification:list` | 查询通知 | `/notifications` | GET |
| `notification:create` | 发送通知 | `/notifications` | POST |
| `notification:read` | 标记通知已读 | `/notifications/{notificationId}/read` | POST |
| `notification:delete` | 删除通知 | `/notifications/admin/{notificationId}` | DELETE |
| `image:upload` | 上传图床图片 | `/image-hosting/images` | POST |
| `image:list` | 管理图床图片 | `/image-hosting/admin/images` | GET |
| `image:delete` | 删除图床图片 | `/image-hosting/admin/images/{imageId}` | DELETE |

## OAuth 与数据开放

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `oauth-client:list` | 查询认证应用 | `/oauth/admin/clients` | GET |
| `oauth-client:manage` | 管理认证应用 | `/oauth/admin/clients/**` | POST |
| `data-open:list` | 查询数据开放申请 | `/data-open/admin/applications` | GET |
| `data-open:review` | 审核数据开放申请 | `/data-open/admin/applications/{applicationId}/review` | POST |

## 展示与日志

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `showcase-app:list` | 查询应用展示 | `/showcase-apps` | GET |
| `showcase-app:manage` | 管理应用展示 | `/showcase-apps/**` | POST |
| `showcase-app:delete` | 删除应用展示 | `/showcase-apps/{appId}` | DELETE |
| `log:operation:list` | 查询操作日志 | `/operation-logs` | GET |
| `log:login:list` | 查询登录日志 | `/login-logs` | GET |

## QQ 机器人

| 权限编码 | 权限名称 | 路径 | 方法 |
|----------|----------|------|------|
| `bot-management:list` | 查看 QQ 机器人群管后台 | `/bot-management/**` | GET |
| `bot-management:detail` | 查看 QQ 群详情 | `/bot-management/groups/{groupId}` | GET |
| `bot-management:members` | 查看 QQ 群成员 | `/bot-management/groups/{groupId}/members` | GET |
| `bot-management:sync` | 同步 QQ 群与成员 | `/bot-management/groups/**/sync` | POST |
| `bot-management:config` | 配置 QQ 群机器人 | `/bot-management/groups/**/config` | PATCH |
| `bot-management:mute` | 执行 QQ 群禁言 | `/bot-management/groups/**/mute*` | POST |
| `bot-management:messages` | 发送 QQ 群消息 | `/bot-management/groups/{groupId}/messages/**` | POST |
| `bot-management:announcements` | 管理 QQ 群公告 | `/bot-management/groups/{groupId}/announcements/**` | POST |
| `bot-management:join-requests` | 处理 QQ 入群申请 | `/bot-management/groups/{groupId}/join-requests/**` | POST |
| `bot-management:sensitive-words` | 管理 QQ 群敏感词 | `/bot-management/sensitive-words/**` | POST |
| `bot-management:auto-review` | 管理 QQ 自动审核规则 | `/bot-management/auto-review-rules/**` | POST |
| `bot-management:statistics` | 查看 QQ 群统计 | `/bot-management/statistics/**` | GET |
