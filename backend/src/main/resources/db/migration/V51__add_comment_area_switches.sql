ALTER TABLE `blog_article`
    ADD COLUMN `comments_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开放评论区' AFTER `featured`;

ALTER TABLE `member_profile`
    ADD COLUMN `comments_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开放评论区' AFTER `status`;
