ALTER TABLE `recruitment_campaign`
    ADD COLUMN IF NOT EXISTS `login_required` TINYINT(1) DEFAULT 1 COMMENT '是否要求登录后提交' AFTER `max_applicants`,
    ADD COLUMN IF NOT EXISTS `form_schema` JSON DEFAULT NULL COMMENT '自定义报名表单结构' AFTER `login_required`;

ALTER TABLE `membership_application`
    MODIFY COLUMN `user_id` INT NULL COMMENT '申请用户ID，匿名提交时可为空';
