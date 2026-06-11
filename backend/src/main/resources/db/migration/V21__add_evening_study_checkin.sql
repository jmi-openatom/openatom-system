ALTER TABLE `checkin_session`
    ADD COLUMN `schedule_id` INT DEFAULT NULL COMMENT '晚自习计划ID' AFTER `group_id`,
    ADD COLUMN `session_type` VARCHAR(40) NOT NULL DEFAULT 'regular' COMMENT '签到类型: regular/evening_study' AFTER `schedule_id`,
    ADD COLUMN `attendance_date` DATE DEFAULT NULL COMMENT '考勤日期' AFTER `session_type`,
    ADD KEY `idx_checkin_session_type_date` (`session_type`, `attendance_date`),
    ADD KEY `idx_checkin_session_schedule` (`schedule_id`),
    ADD UNIQUE KEY `uk_checkin_evening_schedule_date` (`schedule_id`, `attendance_date`, `session_type`);

CREATE TABLE IF NOT EXISTS `evening_study_schedule`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `group_id` INT NOT NULL COMMENT '实验室签到分组ID',
    `title` VARCHAR(120) NOT NULL DEFAULT '晚自习签到' COMMENT '计划标题',
    `location` VARCHAR(120) DEFAULT NULL COMMENT '晚自习地点',
    `start_time` TIME NOT NULL DEFAULT '19:00:00' COMMENT '每日开始时间',
    `end_time` TIME NOT NULL DEFAULT '21:30:00' COMMENT '每日结束时间',
    `checkin_points` BIGINT NOT NULL DEFAULT 0 COMMENT '签到奖励积分',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_by` INT DEFAULT NULL COMMENT '创建人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_evening_schedule_group` (`group_id`),
    KEY `idx_evening_schedule_club` (`club_id`),
    KEY `idx_evening_schedule_enabled` (`enabled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='晚自习签到计划表';

CREATE TABLE IF NOT EXISTS `checkin_exclusion`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` INT NOT NULL COMMENT '签到场次ID',
    `user_id` INT NOT NULL COMMENT '被剔除用户ID',
    `source_type` VARCHAR(40) NOT NULL DEFAULT 'leave' COMMENT '剔除来源类型',
    `source_id` INT DEFAULT NULL COMMENT '来源记录ID',
    `reason` VARCHAR(255) DEFAULT NULL COMMENT '剔除原因',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkin_exclusion_user` (`session_id`, `user_id`),
    KEY `idx_checkin_exclusion_session` (`session_id`),
    KEY `idx_checkin_exclusion_user` (`user_id`),
    KEY `idx_checkin_exclusion_source` (`source_type`, `source_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到自动剔除记录表';
