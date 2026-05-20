ALTER TABLE `leave_application`
    ADD COLUMN `bot_notify_origin` VARCHAR(255) DEFAULT NULL COMMENT '机器人通知会话来源' AFTER `reviewed_at`,
    ADD COLUMN `bot_notify_user_id` VARCHAR(80) DEFAULT NULL COMMENT '机器人通知用户ID' AFTER `bot_notify_origin`,
    ADD COLUMN `bot_notified_at` TIMESTAMP NULL DEFAULT NULL COMMENT '机器人通知时间' AFTER `bot_notify_user_id`;
