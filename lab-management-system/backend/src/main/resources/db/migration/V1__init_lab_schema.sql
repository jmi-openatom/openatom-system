CREATE TABLE `lab_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `club_user_id` bigint NOT NULL,
  `username` varchar(64) DEFAULT NULL,
  `real_name` varchar(80) DEFAULT NULL,
  `lab_role` tinyint DEFAULT 0,
  `reputation_score` int DEFAULT 100,
  `active` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_users_club_uid` (`club_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `lab_problem` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(160) NOT NULL,
  `description_md` mediumtext NOT NULL,
  `difficulty` varchar(30) DEFAULT 'easy',
  `time_limit_ms` int DEFAULT 1000,
  `memory_limit_mb` int DEFAULT 128,
  `status` varchar(30) DEFAULT 'draft',
  `publish_date` date DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lab_problem_status_date` (`status`, `publish_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `lab_problem_case` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `input_data` mediumtext NOT NULL,
  `expected_output` mediumtext NOT NULL,
  `sample_flag` tinyint DEFAULT 0,
  `sort_order` int DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_lab_problem_case_problem` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `lab_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `lab_user_id` bigint NOT NULL,
  `language` varchar(30) NOT NULL,
  `code` mediumtext NOT NULL,
  `status` varchar(20) NOT NULL,
  `judge_message` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lab_submission_user` (`lab_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `lab_checkin_score_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_user_id` bigint NOT NULL,
  `checkin_date` date NOT NULL,
  `attendance_status` tinyint DEFAULT 0,
  `source_type` varchar(30) DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `local_score_change` int DEFAULT 0,
  `club_score_change` int DEFAULT 0,
  `club_sync_status` tinyint DEFAULT 0,
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_checkin_user_date` (`lab_user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `lab_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_lab_user_id` bigint DEFAULT NULL,
  `title` varchar(160) NOT NULL,
  `content` text NOT NULL,
  `type` varchar(40) DEFAULT 'system',
  `read_flag` tinyint DEFAULT 0,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lab_notice_receiver` (`receiver_lab_user_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
