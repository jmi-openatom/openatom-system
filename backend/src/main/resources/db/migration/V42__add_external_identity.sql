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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方身份绑定';
