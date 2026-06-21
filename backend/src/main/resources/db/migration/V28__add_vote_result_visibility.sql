ALTER TABLE `vote_campaign`
    ADD COLUMN `result_visibility` VARCHAR(20) NOT NULL DEFAULT 'public'
        COMMENT '前台结果可见范围: public/after_vote/private'
        AFTER `result_visible`;

UPDATE `vote_campaign`
SET `result_visibility` = CASE
    WHEN `result_visible` = 0 THEN 'after_vote'
    ELSE 'public'
END;
