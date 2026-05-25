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
    `bot_notify_origin` VARCHAR(255) DEFAULT NULL COMMENT '机器人通知会话来源',
    `bot_notify_user_id` VARCHAR(80) DEFAULT NULL COMMENT '机器人通知用户ID',
    `bot_notified_at` TIMESTAMP NULL DEFAULT NULL COMMENT '机器人通知时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_leave_application_club` (`club_id`),
    KEY `idx_leave_application_user` (`user_id`),
    KEY `idx_leave_application_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='请假申请表';
