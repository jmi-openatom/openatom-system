CREATE TABLE IF NOT EXISTS `club_expense` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `club_id` INT NOT NULL,
    `paid_on` DATE NOT NULL,
    `title` VARCHAR(160) NOT NULL,
    `category` VARCHAR(40) NOT NULL,
    `amount` DECIMAL(12, 2) NOT NULL,
    `handled_by` VARCHAR(80) NOT NULL,
    `payment_method` VARCHAR(20) DEFAULT NULL,
    `activity_id` INT DEFAULT NULL,
    `note` VARCHAR(1000) DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'active',
    `void_reason` VARCHAR(500) DEFAULT NULL,
    `created_by` INT NOT NULL,
    `updated_by` INT DEFAULT NULL,
    `voided_by` INT DEFAULT NULL,
    `version` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `voided_at` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_expense_club_paid` (`club_id`, `paid_on`, `id`),
    KEY `idx_expense_club_status` (`club_id`, `status`),
    KEY `idx_expense_activity` (`activity_id`),
    CONSTRAINT `chk_expense_amount_positive` CHECK (`amount` > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社团已发生支出台账';

CREATE TABLE IF NOT EXISTS `club_expense_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `expense_id` BIGINT NOT NULL,
    `storage_name` VARCHAR(80) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `mime_type` VARCHAR(80) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `uploaded_by` INT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP NULL DEFAULT NULL,
    `deleted_by` INT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_expense_attachment_storage` (`storage_name`),
    KEY `idx_expense_attachment_expense` (`expense_id`, `deleted_at`),
    CONSTRAINT `fk_expense_attachment_expense` FOREIGN KEY (`expense_id`) REFERENCES `club_expense` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社团支出私有凭证';

CREATE TABLE IF NOT EXISTS `club_expense_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `expense_id` BIGINT NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `before_json` JSON DEFAULT NULL,
    `after_json` JSON DEFAULT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `operator_id` INT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_expense_history_expense` (`expense_id`, `id`),
    CONSTRAINT `fk_expense_history_expense` FOREIGN KEY (`expense_id`) REFERENCES `club_expense` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社团支出变更历史';
