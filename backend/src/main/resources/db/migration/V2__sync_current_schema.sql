-- 将历史环境补齐到当前应用所需结构。
-- 该迁移必须兼容“已经存在业务数据、但还没有 Flyway 历史表”的老库。

CREATE TABLE IF NOT EXISTS `system_setting`
(
    `setting_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `setting_value` VARCHAR(255) DEFAULT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`setting_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表';

INSERT INTO `system_setting` (`setting_key`, `setting_value`, `description`)
SELECT 'register_enabled', 'false', '是否开放用户自助注册'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_setting` WHERE `setting_key` = 'register_enabled'
);

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user' AND COLUMN_NAME = 'class_name'
    ),
    'ALTER TABLE `tb_user` ADD COLUMN `class_name` VARCHAR(100) DEFAULT NULL COMMENT ''班级'' AFTER `grade`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user' AND COLUMN_NAME = 'miniapp_openid'
    ),
    'ALTER TABLE `tb_user` ADD COLUMN `miniapp_openid` VARCHAR(80) DEFAULT NULL COMMENT ''微信小程序openid'' AFTER `avatar`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user' AND COLUMN_NAME = 'wechat_unionid'
    ),
    'ALTER TABLE `tb_user` ADD COLUMN `wechat_unionid` VARCHAR(80) DEFAULT NULL COMMENT ''微信unionid'' AFTER `miniapp_openid`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_user' AND INDEX_NAME = 'uk_miniapp_openid'
    ),
    'ALTER TABLE `tb_user` ADD UNIQUE KEY `uk_miniapp_openid` (`miniapp_openid`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `tb_user`
SET `password` = 'ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78'
WHERE `user_name` = 'admin'
  AND `password` = '240be518fabd2724bfcdd75c7315e70b8a97ef591dce6c584b77f21cd2e40dbb';

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recruitment_campaign'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recruitment_campaign' AND COLUMN_NAME = 'login_required'
    ),
    'ALTER TABLE `recruitment_campaign` ADD COLUMN `login_required` TINYINT(1) DEFAULT 1 COMMENT ''是否要求登录后提交'' AFTER `max_applicants`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recruitment_campaign'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recruitment_campaign' AND COLUMN_NAME = 'form_schema'
    ),
    'ALTER TABLE `recruitment_campaign` ADD COLUMN `form_schema` JSON DEFAULT NULL COMMENT ''自定义报名表单结构'' AFTER `login_required`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='信息收集表单表';

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_form' AND COLUMN_NAME = 'start_at'
    ),
    'ALTER TABLE `site_form` ADD COLUMN `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT ''收集开始时间''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_form' AND COLUMN_NAME = 'end_at'
    ),
    'ALTER TABLE `site_form` ADD COLUMN `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT ''收集结束时间''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_form' AND COLUMN_NAME = 'login_required'
    ),
    'ALTER TABLE `site_form` ADD COLUMN `login_required` TINYINT(1) DEFAULT 1 COMMENT ''是否要求登录后提交''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_form' AND COLUMN_NAME = 'form_schema'
    ),
    'ALTER TABLE `site_form` ADD COLUMN `form_schema` JSON DEFAULT NULL COMMENT ''自定义表单结构''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'site_form' AND COLUMN_NAME = 'updated_at'
    ),
    'ALTER TABLE `site_form` ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'membership_application'
    ),
    'ALTER TABLE `membership_application` MODIFY COLUMN `user_id` INT NULL COMMENT ''申请用户ID，匿名提交时可为空''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通用表单提交记录表';

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'form_submission' AND COLUMN_NAME = 'campaign_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'form_submission' AND COLUMN_NAME = 'form_id'
    ),
    'ALTER TABLE `form_submission` CHANGE COLUMN `campaign_id` `form_id` INT NOT NULL COMMENT ''表单ID''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'form_submission' AND INDEX_NAME = 'idx_form_submission_form'
    ),
    'ALTER TABLE `form_submission` ADD INDEX `idx_form_submission_form` (`form_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'form_submission' AND INDEX_NAME = 'idx_form_submission_campaign'
    ),
    'ALTER TABLE `form_submission` DROP INDEX `idx_form_submission_campaign`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到场次表';

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkin_session' AND COLUMN_NAME = 'group_id'
    ),
    'ALTER TABLE `checkin_session` ADD COLUMN `group_id` INT DEFAULT NULL COMMENT ''签到分组ID'' AFTER `activity_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'checkin_session' AND INDEX_NAME = 'idx_checkin_session_group'
    ),
    'ALTER TABLE `checkin_session` ADD KEY `idx_checkin_session_group` (`group_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `checkin_group`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `name` VARCHAR(120) NOT NULL COMMENT '分组名称',
    `created_by` INT DEFAULT NULL COMMENT '创建人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_checkin_group_club` (`club_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到分组表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到分组成员表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到发放人员表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到记录表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='请假申请表';

CREATE TABLE IF NOT EXISTS `school_calendar_setting`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `start_date` DATE NOT NULL COMMENT '开学日期',
    `week_count` INT NOT NULL COMMENT '学期周数',
    `updated_by` INT DEFAULT NULL COMMENT '更新人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='校历基础设置表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='校历调休表';

CREATE TABLE IF NOT EXISTS `office_document`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `doc_type` VARCHAR(50) NOT NULL COMMENT '文书类型',
    `title` VARCHAR(255) NOT NULL COMMENT '文书标题',
    `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态',
    `payload` MEDIUMTEXT DEFAULT NULL COMMENT '文书内容/配置',
    `created_by` INT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` INT DEFAULT NULL COMMENT '更新人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_doc_club_id` (`club_id`),
    KEY `idx_doc_type` (`doc_type`),
    KEY `idx_doc_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='办公文书表';

SET @sql := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_log'
    ),
    'ALTER TABLE `operation_log`
        MODIFY COLUMN `module` VARCHAR(50) DEFAULT NULL COMMENT ''模块'',
        MODIFY COLUMN `action` VARCHAR(120) DEFAULT NULL COMMENT ''动作'',
        MODIFY COLUMN `target_id` VARCHAR(80) DEFAULT NULL COMMENT ''目标ID''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
