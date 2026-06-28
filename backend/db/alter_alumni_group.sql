ALTER TABLE `club_membership` ADD COLUMN `alumni_group` VARCHAR(100) DEFAULT NULL COMMENT '往届管理人员分组名称' AFTER `left_at`;
