CREATE TABLE IF NOT EXISTS `campus_building_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `building_id` VARCHAR(64) NOT NULL,
  `user_id` INT NOT NULL,
  `description` TEXT DEFAULT NULL,
  `replace_description` TINYINT(1) NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending',
  `review_reason` VARCHAR(500) DEFAULT NULL,
  `reviewed_by` INT DEFAULT NULL,
  `reviewed_at` TIMESTAMP(3) NULL DEFAULT NULL,
  `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_campus_building_public` (`building_id`, `status`, `reviewed_at`, `id`),
  KEY `idx_campus_building_mine` (`user_id`, `building_id`, `status`),
  KEY `idx_campus_building_review` (`status`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园楼宇介绍及实拍投稿';

CREATE TABLE IF NOT EXISTS `campus_building_photo` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT NOT NULL,
  `storage_name` VARCHAR(96) NOT NULL,
  `original_name` VARCHAR(255) NOT NULL,
  `mime_type` VARCHAR(32) NOT NULL,
  `file_size` BIGINT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_campus_photo_storage` (`storage_name`),
  KEY `idx_campus_photo_submission` (`submission_id`, `sort_order`, `id`),
  CONSTRAINT `fk_campus_photo_submission` FOREIGN KEY (`submission_id`)
    REFERENCES `campus_building_submission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园楼宇投稿实拍照片';

CREATE TABLE IF NOT EXISTS `campus_building_photo_removal` (
  `submission_id` BIGINT NOT NULL,
  `photo_id` BIGINT NOT NULL,
  PRIMARY KEY (`submission_id`, `photo_id`),
  KEY `idx_campus_photo_removal` (`photo_id`),
  CONSTRAINT `fk_campus_removal_submission` FOREIGN KEY (`submission_id`)
    REFERENCES `campus_building_submission` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_campus_removal_photo` FOREIGN KEY (`photo_id`)
    REFERENCES `campus_building_photo` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待审核的楼宇照片移除请求';

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查看校园楼宇投稿', 'campus-building:list', 'api', '/campus-buildings/admin/submissions', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'campus-building:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '审核校园楼宇投稿', 'campus-building:review', 'api', '/campus-buildings/admin/submissions/{id}/review', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'campus-building:review');
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r JOIN `sys_permission` p
  ON p.code IN ('campus-building:list', 'campus-building:review')
WHERE r.code = 'super_admin' AND NOT EXISTS (
  SELECT 1 FROM `sys_role_permission` rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
