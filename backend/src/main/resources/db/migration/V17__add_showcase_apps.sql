CREATE TABLE IF NOT EXISTS `showcase_app`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(160) NOT NULL COMMENT '应用名称',
    `summary` VARCHAR(800) DEFAULT NULL COMMENT '应用简介',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面或截图URL',
    `open_source` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已开源',
    `github_url` VARCHAR(500) DEFAULT NULL COMMENT 'GitHub仓库地址',
    `gitee_url` VARCHAR(500) DEFAULT NULL COMMENT 'Gitee仓库地址',
    `developer` VARCHAR(160) DEFAULT NULL COMMENT '开发者',
    `owner` VARCHAR(160) DEFAULT NULL COMMENT '所有者',
    `license_name` VARCHAR(120) DEFAULT NULL COMMENT '开源协议',
    `version` VARCHAR(80) DEFAULT NULL COMMENT '版本号',
    `download_url` VARCHAR(500) DEFAULT NULL COMMENT '下载链接',
    `status` VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/hidden',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示排序',
    `created_by` INT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` INT DEFAULT NULL COMMENT '更新人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_showcase_app_status_sort` (`status`, `sort_order`, `id`),
    KEY `idx_showcase_app_open_source` (`open_source`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='应用展示表';

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询应用展示', 'showcase-app:list', 'api', '/showcase-apps', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'showcase-app:list');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '管理应用展示', 'showcase-app:manage', 'api', '/showcase-apps/**', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'showcase-app:manage');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除应用展示', 'showcase-app:delete', 'api', '/showcase-apps/{appId}', 'DELETE'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'showcase-app:delete');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN ('showcase-app:list', 'showcase-app:manage', 'showcase-app:delete')
WHERE r.`code` IN ('super_admin', 'club_admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
);
