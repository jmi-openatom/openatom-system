ALTER TABLE `blog_article`
    ADD COLUMN `favorite_count` INT DEFAULT 0 COMMENT '收藏数' AFTER `like_count`,
    ADD COLUMN `share_count` INT DEFAULT 0 COMMENT '分享数' AFTER `favorite_count`;

ALTER TABLE `blog_comment`
    ADD COLUMN `parent_id` INT DEFAULT NULL COMMENT '父评论ID' AFTER `user_id`,
    ADD KEY `idx_blog_comment_parent` (`parent_id`);

CREATE TABLE IF NOT EXISTS `blog_article_interaction`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` INT NOT NULL COMMENT '文章ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `interaction_type` VARCHAR(30) NOT NULL COMMENT '互动类型: like/favorite/share',
    `channel` VARCHAR(40) DEFAULT NULL COMMENT '互动来源渠道',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_interaction_article` (`article_id`),
    KEY `idx_blog_interaction_user_type` (`user_id`, `interaction_type`),
    KEY `idx_blog_interaction_type_time` (`interaction_type`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='博客互动记录表';
