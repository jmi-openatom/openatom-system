CREATE TABLE IF NOT EXISTS `mailbox_outbox_event`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id`      VARCHAR(64)  NOT NULL COMMENT '跨系统幂等事件ID',
    `event_type`    VARCHAR(64)  NOT NULL COMMENT 'USER_CREATED/USER_UPDATED/USER_DELETION_REQUESTED',
    `aggregate_id`  VARCHAR(64)  NOT NULL COMMENT 'OpenAtom用户ID/OAuth sub',
    `payload_json`  JSON         NOT NULL COMMENT '用户快照',
    `status`        VARCHAR(24)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RETRY/PROCESSED/FAILED',
    `retry_count`   INT          NOT NULL DEFAULT 0,
    `next_retry_at` TIMESTAMP    NULL,
    `processing_started_at` TIMESTAMP NULL,
    `last_error`    VARCHAR(1000) NULL,
    `created_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at`  TIMESTAMP    NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mailbox_outbox_event_id` (`event_id`),
    KEY `idx_mailbox_outbox_poll` (`status`, `next_retry_at`, `id`),
    KEY `idx_mailbox_outbox_aggregate` (`aggregate_id`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='邮箱账号同步事务Outbox';

DROP TRIGGER IF EXISTS `trg_tb_user_mailbox_created`;
CREATE TRIGGER `trg_tb_user_mailbox_created`
    AFTER INSERT
    ON `tb_user`
    FOR EACH ROW
    INSERT INTO `mailbox_outbox_event`
        (`event_id`, `event_type`, `aggregate_id`, `payload_json`)
    VALUES
        (LOWER(REPLACE(UUID(), '-', '')),
         'USER_CREATED',
         CAST(NEW.`id` AS CHAR),
         JSON_OBJECT(
             'sub', CAST(NEW.`id` AS CHAR),
             'userId', NEW.`id`,
             'username', NEW.`user_name`,
             'displayName', NEW.`real_name`,
             'status', CASE NEW.`user_status`
                           WHEN 1 THEN 'ACTIVE'
                           WHEN 2 THEN 'LOCKED'
                           ELSE 'DISABLED'
                       END));

DROP TRIGGER IF EXISTS `trg_tb_user_mailbox_updated`;
CREATE TRIGGER `trg_tb_user_mailbox_updated`
    AFTER UPDATE
    ON `tb_user`
    FOR EACH ROW
    INSERT INTO `mailbox_outbox_event`
        (`event_id`, `event_type`, `aggregate_id`, `payload_json`)
    SELECT LOWER(REPLACE(UUID(), '-', '')),
           'USER_UPDATED',
           CAST(NEW.`id` AS CHAR),
           JSON_OBJECT(
               'sub', CAST(NEW.`id` AS CHAR),
               'userId', NEW.`id`,
               'username', NEW.`user_name`,
               'displayName', NEW.`real_name`,
               'status', CASE NEW.`user_status`
                             WHEN 1 THEN 'ACTIVE'
                             WHEN 2 THEN 'LOCKED'
                             ELSE 'DISABLED'
                         END)
    WHERE NOT (NEW.`user_name` <=> OLD.`user_name`)
       OR NOT (NEW.`real_name` <=> OLD.`real_name`)
       OR NOT (NEW.`user_status` <=> OLD.`user_status`);

DROP TRIGGER IF EXISTS `trg_tb_user_mailbox_deleted`;
CREATE TRIGGER `trg_tb_user_mailbox_deleted`
    AFTER DELETE
    ON `tb_user`
    FOR EACH ROW
    INSERT INTO `mailbox_outbox_event`
        (`event_id`, `event_type`, `aggregate_id`, `payload_json`)
    VALUES
        (LOWER(REPLACE(UUID(), '-', '')),
         'USER_DELETION_REQUESTED',
         CAST(OLD.`id` AS CHAR),
         JSON_OBJECT(
             'sub', CAST(OLD.`id` AS CHAR),
             'userId', OLD.`id`,
             'username', OLD.`user_name`,
             'displayName', OLD.`real_name`,
             'status', 'DELETION_REQUESTED'));
