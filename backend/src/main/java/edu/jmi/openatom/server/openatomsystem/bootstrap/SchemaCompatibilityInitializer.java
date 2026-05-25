package edu.jmi.openatom.server.openatomsystem.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库结构兼容性初始化器
 *
 * <p>应用启动时检查并修复数据库表结构, 确保表, 列, 索引等与当前代码兼容, 包括系统配置表, 用户表, 招新计划表, 表单表等的结构更新
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaCompatibilityInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    ensureSystemSettingTable();
    ensureRegisterSettingSeed();
    ensureUserColumns();
    fixAdminPassword();
    ensureRecruitmentCampaignColumns();
    ensureSiteFormTable();
    ensureMembershipApplicationColumns();
    ensureFormSubmissionTable();
    ensureCheckInTables();
    ensureLeaveApplicationTable();
    ensureSchoolCalendarTables();
    ensureOfficeDocumentTable();
    ensureBlogTables();
    ensureBotManagementTables();
    ensureOperationLogColumns();
  }

  private void fixAdminPassword() {
    if (!tableExists("tb_user")) {
      return;
    }
    // 修正错误的 admin 初始化密码哈希 (240be... 是错误的 admin123456 哈希)
    int updated =
        jdbcTemplate.update(
            "UPDATE tb_user SET password = ? WHERE user_name = 'admin' AND password = ?",
            "ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78",
            "240be518fabd2724bfcdd75c7315e70b8a97ef591dce6c584b77f21cd2e40dbb");
    if (updated > 0) {
      log.info("Fixed admin initialization password hash");
    }
  }

  private void ensureSystemSettingTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `system_setting`
        (
            `setting_key` VARCHAR(100) NOT NULL COMMENT '配置键',
            `setting_value` VARCHAR(255) DEFAULT NULL COMMENT '配置值',
            `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`setting_key`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表'
        """);
  }

  private void ensureRegisterSettingSeed() {
    jdbcTemplate.update(
        """
        INSERT INTO system_setting (`setting_key`, `setting_value`, `description`)
        SELECT 'register_enabled', 'false', '是否开放用户自助注册'
        WHERE NOT EXISTS (
            SELECT 1 FROM system_setting WHERE setting_key = 'register_enabled'
        )
        """);
  }

  private void ensureUserColumns() {
    addColumnIfAbsent(
        "tb_user", "class_name", "VARCHAR(100) DEFAULT NULL COMMENT '班级' AFTER `grade` ");
    addColumnIfAbsent(
        "tb_user", "miniapp_openid", "VARCHAR(80) DEFAULT NULL COMMENT '微信小程序openid' AFTER `avatar`");
    addColumnIfAbsent(
        "tb_user", "wechat_unionid", "VARCHAR(80) DEFAULT NULL COMMENT '微信unionid' AFTER `miniapp_openid`");
    addColumnIfAbsent(
        "tb_user", "qq_openid", "VARCHAR(80) DEFAULT NULL COMMENT 'QQ号/QQ OpenID' AFTER `wechat_unionid`");
    addIndexIfAbsent("tb_user", "uk_miniapp_openid", "UNIQUE KEY `uk_miniapp_openid` (`miniapp_openid`)");
    addIndexIfAbsent("tb_user", "uk_qq_openid", "UNIQUE KEY `uk_qq_openid` (`qq_openid`)");
  }

  private void ensureRecruitmentCampaignColumns() {
    addColumnIfAbsent(
        "recruitment_campaign", "login_required", "TINYINT(1) DEFAULT 1 COMMENT '是否要求登录后提交'");
    addColumnIfAbsent(
        "recruitment_campaign", "form_schema", "JSON DEFAULT NULL COMMENT '自定义报名表单结构'");
  }

  private void ensureMembershipApplicationColumns() {
    if (!tableExists("membership_application")) {
      return;
    }
    jdbcTemplate.execute(
        """
        ALTER TABLE `membership_application`
        MODIFY COLUMN `user_id` INT NULL COMMENT '申请用户ID，匿名提交时可为空'
        """);
  }

  private void ensureSiteFormTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `site_form`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `name` VARCHAR(100) NOT NULL COMMENT '表单名称',
            `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '收集开始时间',
            `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '收集结束时间',
            `login_required` TINYINT(1) DEFAULT 1 COMMENT '是否要求登录后提交',
            `form_schema` JSON DEFAULT NULL COMMENT '自定义表单结构',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_site_form_club_id` (`club_id`),
            KEY `idx_site_form_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='信息收集表单表'
        """);
    addColumnIfAbsent("site_form", "start_at", "TIMESTAMP NULL DEFAULT NULL COMMENT '收集开始时间'");
    addColumnIfAbsent("site_form", "end_at", "TIMESTAMP NULL DEFAULT NULL COMMENT '收集结束时间'");
    addColumnIfAbsent("site_form", "login_required", "TINYINT(1) DEFAULT 1 COMMENT '是否要求登录后提交'");
    addColumnIfAbsent("site_form", "form_schema", "JSON DEFAULT NULL COMMENT '自定义表单结构'");
    addColumnIfAbsent(
        "site_form",
        "updated_at",
        "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
  }

  private void ensureFormSubmissionTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `form_submission`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `form_id` INT NOT NULL COMMENT '表单ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `user_id` INT NULL COMMENT '提交用户ID',
            `anonymous_name` VARCHAR(80) DEFAULT NULL COMMENT '匿名提交联系人',
            `anonymous_contact` VARCHAR(120) DEFAULT NULL COMMENT '匿名联系方式',
            `form_data` JSON DEFAULT NULL COMMENT '表单数据',
            `status` VARCHAR(30) DEFAULT 'submitted' COMMENT '提交状态',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_form_submission_form` (`form_id`),
            KEY `idx_form_submission_club` (`club_id`),
            KEY `idx_form_submission_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通用表单提交记录表'
        """);
    if (!tableExists("form_submission")) {
      return;
    }
    if (columnExists("form_submission", "campaign_id")
        && !columnExists("form_submission", "form_id")) {
      jdbcTemplate.execute(
          """
          ALTER TABLE `form_submission`
          CHANGE COLUMN `campaign_id` `form_id` INT NOT NULL COMMENT '表单ID'
          """);
      log.info("Renamed legacy column: form_submission.campaign_id -> form_id");
    }
    if (!indexExists("form_submission", "idx_form_submission_form")) {
      jdbcTemplate.execute(
          "ALTER TABLE `form_submission` ADD INDEX `idx_form_submission_form` (`form_id`)");
      log.info("Added missing index: form_submission.idx_form_submission_form");
    }
    if (indexExists("form_submission", "idx_form_submission_campaign")) {
      jdbcTemplate.execute(
          "ALTER TABLE `form_submission` DROP INDEX `idx_form_submission_campaign`");
      log.info("Dropped legacy index: form_submission.idx_form_submission_campaign");
    }
  }

  private void ensureOfficeDocumentTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `office_document`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `doc_type` VARCHAR(40) NOT NULL COMMENT '单据类型',
            `title` VARCHAR(200) NOT NULL COMMENT '单据标题',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '单据状态',
            `payload` JSON DEFAULT NULL COMMENT '单据业务数据',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `updated_by` INT DEFAULT NULL COMMENT '更新人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_office_document_club` (`club_id`),
            KEY `idx_office_document_type` (`doc_type`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='后台文书单据表'
        """);
  }

  private void ensureBlogTables() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `blog_article`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `author_id` INT NOT NULL COMMENT '作者用户ID',
            `title` VARCHAR(180) NOT NULL COMMENT '文章标题',
            `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
            `content_markdown` LONGTEXT NOT NULL COMMENT 'Markdown正文',
            `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
            `category` VARCHAR(80) DEFAULT NULL COMMENT '分类',
            `tags` JSON DEFAULT NULL COMMENT '标签列表',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态: draft/published/hidden/rejected',
            `featured` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
            `view_count` INT DEFAULT 0 COMMENT '浏览量',
            `like_count` INT DEFAULT 0 COMMENT '点赞数',
            `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
            `share_count` INT DEFAULT 0 COMMENT '分享数',
            `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回或隐藏原因',
            `reviewed_by` INT DEFAULT NULL COMMENT '审核人ID',
            `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
            `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_blog_article_club` (`club_id`),
            KEY `idx_blog_article_author` (`author_id`),
            KEY `idx_blog_article_status_publish` (`status`, `published_at`),
            KEY `idx_blog_article_category` (`category`),
            KEY `idx_blog_article_featured` (`featured`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='博客文章表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `blog_comment`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `article_id` INT NOT NULL COMMENT '文章ID',
            `user_id` INT NOT NULL COMMENT '评论用户ID',
            `parent_id` INT DEFAULT NULL COMMENT '父评论ID',
            `content` TEXT NOT NULL COMMENT '评论内容',
            `status` VARCHAR(30) DEFAULT 'visible' COMMENT '状态: visible/hidden',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_blog_comment_article` (`article_id`),
            KEY `idx_blog_comment_parent` (`parent_id`),
            KEY `idx_blog_comment_user` (`user_id`),
            KEY `idx_blog_comment_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='博客评论表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `blog_article_interaction`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `article_id` INT NOT NULL COMMENT '文章ID',
            `user_id` INT NOT NULL COMMENT '用户ID',
            `interaction_type` VARCHAR(30) NOT NULL COMMENT '互动类型: like/favorite/share',
            `channel` VARCHAR(40) DEFAULT NULL COMMENT '互动来源渠道',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            KEY `idx_blog_interaction_article` (`article_id`),
            KEY `idx_blog_interaction_user_type` (`user_id`, `interaction_type`),
            KEY `idx_blog_interaction_type_time` (`interaction_type`, `created_at`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='博客互动记录表'
        """);
    addColumnIfAbsent(
        "blog_article", "favorite_count", "INT DEFAULT 0 COMMENT '收藏数' AFTER `like_count`");
    addColumnIfAbsent(
        "blog_article", "share_count", "INT DEFAULT 0 COMMENT '分享数' AFTER `favorite_count`");
    addColumnIfAbsent(
        "blog_comment", "parent_id", "INT DEFAULT NULL COMMENT '父评论ID' AFTER `user_id`");
    addIndexIfAbsent(
        "blog_comment", "idx_blog_comment_parent", "KEY `idx_blog_comment_parent` (`parent_id`)");
  }

  private void ensureCheckInTables() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `checkin_session`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `activity_id` INT DEFAULT NULL COMMENT '关联活动ID',
            `group_id` INT DEFAULT NULL COMMENT '签到分组ID',
            `title` VARCHAR(120) NOT NULL COMMENT '签到标题',
            `location` VARCHAR(120) DEFAULT NULL COMMENT '签到地点',
            `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '签到开始时间',
            `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '签到结束时间',
            `status` VARCHAR(30) DEFAULT 'open' COMMENT '状态: draft/open/closed',
            `token` VARCHAR(64) NOT NULL COMMENT '扫码签到令牌',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_checkin_session_token` (`token`),
            KEY `idx_checkin_session_club` (`club_id`),
            KEY `idx_checkin_session_activity` (`activity_id`),
            KEY `idx_checkin_session_group` (`group_id`),
            KEY `idx_checkin_session_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到场次表'
        """);
    addColumnIfAbsent("checkin_session", "group_id", "INT DEFAULT NULL COMMENT '签到分组ID' AFTER `activity_id`");
    addIndexIfAbsent("checkin_session", "idx_checkin_session_group", "KEY `idx_checkin_session_group` (`group_id`)");
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `checkin_group`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `name` VARCHAR(120) NOT NULL COMMENT '分组名称',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            KEY `idx_checkin_group_club` (`club_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到分组表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `checkin_group_member`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` INT NOT NULL COMMENT '签到分组ID',
            `user_id` INT NOT NULL COMMENT '用户ID',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_checkin_group_member_user` (`group_id`, `user_id`),
            KEY `idx_checkin_group_member_group` (`group_id`),
            KEY `idx_checkin_group_member_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到分组成员表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `checkin_target`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `session_id` INT NOT NULL COMMENT '签到场次ID',
            `user_id` INT NOT NULL COMMENT '发放用户ID',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_checkin_target_user` (`session_id`, `user_id`),
            KEY `idx_checkin_target_session` (`session_id`),
            KEY `idx_checkin_target_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到发放人员表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `checkin_record`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `session_id` INT NOT NULL COMMENT '签到场次ID',
            `user_id` INT NOT NULL COMMENT '签到用户ID',
            `checkin_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
            `source` VARCHAR(40) DEFAULT 'miniapp_scan' COMMENT '签到来源',
            `status` VARCHAR(30) DEFAULT 'checked' COMMENT '签到状态',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_checkin_record_user` (`session_id`, `user_id`),
            KEY `idx_checkin_record_session` (`session_id`),
            KEY `idx_checkin_record_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到记录表'
        """);
  }

  private void ensureLeaveApplicationTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `leave_application`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `user_id` INT NOT NULL COMMENT '申请用户ID',
            `title` VARCHAR(160) NOT NULL COMMENT '请假标题',
            `reason` TEXT NOT NULL COMMENT '请假理由',
            `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '请假开始时间',
            `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '请假结束时间',
            `attachments` JSON DEFAULT NULL COMMENT '附件列表',
            `status` VARCHAR(30) DEFAULT 'submitted' COMMENT '状态: submitted/approved/rejected',
            `reviewer_id` INT DEFAULT NULL COMMENT '审批人ID',
            `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
            `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '审批时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_leave_application_club` (`club_id`),
            KEY `idx_leave_application_user` (`user_id`),
            KEY `idx_leave_application_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='请假申请表'
        """);
    addColumnIfAbsent(
        "leave_application", "reviewer_id", "INT DEFAULT NULL COMMENT '审批人ID' AFTER `status`");
    addColumnIfAbsent(
        "leave_application",
        "review_comment",
        "VARCHAR(500) DEFAULT NULL COMMENT '审批意见' AFTER `reviewer_id`");
    addColumnIfAbsent(
        "leave_application",
        "reviewed_at",
        "TIMESTAMP NULL DEFAULT NULL COMMENT '审批时间' AFTER `review_comment`");
    addColumnIfAbsent(
        "leave_application",
        "bot_notify_origin",
        "VARCHAR(255) DEFAULT NULL COMMENT '机器人通知会话来源' AFTER `reviewed_at`");
    addColumnIfAbsent(
        "leave_application",
        "bot_notify_user_id",
        "VARCHAR(80) DEFAULT NULL COMMENT '机器人通知用户ID' AFTER `bot_notify_origin`");
    addColumnIfAbsent(
        "leave_application",
        "bot_notified_at",
        "TIMESTAMP NULL DEFAULT NULL COMMENT '机器人通知时间' AFTER `bot_notify_user_id`");
  }

  private void ensureSchoolCalendarTables() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `school_calendar_setting`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `start_date` DATE NOT NULL COMMENT '开学日期',
            `week_count` INT NOT NULL COMMENT '学期周数',
            `updated_by` INT DEFAULT NULL COMMENT '更新人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='校历基础设置表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `school_calendar_adjustment`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `calendar_date` DATE NOT NULL COMMENT '调休日期',
            `type` VARCHAR(20) NOT NULL COMMENT '类型: workday/restday',
            `reason` VARCHAR(255) DEFAULT NULL COMMENT '调休说明',
            `updated_by` INT DEFAULT NULL COMMENT '更新人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_school_calendar_adjustment_date` (`calendar_date`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='校历调休表'
        """);
  }

  private void ensureOperationLogColumns() {
    if (!tableExists("operation_log")) {
      return;
    }
    jdbcTemplate.execute(
        """
        ALTER TABLE `operation_log`
        MODIFY COLUMN `module` VARCHAR(50) DEFAULT NULL COMMENT '模块',
        MODIFY COLUMN `action` VARCHAR(120) DEFAULT NULL COMMENT '动作',
        MODIFY COLUMN `target_id` VARCHAR(80) DEFAULT NULL COMMENT '目标ID'
        """);
  }

  private void ensureBotManagementTables() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_account`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `account_id` VARCHAR(80) NOT NULL COMMENT '机器人账号',
            `nickname` VARCHAR(120) DEFAULT NULL COMMENT '机器人昵称',
            `platform` VARCHAR(40) DEFAULT 'qq' COMMENT '接入平台',
            `status` VARCHAR(30) DEFAULT 'unknown' COMMENT '连接状态',
            `api_base_url` VARCHAR(255) DEFAULT NULL COMMENT 'OneBot HTTP API 地址',
            `access_token` VARCHAR(255) DEFAULT NULL COMMENT 'OneBot 访问令牌',
            `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
            `last_seen_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后在线时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_account_account_id` (`account_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='机器人账号表'
        """);
    jdbcTemplate.update(
        """
        INSERT INTO `bot_account` (`id`, `account_id`, `nickname`, `platform`, `status`, `enabled`)
        VALUES (1, 'default', '默认 QQ 机器人', 'qq', 'unknown', 1)
        ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`)
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_group`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `bot_account_id` INT DEFAULT 1 COMMENT '机器人账号ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `group_name` VARCHAR(160) DEFAULT NULL COMMENT '群名称',
            `owner_id` VARCHAR(80) DEFAULT NULL COMMENT '群主QQ',
            `owner_nickname` VARCHAR(120) DEFAULT NULL COMMENT '群主昵称',
            `member_count` INT DEFAULT 0 COMMENT '成员数量',
            `admin_count` INT DEFAULT 0 COMMENT '管理员数量',
            `bot_role` VARCHAR(30) DEFAULT 'member' COMMENT '机器人群身份',
            `bot_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用机器人',
            `mode` VARCHAR(30) DEFAULT 'enabled' COMMENT '响应模式',
            `last_active_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后活跃时间',
            `last_synced_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后同步时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_group_group_id` (`group_id`),
            KEY `idx_bot_group_account` (`bot_account_id`),
            KEY `idx_bot_group_enabled` (`bot_enabled`),
            KEY `idx_bot_group_mode` (`mode`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='机器人QQ群表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_group_member`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `user_id` VARCHAR(80) NOT NULL COMMENT '成员QQ号',
            `nickname` VARCHAR(120) DEFAULT NULL COMMENT 'QQ昵称',
            `card` VARCHAR(120) DEFAULT NULL COMMENT '群名片',
            `role` VARCHAR(30) DEFAULT 'member' COMMENT '群身份',
            `join_time` TIMESTAMP NULL DEFAULT NULL COMMENT '入群时间',
            `last_sent_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后发言时间',
            `muted_until` TIMESTAMP NULL DEFAULT NULL COMMENT '禁言截止时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_group_member_user` (`group_id`, `user_id`),
            KEY `idx_bot_group_member_group` (`group_id`),
            KEY `idx_bot_group_member_role` (`role`),
            KEY `idx_bot_group_member_mute` (`muted_until`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群成员缓存表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_group_config`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `welcome_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用欢迎语',
            `welcome_text` TEXT DEFAULT NULL COMMENT '欢迎语',
            `at_new_member` TINYINT(1) DEFAULT 1 COMMENT '是否@新成员',
            `welcome_image_url` VARCHAR(500) DEFAULT NULL COMMENT '欢迎图片',
            `welcome_attachment_url` VARCHAR(500) DEFAULT NULL COMMENT '欢迎附件',
            `welcome_delay_seconds` INT DEFAULT 0 COMMENT '延迟发送秒数',
            `plugin_config` JSON DEFAULT NULL COMMENT '按群插件开关',
            `auto_review_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用自动审核',
            `auto_review_keywords` JSON DEFAULT NULL COMMENT '自动审核关键词',
            `sensitive_filter_enabled` TINYINT(1) DEFAULT 0 COMMENT '是否启用敏感词',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_group_config_group` (`group_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群机器人配置表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_group_announcement`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
            `content` TEXT NOT NULL COMMENT '公告正文',
            `attachments` JSON DEFAULT NULL COMMENT '附件列表',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '发布状态',
            `notice_id` VARCHAR(120) DEFAULT NULL COMMENT 'NapCat群公告ID',
            `result_message` VARCHAR(500) DEFAULT NULL COMMENT '发布结果',
            `published_by` INT DEFAULT NULL COMMENT '发布人',
            `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_bot_announcement_group` (`group_id`),
            KEY `idx_bot_announcement_notice` (`notice_id`),
            KEY `idx_bot_announcement_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群公告记录表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_group_message_task`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `content` TEXT NOT NULL COMMENT '消息正文',
            `at_all` TINYINT(1) DEFAULT 0 COMMENT '是否@全体',
            `status` VARCHAR(30) DEFAULT 'pending' COMMENT '状态: pending/sending/sent/failed/canceled',
            `scheduled_at` TIMESTAMP NULL DEFAULT NULL COMMENT '定时发送时间',
            `sent_at` TIMESTAMP NULL DEFAULT NULL COMMENT '实际发送时间',
            `result_message` VARCHAR(500) DEFAULT NULL COMMENT '发送结果',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_bot_message_task_group` (`group_id`),
            KEY `idx_bot_message_task_status_time` (`status`, `scheduled_at`),
            KEY `idx_bot_message_task_created` (`created_at`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群消息发送任务表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_join_request`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `request_id` VARCHAR(120) DEFAULT NULL COMMENT '申请编号',
            `flag` VARCHAR(255) DEFAULT NULL COMMENT 'OneBot 申请flag',
            `user_id` VARCHAR(80) DEFAULT NULL COMMENT '申请人QQ',
            `nickname` VARCHAR(120) DEFAULT NULL COMMENT '申请人昵称',
            `comment` VARCHAR(500) DEFAULT NULL COMMENT '申请理由',
            `status` VARCHAR(30) DEFAULT 'pending' COMMENT '处理状态',
            `reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因或处理备注',
            `handled_by` INT DEFAULT NULL COMMENT '处理人',
            `requested_at` TIMESTAMP NULL DEFAULT NULL COMMENT '申请时间',
            `handled_at` TIMESTAMP NULL DEFAULT NULL COMMENT '处理时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_join_request` (`group_id`, `request_id`),
            KEY `idx_bot_join_group_status` (`group_id`, `status`),
            KEY `idx_bot_join_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群入群申请表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_sensitive_word`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `word` VARCHAR(120) NOT NULL COMMENT '敏感词',
            `action` VARCHAR(30) DEFAULT 'warn' COMMENT '命中动作',
            `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_sensitive_word` (`word`),
            KEY `idx_bot_sensitive_enabled` (`enabled`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群敏感词表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_auto_review_rule`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `name` VARCHAR(120) NOT NULL COMMENT '规则名称',
            `group_id` VARCHAR(80) DEFAULT NULL COMMENT 'QQ群号，空表示全局',
            `keywords` JSON DEFAULT NULL COMMENT '关键词',
            `approve` TINYINT(1) DEFAULT 1 COMMENT '是否自动同意',
            `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_bot_auto_review_group` (`group_id`),
            KEY `idx_bot_auto_review_enabled` (`enabled`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群自动审核规则表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_message_stat`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `stat_date` DATE NOT NULL COMMENT '统计日期',
            `message_count` INT DEFAULT 0 COMMENT '消息数',
            `active_member_count` INT DEFAULT 0 COMMENT '活跃成员数',
            `command_count` INT DEFAULT 0 COMMENT '指令数',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_message_stat` (`group_id`, `stat_date`),
            KEY `idx_bot_message_stat_date` (`stat_date`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群消息统计表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `bot_message_active_member`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
            `stat_date` DATE NOT NULL COMMENT '统计日期',
            `user_id` VARCHAR(80) NOT NULL COMMENT '成员QQ号',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_bot_message_active_member` (`group_id`, `stat_date`, `user_id`),
            KEY `idx_bot_message_active_date` (`stat_date`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群每日活跃成员去重表'
        """);
    addColumnIfAbsent(
        "bot_group_announcement",
        "notice_id",
        "VARCHAR(120) DEFAULT NULL COMMENT 'NapCat群公告ID' AFTER `status`");
    addIndexIfAbsent(
        "bot_group_announcement",
        "idx_bot_announcement_notice",
        "KEY `idx_bot_announcement_notice` (`notice_id`)");
  }

  private void addColumnIfAbsent(String tableName, String columnName, String definition) {
    if (!tableExists(tableName) || columnExists(tableName, columnName)) {
      return;
    }
    jdbcTemplate.execute(
        "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
    log.info("Added missing column: {}.{}", tableName, columnName);
  }

  private void addIndexIfAbsent(String tableName, String indexName, String definition) {
    if (!tableExists(tableName) || indexExists(tableName, indexName)) {
      return;
    }
    jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD " + definition);
    log.info("Added missing index: {}.{}", tableName, indexName);
  }

  private boolean tableExists(String tableName) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """,
            Integer.class,
            tableName);
    return count != null && count > 0;
  }

  private boolean columnExists(String tableName, String columnName) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
            """,
            Integer.class,
            tableName,
            columnName);
    return count != null && count > 0;
  }

  private boolean indexExists(String tableName, String indexName) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
            """,
            Integer.class,
            tableName,
            indexName);
    return count != null && count > 0;
  }
}
