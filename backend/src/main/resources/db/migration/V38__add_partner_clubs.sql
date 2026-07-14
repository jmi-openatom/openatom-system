CREATE TABLE IF NOT EXISTS `partner_club`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '社团名称',
    `logo_url` VARCHAR(500) NOT NULL COMMENT 'Logo 地址',
    `description` VARCHAR(300) NOT NULL COMMENT '社团简介',
    `website_url` VARCHAR(500) NOT NULL COMMENT '官方网站或主页',
    `organization` VARCHAR(160) DEFAULT NULL COMMENT '所属学校或组织',
    `category` VARCHAR(80) DEFAULT NULL COMMENT '合作类型',
    `tags` JSON DEFAULT NULL COMMENT '技术方向标签',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示排序，数值越小越靠前',
    `featured` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首页推荐',
    `status` VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/disabled',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_partner_club_public` (`status`, `featured`, `sort_order`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='开源伙伴展示表';

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询开源伙伴', 'partner-club:list', 'api', '/partner-clubs', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'partner-club:list');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '新增开源伙伴', 'partner-club:create', 'api', '/partner-clubs', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'partner-club:create');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '编辑开源伙伴', 'partner-club:update', 'api', '/partner-clubs/{partnerClubId}', 'PATCH'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'partner-club:update');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新开源伙伴状态', 'partner-club:status:update', 'api', '/partner-clubs/{partnerClubId}/status', 'PATCH'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'partner-club:status:update');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除开源伙伴', 'partner-club:delete', 'api', '/partner-clubs/{partnerClubId}', 'DELETE'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'partner-club:delete');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN (
                'partner-club:list',
                'partner-club:create',
                'partner-club:update',
                'partner-club:status:update',
                'partner-club:delete'
              )
WHERE r.`code` IN ('super_admin', 'club_admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
  );

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` = 'partner-club:list'
WHERE r.`code` = 'operations_lead'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
  );
