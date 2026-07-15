CREATE TABLE IF NOT EXISTS `member_profile_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `profile_user_id` INT NOT NULL COMMENT '被点赞的成员用户ID',
    `user_id` INT NOT NULL COMMENT '点赞用户ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_profile_like_user` (`profile_user_id`, `user_id`),
    KEY `idx_member_profile_like_target` (`profile_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员主页点赞';

CREATE TABLE IF NOT EXISTS `member_profile_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `profile_user_id` INT NOT NULL COMMENT '被评论的成员用户ID',
    `user_id` INT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
    `content` VARCHAR(1000) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'visible',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_profile_comment_target` (`profile_user_id`, `status`, `id`),
    KEY `idx_member_profile_comment_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员主页评论';
