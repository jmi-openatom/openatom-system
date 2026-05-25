CREATE TABLE IF NOT EXISTS `blog_article`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `author_id` INT NOT NULL COMMENT '作者用户ID',
    `title` VARCHAR(180) NOT NULL COMMENT '文章标题',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    `content_markdown` LONGTEXT NOT NULL COMMENT 'Markdown正文',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    `category` VARCHAR(80) DEFAULT NULL COMMENT '分类',
    `tags` JSON DEFAULT NULL COMMENT '标签列表',
    `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态: draft/published/hidden/rejected',
    `featured` TINYINT(1) DEFAULT 0 COMMENT '是否推荐',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回或隐藏原因',
    `reviewed_by` INT DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
    `published_at` TIMESTAMP NULL DEFAULT NULL COMMENT '发布时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_article_club` (`club_id`),
    KEY `idx_blog_article_author` (`author_id`),
    KEY `idx_blog_article_status_publish` (`status`, `published_at`),
    KEY `idx_blog_article_category` (`category`),
    KEY `idx_blog_article_featured` (`featured`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='博客文章表';

CREATE TABLE IF NOT EXISTS `blog_comment`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` INT NOT NULL COMMENT '文章ID',
    `user_id` INT NOT NULL COMMENT '评论用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `status` VARCHAR(30) DEFAULT 'visible' COMMENT '状态: visible/hidden',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_comment_article` (`article_id`),
    KEY `idx_blog_comment_user` (`user_id`),
    KEY `idx_blog_comment_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='博客评论表';
