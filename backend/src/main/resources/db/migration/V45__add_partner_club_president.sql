ALTER TABLE `partner_club`
    ADD COLUMN `president_name` VARCHAR(50) NULL COMMENT '社长姓名' AFTER `category`,
    ADD COLUMN `president_avatar_url` VARCHAR(500) NULL COMMENT '社长头像地址' AFTER `president_name`;
