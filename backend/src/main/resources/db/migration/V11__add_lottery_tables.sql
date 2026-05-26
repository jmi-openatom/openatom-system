CREATE TABLE IF NOT EXISTS `lottery_campaign`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `form_id` INT NOT NULL COMMENT '参与表单ID',
    `title` VARCHAR(160) NOT NULL COMMENT '抽奖标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '抽奖说明',
    `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态: draft/open/closed',
    `created_by` INT DEFAULT NULL COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_lottery_campaign_club` (`club_id`),
    KEY `idx_lottery_campaign_form` (`form_id`),
    KEY `idx_lottery_campaign_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动表';

CREATE TABLE IF NOT EXISTS `lottery_prize`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `lottery_id` INT NOT NULL COMMENT '抽奖活动ID',
    `name` VARCHAR(120) NOT NULL COMMENT '奖品名称',
    `level` VARCHAR(60) DEFAULT NULL COMMENT '奖项等级',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '奖品数量',
    `sort_order` INT DEFAULT 0 COMMENT '展示排序',
    `color` VARCHAR(40) DEFAULT NULL COMMENT '大屏颜色',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_lottery_prize_lottery` (`lottery_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖奖品表';

CREATE TABLE IF NOT EXISTS `lottery_winner`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `lottery_id` INT NOT NULL COMMENT '抽奖活动ID',
    `prize_id` INT NOT NULL COMMENT '奖品ID',
    `submission_id` INT NOT NULL COMMENT '表单提交记录ID',
    `user_id` INT DEFAULT NULL COMMENT '中奖用户ID',
    `winner_name` VARCHAR(120) DEFAULT NULL COMMENT '中奖人名称',
    `winner_contact` VARCHAR(160) DEFAULT NULL COMMENT '中奖人联系方式',
    `winner_account` VARCHAR(120) DEFAULT NULL COMMENT '中奖人账号',
    `won_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '中奖时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lottery_winner_submission` (`lottery_id`, `submission_id`),
    KEY `idx_lottery_winner_lottery` (`lottery_id`),
    KEY `idx_lottery_winner_prize` (`prize_id`),
    KEY `idx_lottery_winner_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖中奖记录表';
