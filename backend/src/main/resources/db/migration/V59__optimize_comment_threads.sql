-- 将博客与成员主页评论统一为二层讨论串，并补齐互动与举报能力。
ALTER TABLE `blog_comment`
    ADD COLUMN `root_id` INT DEFAULT NULL COMMENT '所属一级评论ID' AFTER `parent_id`,
    ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `status`,
    ADD INDEX `idx_blog_comment_thread` (`article_id`, `status`, `root_id`, `id`);

ALTER TABLE `member_profile_comment`
    ADD COLUMN `root_id` BIGINT DEFAULT NULL COMMENT '所属一级评论ID' AFTER `parent_id`,
    ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `status`,
    ADD INDEX `idx_member_profile_comment_thread` (`profile_user_id`, `status`, `root_id`, `id`);

-- MySQL 8 递归回填历史多级回复的一级根评论。
WITH RECURSIVE blog_threads AS (
    SELECT id, parent_id, id AS root_id
    FROM blog_comment
    WHERE parent_id IS NULL
    UNION ALL
    SELECT child.id, child.parent_id, parent.root_id
    FROM blog_comment child
    JOIN blog_threads parent ON child.parent_id = parent.id
)
UPDATE blog_comment bc
JOIN blog_threads thread ON thread.id = bc.id
SET bc.root_id = IF(bc.parent_id IS NULL, NULL, thread.root_id);

WITH RECURSIVE profile_threads AS (
    SELECT id, parent_id, id AS root_id
    FROM member_profile_comment
    WHERE parent_id IS NULL
    UNION ALL
    SELECT child.id, child.parent_id, parent.root_id
    FROM member_profile_comment child
    JOIN profile_threads parent ON child.parent_id = parent.id
)
UPDATE member_profile_comment mpc
JOIN profile_threads thread ON thread.id = mpc.id
SET mpc.root_id = IF(mpc.parent_id IS NULL, NULL, thread.root_id);

CREATE TABLE `comment_interaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `target_type` VARCHAR(32) NOT NULL COMMENT 'blog或member_profile',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` INT NOT NULL COMMENT '操作用户ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_comment_interaction_user` (`target_type`, `comment_id`, `user_id`),
    KEY `idx_comment_interaction_comment` (`target_type`, `comment_id`),
    KEY `idx_comment_interaction_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞记录';

CREATE TABLE `comment_report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `target_type` VARCHAR(32) NOT NULL COMMENT 'blog或member_profile',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `reporter_user_id` INT NOT NULL COMMENT '举报用户ID',
    `reason` VARCHAR(500) NOT NULL COMMENT '举报原因',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending或resolved',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_comment_report_user` (`target_type`, `comment_id`, `reporter_user_id`),
    KEY `idx_comment_report_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论举报记录';

-- 同一管理权限覆盖单条和批量状态接口。
UPDATE `sys_permission`
SET `path` = '/blog/admin/comments/**'
WHERE `code` = 'blog-comment:manage';

UPDATE `sys_permission`
SET `path` = '/member-profile-comments/**'
WHERE `code` = 'member-profile-comment:manage';
