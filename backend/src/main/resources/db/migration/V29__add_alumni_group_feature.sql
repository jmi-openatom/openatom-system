-- 往届管理人员分组功能

-- 1. 给 club_membership 表添加 alumni_group 列
ALTER TABLE `club_membership` ADD COLUMN `alumni_group` VARCHAR(100) DEFAULT NULL COMMENT '往届管理人员分组名称' AFTER `left_at`;

-- 2. 创建往届管理人员分组表
CREATE TABLE IF NOT EXISTS `club_alumni_group`
(
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id`     INT          NOT NULL COMMENT '社团ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '分组名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '分组描述',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `created_at`  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_club_alumni_group_name` (`club_id`, `name`),
    INDEX `idx_club_alumni_group_club` (`club_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '往届管理人员分组';
