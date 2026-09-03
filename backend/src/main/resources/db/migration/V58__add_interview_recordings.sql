CREATE TABLE IF NOT EXISTS `interview_recording`
(
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `interview_id`          INT          NOT NULL,
    `interviewer_id`        INT          NOT NULL,
    `file_name`             VARCHAR(96)  NOT NULL,
    `mime_type`             VARCHAR(80)  NOT NULL,
    `file_size`             BIGINT       NOT NULL,
    `duration_seconds`      INT          DEFAULT NULL,
    `transcript`            MEDIUMTEXT   DEFAULT NULL,
    `transcript_updated_at` TIMESTAMP    NULL DEFAULT NULL,
    `created_at`            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_recording_file` (`file_name`),
    KEY `idx_interview_recording_interview` (`interview_id`, `id`),
    KEY `idx_interview_recording_interviewer` (`interviewer_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试录音与实时转写记录';
