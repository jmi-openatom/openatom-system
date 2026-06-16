CREATE TABLE IF NOT EXISTS `ai_activity_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` INT DEFAULT NULL,
  `user_id` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'drafting',
  `requirement_summary` JSON DEFAULT NULL,
  `confirmed_plan_id` BIGINT DEFAULT NULL,
  `activity_id` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_activity_session_user` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI活动自动化会话表';

ALTER TABLE `system_setting`
  MODIFY COLUMN `setting_value` VARCHAR(1000) DEFAULT NULL COMMENT '配置值';

CREATE TABLE IF NOT EXISTS `ai_activity_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `role` VARCHAR(32) NOT NULL COMMENT 'user/assistant/system',
  `content` MEDIUMTEXT NOT NULL,
  `structured_payload` JSON DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_activity_message_session` (`session_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI活动自动化对话消息表';

CREATE TABLE IF NOT EXISTS `ai_activity_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `title` VARCHAR(255) NOT NULL,
  `content_markdown` MEDIUMTEXT NOT NULL,
  `structured_fields` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_activity_plan_session` (`session_id`, `version`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI活动策划案表';

CREATE TABLE IF NOT EXISTS `document_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` INT DEFAULT NULL,
  `template_type` VARCHAR(64) NOT NULL,
  `template_name` VARCHAR(128) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `file_path` VARCHAR(512) NOT NULL,
  `original_file_name` VARCHAR(255) DEFAULT NULL,
  `variables` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'enabled',
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_document_template_type` (`template_type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档模板表';

CREATE TABLE IF NOT EXISTS `generated_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `template_id` BIGINT NOT NULL,
  `document_type` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(512) NOT NULL,
  `filled_variables` JSON DEFAULT NULL,
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_generated_document_session` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI生成文档表';

CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `scene` VARCHAR(64) NOT NULL,
  `provider` VARCHAR(32) NOT NULL DEFAULT 'deepseek',
  `model` VARCHAR(64) NOT NULL,
  `prompt_version` VARCHAR(64) DEFAULT NULL,
  `request_summary` VARCHAR(1000) DEFAULT NULL,
  `response_summary` VARCHAR(1000) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_call_log_scene` (`scene`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI调用日志表';
