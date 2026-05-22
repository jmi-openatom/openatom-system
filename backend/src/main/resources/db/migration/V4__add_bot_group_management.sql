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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='机器人账号表';

INSERT INTO `bot_account` (`id`, `account_id`, `nickname`, `platform`, `status`, `enabled`)
VALUES (1, 'default', '默认 QQ 机器人', 'qq', 'unknown', 1)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='机器人QQ群表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群成员缓存表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群机器人配置表';

CREATE TABLE IF NOT EXISTS `bot_group_announcement`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id` VARCHAR(80) NOT NULL COMMENT 'QQ群号',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告正文',
    `attachments` JSON DEFAULT NULL COMMENT '附件列表',
    `status` VARCHAR(30) DEFAULT 'draft' COMMENT '发布状态',
    `result_message` VARCHAR(500) DEFAULT NULL COMMENT '发布结果',
    `published_by` INT DEFAULT NULL COMMENT '发布人',
    `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_bot_announcement_group` (`group_id`),
    KEY `idx_bot_announcement_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群公告记录表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群入群申请表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群敏感词表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群自动审核规则表';

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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群消息统计表';
