# 数据库表结构

## 概述

系统使用 MySQL 8.0 数据库，字符集 `utf8mb4`、排序规则 `utf8mb4_general_ci`。

**命名约定**：系统表使用 `sys_` 前缀（`sys_role`、`sys_permission` 等）；业务表**无前缀**（`club`、`club_activity` 等），仅历史遗留的 `tb_user` 保留 `tb_` 前缀。

数据库迁移由 **Flyway** 管理，初始化脚本为 `V1__init_schema.sql`，已演进到 **V53**。另有少量表（`doc_center_document`、`shared_file`、`school_calendar_setting`、`school_calendar_adjustment`）由启动初始化器 `SchemaCompatibilityInitializer` 以 `CREATE TABLE IF NOT EXISTS` 运行时创建，不在 Flyway 脚本中。

## 数据表清单（按业务域）

### 用户与权限

| 表 | 说明 |
|------|------|
| `tb_user` | 用户表（唯一保留 `tb_` 前缀） |
| `sys_role` | 角色表 |
| `sys_permission` | 权限点表 |
| `sys_user_role` | 用户-角色关联 |
| `sys_role_permission` | 角色-权限关联 |
| `user_external_identity` | 第三方身份绑定（Google/GitHub/Gitee，V42） |
| `system_setting` | 系统设置 |

### 社团与组织

| 表 | 说明 |
|------|------|
| `club` | 社团 |
| `club_department` | 部门 |
| `club_position` | 岗位 |
| `club_position_role` | 岗位角色 |
| `club_membership` | 成员关系 |
| `club_vice_president` | 副社长（V34） |
| `club_alumni_group` | 校友分组（V29） |
| `club_regulation` | 规章制度（V27） |
| `partner_club` | 合作社团（V38） |
| `unified_group` | 统一分组中心（V48） |
| `unified_group_member` | 统一分组成员 |
| `unified_group_binding` | 统一分组绑定（部门/外群/校友） |

### 招新系统

| 表 | 说明 |
|------|------|
| `membership_application` | 入会申请 |
| `recruitment_campaign` | 招新计划 |
| `approval_record` | 审批记录 |
| `interview` | 面试 |
| `interview_feedback` | 面试反馈 |
| `interview_interviewer` | 面试官关联 |

### 活动与签到

| 表 | 说明 |
|------|------|
| `club_activity` | 社团活动 |
| `activity_registration` | 活动报名 |
| `club_award` | 获奖记录 |
| `checkin_session` | 签到会话 |
| `checkin_group` / `checkin_group_member` | 签到分组 |
| `checkin_record` | 签到记录 |
| `checkin_target` | 签到对象 |
| `checkin_exclusion` | 签到豁免 |
| `evening_study_schedule` | 晚自习安排（V21） |

### 办公自动化

| 表 | 说明 |
|------|------|
| `leave_application` | 请假申请 |
| `office_document` | 文书 |
| `document_template` | 文书模板 |
| `generated_document` | 生成文档 |
| `notification` | 通知 |
| `notification_receiver` | 通知接收人 |
| `school_calendar_setting` / `school_calendar_adjustment` | 校历（运行时建表） |
| `doc_center_document` | 文档中心文档（运行时建表） |
| `shared_file` | 共享文件（运行时建表） |
| `mailbox_outbox_event` | 内部邮件出站队列（V52） |

### 互动功能

| 表 | 说明 |
|------|------|
| `blog_article` | 博客文章（V8） |
| `blog_comment` | 博客评论 |
| `blog_article_interaction` | 文章互动（点赞/收藏，V9） |
| `vote_campaign` | 投票活动（V16） |
| `vote_option` / `vote_record` | 投票选项 / 记录 |
| `lottery_campaign` | 抽奖活动（V11） |
| `lottery_prize` / `lottery_winner` | 奖品 / 中奖记录 |
| `member_profile` | 成员主页（V43） |
| `member_profile_module` | 主页模块 |
| `member_profile_social_link` | 主页社交链接 |
| `member_profile_comment` | 主页评论（V44） |
| `member_profile_like` | 主页点赞 |
| `next_page_stats` / `next_page_join` | 加入页统计与报名（V30/V31） |

### 积分系统

| 表 | 说明 |
|------|------|
| `point_account` | 积分账户（V12） |
| `point_transaction` | 积分流水 |
| `point_redeem_item` | 兑换商品 |
| `point_redemption` | 兑换记录 |

### 内容与资源

| 表 | 说明 |
|------|------|
| `image_hosting_asset` | 图床资源（V10） |
| `showcase_app` | 应用展示（V17） |
| `data_open_application` | 数据开放申请（V14） |
| `site_form` | 在线表单 |
| `form_submission` | 表单提交 |

### 认证与 OAuth

| 表 | 说明 |
|------|------|
| `oauth_client` | OAuth 客户端（V15） |
| `oauth_authorization_code` | 授权码 |

### 机器人（QQ 群）

| 表 | 说明 |
|------|------|
| `bot_account` | 机器人账号 |
| `bot_group` | QQ 群（V4） |
| `bot_group_member` | 群成员 |
| `bot_group_config` / `bot_group_announcement` | 群配置 / 公告 |
| `bot_group_message_task` | 群消息任务（V7） |
| `bot_join_request` | 入群申请 |
| `bot_message_active_member` | 活跃成员统计（V6） |
| `bot_message_stat` | 消息统计 |
| `bot_sensitive_word` | 敏感词 |
| `bot_auto_review_rule` | 自动审核规则 |

### AI 自动化

| 表 | 说明 |
|------|------|
| `ai_activity_session` | AI 活动会话（V25） |
| `ai_activity_plan` | AI 活动计划 |
| `ai_activity_message` | AI 活动消息 |
| `ai_call_log` | AI 调用日志 |

### 系统日志

| 表 | 说明 |
|------|------|
| `operation_log` | 操作日志 |
| `login_log` | 登录日志 |

## Flyway 迁移历史

| 版本 | 说明 |
|------|------|
| V1 | 初始化全部基础表结构 |
| V2 | 用户表添加 QQ openid |
| V3 | 请假机器人通知字段 |
| V4-V7 | 机器人群管理、公告、消息任务、活跃成员 |
| V8-V9 | 博客表结构与互动 |
| V10 | 图床资源表 |
| V11 | 抽奖表 |
| V12-V13 | 积分系统与博客审核流 |
| V14 | 数据开放申请 |
| V15 | OIDC 认证中心 |
| V16 | 投票表 |
| V17-V18 | 应用展示 |
| V19 | 积分金额扩展为 BIGINT |
| V20 | OAuth 客户端修复 |
| V21-V23 | 晚自习签到与惩罚规则 |
| V24 | LMS OAuth 客户端 |
| V25 | AI 活动自动化 |
| V26 | 运营负责人角色 |
| V27 | 社团规章制度 |
| V28 | 投票结果可见性 |
| V29 | 校友分组功能 |
| V30-V31 | 加入页统计与报名 |
| V32 | 成员删除权限 |
| V33 | 用户 onboarding 完成时间 |
| V34-V35 | 账号激活、副社长、部门副主管 |
| V36-V37 | 群二维码、用户入群时间 |
| V38-V40 | 合作社团及权限修复 |
| V41-V42 | Google 身份、外部身份绑定 |
| V43-V44 | 成员主页与互动 |
| V45-V47 | 合作社团社长、主页评论管理权限 |
| V48-V49 | 统一分组中心 |
| V50 | 应用展示 AtomGit |
| V51 | 评论区开关 |
| V52 | 内部邮件出站队列 |
| V53 | Seafile OAuth 客户端 |

## 注意

- 迁移脚本命名遵循 Flyway 约定：`V{数字}__{描述}.sql`，序号连续、不可修改已执行脚本。
- 新增字段或表优先通过 Flyway 迁移脚本，只有极少数运行时兼容场景才在 `SchemaCompatibilityInitializer` 中兜底。
- 运维可运行 `backend/scripts/check-flyway-migrations.sh` 校验迁移脚本完整性。