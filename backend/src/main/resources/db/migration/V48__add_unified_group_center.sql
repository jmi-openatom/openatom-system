-- 统一分组中心：统一管理组织部门、签到分组、往届分组和外部群组。

CREATE TABLE IF NOT EXISTS `unified_group`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '统一分组ID',
    `club_id`       INT          NOT NULL COMMENT '所属社团ID',
    `parent_id`     BIGINT       DEFAULT NULL COMMENT '父分组ID',
    `name`          VARCHAR(160) NOT NULL COMMENT '分组名称',
    `code`          VARCHAR(120) DEFAULT NULL COMMENT '业务编码',
    `type`          VARCHAR(32)  NOT NULL COMMENT 'department/checkin/alumni/external/custom',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '分组描述',
    `status`        VARCHAR(30)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled/archived',
    `owner_user_id` INT          DEFAULT NULL COMMENT '负责人用户ID',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `source_type`   VARCHAR(40)  NOT NULL COMMENT '来源类型',
    `source_id`     VARCHAR(80)  NOT NULL COMMENT '来源主键',
    `config_json`   JSON         DEFAULT NULL COMMENT '类型扩展配置',
    `created_by`    INT          DEFAULT NULL COMMENT '创建人',
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unified_group_source` (`source_type`, `source_id`),
    KEY `idx_unified_group_club_type` (`club_id`, `type`, `status`),
    KEY `idx_unified_group_parent` (`parent_id`),
    KEY `idx_unified_group_owner` (`owner_user_id`),
    CONSTRAINT `fk_unified_group_parent` FOREIGN KEY (`parent_id`) REFERENCES `unified_group` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '统一分组表';

CREATE TABLE IF NOT EXISTS `unified_group_member`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`    BIGINT      NOT NULL COMMENT '统一分组ID',
    `user_id`     INT         NOT NULL COMMENT '系统用户ID',
    `member_role` VARCHAR(30) NOT NULL DEFAULT 'member' COMMENT 'owner/manager/vice_manager/member',
    `status`      VARCHAR(30) NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
    `joined_at`   TIMESTAMP   NULL DEFAULT NULL COMMENT '加入分组时间',
    `left_at`     TIMESTAMP   NULL DEFAULT NULL COMMENT '离开分组时间',
    `created_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unified_group_member` (`group_id`, `user_id`),
    KEY `idx_unified_group_member_user` (`user_id`, `status`),
    CONSTRAINT `fk_unified_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `unified_group` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '统一分组成员表';

CREATE TABLE IF NOT EXISTS `unified_group_binding`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`     BIGINT       NOT NULL COMMENT '统一分组ID',
    `binding_type` VARCHAR(40)  NOT NULL COMMENT 'department/checkin/alumni/qq_group/wechat_group',
    `external_id`  VARCHAR(160) NOT NULL COMMENT '业务或外部系统标识',
    `status`       VARCHAR(30)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
    `config_json`  JSON         DEFAULT NULL COMMENT '绑定扩展配置',
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unified_group_binding` (`group_id`, `binding_type`, `external_id`),
    KEY `idx_unified_binding_external` (`binding_type`, `external_id`),
    CONSTRAINT `fk_unified_group_binding_group` FOREIGN KEY (`group_id`) REFERENCES `unified_group` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '统一分组业务绑定表';

ALTER TABLE `club_membership`
    ADD COLUMN `alumni_group_id` INT DEFAULT NULL COMMENT '往届分组ID' AFTER `alumni_group`,
    ADD KEY `idx_membership_alumni_group_id` (`alumni_group_id`);

UPDATE `club_membership` membership
    JOIN `club_alumni_group` alumni_group
      ON alumni_group.`club_id` = membership.`club_id`
     AND alumni_group.`name` = membership.`alumni_group`
SET membership.`alumni_group_id` = alumni_group.`id`
WHERE membership.`alumni_group` IS NOT NULL;

INSERT INTO `unified_group`
    (`club_id`, `name`, `type`, `description`, `status`, `owner_user_id`, `source_type`, `source_id`, `created_at`)
SELECT department.`club_id`, department.`name`, 'department', department.`description`, 'active',
       department.`manager_user_id`, 'department', CAST(department.`id` AS CHAR), department.`created_at`
FROM `club_department` department
ON DUPLICATE KEY UPDATE
    `club_id` = VALUES(`club_id`), `name` = VALUES(`name`), `description` = VALUES(`description`),
    `owner_user_id` = VALUES(`owner_user_id`), `status` = 'active';

INSERT INTO `unified_group`
    (`club_id`, `name`, `type`, `status`, `source_type`, `source_id`, `created_by`, `created_at`)
SELECT checkin_group.`club_id`, checkin_group.`name`, 'checkin', 'active', 'checkin',
       CAST(checkin_group.`id` AS CHAR), checkin_group.`created_by`, checkin_group.`created_at`
FROM `checkin_group` checkin_group
ON DUPLICATE KEY UPDATE
    `club_id` = VALUES(`club_id`), `name` = VALUES(`name`), `status` = 'active';

INSERT INTO `unified_group`
    (`club_id`, `name`, `type`, `description`, `status`, `sort_order`, `source_type`, `source_id`, `created_at`)
SELECT alumni_group.`club_id`, alumni_group.`name`, 'alumni', alumni_group.`description`, 'active',
       alumni_group.`sort_order`, 'alumni', CAST(alumni_group.`id` AS CHAR), alumni_group.`created_at`
FROM `club_alumni_group` alumni_group
ON DUPLICATE KEY UPDATE
    `club_id` = VALUES(`club_id`), `name` = VALUES(`name`), `description` = VALUES(`description`),
    `sort_order` = VALUES(`sort_order`), `status` = 'active';

INSERT INTO `unified_group`
    (`club_id`, `name`, `type`, `description`, `status`, `source_type`, `source_id`, `config_json`, `created_at`)
SELECT default_club.`id`, COALESCE(bot_group.`group_name`, CONCAT('QQ群 ', bot_group.`group_id`)), 'external',
       CONCAT('QQ群号 ', bot_group.`group_id`), IF(bot_group.`bot_enabled` = 1, 'active', 'disabled'),
       'qq_group', bot_group.`group_id`, JSON_OBJECT('platform', 'qq', 'mode', bot_group.`mode`), bot_group.`created_at`
FROM `bot_group` bot_group
JOIN (SELECT COALESCE(MAX(CASE WHEN `code` = 'JMI-OPENATOM' THEN `id` END), MIN(`id`)) AS `id` FROM `club`) default_club
  ON default_club.`id` IS NOT NULL
ON DUPLICATE KEY UPDATE
    `club_id` = VALUES(`club_id`), `name` = VALUES(`name`), `description` = VALUES(`description`),
    `status` = VALUES(`status`), `config_json` = VALUES(`config_json`);

INSERT IGNORE INTO `unified_group_member` (`group_id`, `user_id`, `member_role`, `status`, `joined_at`, `left_at`)
SELECT unified_group.`id`, membership.`user_id`,
       CASE
         WHEN department.`manager_user_id` = membership.`user_id` THEN 'manager'
         WHEN department.`vice_manager_user_id` = membership.`user_id` THEN 'vice_manager'
         ELSE 'member'
       END,
       IF(membership.`left_at` IS NULL, 'active', 'inactive'), membership.`joined_at`, membership.`left_at`
FROM `club_membership` membership
JOIN `club_department` department ON department.`id` = membership.`department_id`
JOIN `unified_group` unified_group
  ON unified_group.`source_type` = 'department' AND unified_group.`source_id` = CAST(department.`id` AS CHAR);

INSERT IGNORE INTO `unified_group_member` (`group_id`, `user_id`, `member_role`, `status`, `joined_at`)
SELECT unified_group.`id`, checkin_member.`user_id`, 'member', 'active', checkin_member.`created_at`
FROM `checkin_group_member` checkin_member
JOIN `unified_group` unified_group
  ON unified_group.`source_type` = 'checkin' AND unified_group.`source_id` = CAST(checkin_member.`group_id` AS CHAR);

INSERT IGNORE INTO `unified_group_member` (`group_id`, `user_id`, `member_role`, `status`, `joined_at`, `left_at`)
SELECT unified_group.`id`, membership.`user_id`, 'member', 'active', membership.`joined_at`, membership.`left_at`
FROM `club_membership` membership
JOIN `unified_group` unified_group
  ON unified_group.`source_type` = 'alumni'
 AND unified_group.`source_id` = CAST(membership.`alumni_group_id` AS CHAR)
WHERE membership.`alumni_group_id` IS NOT NULL;

INSERT IGNORE INTO `unified_group_binding` (`group_id`, `binding_type`, `external_id`, `config_json`)
SELECT `id`, `source_type`, `source_id`, JSON_OBJECT('migrated', TRUE)
FROM `unified_group`;

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询统一分组', 'group:list', 'api', '/admin/groups', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'group:list');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查看统一分组详情', 'group:detail', 'api', '/admin/groups/{groupId}', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'group:detail');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建统一分组', 'group:create', 'api', '/admin/groups', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'group:create');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '同步统一分组', 'group:sync', 'api', '/admin/groups/sync', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'group:sync');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT role.`id`, permission.`id`
FROM `sys_role` role
JOIN `sys_permission` permission ON permission.`code` IN ('group:list', 'group:detail', 'group:create', 'group:sync')
WHERE role.`code` IN ('super_admin', 'club_admin')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` role_permission
      WHERE role_permission.`role_id` = role.`id` AND role_permission.`permission_id` = permission.`id`
  );

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT role.`id`, permission.`id`
FROM `sys_role` role
JOIN `sys_permission` permission ON permission.`code` IN ('group:list', 'group:detail')
WHERE role.`code` IN ('department_head', 'operations_lead')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` role_permission
      WHERE role_permission.`role_id` = role.`id` AND role_permission.`permission_id` = permission.`id`
  );
