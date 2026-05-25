CREATE TABLE IF NOT EXISTS `image_hosting_asset`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uploader_id` INT NOT NULL COMMENT '上传用户ID',
    `file_name` VARCHAR(120) NOT NULL COMMENT '存储文件名',
    `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `content_type` VARCHAR(80) DEFAULT NULL COMMENT '内容类型',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小',
    `url` VARCHAR(500) NOT NULL COMMENT '公开访问URL',
    `status` VARCHAR(30) DEFAULT 'active' COMMENT '状态: active/deleted',
    `deleted_by` INT DEFAULT NULL COMMENT '删除人ID',
    `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '删除时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_image_hosting_file` (`file_name`),
    KEY `idx_image_hosting_uploader` (`uploader_id`),
    KEY `idx_image_hosting_status_time` (`status`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='图床资产表';
