CREATE TABLE `club_vice_president`
(
    `id`         INT       NOT NULL AUTO_INCREMENT,
    `club_id`    INT       NOT NULL COMMENT '社团ID',
    `user_id`    INT       NOT NULL COMMENT '副社长用户ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_club_user` (`club_id`, `user_id`),
    KEY `idx_club_id` (`club_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='社团副社长';

ALTER TABLE `club`
    ADD COLUMN `league_secretary_user_id` INT NULL DEFAULT NULL COMMENT '团支书用户ID'
    AFTER `vice_president_user_id`;

ALTER TABLE `club_department`
    ADD COLUMN `vice_manager_user_id` INT NULL DEFAULT NULL COMMENT '副部长用户ID'
    AFTER `manager_user_id`;
