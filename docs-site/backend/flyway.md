# 数据库迁移（Flyway）

## 概述

系统使用 [Flyway](https://flywaydb.org) 管理数据库版本迁移，确保生产环境数据库 Schema 的版本控制和自动化执行。

## 环境配置

### 开发环境（application-dev.yaml）

```yaml
spring:
  flyway:
    enabled: false  # 开发环境禁用，需手动执行 SQL
```

::: warning 重要
开发环境 Flyway 已禁用，修改实体类添加新字段后需要手动执行对应的 SQL 迁移脚本。否则会报 `Unknown column` SQL 语法错误。
:::

### 生产环境（application-prod.yaml）

```yaml
spring:
  flyway:
    enabled: true                    # 生产环境启用
    locations: classpath:db/migration # 迁移脚本目录
    baseline-on-migrate: true        # 首次迁移时自动 baseline
    baseline-version: 0              # baseline 版本号
    validate-on-migrate: true        # 迁移前校验
```

## 迁移脚本目录

迁移脚本必须放在以下目录：

```
backend/src/main/resources/db/migration/
```

::: danger 关键规则
- 迁移文件**必须**放在 `backend/src/main/resources/db/migration/` 目录下
- 放在 `backend/db/` 等其他目录的 SQL 文件**不会被** Flyway 执行
- 这是常见的坑，请务必注意
:::

## 命名规范

Flyway 迁移文件命名格式：

```
V{版本号}__{描述}.sql
```

- **V** — 表示版本化迁移（Versioned Migration）
- **版本号** — 必须是递增的数字，如 `1`, `2`, `3`, ..., `30`
- **双下划线** — 版本号和描述之间用**双下划线** `__` 分隔
- **描述** — 简短的迁移描述，单词间用下划线分隔

### 示例

```
V1__init_schema.sql                          # 初始化建表
V2__add_qq_openid_to_user.sql                # 给用户表添加 QQ openid 字段
V12__add_points_system.sql                    # 添加积分系统
V53__add_seafile_oauth_client.sql             # Seafile OAuth 客户端
V54__add_new_field.sql                        # 新字段示例
```

## 现有迁移脚本

系统已有 53 个迁移脚本（V1 ~ V53），按功能分类：

### 基础初始化

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__init_schema.sql` | 初始化全部基础表结构 |

### 用户与认证

| 版本 | 文件 | 说明 |
|------|------|------|
| V2 | `V2__add_qq_openid_to_user.sql` | 用户表添加 QQ openid |
| V15 | `V15__add_auth_center_oidc.sql` | OIDC 认证中心 |
| V20 | `V20__fix_default_oauth_client_settings.sql` | 修复 OAuth 客户端默认配置 |
| V24 | `V24__add_lab_lms_oauth_client.sql` | LMS OAuth 客户端 |

### 机器人

| 版本 | 文件 | 说明 |
|------|------|------|
| V3 | `V3__add_leave_bot_notification_fields.sql` | 请假机器人通知字段 |
| V4 | `V4__add_bot_group_management.sql` | 机器人群管理 |
| V5 | `V5__add_bot_announcement_notice_id.sql` | 机器人公告通知 ID |
| V6 | `V6__add_bot_message_active_member.sql` | 机器人活跃成员消息 |
| V7 | `V7__add_bot_group_message_tasks.sql` | 机器人群消息任务 |

### 博客

| 版本 | 文件 | 说明 |
|------|------|------|
| V8 | `V8__add_blog_tables.sql` | 博客表结构 |
| V9 | `V9__extend_blog_interactions.sql` | 博客互动扩展 |

### 图床与展示

| 版本 | 文件 | 说明 |
|------|------|------|
| V10 | `V10__add_image_hosting_assets.sql` | 图床资源表 |
| V17 | `V17__add_showcase_apps.sql` | 应用展示 |
| V18 | `V18__add_showcase_app_status.sql` | 应用展示状态 |

### 积分系统

| 版本 | 文件 | 说明 |
|------|------|------|
| V12 | `V12__add_points_system.sql` | 积分系统 |
| V13 | `V13__add_point_rules_and_blog_review_flow.sql` | 积分规则与博客审核流程 |
| V19 | `V19__widen_point_amounts_to_bigint.sql` | 积分金额字段扩展为 BIGINT |

### 抽奖与投票

| 版本 | 文件 | 说明 |
|------|------|------|
| V11 | `V11__add_lottery_tables.sql` | 抽奖表 |
| V16 | `V16__add_vote_tables.sql` | 投票表 |
| V28 | `V28__add_vote_result_visibility.sql` | 投票结果可见性 |

### 数据开放

| 版本 | 文件 | 说明 |
|------|------|------|
| V14 | `V14__add_data_open_applications.sql` | 数据开放申请 |

### 晚自习与签到

| 版本 | 文件 | 说明 |
|------|------|------|
| V21 | `V21__add_evening_study_checkin.sql` | 晚自习签到 |
| V22 | `V22__cleanup_timeless_leave_checkin_exclusions.sql` | 清理请假签到排除 |
| V23 | `V23__add_evening_study_penalty_rules.sql` | 晚自习扣分规则 |

### 规章与校友

| 版本 | 文件 | 说明 |
|------|------|------|
| V25 | `V25__add_ai_activity_automation.sql` | AI 活动自动化 |
| V26 | `V26__add_operations_lead_role.sql` | 运营负责人角色 |
| V27 | `V27__add_club_regulations.sql` | 社团规章制度 |
| V29 | `V29__add_alumni_group_feature.sql` | 校友分组功能 |

### 加入页与用户演进

| 版本 | 文件 | 说明 |
|------|------|------|
| V30 | `V30__add_next_page_stats.sql` | 加入页统计 |
| V31 | `V31__add_next_page_join.sql` | 加入页报名 |
| V32 | `V32__add_membership_delete_permission.sql` | 成员删除权限 |
| V33 | `V33__add_user_onboarding_completed_at.sql` | 用户 onboarding 完成时间 |
| V34 | `V34__add_user_activation_and_club_vice_president.sql` | 账号激活与副社长 |
| V35 | `V35__add_club_officers_and_department_vice_manager.sql` | 社团干部与部门副主管 |
| V36 | `V36__add_group_qrcode_fields.sql` | 群二维码字段 |
| V37 | `V37__add_user_qq_group_joined_at.sql` | 用户入群时间 |

### 合作社团与外部身份

| 版本 | 文件 | 说明 |
|------|------|------|
| V38 | `V38__add_partner_clubs.sql` | 合作社团 |
| V39 | `V39__repair_partner_club_permissions.sql` | 修复合作社团权限 |
| V40 | `V40__allow_partner_club_without_website.sql` | 允许合作社团无官网 |
| V41 | `V41__add_google_identity.sql` | Google 身份 |
| V42 | `V42__add_external_identity.sql` | 外部身份绑定 |
| V43 | `V43__add_member_profiles.sql` | 成员主页 |
| V44 | `V44__add_member_profile_interactions.sql` | 成员主页互动 |
| V45 | `V45__add_partner_club_president.sql` | 合作社团社长 |
| V46 | `V46__bind_partner_club_president_user.sql` | 绑定合作社团社长 |
| V47 | `V47__add_member_profile_comment_management_permissions.sql` | 主页评论管理权限 |

### 统一分组与近期功能

| 版本 | 文件 | 说明 |
|------|------|------|
| V48 | `V48__add_unified_group_center.sql` | 统一分组中心 |
| V49 | `V49__add_unified_group_update_permission.sql` | 统一分组更新权限 |
| V50 | `V50__add_showcase_app_atomgit.sql` | 应用展示 AtomGit |
| V51 | `V51__add_comment_area_switches.sql` | 评论区开关 |
| V52 | `V52__add_mailbox_outbox.sql` | 内部邮件出站队列 |
| V53 | `V53__add_seafile_oauth_client.sql` | Seafile OAuth 客户端 |

## 新增迁移脚本流程

### 1. 确定版本号

查看现有最大版本号（当前为 V53），下一个版本号为 V54。

### 2. 创建迁移文件

```bash
# 在正确目录下创建
touch backend/src/main/resources/db/migration/V54__add_new_feature.sql
```

### 3. 编写 SQL

```sql
-- V54__add_new_feature.sql

-- 添加新字段
ALTER TABLE `club_membership` ADD COLUMN `new_field` VARCHAR(255) DEFAULT NULL COMMENT '新字段说明';

-- 添加新表
CREATE TABLE IF NOT EXISTS `new_table` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 4. 开发环境手动执行

```bash
mysql -u root -p openatom_db < backend/src/main/resources/db/migration/V54__add_new_feature.sql
```

### 5. 生产环境自动执行

部署后 Flyway 会自动执行新迁移脚本。

## Flyway 兼容策略

系统已有历史数据库首次接入 Flyway 时的处理策略：

1. **Baseline**：首次接入时 baseline 到版本 `0`
2. **V1 幂等**：`V1__init_schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，确保幂等
3. **兜底**：`SchemaCompatibilityInitializer` 在应用层做 Schema 兼容性兜底
4. **持续演进**：从 V2 开始按版本号递增

## 常见问题

### 迁移校验失败

**现象**：启动报 `Flyway validation failed`。

**原因**：已执行的迁移脚本被修改，导致 checksum 不匹配。

**解决**：
- 不要修改已执行的迁移脚本
- 如确需修复，使用 `flyway repair` 命令重置 checksum

### 迁移版本号冲突

**现象**：启动报 `Flyway duplicate migration version`。

**解决**：确保每个版本号只使用一次，新脚本使用下一个可用版本号。

### 脚本未执行

**现象**：新增了 SQL 文件但 Flyway 未执行。

**排查**：
1. 确认文件在 `backend/src/main/resources/db/migration/` 目录下
2. 确认文件名格式为 `V{数字}__{描述}.sql`（双下划线）
3. 确认 Flyway 已启用（`enabled: true`）
