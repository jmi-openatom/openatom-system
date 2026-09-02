CREATE TABLE IF NOT EXISTS `interview_queue_state`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `interview_id`  INT         NOT NULL,
    `session_id`    INT         NOT NULL,
    `room_id`       INT         NOT NULL,
    `status`        VARCHAR(24) NOT NULL DEFAULT 'waiting' COMMENT 'waiting/called/completed/cancelled',
    `checked_in_by` INT         DEFAULT NULL,
    `checked_in_at` TIMESTAMP   NULL DEFAULT NULL,
    `called_at`     TIMESTAMP   NULL DEFAULT NULL,
    `call_count`    INT         NOT NULL DEFAULT 0,
    `updated_at`    TIMESTAMP   NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_queue_interview` (`interview_id`),
    KEY `idx_interview_queue_session` (`session_id`, `status`, `room_id`),
    KEY `idx_interview_queue_room` (`room_id`, `status`, `called_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试现场签到与叫号状态';
