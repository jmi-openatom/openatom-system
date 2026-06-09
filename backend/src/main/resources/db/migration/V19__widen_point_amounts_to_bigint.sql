ALTER TABLE `club_activity`
    MODIFY COLUMN `participation_points` BIGINT NOT NULL DEFAULT 0 COMMENT '参加活动奖励积分';

ALTER TABLE `checkin_session`
    MODIFY COLUMN `checkin_points` BIGINT NOT NULL DEFAULT 0 COMMENT '扫码签到奖励积分';

ALTER TABLE `point_account`
    MODIFY COLUMN `balance` BIGINT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    MODIFY COLUMN `total_earned` BIGINT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    MODIFY COLUMN `total_spent` BIGINT NOT NULL DEFAULT 0 COMMENT '累计兑换消耗积分';

ALTER TABLE `point_transaction`
    MODIFY COLUMN `delta` BIGINT NOT NULL COMMENT '积分变动',
    MODIFY COLUMN `balance_after` BIGINT NOT NULL COMMENT '变动后余额';

ALTER TABLE `point_redeem_item`
    MODIFY COLUMN `point_cost` BIGINT NOT NULL COMMENT '所需积分';

ALTER TABLE `point_redemption`
    MODIFY COLUMN `points` BIGINT NOT NULL COMMENT '消耗积分';
