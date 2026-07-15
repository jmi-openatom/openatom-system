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
    insertSettingIfAbsent("activation_enabled", "true", "是否启用激活页面引导流程");
    ensureUserColumns();
    ensureExternalIdentityTable();
    fixAdminPassword();
    ensureRecruitmentCampaignColumns();
    ensureSiteFormTable();
    ensureMembershipApplicationColumns();
    ensureFormSubmissionTable();
    ensureLotteryTables();
    ensureVoteTables();
    ensureCheckInTables();
    ensureEveningStudyCheckInTables();
    ensurePointSystemTables();
    ensurePointSettingSeed();
    ensureLeaveApplicationTable();
    ensureSchoolCalendarTables();
    ensureOfficeDocumentTable();
    ensureClubRegulationTable();
    ensureBlogTables();
    ensureImageHostingTable();
    ensureMemberProfileTables();
    ensureShowcaseAppTable();
    ensureDataOpenApplicationTable();
    ensureOauthTables();
    ensureBotManagementTables();
    ensureOperationLogColumns();
  }

  private void ensureMemberProfileTables() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `member_profile` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `user_id` INT NOT NULL,
            `slug` VARCHAR(64) NOT NULL,
            `display_name` VARCHAR(80) DEFAULT NULL,
            `headline` VARCHAR(160) DEFAULT NULL,
            `bio` TEXT DEFAULT NULL,
            `avatar_url` VARCHAR(500) DEFAULT NULL,
            `banner_url` VARCHAR(500) DEFAULT NULL,
            `card_background_url` VARCHAR(500) DEFAULT NULL,
            `card_focus_x` DECIMAL(5,2) NOT NULL DEFAULT 50.00,
            `card_focus_y` DECIMAL(5,2) NOT NULL DEFAULT 50.00,
            `theme_key` VARCHAR(32) NOT NULL DEFAULT 'minimal',
            `color_mode` VARCHAR(16) NOT NULL DEFAULT 'system',
            `visibility` VARCHAR(16) NOT NULL DEFAULT 'members',
            `status` VARCHAR(16) NOT NULL DEFAULT 'draft',
            `show_department` TINYINT(1) NOT NULL DEFAULT 1,
            `show_position` TINYINT(1) NOT NULL DEFAULT 1,
            `skills` JSON DEFAULT NULL,
            `version` INT NOT NULL DEFAULT 0,
            `published_at` TIMESTAMP NULL DEFAULT NULL,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_member_profile_user` (`user_id`),
            UNIQUE KEY `uk_member_profile_slug` (`slug`),
            KEY `idx_member_profile_status_visibility` (`status`, `visibility`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员公开主页'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `member_profile_module` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `profile_id` BIGINT NOT NULL,
            `module_key` VARCHAR(32) NOT NULL,
            `title` VARCHAR(80) DEFAULT NULL,
            `sort_order` INT NOT NULL DEFAULT 0,
            `column_span` TINYINT NOT NULL DEFAULT 12,
            `enabled` TINYINT(1) NOT NULL DEFAULT 1,
            `visibility` VARCHAR(16) NOT NULL DEFAULT 'members',
            `config_json` JSON DEFAULT NULL,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_member_profile_module` (`profile_id`, `module_key`),
            KEY `idx_member_profile_module_order` (`profile_id`, `sort_order`, `id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员主页组件'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `member_profile_social_link` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `profile_id` BIGINT NOT NULL,
            `platform` VARCHAR(32) NOT NULL,
            `label` VARCHAR(60) DEFAULT NULL,
            `url` VARCHAR(500) NOT NULL,
            `sort_order` INT NOT NULL DEFAULT 0,
            `enabled` TINYINT(1) NOT NULL DEFAULT 1,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            KEY `idx_member_profile_social_order` (`profile_id`, `sort_order`, `id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员主页公开链接'
        """);
  }

  private void ensureClubRegulationTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `club_regulation`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `title` VARCHAR(200) NOT NULL COMMENT '制度标题',
            `summary` VARCHAR(800) DEFAULT NULL COMMENT '制度摘要',
            `content_markdown` LONGTEXT NOT NULL COMMENT 'Markdown正文',
            `status` VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published',
            `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
            `created_by` INT DEFAULT NULL COMMENT '创建人ID',
            `updated_by` INT DEFAULT NULL COMMENT '最后更新人ID',
            `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_club_regulation_club_status` (`club_id`, `status`),
            KEY `idx_club_regulation_sort` (`sort_order`, `published_at`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='社团规章制度表'
        """);
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

  private void ensurePointSettingSeed() {
    insertSettingIfAbsent("point.daily_login_points", "1", "每日首次登录奖励积分");
    insertSettingIfAbsent("point.blog_publish_points", "20", "博客审核通过奖励积分");
  }

  private void insertSettingIfAbsent(String key, String value, String description) {
    jdbcTemplate.update(
        """
        INSERT INTO system_setting (`setting_key`, `setting_value`, `description`)
        SELECT ?, ?, ?
        WHERE NOT EXISTS (
            SELECT 1 FROM system_setting WHERE setting_key = ?
        )
        """,
        key,
        value,
        description,
        key);
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
    addColumnIfAbsent(
        "tb_user", "google_sub", "VARCHAR(128) DEFAULT NULL COMMENT 'Google OIDC subject' AFTER `qq_openid`");
    addIndexIfAbsent("tb_user", "uk_miniapp_openid", "UNIQUE KEY `uk_miniapp_openid` (`miniapp_openid`)");
    addIndexIfAbsent("tb_user", "uk_qq_openid", "UNIQUE KEY `uk_qq_openid` (`qq_openid`)");
    addIndexIfAbsent("tb_user", "uk_google_sub", "UNIQUE KEY `uk_google_sub` (`google_sub`)");
  }

  private void ensureExternalIdentityTable() {
    jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS `user_external_identity` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `user_id` INT NOT NULL,
            `provider` VARCHAR(32) NOT NULL,
            `subject` VARCHAR(128) NOT NULL,
            `provider_username` VARCHAR(120) DEFAULT NULL,
            `avatar_url` VARCHAR(500) DEFAULT NULL,
            `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `last_login_at` TIMESTAMP NULL DEFAULT NULL,
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_external_provider_subject` (`provider`, `subject`),
            UNIQUE KEY `uk_external_user_provider` (`user_id`, `provider`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方身份绑定'
        """);
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

  private void ensureLotteryTables() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `lottery_campaign`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `form_id` INT NOT NULL COMMENT '参与表单ID',
            `title` VARCHAR(160) NOT NULL COMMENT '抽奖标题',
            `description` VARCHAR(500) DEFAULT NULL COMMENT '抽奖说明',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态: draft/open/closed',
            `created_by` INT DEFAULT NULL COMMENT '创建人ID',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_lottery_campaign_club` (`club_id`),
            KEY `idx_lottery_campaign_form` (`form_id`),
            KEY `idx_lottery_campaign_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `lottery_prize`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `lottery_id` INT NOT NULL COMMENT '抽奖活动ID',
            `name` VARCHAR(120) NOT NULL COMMENT '奖品名称',
            `level` VARCHAR(60) DEFAULT NULL COMMENT '奖项等级',
            `quantity` INT NOT NULL DEFAULT 1 COMMENT '奖品数量',
            `sort_order` INT DEFAULT 0 COMMENT '展示排序',
            `color` VARCHAR(40) DEFAULT NULL COMMENT '大屏颜色',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_lottery_prize_lottery` (`lottery_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖奖品表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `lottery_winner`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `lottery_id` INT NOT NULL COMMENT '抽奖活动ID',
            `prize_id` INT NOT NULL COMMENT '奖品ID',
            `submission_id` INT NOT NULL COMMENT '表单提交记录ID',
            `user_id` INT DEFAULT NULL COMMENT '中奖用户ID',
            `winner_name` VARCHAR(120) DEFAULT NULL COMMENT '中奖人名称',
            `winner_contact` VARCHAR(160) DEFAULT NULL COMMENT '中奖人联系方式',
            `winner_account` VARCHAR(120) DEFAULT NULL COMMENT '中奖人账号',
            `won_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '中奖时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_lottery_winner_submission` (`lottery_id`, `submission_id`),
            KEY `idx_lottery_winner_lottery` (`lottery_id`),
            KEY `idx_lottery_winner_prize` (`prize_id`),
            KEY `idx_lottery_winner_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖中奖记录表'
        """);
  }

  private void ensureVoteTables() {
    boolean hadVoteCampaignTable = tableExists("vote_campaign");
    boolean hadResultVisibilityColumn =
        hadVoteCampaignTable && columnExists("vote_campaign", "result_visibility");
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `vote_campaign`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `title` VARCHAR(160) NOT NULL COMMENT '投票标题',
            `description` VARCHAR(800) DEFAULT NULL COMMENT '投票说明',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态: draft/open/closed',
            `vote_type` VARCHAR(30) DEFAULT 'single' COMMENT '投票类型: single/multiple',
            `max_choices` INT NOT NULL DEFAULT 1 COMMENT '最多可选数量',
            `anonymous_allowed` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许匿名投票',
            `result_visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '前台是否公开结果',
            `result_visibility` VARCHAR(20) NOT NULL DEFAULT 'public' COMMENT '前台结果可见范围: public/after_vote/private',
            `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '投票开始时间',
            `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '投票结束时间',
            `created_by` INT DEFAULT NULL COMMENT '创建人ID',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_vote_campaign_club` (`club_id`),
            KEY `idx_vote_campaign_status` (`status`),
            KEY `idx_vote_campaign_time` (`start_at`, `end_at`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='投票活动表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `vote_option`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `vote_id` INT NOT NULL COMMENT '投票活动ID',
            `title` VARCHAR(160) NOT NULL COMMENT '选项标题',
            `description` VARCHAR(800) DEFAULT NULL COMMENT '选项说明',
            `sort_order` INT DEFAULT 0 COMMENT '展示排序',
            `color` VARCHAR(40) DEFAULT NULL COMMENT '展示颜色',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_vote_option_vote` (`vote_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='投票选项表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `vote_record`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `vote_id` INT NOT NULL COMMENT '投票活动ID',
            `user_id` INT DEFAULT NULL COMMENT '投票用户ID',
            `voter_name` VARCHAR(120) DEFAULT NULL COMMENT '投票人姓名',
            `voter_contact` VARCHAR(160) DEFAULT NULL COMMENT '投票人联系方式',
            `voter_key` VARCHAR(180) NOT NULL COMMENT '投票人唯一键',
            `selected_option_ids` JSON NOT NULL COMMENT '已选选项ID列表',
            `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
            `voted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_vote_record_voter` (`vote_id`, `voter_key`),
            KEY `idx_vote_record_vote` (`vote_id`),
            KEY `idx_vote_record_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='投票记录表'
        """);
    addColumnIfAbsent(
        "vote_campaign",
        "result_visibility",
        "VARCHAR(20) NOT NULL DEFAULT 'public' COMMENT '前台结果可见范围: public/after_vote/private' AFTER `result_visible`");
    if (hadVoteCampaignTable && !hadResultVisibilityColumn) {
      jdbcTemplate.update(
          """
          UPDATE `vote_campaign`
          SET `result_visibility` = CASE
              WHEN `result_visible` = 0 THEN 'after_vote'
              ELSE 'public'
          END
          """);
      log.info("Backfilled vote_campaign.result_visibility from legacy result_visible");
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

  private void ensureImageHostingTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `image_hosting_asset`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `uploader_id` INT NOT NULL COMMENT '上传用户ID',
            `file_name` VARCHAR(120) NOT NULL COMMENT '存储文件名',
            `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
            `content_type` VARCHAR(80) DEFAULT NULL COMMENT '内容类型',
            `file_size` BIGINT DEFAULT 0 COMMENT '文件大小',
            `url` VARCHAR(500) NOT NULL COMMENT '公开访问URL',
            `status` VARCHAR(30) DEFAULT 'active' COMMENT '状态: active/deleted',
            `deleted_by` INT DEFAULT NULL COMMENT '删除人ID',
            `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '删除时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_image_hosting_file` (`file_name`),
            KEY `idx_image_hosting_uploader` (`uploader_id`),
            KEY `idx_image_hosting_status_time` (`status`, `created_at`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='图床资产表'
        """);
  }

  private void ensureShowcaseAppTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `showcase_app`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `name` VARCHAR(160) NOT NULL COMMENT '应用名称',
            `summary` VARCHAR(800) DEFAULT NULL COMMENT '应用简介',
            `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面或截图URL',
            `open_source` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已开源',
            `github_url` VARCHAR(500) DEFAULT NULL COMMENT 'GitHub仓库地址',
            `gitee_url` VARCHAR(500) DEFAULT NULL COMMENT 'Gitee仓库地址',
            `developer` VARCHAR(160) DEFAULT NULL COMMENT '开发者',
            `owner` VARCHAR(160) DEFAULT NULL COMMENT '所有者',
            `license_name` VARCHAR(120) DEFAULT NULL COMMENT '开源协议',
            `version` VARCHAR(80) DEFAULT NULL COMMENT '版本号',
            `download_url` VARCHAR(500) DEFAULT NULL COMMENT '下载链接',
            `status` VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/hidden',
            `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示排序',
            `created_by` INT DEFAULT NULL COMMENT '创建人ID',
            `updated_by` INT DEFAULT NULL COMMENT '更新人ID',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_showcase_app_status_sort` (`status`, `sort_order`, `id`),
            KEY `idx_showcase_app_open_source` (`open_source`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='应用展示表'
        """);
    addColumnIfAbsent(
        "showcase_app", "summary", "VARCHAR(800) DEFAULT NULL COMMENT '应用简介' AFTER `name`");
    addColumnIfAbsent(
        "showcase_app", "cover_url", "VARCHAR(500) DEFAULT NULL COMMENT '封面或截图URL' AFTER `summary`");
    addColumnIfAbsent(
        "showcase_app",
        "open_source",
        "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已开源' AFTER `cover_url`");
    addColumnIfAbsent(
        "showcase_app",
        "github_url",
        "VARCHAR(500) DEFAULT NULL COMMENT 'GitHub仓库地址' AFTER `open_source`");
    addColumnIfAbsent(
        "showcase_app",
        "gitee_url",
        "VARCHAR(500) DEFAULT NULL COMMENT 'Gitee仓库地址' AFTER `github_url`");
    addColumnIfAbsent(
        "showcase_app", "developer", "VARCHAR(160) DEFAULT NULL COMMENT '开发者' AFTER `gitee_url`");
    addColumnIfAbsent(
        "showcase_app", "owner", "VARCHAR(160) DEFAULT NULL COMMENT '所有者' AFTER `developer`");
    addColumnIfAbsent(
        "showcase_app",
        "license_name",
        "VARCHAR(120) DEFAULT NULL COMMENT '开源协议' AFTER `owner`");
    addColumnIfAbsent(
        "showcase_app", "version", "VARCHAR(80) DEFAULT NULL COMMENT '版本号' AFTER `license_name`");
    addColumnIfAbsent(
        "showcase_app",
        "app_status",
        "VARCHAR(80) DEFAULT NULL COMMENT '应用展示状态' AFTER `version`");
    addColumnIfAbsent(
        "showcase_app",
        "download_url",
        "VARCHAR(500) DEFAULT NULL COMMENT '下载链接' AFTER `app_status`");
    addColumnIfAbsent(
        "showcase_app",
        "status",
        "VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/hidden' AFTER `download_url`");
    addColumnIfAbsent(
        "showcase_app", "sort_order", "INT NOT NULL DEFAULT 0 COMMENT '展示排序' AFTER `status`");
    addIndexIfAbsent(
        "showcase_app",
        "idx_showcase_app_status_sort",
        "KEY `idx_showcase_app_status_sort` (`status`, `sort_order`, `id`)");
    addIndexIfAbsent(
        "showcase_app",
        "idx_showcase_app_open_source",
        "KEY `idx_showcase_app_open_source` (`open_source`)");
  }

	  private void ensureDataOpenApplicationTable() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `data_open_application`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `app_name` VARCHAR(120) NOT NULL COMMENT '接入应用名称',
            `applicant_name` VARCHAR(80) NOT NULL COMMENT '申请人',
            `applicant_contact` VARCHAR(160) NOT NULL COMMENT '联系方式',
            `organization` VARCHAR(160) DEFAULT NULL COMMENT '组织或团队',
            `purpose` VARCHAR(800) NOT NULL COMMENT '使用场景',
            `endpoint_scope` VARCHAR(80) NOT NULL DEFAULT 'public_login' COMMENT '可调用接口范围',
            `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
            `api_key` VARCHAR(128) DEFAULT NULL COMMENT '调用密钥',
            `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
            `reviewed_by` INT DEFAULT NULL COMMENT '审核人ID',
            `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
            `last_used_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最近调用时间',
            `call_count` INT NOT NULL DEFAULT 0 COMMENT '调用次数',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_data_open_api_key` (`api_key`),
            KEY `idx_data_open_status_time` (`status`, `created_at`),
            KEY `idx_data_open_applicant` (`applicant_contact`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据开放平台申请表'
        """);
    addColumnIfAbsent(
        "data_open_application",
        "endpoint_scope",
        "VARCHAR(80) NOT NULL DEFAULT 'public_login' COMMENT '可调用接口范围' AFTER `purpose`");
    addColumnIfAbsent(
        "data_open_application",
        "api_key",
        "VARCHAR(128) DEFAULT NULL COMMENT '调用密钥' AFTER `status`");
    addColumnIfAbsent(
        "data_open_application",
        "last_used_at",
        "TIMESTAMP NULL DEFAULT NULL COMMENT '最近调用时间' AFTER `reviewed_at`");
    addColumnIfAbsent(
        "data_open_application",
        "call_count",
        "INT NOT NULL DEFAULT 0 COMMENT '调用次数' AFTER `last_used_at`");
    addIndexIfAbsent(
        "data_open_application",
        "uk_data_open_api_key",
        "UNIQUE KEY `uk_data_open_api_key` (`api_key`)");
    addIndexIfAbsent(
        "data_open_application",
        "idx_data_open_status_time",
        "KEY `idx_data_open_status_time` (`status`, `created_at`)");
	  }

	  private void ensureOauthTables() {
	    jdbcTemplate.execute(
	        """
	        CREATE TABLE IF NOT EXISTS `oauth_client`
	        (
	            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	            `client_id` VARCHAR(80) NOT NULL COMMENT '客户端ID',
	            `client_secret` VARCHAR(120) DEFAULT NULL COMMENT '客户端密钥BCrypt哈希，公开客户端可为空',
	            `client_name` VARCHAR(120) NOT NULL COMMENT '客户端名称',
	            `redirect_uris` TEXT NOT NULL COMMENT '允许回调地址，逗号分隔',
	            `scopes` VARCHAR(500) NOT NULL DEFAULT 'openid profile email' COMMENT '允许scope，空格分隔',
	            `grant_types` VARCHAR(300) NOT NULL DEFAULT 'authorization_code refresh_token' COMMENT '允许授权类型，空格分隔',
	            `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
	            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
	            PRIMARY KEY (`id`),
	            UNIQUE KEY `uk_oauth_client_id` (`client_id`)
	        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='OAuth/OIDC客户端'
	        """);
	    jdbcTemplate.execute(
	        """
	        CREATE TABLE IF NOT EXISTS `oauth_authorization_code`
	        (
	            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
	            `code` VARCHAR(160) NOT NULL COMMENT '授权码',
	            `client_id` VARCHAR(80) NOT NULL COMMENT '客户端ID',
	            `user_id` INT NOT NULL COMMENT '授权用户ID',
	            `redirect_uri` VARCHAR(500) NOT NULL COMMENT '回调地址',
	            `scope` VARCHAR(500) NOT NULL DEFAULT 'openid profile' COMMENT '授权scope',
	            `code_challenge` VARCHAR(160) DEFAULT NULL COMMENT 'PKCE challenge',
	            `code_challenge_method` VARCHAR(20) DEFAULT NULL COMMENT 'PKCE challenge方法',
	            `nonce` VARCHAR(160) DEFAULT NULL COMMENT 'OIDC nonce',
	            `state` VARCHAR(300) DEFAULT NULL COMMENT '客户端state',
	            `expires_at` TIMESTAMP NOT NULL COMMENT '过期时间',
	            `consumed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '消费时间',
	            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	            PRIMARY KEY (`id`),
	            UNIQUE KEY `uk_oauth_authorization_code` (`code`),
	            KEY `idx_oauth_authorization_code_client` (`client_id`),
	            KEY `idx_oauth_authorization_code_user` (`user_id`)
	        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='OAuth/OIDC授权码'
	        """);
	    addIndexIfAbsent(
	        "oauth_client", "uk_oauth_client_id", "UNIQUE KEY `uk_oauth_client_id` (`client_id`)");
	    seedDefaultOauthClient();
	    seedLabLmsOauthClient();
	    ensureDefaultOauthClientSettings();
	    seedOauthPermissions();
	  }

	  private void seedDefaultOauthClient() {
	    jdbcTemplate.update(
	        """
	        INSERT INTO `oauth_client` (`client_id`, `client_secret`, `client_name`, `redirect_uris`, `scopes`, `grant_types`)
	        SELECT 'openatom-web',
	               NULL,
	               'OpenAtom Web',
	               'https://jmi-openatom.cn/auth/callback,https://www.jmi-openatom.cn/auth/callback,http://localhost:18080/auth/callback,http://127.0.0.1:18080/auth/callback,http://localhost:5173/auth/callback,http://localhost:5175/auth/callback,http://127.0.0.1:5173/auth/callback,http://127.0.0.1:5175/auth/callback',
	               'openid profile email roles permissions',
	               'authorization_code refresh_token'
	        WHERE NOT EXISTS (SELECT 1 FROM `oauth_client` WHERE `client_id` = 'openatom-web')
	        """);
	  }

	  private void seedLabLmsOauthClient() {
	    jdbcTemplate.update(
	        """
	        INSERT INTO `oauth_client` (`client_id`, `client_secret`, `client_name`, `redirect_uris`, `scopes`, `grant_types`, `enabled`)
	        SELECT 'lab-lms',
	               NULL,
	               'OpenAtom Lab LMS',
	               'https://lms.jmi-openatom.cn/auth/callback,https://lab.jmi-openatom.cn/auth/callback,http://127.0.0.1:5174/auth/callback',
	               'openid profile email roles permissions',
	               'authorization_code refresh_token',
	               1
	        WHERE NOT EXISTS (SELECT 1 FROM `oauth_client` WHERE `client_id` = 'lab-lms')
	        """);
	    jdbcTemplate.update("UPDATE `oauth_client` SET `client_secret` = NULL, `enabled` = 1 WHERE `client_id` = 'lab-lms'");
	    appendOauthRedirectUriIfMissing("lab-lms", "https://lms.jmi-openatom.cn/auth/callback");
	    appendOauthRedirectUriIfMissing("lab-lms", "https://lab.jmi-openatom.cn/auth/callback");
	    appendOauthRedirectUriIfMissing("lab-lms", "http://127.0.0.1:5174/auth/callback");
	    appendOauthGrantTypeIfMissing("lab-lms", "authorization_code");
	    appendOauthGrantTypeIfMissing("lab-lms", "refresh_token");
	    appendOauthScopeIfMissing("lab-lms", "openid");
	    appendOauthScopeIfMissing("lab-lms", "profile");
	  }

	  private void ensureDefaultOauthClientSettings() {
	    appendOauthRedirectUriIfMissing("openatom-web", "https://jmi-openatom.cn/auth/callback");
	    appendOauthRedirectUriIfMissing("openatom-web", "https://www.jmi-openatom.cn/auth/callback");
	    appendOauthScopeIfMissing("openatom-web", "roles");
	    appendOauthScopeIfMissing("openatom-web", "permissions");
	  }

	  private void appendOauthRedirectUriIfMissing(String clientId, String redirectUri) {
	    int updated =
	        jdbcTemplate.update(
	            """
	            UPDATE `oauth_client`
	            SET `redirect_uris` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM `redirect_uris`), ''), ?)
	            WHERE `client_id` = ?
	              AND FIND_IN_SET(?, `redirect_uris`) = 0
	            """,
	            redirectUri,
	            clientId,
	            redirectUri);
	    if (updated > 0) {
	      log.info("Added OAuth redirect URI for {}: {}", clientId, redirectUri);
	    }
	  }

	  private void appendOauthScopeIfMissing(String clientId, String scope) {
	    int updated =
	        jdbcTemplate.update(
	            """
	            UPDATE `oauth_client`
	            SET `scopes` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`scopes`), ''), ''), ' ', ?))
	            WHERE `client_id` = ?
	              AND INSTR(CONCAT(' ', `scopes`, ' '), CONCAT(' ', ?, ' ')) = 0
	            """,
	            scope,
	            clientId,
	            scope);
	    if (updated > 0) {
	      log.info("Added OAuth scope for {}: {}", clientId, scope);
	    }
	  }

	  private void appendOauthGrantTypeIfMissing(String clientId, String grantType) {
	    int updated =
	        jdbcTemplate.update(
	            """
	            UPDATE `oauth_client`
	            SET `grant_types` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`grant_types`), ''), ''), ' ', ?))
	            WHERE `client_id` = ?
	              AND INSTR(CONCAT(' ', `grant_types`, ' '), CONCAT(' ', ?, ' ')) = 0
	            """,
	            grantType,
	            clientId,
	            grantType);
	    if (updated > 0) {
	      log.info("Added OAuth grant type for {}: {}", clientId, grantType);
	    }
	  }

	  private void seedOauthPermissions() {
	    jdbcTemplate.update(
	        """
	        INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
	        SELECT '查询认证应用', 'oauth-client:list', 'api', '/oauth/admin/clients', 'GET'
	        WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'oauth-client:list')
	        """);
	    jdbcTemplate.update(
	        """
	        INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
	        SELECT '管理认证应用', 'oauth-client:manage', 'api', '/oauth/admin/clients/**', 'POST'
	        WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'oauth-client:manage')
	        """);
	    jdbcTemplate.update(
	        """
	        INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
	        SELECT r.`id`, p.`id`
	        FROM `sys_role` r
	                 JOIN `sys_permission` p ON p.`code` IN ('oauth-client:list', 'oauth-client:manage')
	        WHERE r.`code` = 'super_admin'
	          AND NOT EXISTS (
	            SELECT 1
	            FROM `sys_role_permission` rp
	            WHERE rp.`role_id` = r.`id`
	              AND rp.`permission_id` = p.`id`
	        )
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

  private void ensureEveningStudyCheckInTables() {
    addColumnIfAbsent(
        "checkin_session",
        "schedule_id",
        "INT DEFAULT NULL COMMENT '晚自习计划ID' AFTER `group_id`");
    addColumnIfAbsent(
        "checkin_session",
        "session_type",
        "VARCHAR(40) NOT NULL DEFAULT 'regular' COMMENT '签到类型: regular/evening_study' AFTER `schedule_id`");
    addColumnIfAbsent(
        "checkin_session",
        "attendance_date",
        "DATE DEFAULT NULL COMMENT '考勤日期' AFTER `session_type`");
    addIndexIfAbsent(
        "checkin_session",
        "idx_checkin_session_type_date",
        "KEY `idx_checkin_session_type_date` (`session_type`, `attendance_date`)");
    addIndexIfAbsent(
        "checkin_session",
        "idx_checkin_session_schedule",
        "KEY `idx_checkin_session_schedule` (`schedule_id`)");
    addIndexIfAbsent(
        "checkin_session",
        "uk_checkin_evening_schedule_date",
        "UNIQUE KEY `uk_checkin_evening_schedule_date` (`schedule_id`, `attendance_date`, `session_type`)");

    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `evening_study_schedule`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `group_id` INT NOT NULL COMMENT '实验室签到分组ID',
            `title` VARCHAR(120) NOT NULL DEFAULT '晚自习签到' COMMENT '计划标题',
            `location` VARCHAR(120) DEFAULT NULL COMMENT '晚自习地点',
            `start_time` TIME NOT NULL DEFAULT '19:00:00' COMMENT '每日开始时间',
            `end_time` TIME NOT NULL DEFAULT '21:30:00' COMMENT '每日结束时间',
            `checkin_points` BIGINT NOT NULL DEFAULT 0 COMMENT '签到奖励积分',
            `checkin_window_minutes` INT NOT NULL DEFAULT 30 COMMENT '签到窗口分钟数',
            `late_after_minutes` INT NOT NULL DEFAULT 10 COMMENT '开始后超过多少分钟算迟到',
            `late_penalty_points` BIGINT NOT NULL DEFAULT 1 COMMENT '迟到扣分',
            `absent_penalty_points` BIGINT NOT NULL DEFAULT 2 COMMENT '旷课扣分',
            `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_evening_schedule_group` (`group_id`),
            KEY `idx_evening_schedule_club` (`club_id`),
            KEY `idx_evening_schedule_enabled` (`enabled`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='晚自习签到计划表'
        """);
    addColumnIfAbsent(
        "evening_study_schedule",
        "checkin_window_minutes",
        "INT NOT NULL DEFAULT 30 COMMENT '签到窗口分钟数' AFTER `checkin_points`");
    addColumnIfAbsent(
        "evening_study_schedule",
        "late_after_minutes",
        "INT NOT NULL DEFAULT 10 COMMENT '开始后超过多少分钟算迟到' AFTER `checkin_window_minutes`");
    addColumnIfAbsent(
        "evening_study_schedule",
        "late_penalty_points",
        "BIGINT NOT NULL DEFAULT 1 COMMENT '迟到扣分' AFTER `late_after_minutes`");
    addColumnIfAbsent(
        "evening_study_schedule",
        "absent_penalty_points",
        "BIGINT NOT NULL DEFAULT 2 COMMENT '旷课扣分' AFTER `late_penalty_points`");
    addColumnIfAbsent(
        "checkin_session",
        "checkin_window_minutes",
        "INT NOT NULL DEFAULT 30 COMMENT '签到窗口分钟数' AFTER `token`");
    addColumnIfAbsent(
        "checkin_session",
        "late_after_minutes",
        "INT NOT NULL DEFAULT 10 COMMENT '开始后超过多少分钟算迟到' AFTER `checkin_window_minutes`");
    addColumnIfAbsent(
        "checkin_session",
        "late_penalty_points",
        "BIGINT NOT NULL DEFAULT 1 COMMENT '迟到扣分' AFTER `late_after_minutes`");
    addColumnIfAbsent(
        "checkin_session",
        "absent_penalty_points",
        "BIGINT NOT NULL DEFAULT 2 COMMENT '旷课扣分' AFTER `late_penalty_points`");

    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `checkin_exclusion`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `session_id` INT NOT NULL COMMENT '签到场次ID',
            `user_id` INT NOT NULL COMMENT '被剔除用户ID',
            `source_type` VARCHAR(40) NOT NULL DEFAULT 'leave' COMMENT '剔除来源类型',
            `source_id` INT DEFAULT NULL COMMENT '来源记录ID',
            `reason` VARCHAR(255) DEFAULT NULL COMMENT '剔除原因',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_checkin_exclusion_user` (`session_id`, `user_id`),
            KEY `idx_checkin_exclusion_session` (`session_id`),
            KEY `idx_checkin_exclusion_user` (`user_id`),
            KEY `idx_checkin_exclusion_source` (`source_type`, `source_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到自动剔除记录表'
        """);
  }

  private void ensurePointSystemTables() {
    addColumnIfAbsent(
        "club_activity",
        "participation_points",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '参加活动奖励积分' AFTER `registration_fields`");
    addColumnIfAbsent(
        "checkin_session",
        "checkin_points",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '扫码签到奖励积分' AFTER `token`");
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `point_account`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `user_id` INT NOT NULL COMMENT '用户ID',
            `balance` BIGINT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
            `total_earned` BIGINT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
            `total_spent` BIGINT NOT NULL DEFAULT 0 COMMENT '累计兑换消耗积分',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_point_account_user` (`user_id`),
            KEY `idx_point_account_balance` (`balance`, `total_earned`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分账户表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `point_transaction`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `user_id` INT NOT NULL COMMENT '用户ID',
            `delta` BIGINT NOT NULL COMMENT '积分变动',
            `balance_after` BIGINT NOT NULL COMMENT '变动后余额',
            `type` VARCHAR(40) NOT NULL COMMENT '流水类型',
            `source_type` VARCHAR(60) DEFAULT NULL COMMENT '来源类型',
            `source_id` INT DEFAULT NULL COMMENT '来源ID',
            `source_key` VARCHAR(160) DEFAULT NULL COMMENT '幂等来源键',
            `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
            `operator_id` INT DEFAULT NULL COMMENT '操作人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            KEY `idx_point_transaction_user_time` (`user_id`, `created_at`),
            KEY `idx_point_transaction_source` (`user_id`, `source_key`),
            KEY `idx_point_transaction_type` (`type`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分流水表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `point_redeem_item`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `name` VARCHAR(120) NOT NULL COMMENT '兑换项名称',
            `description` VARCHAR(500) DEFAULT NULL COMMENT '兑换说明',
            `point_cost` BIGINT NOT NULL COMMENT '所需积分',
            `stock` INT DEFAULT NULL COMMENT '库存，空表示不限',
            `exchanged_count` INT NOT NULL DEFAULT 0 COMMENT '已兑换数量',
            `status` VARCHAR(30) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive',
            `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
            `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
            `created_by` INT DEFAULT NULL COMMENT '创建人',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_point_redeem_item_status` (`status`, `sort_order`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分兑换项表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `point_redemption`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `user_id` INT NOT NULL COMMENT '兑换用户ID',
            `item_id` INT NOT NULL COMMENT '兑换项ID',
            `points` BIGINT NOT NULL COMMENT '消耗积分',
            `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/fulfilled/cancelled/rejected',
            `receiver_name` VARCHAR(80) DEFAULT NULL COMMENT '领取人',
            `receiver_contact` VARCHAR(120) DEFAULT NULL COMMENT '联系方式',
            `remark` VARCHAR(500) DEFAULT NULL COMMENT '用户备注',
            `admin_note` VARCHAR(500) DEFAULT NULL COMMENT '后台备注',
            `reviewed_by` INT DEFAULT NULL COMMENT '处理人',
            `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '处理时间',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_point_redemption_user` (`user_id`, `created_at`),
            KEY `idx_point_redemption_item` (`item_id`),
            KEY `idx_point_redemption_status` (`status`, `created_at`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分兑换记录表'
        """);
    ensurePointAmountColumns();
  }

  private void ensurePointAmountColumns() {
    modifyColumnToBigIntIfNeeded(
        "club_activity",
        "participation_points",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '参加活动奖励积分'");
    modifyColumnToBigIntIfNeeded(
        "checkin_session",
        "checkin_points",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '扫码签到奖励积分'");
    modifyColumnToBigIntIfNeeded(
        "point_account",
        "balance",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '当前积分余额'");
    modifyColumnToBigIntIfNeeded(
        "point_account",
        "total_earned",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '累计获得积分'");
    modifyColumnToBigIntIfNeeded(
        "point_account",
        "total_spent",
        "BIGINT NOT NULL DEFAULT 0 COMMENT '累计兑换消耗积分'");
    modifyColumnToBigIntIfNeeded(
        "point_transaction",
        "delta",
        "BIGINT NOT NULL COMMENT '积分变动'");
    modifyColumnToBigIntIfNeeded(
        "point_transaction",
        "balance_after",
        "BIGINT NOT NULL COMMENT '变动后余额'");
    modifyColumnToBigIntIfNeeded(
        "point_redeem_item",
        "point_cost",
        "BIGINT NOT NULL COMMENT '所需积分'");
    modifyColumnToBigIntIfNeeded(
        "point_redemption",
        "points",
        "BIGINT NOT NULL COMMENT '消耗积分'");
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

  private void modifyColumnToBigIntIfNeeded(String tableName, String columnName, String definition) {
    if (!tableExists(tableName) || !columnExists(tableName, columnName)) {
      return;
    }
    String dataType =
        jdbcTemplate.queryForObject(
            """
            SELECT DATA_TYPE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
            """,
            String.class,
            tableName,
            columnName);
    if ("bigint".equalsIgnoreCase(dataType)) {
      return;
    }
    jdbcTemplate.execute(
        "ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName + "` " + definition);
    log.info("Modified column to BIGINT: {}.{}", tableName, columnName);
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
