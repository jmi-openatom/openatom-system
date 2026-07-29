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

-- Keep this migration compatible with the least-privilege production database account.
-- MySQL rejects CREATE TRIGGER when binary logging is enabled unless the account has SUPER
-- or log_bin_trust_function_creators is enabled. MailboxOutboxScheduler performs an
-- idempotent user reconciliation after startup and hourly, so database triggers are not
-- required for reliable mailbox provisioning.
