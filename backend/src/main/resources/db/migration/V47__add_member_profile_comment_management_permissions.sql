-- 为成员主页评论后台增加独立的查看与管理权限。
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询成员主页评论', 'member-profile-comment:list', 'api', '/member-profile-comments', 'GET'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_permission` WHERE `code` = 'member-profile-comment:list'
);

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '管理成员主页评论', 'member-profile-comment:manage', 'api', '/member-profile-comments/{commentId}/status', 'PATCH'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_permission` WHERE `code` = 'member-profile-comment:manage'
);

-- 系统管理员、社团管理员和运营负责人可查看并审核主页评论。
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p
  ON p.`code` IN ('member-profile-comment:list', 'member-profile-comment:manage')
WHERE r.`code` IN ('super_admin', 'club_admin', 'operations_lead')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
  );
