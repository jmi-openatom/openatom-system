CREATE TABLE IF NOT EXISTS `vote_campaign`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `title` VARCHAR(160) NOT NULL COMMENT '投票标题',
    `description` VARCHAR(800) DEFAULT NULL COMMENT '投票说明',
    `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态: draft/open/closed',
    `vote_type` VARCHAR(30) DEFAULT 'single' COMMENT '投票类型: single/multiple',
    `max_choices` INT NOT NULL DEFAULT 1 COMMENT '最多可选数量',
    `anonymous_allowed` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许匿名投票',
    `result_visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '前台是否公开结果',
    `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '投票开始时间',
    `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '投票结束时间',
    `created_by` INT DEFAULT NULL COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vote_campaign_club` (`club_id`),
    KEY `idx_vote_campaign_status` (`status`),
    KEY `idx_vote_campaign_time` (`start_at`, `end_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='投票活动表';

CREATE TABLE IF NOT EXISTS `vote_option`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `vote_id` INT NOT NULL COMMENT '投票活动ID',
    `title` VARCHAR(160) NOT NULL COMMENT '选项标题',
    `description` VARCHAR(800) DEFAULT NULL COMMENT '选项说明',
    `sort_order` INT DEFAULT 0 COMMENT '展示排序',
    `color` VARCHAR(40) DEFAULT NULL COMMENT '展示颜色',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vote_option_vote` (`vote_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='投票选项表';

CREATE TABLE IF NOT EXISTS `vote_record`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `vote_id` INT NOT NULL COMMENT '投票活动ID',
    `user_id` INT DEFAULT NULL COMMENT '投票用户ID',
    `voter_name` VARCHAR(120) DEFAULT NULL COMMENT '投票人姓名',
    `voter_contact` VARCHAR(160) DEFAULT NULL COMMENT '投票人联系方式',
    `voter_key` VARCHAR(180) NOT NULL COMMENT '投票人唯一键',
    `selected_option_ids` JSON NOT NULL COMMENT '已选选项ID列表',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `voted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vote_record_voter` (`vote_id`, `voter_key`),
    KEY `idx_vote_record_vote` (`vote_id`),
    KEY `idx_vote_record_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='投票记录表';

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询投票活动', 'vote:list', 'api', '/votes', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'vote:list');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建投票活动', 'vote:create', 'api', '/clubs/{clubId}/votes', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'vote:create');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取投票详情', 'vote:detail', 'api', '/votes/{voteId}', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'vote:detail');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新投票活动', 'vote:update', 'api', '/votes/{voteId}', 'PATCH'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'vote:update');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '管理投票记录', 'vote:manage-records', 'api', '/votes/{voteId}/reset', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'vote:manage-records');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
              ON p.`code` IN ('vote:list', 'vote:create', 'vote:detail', 'vote:update', 'vote:manage-records')
WHERE r.`code` IN ('super_admin', 'club_admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
);
