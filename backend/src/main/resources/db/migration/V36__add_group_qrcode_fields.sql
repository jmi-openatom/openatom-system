ALTER TABLE `club`
    ADD COLUMN `wechat_group_qrcode` VARCHAR(500) NULL DEFAULT NULL COMMENT '社团总群微信群二维码URL'
    AFTER `league_secretary_user_id`,
    ADD COLUMN `qq_group_number`       VARCHAR(64)  NULL DEFAULT NULL COMMENT '社团QQ群号'
    AFTER `wechat_group_qrcode`;

ALTER TABLE `club_department`
    ADD COLUMN `wechat_group_qrcode` VARCHAR(500) NULL DEFAULT NULL COMMENT '部门微信群二维码URL'
    AFTER `vice_manager_user_id`;
