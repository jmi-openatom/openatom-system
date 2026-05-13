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
    ensureOfficeDocumentTable();
    ensureOperationLogColumns();
  }

  private void fixAdminPassword() {
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
    addIndexIfAbsent("tb_user", "uk_miniapp_openid", "UNIQUE KEY `uk_miniapp_openid` (`miniapp_openid`)");
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
