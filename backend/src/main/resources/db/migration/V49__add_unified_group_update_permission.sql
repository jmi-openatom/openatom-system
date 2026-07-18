INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新统一分组', 'group:update', 'api', '/admin/groups/{groupId}', 'PUT'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'group:update');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT role.`id`, permission.`id`
FROM `sys_role` role
JOIN `sys_permission` permission ON permission.`code` = 'group:update'
WHERE role.`code` IN ('super_admin', 'club_admin')
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` role_permission
      WHERE role_permission.`role_id` = role.`id` AND role_permission.`permission_id` = permission.`id`
  );
