INSERT INTO `sys_role` (`name`, `code`, `data_scope`, `description`)
SELECT '运营负责人', 'operations_lead', 'club', '发布活动和签到，查看后台运营数据，不管理成员和高危删除操作'
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `code` = 'operations_lead');

UPDATE `sys_role`
SET `name` = '运营负责人',
    `data_scope` = 'club',
    `description` = '发布活动和签到，查看后台运营数据，不管理成员和高危删除操作'
WHERE `code` = 'operations_lead';

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` IN (
                                                 'auth:me',
                                                 'auth:logout',
                                                 'club:list',
                                                 'club:detail',
                                                 'department:list',
                                                 'department:detail',
                                                 'position:list',
                                                 'position:detail',
                                                 'user:list',
                                                 'user:info',
                                                 'user:membership:list',
                                                 'recruitment-campaign:list',
                                                 'recruitment-campaign:detail',
                                                 'application:list',
                                                 'application:detail',
                                                 'approval-record:list',
                                                 'interview:list',
                                                 'interview:detail',
                                                 'membership:list',
                                                 'membership:detail',
                                                 'exit-application:list',
                                                 'exit-application:detail',
                                                 'activity:list',
                                                 'activity:create',
                                                 'activity:detail',
                                                 'activity:update',
                                                 'activity-registration:list',
                                                 'check-in:list',
                                                 'check-in:detail',
                                                 'check-in:create',
                                                 'check-in:update',
                                                 'check-in:records',
                                                 'leave-application:list',
                                                 'leave-application:detail',
                                                 'site-form:list',
                                                 'site-form:detail',
                                                 'lottery:list',
                                                 'lottery:detail',
                                                 'vote:list',
                                                 'vote:detail',
                                                 'point:account:list',
                                                 'point:transaction:list',
                                                 'point:item:list',
                                                 'point:redemption:list',
                                                 'award:list',
                                                 'document:list',
                                                 'image:list',
                                                 'showcase-app:list',
                                                 'data-open:list',
                                                 'blog:list',
                                                 'blog-comment:list',
                                                 'blog-interaction:list',
                                                 'notification:list',
                                                 'bot-management:list',
                                                 'bot-management:detail',
                                                 'bot-management:members',
                                                 'bot-management:statistics'
    )
WHERE r.`code` = 'operations_lead'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
);
