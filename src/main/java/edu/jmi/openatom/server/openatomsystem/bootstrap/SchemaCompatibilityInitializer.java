package edu.jmi.openatom.server.openatomsystem.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    ensureMembershipApplicationColumns();
    ensureFormSubmissionTable();
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
        "tb_user",
        "class_name",
        "VARCHAR(100) DEFAULT NULL COMMENT '班级' AFTER `grade` ");
  }

  private void ensureRecruitmentCampaignColumns() {
    addColumnIfAbsent(
        "recruitment_campaign",
        "login_required",
        "TINYINT(1) DEFAULT 1 COMMENT '是否要求登录后提交'");
    addColumnIfAbsent(
        "recruitment_campaign",
        "form_schema",
        "JSON DEFAULT NULL COMMENT '自定义报名表单结构'");
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

  private void ensureFormSubmissionTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `form_submission`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `campaign_id` INT NOT NULL COMMENT '表单ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `user_id` INT NULL COMMENT '提交用户ID',
            `anonymous_name` VARCHAR(80) DEFAULT NULL COMMENT '匿名提交联系人',
            `anonymous_contact` VARCHAR(120) DEFAULT NULL COMMENT '匿名联系方式',
            `form_data` JSON DEFAULT NULL COMMENT '表单数据',
            `status` VARCHAR(30) DEFAULT 'submitted' COMMENT '提交状态',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_form_submission_campaign` (`campaign_id`),
            KEY `idx_form_submission_club` (`club_id`),
            KEY `idx_form_submission_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通用表单提交记录表'
        """);
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
}
