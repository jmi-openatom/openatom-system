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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='QQ群消息发送任务表';
