ALTER TABLE `bot_group_announcement`
    ADD COLUMN `notice_id` VARCHAR(120) DEFAULT NULL COMMENT 'NapCat群公告ID' AFTER `status`;

ALTER TABLE `bot_group_announcement`
    ADD INDEX `idx_bot_announcement_notice` (`notice_id`);
