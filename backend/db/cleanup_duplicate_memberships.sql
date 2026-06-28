-- 清理 club_membership 表中的重复记录
-- 保留每组 user_id + club_id 中 status != 'left' 的最新一条记录（id 最大）
-- 删除其余重复记录

DELETE FROM club_membership
WHERE id NOT IN (
    SELECT keep_id FROM (
        SELECT MAX(id) AS keep_id
        FROM club_membership
        WHERE status != 'left'
        GROUP BY user_id, club_id
    ) AS keep_ids
)
AND status != 'left';
