-- 本地面试全流程演练数据（可重复执行）
-- 生成：250 名候选人、250 份申请、5 个面试间、15 名固定面试官、250 场面试。
-- 初始状态：250 位候选人的面试均待确认，便于从“批量确认”完整演练后续流程。

SET NAMES utf8mb4;
SET @mock_campaign_name = '【本地测试】250人面试演练';

-- 只清理本脚本上一次生成的数据，不影响其他招新批次。
SET @old_campaign_id = (
    SELECT `id` FROM `recruitment_campaign`
    WHERE `name` = @mock_campaign_name
    ORDER BY `id` DESC LIMIT 1
);

DELETE r
FROM `interview_feedback_revision` r
JOIN `interview_feedback` f ON f.`id` = r.`feedback_id`
JOIN `interview` i ON i.`id` = f.`interview_id`
JOIN `membership_application` a ON a.`id` = i.`application_id`
WHERE a.`campaign_id` = @old_campaign_id;

DELETE f
FROM `interview_feedback` f
JOIN `interview` i ON i.`id` = f.`interview_id`
JOIN `membership_application` a ON a.`id` = i.`application_id`
WHERE a.`campaign_id` = @old_campaign_id;

DELETE o
FROM `interview_queue_operation` o
JOIN `interview_session` s ON s.`id` = o.`session_id`
WHERE s.`campaign_id` = @old_campaign_id;

DELETE q
FROM `interview_queue_state` q
JOIN `interview_session` s ON s.`id` = q.`session_id`
WHERE s.`campaign_id` = @old_campaign_id;

DELETE ii
FROM `interview_interviewer` ii
JOIN `interview` i ON i.`id` = ii.`interview_id`
JOIN `membership_application` a ON a.`id` = i.`application_id`
WHERE a.`campaign_id` = @old_campaign_id;

DELETE i
FROM `interview` i
JOIN `membership_application` a ON a.`id` = i.`application_id`
WHERE a.`campaign_id` = @old_campaign_id;

DELETE ri
FROM `interview_room_interviewer` ri
JOIN `interview_room` r ON r.`id` = ri.`room_id`
JOIN `interview_session` s ON s.`id` = r.`session_id`
WHERE s.`campaign_id` = @old_campaign_id;

DELETE r
FROM `interview_room` r
JOIN `interview_session` s ON s.`id` = r.`session_id`
WHERE s.`campaign_id` = @old_campaign_id;

DELETE FROM `interview_session` WHERE `campaign_id` = @old_campaign_id;
DELETE FROM `interview_evaluation_template` WHERE `campaign_id` = @old_campaign_id;
DELETE FROM `membership_application` WHERE `campaign_id` = @old_campaign_id;
DELETE FROM `recruitment_campaign` WHERE `id` = @old_campaign_id;
DELETE ur FROM `sys_user_role` ur
JOIN `tb_user` u ON u.`id` = ur.`user_id`
WHERE u.`user_name` = 'mock_panel_admin';
DELETE FROM `tb_user` WHERE `user_name` = 'mock_panel_admin';
DELETE FROM `tb_user` WHERE `user_name` LIKE 'mock_interview_%';

-- 使用现有开放原子社团；若本地数据被清空，则脚本主动报错，避免写入悬空数据。
SET @club_id = (SELECT `id` FROM `club` WHERE `code` = 'JMI-OPENATOM' LIMIT 1);
SET @admin_id = (SELECT `id` FROM `tb_user` WHERE `user_name` = 'admin' LIMIT 1);
SET @mock_password_hash = '$2y$10$Cjtb.aN2AR9a8rt6vWVd3eHFuJ32GkeGuSoTHIjKLROi2JHQseKYi';

INSERT INTO `recruitment_campaign`
    (`club_id`, `name`, `apply_start_at`, `apply_end_at`, `interview_start_at`,
     `interview_end_at`, `result_publish_at`, `target_grades`, `max_applicants`,
     `login_required`, `form_schema`, `status`)
VALUES
    (@club_id, @mock_campaign_name,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY),
     TIMESTAMP(CURRENT_DATE, '09:00:00'), TIMESTAMP(CURRENT_DATE, '19:00:00'),
     DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY), JSON_ARRAY('2024', '2025', '2026'),
     300, 1, JSON_ARRAY(), 'open');

SET @campaign_id = LAST_INSERT_ID();

-- 所有 Mock 账号的本地登录密码均为：Mock@123456。
INSERT INTO `tb_user`
    (`user_name`, `password`, `real_name`, `gender`, `phone`, `email`, `student_id`,
     `college`, `major`, `grade`, `class_name`, `user_status`, `activated_at`)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 250
)
SELECT CONCAT('mock_interview_', LPAD(n, 3, '0')),
       @mock_password_hash,
       CONCAT(
           ELT(MOD(n - 1, 20) + 1,
               '赵','钱','孙','李','周','吴','郑','王','冯','陈',
               '褚','卫','蒋','沈','韩','杨','朱','秦','许','何'),
           ELT(MOD(FLOOR((n - 1) / 20), 10) + 1,
               '子','宇','思','嘉','晨','雨','梓','俊','欣','文'),
           ELT(MOD(n * 7, 10) + 1,
               '轩','涵','悦','辰','宁','航','妍','博','琪','然')
       ),
       IF(MOD(n, 2) = 0, '女', '男'),
       CONCAT('139', LPAD(n, 8, '0')),
       CONCAT('mock.interview.', LPAD(n, 3, '0'), '@example.test'),
       CONCAT('M2026', LPAD(n, 4, '0')),
       ELT(MOD(n - 1, 5) + 1,
           '信息工程学院','船舶与智能制造学院','经济管理学院','航海技术学院','轮机与电气工程学院'),
       ELT(MOD(n - 1, 5) + 1,
           '软件技术','大数据技术','电子商务','航海技术','机电一体化'),
       '2026级',
       CONCAT(ELT(MOD(n - 1, 5) + 1,
           '软件技术','大数据技术','电子商务','航海技术','机电一体化'), '2601'),
       1,
       CURRENT_TIMESTAMP
FROM seq;

-- 独立测试管理员，避免修改本机现有 admin 账号的密码。
INSERT INTO `tb_user`
    (`user_name`, `password`, `real_name`, `gender`, `phone`, `email`, `student_id`,
     `college`, `major`, `grade`, `class_name`, `user_status`, `activated_at`)
VALUES
    ('mock_panel_admin', @mock_password_hash, '面试演练管理员', '男', '13999990000',
     'mock.panel.admin@example.test', 'MOCK-ADMIN-001', '信息工程学院', '软件技术',
     '教师', '面试工作组', 1, CURRENT_TIMESTAMP);

SET @mock_panel_admin_id = LAST_INSERT_ID();

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT @mock_panel_admin_id, r.`id`
FROM `sys_role` r
WHERE r.`code` = 'super_admin';

-- 第一志愿在 5 个部门间均匀分布。
INSERT INTO `membership_application`
    (`campaign_id`, `club_id`, `user_id`, `first_choice_department_id`,
     `second_choice_department_id`, `status`, `profile`, `created_at`)
SELECT @campaign_id,
       @club_id,
       u.`id`,
       ELT(MOD(CAST(RIGHT(u.`user_name`, 3) AS UNSIGNED) - 1, 5) + 1, 1, 2, 3, 4, 9),
       ELT(MOD(CAST(RIGHT(u.`user_name`, 3) AS UNSIGNED), 5) + 1, 1, 2, 3, 4, 9),
       'interview_scheduled',
       JSON_OBJECT(
           'name', u.`real_name`,
           'applicantName', u.`real_name`,
           'studentId', u.`student_id`,
           'sex', u.`gender`,
           'college', u.`college`,
           'class', u.`class_name`,
           'contact', u.`phone`,
           'field_7', CONCAT('希望通过社团项目提升实践能力，并参与开源协作。测试候选人 ', RIGHT(u.`user_name`, 3)),
           'field_8', ELT(MOD(CAST(RIGHT(u.`user_name`, 3) AS UNSIGNED) - 1, 5) + 1,
                          '项目部','宣传组','活动部','外联部','社区部')
       ),
       DATE_SUB(CURRENT_TIMESTAMP, INTERVAL MOD(CAST(RIGHT(u.`user_name`, 3) AS UNSIGNED), 20) DAY)
FROM `tb_user` u
WHERE u.`user_name` LIKE 'mock_interview_%';

INSERT INTO `interview_session`
    (`campaign_id`, `name`, `round`, `scheduled_start_at`, `scheduled_end_at`,
     `duration_minutes`, `gap_minutes`, `mode`, `status`, `created_by`)
VALUES
    (@campaign_id, '本地压力测试面试场', 1,
     TIMESTAMP(CURRENT_DATE, '09:00:00'), TIMESTAMP(CURRENT_DATE, '19:00:00'),
     10, 2, 'offline', 'published', @admin_id);

SET @session_id = LAST_INSERT_ID();

INSERT INTO `interview_room` (`session_id`, `name`, `location`, `sort_order`) VALUES
(@session_id, '星火厅 A', '教学楼 A201', 1),
(@session_id, '星火厅 B', '教学楼 A202', 2),
(@session_id, '星火厅 C', '教学楼 A203', 3),
(@session_id, '星火厅 D', '教学楼 A204', 4),
(@session_id, '星火厅 E', '教学楼 A205', 5);

SET @room_1 = (SELECT `id` FROM `interview_room` WHERE `session_id` = @session_id AND `sort_order` = 1);
SET @room_2 = (SELECT `id` FROM `interview_room` WHERE `session_id` = @session_id AND `sort_order` = 2);
SET @room_3 = (SELECT `id` FROM `interview_room` WHERE `session_id` = @session_id AND `sort_order` = 3);
SET @room_4 = (SELECT `id` FROM `interview_room` WHERE `session_id` = @session_id AND `sort_order` = 4);
SET @room_5 = (SELECT `id` FROM `interview_room` WHERE `session_id` = @session_id AND `sort_order` = 5);

-- 测试管理员账号在 A 厅，可直接测试面试官工作台。
INSERT INTO `interview_room_interviewer` (`room_id`, `interviewer_id`) VALUES
(@room_1, @mock_panel_admin_id), (@room_1, 2), (@room_1, 3),
(@room_2, 4), (@room_2, 5), (@room_2, 6),
(@room_3, 7), (@room_3, 8), (@room_3, 9),
(@room_4, 10), (@room_4, 11), (@room_4, 12),
(@room_5, 13), (@room_5, 16), (@room_5, 19);

INSERT INTO `interview`
    (`session_id`, `room_id`, `queue_number`, `application_id`, `round`,
     `scheduled_start_at`, `scheduled_end_at`, `location`, `mode`, `status`)
SELECT @session_id,
       ELT(MOD(ranked.seq_no - 1, 5) + 1, @room_1, @room_2, @room_3, @room_4, @room_5),
       ranked.seq_no,
       ranked.application_id,
       1,
       DATE_ADD(TIMESTAMP(CURRENT_DATE, '09:00:00'), INTERVAL (FLOOR((ranked.seq_no - 1) / 5) * 12) MINUTE),
       DATE_ADD(TIMESTAMP(CURRENT_DATE, '09:00:00'), INTERVAL (FLOOR((ranked.seq_no - 1) / 5) * 12 + 10) MINUTE),
       ELT(MOD(ranked.seq_no - 1, 5) + 1,
           '教学楼 A201','教学楼 A202','教学楼 A203','教学楼 A204','教学楼 A205'),
       'offline',
       'pending'
FROM (
    SELECT a.`id` AS application_id,
           ROW_NUMBER() OVER (ORDER BY a.`id`) AS seq_no
    FROM `membership_application` a
    WHERE a.`campaign_id` = @campaign_id
) ranked;

INSERT INTO `interview_interviewer` (`interview_id`, `interviewer_id`)
SELECT i.`id`, ri.`interviewer_id`
FROM `interview` i
JOIN `interview_room_interviewer` ri ON ri.`room_id` = i.`room_id`
WHERE i.`session_id` = @session_id;

-- 工作人员完成“确认面试”后，再由现场手动签到进入叫号队列。

SELECT @campaign_id AS mock_campaign_id,
       @session_id AS mock_session_id,
       (SELECT COUNT(*) FROM `membership_application` WHERE `campaign_id` = @campaign_id) AS applications,
       (SELECT COUNT(*) FROM `interview` WHERE `session_id` = @session_id) AS interviews,
       (SELECT COUNT(*) FROM `interview_room` WHERE `session_id` = @session_id) AS rooms,
       (SELECT COUNT(*) FROM `interview_queue_state` WHERE `session_id` = @session_id) AS checked_in;
