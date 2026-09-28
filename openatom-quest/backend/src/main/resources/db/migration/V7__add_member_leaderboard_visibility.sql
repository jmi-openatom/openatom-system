ALTER TABLE quest_member
    ADD COLUMN leaderboard_visible BOOLEAN NOT NULL DEFAULT TRUE,
    ADD INDEX idx_quest_member_leaderboard (status, leaderboard_visible, total_points DESC, id);

UPDATE quest_member
SET leaderboard_visible = FALSE
WHERE email = 'quest-content@system.local';
