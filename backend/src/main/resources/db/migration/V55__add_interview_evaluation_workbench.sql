CREATE TABLE IF NOT EXISTS `interview_evaluation_template`
(
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `campaign_id` INT          DEFAULT NULL COMMENT 'NULL表示系统默认模板',
    `name`        VARCHAR(120) NOT NULL,
    `schema_json` JSON         NOT NULL,
    `version`     INT          NOT NULL DEFAULT 1,
    `status`      VARCHAR(24)  NOT NULL DEFAULT 'active',
    `created_by`  INT          DEFAULT NULL,
    `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  TIMESTAMP    NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_evaluation_template_campaign` (`campaign_id`, `status`, `version`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试评价模板';

INSERT INTO `interview_evaluation_template`
    (`campaign_id`, `name`, `schema_json`, `version`, `status`)
SELECT NULL,
       '开放原子默认面试评价模板',
       JSON_OBJECT(
           'dimensions', JSON_ARRAY(
               JSON_OBJECT('key', 'motivation', 'label', '加入动机', 'description', '对社团的了解与真实加入意愿', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'technical', 'label', '技术基础', 'description', '报名方向的基础知识与实践能力', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'problemSolving', 'label', '问题分析', 'description', '拆解问题并形成解决方案的能力', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'communication', 'label', '沟通表达', 'description', '理解问题和清晰表达的能力', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'collaboration', 'label', '团队协作', 'description', '合作意识、责任感与接受反馈能力', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'learning', 'label', '学习潜力', 'description', '自主学习、复盘和持续成长能力', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'openSourceValues', 'label', '开源价值观', 'description', '分享、协作与尊重社区规则', 'required', TRUE, 'weight', 1),
               JSON_OBJECT('key', 'overallFit', 'label', '综合匹配度', 'description', '与社团文化和实际工作的整体匹配程度', 'required', TRUE, 'weight', 1)
           ),
           'scoreAnchors', JSON_OBJECT('1', '明显不足', '2', '低于要求', '3', '达到要求', '4', '表现良好', '5', '明显超出预期')
       ),
       1,
       'active'
WHERE NOT EXISTS (
    SELECT 1 FROM `interview_evaluation_template` WHERE `campaign_id` IS NULL
);

ALTER TABLE `interview_feedback`
    ADD COLUMN `template_id` INT DEFAULT NULL AFTER `interviewer_id`,
    ADD COLUMN `template_version` INT DEFAULT NULL AFTER `template_id`,
    ADD COLUMN `details` JSON DEFAULT NULL AFTER `scores`,
    ADD COLUMN `status` VARCHAR(24) NOT NULL DEFAULT 'submitted' AFTER `comment`,
    ADD COLUMN `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
    ADD COLUMN `submitted_at` TIMESTAMP NULL DEFAULT NULL AFTER `updated_at`,
    ADD COLUMN `withdrawn_at` TIMESTAMP NULL DEFAULT NULL AFTER `submitted_at`,
    ADD KEY `idx_feedback_progress` (`interview_id`, `status`, `interviewer_id`);

UPDATE `interview_feedback`
SET `submitted_at` = COALESCE(`submitted_at`, `created_at`)
WHERE `status` = 'submitted';

CREATE TABLE IF NOT EXISTS `interview_feedback_revision`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `feedback_id`    INT          NOT NULL,
    `interview_id`   INT          NOT NULL,
    `interviewer_id` INT          NOT NULL,
    `action`         VARCHAR(24)  NOT NULL COMMENT 'save/submit/withdraw',
    `snapshot_json`  JSON         NOT NULL,
    `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_feedback_revision_feedback` (`feedback_id`, `id`),
    KEY `idx_feedback_revision_interview` (`interview_id`, `interviewer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '面试评价修改记录';
