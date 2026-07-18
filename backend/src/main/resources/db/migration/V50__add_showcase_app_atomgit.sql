ALTER TABLE `showcase_app`
    ADD COLUMN `atomgit_url` VARCHAR(500) DEFAULT NULL COMMENT 'AtomGit仓库地址' AFTER `gitee_url`;
