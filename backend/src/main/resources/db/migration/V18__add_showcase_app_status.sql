ALTER TABLE `showcase_app`
    ADD COLUMN `app_status` VARCHAR(80) DEFAULT NULL COMMENT '应用展示状态' AFTER `version`;
