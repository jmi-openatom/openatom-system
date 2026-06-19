CREATE TABLE IF NOT EXISTS `club_regulation`
(
    `id`               INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id`          INT          NOT NULL COMMENT '社团ID',
    `title`            VARCHAR(200) NOT NULL COMMENT '制度标题',
    `summary`          VARCHAR(800) DEFAULT NULL COMMENT '制度摘要',
    `content_markdown` LONGTEXT     NOT NULL COMMENT 'Markdown正文',
    `status`           VARCHAR(30)  NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published',
    `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '排序值',
    `created_by`       INT          DEFAULT NULL COMMENT '创建人ID',
    `updated_by`       INT          DEFAULT NULL COMMENT '最后更新人ID',
    `published_at`     TIMESTAMP    NULL DEFAULT NULL COMMENT '发布时间',
    `created_at`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       TIMESTAMP    NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_club_regulation_club_status` (`club_id`, `status`),
    KEY `idx_club_regulation_sort` (`sort_order`, `published_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='社团规章制度表';
