ALTER TABLE `evening_study_schedule`
    ADD COLUMN `checkin_window_minutes` INT NOT NULL DEFAULT 30 COMMENT '签到窗口分钟数' AFTER `checkin_points`,
    ADD COLUMN `late_after_minutes` INT NOT NULL DEFAULT 10 COMMENT '开始后超过多少分钟算迟到' AFTER `checkin_window_minutes`,
    ADD COLUMN `late_penalty_points` BIGINT NOT NULL DEFAULT 1 COMMENT '迟到扣分' AFTER `late_after_minutes`,
    ADD COLUMN `absent_penalty_points` BIGINT NOT NULL DEFAULT 2 COMMENT '旷课扣分' AFTER `late_penalty_points`;

ALTER TABLE `checkin_session`
    ADD COLUMN `checkin_window_minutes` INT NOT NULL DEFAULT 30 COMMENT '签到窗口分钟数' AFTER `checkin_points`,
    ADD COLUMN `late_after_minutes` INT NOT NULL DEFAULT 10 COMMENT '开始后超过多少分钟算迟到' AFTER `checkin_window_minutes`,
    ADD COLUMN `late_penalty_points` BIGINT NOT NULL DEFAULT 1 COMMENT '迟到扣分' AFTER `late_after_minutes`,
    ADD COLUMN `absent_penalty_points` BIGINT NOT NULL DEFAULT 2 COMMENT '旷课扣分' AFTER `late_penalty_points`;
