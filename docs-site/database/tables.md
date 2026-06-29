# 数据库表结构

## 概述

系统使用 MySQL 8.0 数据库，表名使用 `tb_` 前缀（业务表）和 `sys_` 前缀（系统表）。所有表使用 `utf8mb4` 字符集和 `utf8mb4_general_ci` 排序规则。

数据库迁移由 Flyway 管理，初始化脚本为 `V1__init_schema.sql`，后续通过 `V2` ~ `V29` 持续演进。

## 核心表清单

### 用户与权限

#### tb_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `user_name` | VARCHAR(50) | 用户名（唯一） |
| `password` | VARCHAR(255) | 密码（BCrypt 哈希） |
| `real_name` | VARCHAR(50) | 真实姓名 |
| `gender` | VARCHAR(10) | 性别 |
| `phone` | VARCHAR(20) | 手机号 |
| `email` | VARCHAR(100) | 邮箱 |
| `student_id` | VARCHAR(30) | 学号/工号（唯一） |
| `college` | VARCHAR(100) | 学院 |
| `major` | VARCHAR(100) | 专业 |
| `grade` | VARCHAR(20) | 年级 |
| `class_name` | VARCHAR(100) | 班级 |
| `avatar` | VARCHAR(255) | 头像 URL |
| `miniapp_openid` | VARCHAR(80) | 微信小程序 openid（唯一） |
| `wechat_unionid` | VARCHAR(80) | 微信 unionid |
| `qq_openid` | VARCHAR(80) | QQ openid（V2 新增） |
| `user_status` | TINYINT | 用户状态：1=active, 0=disabled, 2=locked |
| `create_time` | TIMESTAMP | 创建时间 |
| `last_login_at` | TIMESTAMP | 最后登录时间 |

#### sys_role（角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `role_name` | VARCHAR(50) | 角色名称 |
| `role_code` | VARCHAR(50) | 角色编码 |
| `description` | VARCHAR(255) | 描述 |
| `is_system` | TINYINT | 是否系统内置 |
| `status` | TINYINT | 状态 |
| `create_time` | TIMESTAMP | 创建时间 |

#### sys_permission（权限表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `permission_name` | VARCHAR(50) | 权限名称 |
| `permission_code` | VARCHAR(100) | 权限编码 |
| `permission_type` | VARCHAR(20) | 权限类型 |
| `api_path` | VARCHAR(255) | API 路径 |
| `http_method` | VARCHAR(10) | HTTP 方法 |
| `description` | VARCHAR(255) | 描述 |

#### sys_user_role（用户-角色关联表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `user_id` | INT | 用户 ID |
| `role_id` | INT | 角色 ID |

#### sys_role_permission（角色-权限关联表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `role_id` | INT | 角色 ID |
| `permission_id` | INT | 权限 ID |

### 社团与组织

#### tb_club（社团表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_name` | VARCHAR(100) | 社团名称 |
| `description` | TEXT | 社团描述 |
| `logo` | VARCHAR(255) | Logo URL |
| `status` | TINYINT | 状态 |
| `recruitment_status` | TINYINT | 招新状态 |

#### tb_club_department（部门表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `department_name` | VARCHAR(100) | 部门名称 |
| `description` | VARCHAR(255) | 描述 |

#### tb_club_position（岗位表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `position_name` | VARCHAR(100) | 岗位名称 |
| `level` | INT | 层级 |

#### tb_club_membership（成员关系表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `user_id` | INT | 用户 ID |
| `department_id` | INT | 部门 ID |
| `position_id` | INT | 岗位 ID |
| `status` | TINYINT | 成员状态 |
| `joined_at` | TIMESTAMP | 加入时间 |
| `left_at` | TIMESTAMP | 离开时间（往届成员查询需不为空） |

### 招新系统

#### tb_membership_application（入会申请表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `user_id` | INT | 用户 ID |
| `campaign_id` | INT | 招新计划 ID |
| `form_data` | JSON | 自定义表单数据 |
| `status` | VARCHAR(20) | 申请状态 |
| `submitted_at` | TIMESTAMP | 提交时间 |

#### tb_recruitment_campaign（招新计划表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `name` | VARCHAR(100) | 计划名称 |
| `form_schema` | JSON | 表单结构 |
| `status` | VARCHAR(20) | 状态 |
| `start_at` | TIMESTAMP | 开始时间 |
| `end_at` | TIMESTAMP | 结束时间 |

#### tb_approval_record（审批记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `application_id` | INT | 申请 ID |
| `approver_id` | INT | 审批人 ID |
| `action` | VARCHAR(20) | 审批动作 |
| `comment` | TEXT | 审批意见 |
| `created_at` | TIMESTAMP | 审批时间 |

#### tb_interview（面试表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `application_id` | INT | 申请 ID |
| `scheduled_at` | TIMESTAMP | 面试时间 |
| `location` | VARCHAR(255) | 面试地点 |
| `status` | VARCHAR(20) | 面试状态 |

### 活动与签到

#### tb_club_activity（社团活动表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `title` | VARCHAR(200) | 活动标题 |
| `content` | TEXT | 活动内容 |
| `start_at` | TIMESTAMP | 开始时间 |
| `end_at` | TIMESTAMP | 结束时间 |
| `status` | TINYINT | 状态 |

#### tb_check_in_session（签到会话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `title` | VARCHAR(200) | 签到标题 |
| `start_at` | TIMESTAMP | 开始时间 |
| `end_at` | TIMESTAMP | 结束时间 |
| `status` | VARCHAR(20) | 状态 |

### 互动功能

#### tb_blog_article（博客文章表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `author_id` | INT | 作者 ID |
| `title` | VARCHAR(200) | 标题 |
| `content` | LONGTEXT | 内容 |
| `status` | VARCHAR(20) | 审核状态 |
| `view_count` | INT | 浏览数 |
| `like_count` | INT | 点赞数 |

#### tb_vote_campaign（投票活动表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `title` | VARCHAR(200) | 投票标题 |
| `result_visibility` | VARCHAR(20) | 结果可见性 |

#### tb_lottery_campaign（抽奖活动表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `club_id` | INT | 社团 ID |
| `title` | VARCHAR(200) | 抽奖标题 |
| `status` | VARCHAR(20) | 状态 |

### 积分系统

#### tb_point_account（积分账户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `user_id` | INT | 用户 ID |
| `balance` | BIGINT | 积分余额（V19 扩展为 BIGINT） |
| `total_earned` | BIGINT | 累计获得 |
| `total_spent` | BIGINT | 累计消费 |

#### tb_point_transaction（积分流水表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `account_id` | INT | 账户 ID |
| `amount` | BIGINT | 变动金额 |
| `type` | VARCHAR(20) | 变动类型 |
| `description` | VARCHAR(255) | 描述 |

### 办公自动化

#### tb_leave_application（请假申请表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `user_id` | INT | 用户 ID |
| `leave_type` | VARCHAR(20) | 请假类型 |
| `start_at` | TIMESTAMP | 开始时间 |
| `end_at` | TIMESTAMP | 结束时间 |
| `reason` | TEXT | 请假原因 |
| `status` | VARCHAR(20) | 审批状态 |
| `bot_notified` | TINYINT | 是否已通知机器人（V3 新增） |

#### tb_office_document（文书表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `title` | VARCHAR(200) | 文书标题 |
| `content` | TEXT | 文书内容 |
| `template_id` | INT | 模板 ID |

#### tb_notification（通知表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `title` | VARCHAR(200) | 通知标题 |
| `content` | TEXT | 通知内容 |
| `type` | VARCHAR(20) | 通知类型 |
| `sender_id` | INT | 发送人 ID |

### 系统管理

#### tb_operation_log（操作日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `user_id` | INT | 操作人 ID |
| `module` | VARCHAR(50) | 模块 |
| `action` | VARCHAR(50) | 操作 |
| `params` | TEXT | 请求参数 |
| `result` | TEXT | 响应结果 |
| `ip` | VARCHAR(50) | IP 地址 |
| `duration_ms` | INT | 耗时（毫秒） |
| `created_at` | TIMESTAMP | 操作时间 |

#### tb_login_log（登录日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `user_id` | INT | 用户 ID |
| `login_type` | VARCHAR(20) | 登录类型 |
| `ip` | VARCHAR(50) | IP 地址 |
| `status` | TINYINT | 登录状态 |
| `created_at` | TIMESTAMP | 登录时间 |

### OAuth 认证

#### tb_oauth_client（OAuth 客户端表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, AUTO_INCREMENT | 主键 ID |
| `client_id` | VARCHAR(100) | 客户端 ID（唯一） |
| `client_secret` | VARCHAR(255) | 客户端密钥 |
| `redirect_uri` | VARCHAR(500) | 回调地址 |
| `scopes` | VARCHAR(255) | 授权范围 |
| `enabled` | TINYINT | 是否启用 |

## Flyway 迁移历史

| 版本 | 说明 |
|------|------|
| V1 | 初始化全部基础表结构 |
| V2 | 用户表添加 QQ openid |
| V3 | 请假机器人通知字段 |
| V4 | 机器人群管理 |
| V5-V7 | 机器人公告、消息、群消息任务 |
| V8-V9 | 博客表结构与互动 |
| V10 | 图床资源表 |
| V11 | 抽奖表 |
| V12-V13 | 积分系统与规则 |
| V14 | 数据开放申请 |
| V15 | OIDC 认证中心 |
| V16 | 投票表 |
| V17-V18 | 应用展示 |
| V19 | 积分金额扩展为 BIGINT |
| V20 | OAuth 客户端修复 |
| V21-V23 | 晚自习签到 |
| V24 | LMS OAuth 客户端 |
| V25 | AI 活动自动化 |
| V26 | 运营负责人角色 |
| V27 | 社团规章制度 |
| V28 | 投票结果可见性 |
| V29 | 校友分组功能 |
