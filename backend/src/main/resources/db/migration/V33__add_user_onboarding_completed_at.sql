ALTER TABLE `tb_user`
    ADD COLUMN `onboarding_completed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '入社介绍完成时间'
    AFTER `last_login_at`;
