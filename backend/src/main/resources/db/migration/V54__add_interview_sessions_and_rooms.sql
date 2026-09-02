CREATE TABLE IF NOT EXISTS `interview_session`
(
    `id`               INT          NOT NULL AUTO_INCREMENT,
    `campaign_id`      INT          NOT NULL,
    `name`             VARCHAR(120) NOT NULL,
    `round`            INT          NOT NULL DEFAULT 1,
    `scheduled_start_at` TIMESTAMP  NOT NULL,
    `scheduled_end_at` TIMESTAMP    NOT NULL,
    `duration_minutes` INT          NOT NULL,
    `gap_minutes`      INT          NOT NULL DEFAULT 0,
    `mode`             VARCHAR(20)  NOT NULL DEFAULT 'offline',
    `status`           VARCHAR(24)  NOT NULL DEFAULT 'draft',
    `created_by`       INT          DEFAULT NULL,
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       TIMESTAMP    NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_interview_session_campaign` (`campaign_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试场次';

CREATE TABLE IF NOT EXISTS `interview_room`
(
    `id`               INT          NOT NULL AUTO_INCREMENT,
    `session_id`       INT          NOT NULL,
    `name`             VARCHAR(80)  NOT NULL,
    `location`         VARCHAR(255) DEFAULT NULL,
    `sort_order`       INT          NOT NULL DEFAULT 0,
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_interview_room_session` (`session_id`, `sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试间';

CREATE TABLE IF NOT EXISTS `interview_room_interviewer`
(
    `room_id`          INT NOT NULL,
    `interviewer_id`   INT NOT NULL,
    PRIMARY KEY (`room_id`, `interviewer_id`),
    KEY `idx_room_interviewer_user` (`interviewer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试间固定面试官组';

ALTER TABLE `interview`
    ADD COLUMN `session_id` INT DEFAULT NULL COMMENT '面试场次ID' AFTER `id`,
    ADD COLUMN `room_id` INT DEFAULT NULL COMMENT '面试间ID' AFTER `session_id`,
    ADD COLUMN `queue_number` INT DEFAULT NULL COMMENT '场次内序号' AFTER `room_id`,
    ADD KEY `idx_interview_session_room` (`session_id`, `room_id`, `scheduled_start_at`);
