ALTER TABLE `tb_user`
    ADD COLUMN `google_sub` VARCHAR(128) DEFAULT NULL COMMENT 'Google OIDC subject' AFTER `qq_openid`;

CREATE UNIQUE INDEX `uk_google_sub` ON `tb_user` (`google_sub`);
