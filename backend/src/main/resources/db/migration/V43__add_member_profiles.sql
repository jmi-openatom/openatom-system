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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员公开主页';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员主页组件';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员主页公开链接';
