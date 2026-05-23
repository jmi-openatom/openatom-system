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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群每日活跃成员去重表';
