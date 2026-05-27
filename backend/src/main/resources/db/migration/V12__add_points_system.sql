SET @club_activity_participation_points_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'club_activity'
      AND COLUMN_NAME = 'participation_points'
);
SET @club_activity_participation_points_sql = IF(
    @club_activity_participation_points_exists = 0,
    'ALTER TABLE `club_activity` ADD COLUMN `participation_points` INT NOT NULL DEFAULT 0 COMMENT ''参加活动奖励积分'' AFTER `registration_fields`',
    'SELECT 1'
);
PREPARE club_activity_participation_points_stmt FROM @club_activity_participation_points_sql;
EXECUTE club_activity_participation_points_stmt;
DEALLOCATE PREPARE club_activity_participation_points_stmt;

SET @checkin_session_checkin_points_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'checkin_session'
      AND COLUMN_NAME = 'checkin_points'
);
SET @checkin_session_checkin_points_sql = IF(
    @checkin_session_checkin_points_exists = 0,
    'ALTER TABLE `checkin_session` ADD COLUMN `checkin_points` INT NOT NULL DEFAULT 0 COMMENT ''扫码签到奖励积分'' AFTER `token`',
    'SELECT 1'
);
PREPARE checkin_session_checkin_points_stmt FROM @checkin_session_checkin_points_sql;
EXECUTE checkin_session_checkin_points_stmt;
DEALLOCATE PREPARE checkin_session_checkin_points_stmt;

CREATE TABLE IF NOT EXISTS `point_account`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `balance` INT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    `total_earned` INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    `total_spent` INT NOT NULL DEFAULT 0 COMMENT '累计兑换消耗积分',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point_account_user` (`user_id`),
    KEY `idx_point_account_balance` (`balance`, `total_earned`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分账户表';

CREATE TABLE IF NOT EXISTS `point_transaction`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `delta` INT NOT NULL COMMENT '积分变动',
    `balance_after` INT NOT NULL COMMENT '变动后余额',
    `type` VARCHAR(40) NOT NULL COMMENT '流水类型',
    `source_type` VARCHAR(60) DEFAULT NULL COMMENT '来源类型',
    `source_id` INT DEFAULT NULL COMMENT '来源ID',
    `source_key` VARCHAR(160) DEFAULT NULL COMMENT '幂等来源键',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
    `operator_id` INT DEFAULT NULL COMMENT '操作人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_point_transaction_user_time` (`user_id`, `created_at`),
    KEY `idx_point_transaction_source` (`user_id`, `source_key`),
    KEY `idx_point_transaction_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分流水表';

CREATE TABLE IF NOT EXISTS `point_redeem_item`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(120) NOT NULL COMMENT '兑换项名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '兑换说明',
    `point_cost` INT NOT NULL COMMENT '所需积分',
    `stock` INT DEFAULT NULL COMMENT '库存，空表示不限',
    `exchanged_count` INT NOT NULL DEFAULT 0 COMMENT '已兑换数量',
    `status` VARCHAR(30) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
    `created_by` INT DEFAULT NULL COMMENT '创建人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_point_redeem_item_status` (`status`, `sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分兑换项表';

CREATE TABLE IF NOT EXISTS `point_redemption`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT NOT NULL COMMENT '兑换用户ID',
    `item_id` INT NOT NULL COMMENT '兑换项ID',
    `points` INT NOT NULL COMMENT '消耗积分',
    `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/fulfilled/cancelled/rejected',
    `receiver_name` VARCHAR(80) DEFAULT NULL COMMENT '领取人',
    `receiver_contact` VARCHAR(120) DEFAULT NULL COMMENT '联系方式',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '用户备注',
    `admin_note` VARCHAR(500) DEFAULT NULL COMMENT '后台备注',
    `reviewed_by` INT DEFAULT NULL COMMENT '处理人',
    `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '处理时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_point_redemption_user` (`user_id`, `created_at`),
    KEY `idx_point_redemption_item` (`item_id`),
    KEY `idx_point_redemption_status` (`status`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='积分兑换记录表';
