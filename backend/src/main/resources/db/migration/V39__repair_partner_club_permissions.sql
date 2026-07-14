-- V38 may already have run before the granular permission bindings were finalized.
-- Keep this migration idempotent so deployed databases receive the complete RBAC matrix.

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

-- 超级管理员和社团管理员拥有完整管理权限。
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

-- 运营负责人可维护展示内容，但不授予高风险删除权限。
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN (
                'partner-club:list',
                'partner-club:create',
                'partner-club:update',
                'partner-club:status:update'
              )
WHERE r.`code` = 'operations_lead'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
  );
