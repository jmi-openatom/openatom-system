ALTER TABLE `tb_user`
    ADD COLUMN `activated_at` TIMESTAMP NULL DEFAULT NULL COMMENT '账号激活时间，NULL 表示未激活'
    AFTER `onboarding_completed_at`;

ALTER TABLE `club`
    ADD COLUMN `vice_president_user_id` INT NULL DEFAULT NULL COMMENT '副社长用户ID'
    AFTER `president_user_id`;
