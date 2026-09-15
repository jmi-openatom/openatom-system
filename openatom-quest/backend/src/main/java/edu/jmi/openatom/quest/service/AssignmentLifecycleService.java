package edu.jmi.openatom.quest.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentLifecycleService {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    @Scheduled(fixedDelayString = "${quest.assignment-reconcile-ms:300000}")
    @Transactional
    public void reconcile() {
        notifyUpcomingDeadlines();
        markOverdueAssignments();
    }

    private void notifyUpcomingDeadlines() {
        List<Map<String, Object>> assignments = jdbcTemplate.queryForList("""
            SELECT a.id, a.member_id, t.title, a.due_at
            FROM quest_task_assignment a
            JOIN quest_task t ON t.id = a.task_id
            WHERE a.status IN ('IN_PROGRESS', 'REVISION_REQUIRED')
              AND a.due_at > NOW(3) AND a.due_at <= DATE_ADD(NOW(3), INTERVAL 24 HOUR)
              AND NOT EXISTS (
                  SELECT 1 FROM quest_notification n
                  WHERE n.member_id = a.member_id
                    AND n.notification_type = 'TASK_DEADLINE_SOON'
                    AND n.business_type = 'TASK_ASSIGNMENT'
                    AND CAST(n.business_id AS UNSIGNED) = a.id
              )
            """);
        for (Map<String, Object> assignment : assignments) {
            jdbcTemplate.update("""
                INSERT INTO quest_notification
                    (member_id, notification_type, title, content, action_url, business_type, business_id)
                VALUES (?, 'TASK_DEADLINE_SOON', '任务即将截止', ?, ?, 'TASK_ASSIGNMENT', ?)
                """,
                assignment.get("member_id"),
                "任务「" + assignment.get("title") + "」将在 24 小时内截止，请及时提交。",
                "/assignments/" + assignment.get("id"),
                String.valueOf(assignment.get("id"))
            );
        }
    }

    private void markOverdueAssignments() {
        List<Map<String, Object>> overdue = jdbcTemplate.queryForList("""
            SELECT a.id, a.member_id, t.title
            FROM quest_task_assignment a
            JOIN quest_task t ON t.id = a.task_id
            WHERE a.status IN ('IN_PROGRESS', 'REVISION_REQUIRED') AND a.due_at < NOW(3)
            FOR UPDATE
            """);
        for (Map<String, Object> assignment : overdue) {
            long assignmentId = ((Number) assignment.get("id")).longValue();
            jdbcTemplate.update("UPDATE quest_task_assignment SET status = 'OVERDUE' WHERE id = ?", assignmentId);
            jdbcTemplate.update("""
                INSERT INTO quest_notification
                    (member_id, notification_type, title, content, action_url, business_type, business_id)
                VALUES (?, 'TASK_OVERDUE', '任务已逾期', ?, ?, 'TASK_ASSIGNMENT', ?)
                """,
                assignment.get("member_id"),
                "任务「" + assignment.get("title") + "」已超过截止时间。",
                "/assignments/" + assignmentId,
                String.valueOf(assignmentId)
            );
            auditService.record(null, "TASK_ASSIGNMENT_OVERDUE", "TASK_ASSIGNMENT", assignmentId, Map.of());
        }
    }
}
