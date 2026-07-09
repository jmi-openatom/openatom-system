ALTER TABLE `tb_user`
    ADD COLUMN `qq_group_joined_at` TIMESTAMP NULL DEFAULT NULL COMMENT '加入QQ群时间，NULL 表示尚未加入QQ群'
    AFTER `activated_at`;
