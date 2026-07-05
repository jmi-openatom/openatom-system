-- 新增成员删除权限

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除成员', 'membership:delete', 'api', '/memberships/{membershipId}', 'DELETE'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:delete');

-- 绑定 super_admin 和 club_admin 删除权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` = 'membership:delete'
WHERE r.`code` IN ('super_admin', 'club_admin');
