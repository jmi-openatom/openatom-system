CREATE TABLE IF NOT EXISTS `lab_checkin_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS签到分组';

CREATE TABLE IF NOT EXISTS `lab_checkin_group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_checkin_group_member_user` (`group_id`, `user_id`),
  KEY `idx_lab_checkin_group_member_group` (`group_id`),
  KEY `idx_lab_checkin_group_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS签到分组成员';

CREATE TABLE IF NOT EXISTS `lab_checkin_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint DEFAULT NULL,
  `schedule_id` bigint DEFAULT NULL,
  `session_type` varchar(40) NOT NULL DEFAULT 'regular',
  `attendance_date` date DEFAULT NULL,
  `title` varchar(160) NOT NULL,
  `location` varchar(160) DEFAULT NULL,
  `start_at` timestamp NULL DEFAULT NULL,
  `end_at` timestamp NULL DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'open',
  `token` varchar(80) NOT NULL,
  `checkin_points` int NOT NULL DEFAULT 0,
  `checkin_window_minutes` int NOT NULL DEFAULT 30,
  `late_after_minutes` int NOT NULL DEFAULT 10,
  `late_penalty_points` int NOT NULL DEFAULT 5,
  `absent_penalty_points` int NOT NULL DEFAULT 10,
  `created_by` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_checkin_session_token` (`token`),
  UNIQUE KEY `uk_lab_checkin_evening_schedule_date` (`schedule_id`, `attendance_date`, `session_type`),
  KEY `idx_lab_checkin_session_group` (`group_id`),
  KEY `idx_lab_checkin_session_status` (`status`),
  KEY `idx_lab_checkin_session_type_date` (`session_type`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS签到场次';

CREATE TABLE IF NOT EXISTS `lab_checkin_target` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_checkin_target_user` (`session_id`, `user_id`),
  KEY `idx_lab_checkin_target_session` (`session_id`),
  KEY `idx_lab_checkin_target_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS签到发放名单';

CREATE TABLE IF NOT EXISTS `lab_checkin_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `checkin_at` timestamp NULL DEFAULT NULL,
  `source` varchar(40) DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'checked',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_checkin_record_user` (`session_id`, `user_id`),
  KEY `idx_lab_checkin_record_session` (`session_id`),
  KEY `idx_lab_checkin_record_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS签到记录';

CREATE TABLE IF NOT EXISTS `lab_checkin_exclusion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `source_type` varchar(40) NOT NULL DEFAULT 'manual',
  `source_id` bigint DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_checkin_exclusion_user` (`session_id`, `user_id`),
  KEY `idx_lab_checkin_exclusion_session` (`session_id`),
  KEY `idx_lab_checkin_exclusion_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS签到剔除记录';

CREATE TABLE IF NOT EXISTS `lab_evening_study_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `title` varchar(120) NOT NULL DEFAULT '晚自习签到',
  `location` varchar(120) DEFAULT NULL,
  `start_time` time NOT NULL DEFAULT '19:00:00',
  `end_time` time NOT NULL DEFAULT '21:30:00',
  `checkin_points` int NOT NULL DEFAULT 0,
  `checkin_window_minutes` int NOT NULL DEFAULT 30,
  `late_after_minutes` int NOT NULL DEFAULT 10,
  `late_penalty_points` int NOT NULL DEFAULT 5,
  `absent_penalty_points` int NOT NULL DEFAULT 10,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_by` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_evening_schedule_group` (`group_id`),
  KEY `idx_lab_evening_schedule_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS晚自习签到计划';
