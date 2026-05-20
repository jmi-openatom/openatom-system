ALTER TABLE `tb_user`
    ADD COLUMN `qq_openid` VARCHAR(80) DEFAULT NULL COMMENT 'QQ号/QQ OpenID' AFTER `wechat_unionid`;

ALTER TABLE `tb_user`
    ADD UNIQUE KEY `uk_qq_openid` (`qq_openid`);
