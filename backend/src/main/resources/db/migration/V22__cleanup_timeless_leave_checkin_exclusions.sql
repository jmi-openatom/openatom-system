INSERT IGNORE INTO `checkin_target` (`session_id`, `user_id`)
SELECT exclusion.`session_id`, exclusion.`user_id`
FROM `checkin_exclusion` exclusion
JOIN `leave_application` leave_application
    ON leave_application.`id` = exclusion.`source_id`
JOIN `checkin_session` session
    ON session.`id` = exclusion.`session_id`
JOIN `checkin_group_member` group_member
    ON group_member.`group_id` = session.`group_id`
    AND group_member.`user_id` = exclusion.`user_id`
WHERE exclusion.`source_type` = 'leave'
  AND (leave_application.`start_at` IS NULL OR leave_application.`end_at` IS NULL);

DELETE exclusion
FROM `checkin_exclusion` exclusion
JOIN `leave_application` leave_application
    ON leave_application.`id` = exclusion.`source_id`
WHERE exclusion.`source_type` = 'leave'
  AND (leave_application.`start_at` IS NULL OR leave_application.`end_at` IS NULL);
