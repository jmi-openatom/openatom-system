CREATE TABLE IF NOT EXISTS `interview_queue_operation`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`   INT          NOT NULL,
    `interview_id` INT          DEFAULT NULL,
    `room_id`      INT          DEFAULT NULL,
    `action`       VARCHAR(40)  NOT NULL,
    `operator_id`  INT          DEFAULT NULL,
    `detail_json`  JSON         DEFAULT NULL,
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_queue_operation_session` (`session_id`, `id`),
    KEY `idx_queue_operation_interview` (`interview_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试现场操作与异常恢复记录';
