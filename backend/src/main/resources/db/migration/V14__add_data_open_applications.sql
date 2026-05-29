CREATE TABLE IF NOT EXISTS `data_open_application`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_name` VARCHAR(120) NOT NULL COMMENT '接入应用名称',
    `applicant_name` VARCHAR(80) NOT NULL COMMENT '申请人',
    `applicant_contact` VARCHAR(160) NOT NULL COMMENT '联系方式',
    `organization` VARCHAR(160) DEFAULT NULL COMMENT '组织或团队',
    `purpose` VARCHAR(800) NOT NULL COMMENT '使用场景',
    `endpoint_scope` VARCHAR(80) NOT NULL DEFAULT 'public_login' COMMENT '可调用接口范围',
    `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
    `api_key` VARCHAR(128) DEFAULT NULL COMMENT '调用密钥',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `reviewed_by` INT DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
    `last_used_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最近调用时间',
    `call_count` INT NOT NULL DEFAULT 0 COMMENT '调用次数',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_open_api_key` (`api_key`),
    KEY `idx_data_open_status_time` (`status`, `created_at`),
    KEY `idx_data_open_applicant` (`applicant_contact`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据开放平台申请表';

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询数据开放申请',
       'data-open:list',
       'api',
       '/data-open/admin/applications',
       'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'data-open:list');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '审核数据开放申请',
       'data-open:review',
       'api',
       '/data-open/admin/applications/{applicationId}/review',
       'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'data-open:review');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` IN ('data-open:list', 'data-open:review')
WHERE r.`code` IN ('super_admin', 'club_admin')
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
);
