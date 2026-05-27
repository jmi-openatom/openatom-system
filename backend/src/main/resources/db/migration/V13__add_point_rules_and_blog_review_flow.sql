INSERT INTO `system_setting` (`setting_key`, `setting_value`, `description`)
SELECT 'point.daily_login_points',
       '1',
       '每日首次登录奖励积分'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_setting` WHERE `setting_key` = 'point.daily_login_points'
);

INSERT INTO `system_setting` (`setting_key`, `setting_value`, `description`)
SELECT 'point.blog_publish_points',
       '20',
       '博客审核通过奖励积分'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_setting` WHERE `setting_key` = 'point.blog_publish_points'
);
