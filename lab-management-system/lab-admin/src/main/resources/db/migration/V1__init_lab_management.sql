CREATE TABLE IF NOT EXISTS `lab_users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '实验室本地主键',
  `club_user_id` bigint NOT NULL COMMENT '对应社团系统的用户ID',
  `username` varchar(64) DEFAULT NULL COMMENT '冗余社团用户名',
  `nickname` varchar(128) DEFAULT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `lab_role` tinyint NOT NULL DEFAULT 0 COMMENT '0-学生, 1-助教, 2-主教练',
  `reputation_score` int NOT NULL DEFAULT 100,
  `disabled` tinyint NOT NULL DEFAULT 0,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_lab_users_club_uid` (`club_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室用户冗余及本地权限表';

CREATE TABLE IF NOT EXISTS `lab_problems` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_date` date NOT NULL,
  `title` varchar(128) NOT NULL,
  `slug` varchar(128) NOT NULL,
  `difficulty` varchar(32) NOT NULL DEFAULT '入门',
  `description_markdown` longtext NOT NULL,
  `time_limit_ms` int NOT NULL DEFAULT 1000,
  `memory_limit_mb` int NOT NULL DEFAULT 128,
  `status` varchar(24) NOT NULL DEFAULT 'PUBLISHED',
  `ai_generated` tinyint NOT NULL DEFAULT 1,
  `solution_cpp` longtext,
  `solution_java` longtext,
  `solution_python` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_lab_problems_date` (`problem_date`),
  KEY `idx_lab_problems_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日一练题目';

CREATE TABLE IF NOT EXISTS `lab_problem_test_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `input_text` text NOT NULL,
  `expected_output` text NOT NULL,
  `sample_case` tinyint NOT NULL DEFAULT 0,
  `sort_order` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_lab_cases_problem` (`problem_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日一练测试用例';

CREATE TABLE IF NOT EXISTS `lab_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `problem_id` bigint NOT NULL,
  `language` varchar(24) NOT NULL,
  `code` longtext NOT NULL,
  `judge_status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `score_passed` int NOT NULL DEFAULT 0,
  `total_cases` int NOT NULL DEFAULT 0,
  `runtime_ms` int DEFAULT NULL,
  `memory_kb` int DEFAULT NULL,
  `error_message` text,
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `judged_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_lab_submissions_user_problem` (`user_id`, `problem_id`, `submitted_at`),
  KEY `idx_lab_submissions_status` (`judge_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交记录';

CREATE TABLE IF NOT EXISTS `lab_checkin_score_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '对应lab_users.id',
  `checkin_date` date NOT NULL,
  `attendance_status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未签到, 1-正常刷题AC, 2-正常现场, 3-迟到',
  `source` varchar(24) NOT NULL DEFAULT 'ABSENT',
  `checkin_at` datetime DEFAULT NULL,
  `local_score_change` int NOT NULL DEFAULT 0,
  `club_score_change` int NOT NULL DEFAULT 0,
  `club_sync_status` tinyint NOT NULL DEFAULT 0 COMMENT '0-无需同步, 1-待同步, 2-同步成功, 3-同步失败',
  `sync_error` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_lab_checkin_user_date` (`user_id`, `checkin_date`),
  KEY `idx_lab_checkin_date` (`checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤与积分同步流水表';

CREATE TABLE IF NOT EXISTS `lab_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '为空表示群发模板记录',
  `notice_type` varchar(32) NOT NULL,
  `title` varchar(160) NOT NULL,
  `content` text NOT NULL,
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lab_notice_user_read` (`user_id`, `read_at`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LMS站内通知';
