ALTER TABLE `partner_club`
    ADD COLUMN `president_user_id` INT NULL COMMENT '绑定的站内社长用户ID' AFTER `category`,
    ADD INDEX `idx_partner_club_president_user` (`president_user_id`),
    DROP COLUMN `president_name`,
    DROP COLUMN `president_avatar_url`;
