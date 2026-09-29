package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.RestartAssignmentRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AdminAssignmentManagementTest {
    private JdbcTemplate jdbc;
    private AuditService audit;
    private AdminWorkflowService service;
    private final CurrentMember admin = new CurrentMember(9L, "管理员", null, "ACTIVE", "L0", 0,
        true, true, List.of("ADMIN"), List.of("stats:global"));

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:quest_admin_assignments;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP TABLE IF EXISTS quest_notification");
        jdbc.execute("DROP TABLE IF EXISTS quest_audit_log");
        jdbc.execute("DROP TABLE IF EXISTS quest_task_submission");
        jdbc.execute("DROP TABLE IF EXISTS quest_task_assignment");
        jdbc.execute("DROP TABLE IF EXISTS quest_task");
        jdbc.execute("DROP TABLE IF EXISTS quest_member");
        jdbc.execute("CREATE TABLE quest_member (id BIGINT PRIMARY KEY, nickname VARCHAR(64), email VARCHAR(128), current_level VARCHAR(16))");
        jdbc.execute("CREATE TABLE quest_task (id BIGINT PRIMARY KEY, task_key VARCHAR(64), title VARCHAR(160))");
        jdbc.execute("""
            CREATE TABLE quest_task_assignment (
                id BIGINT PRIMARY KEY, task_id BIGINT, member_id BIGINT, status VARCHAR(32),
                source VARCHAR(24), claimed_at TIMESTAMP, due_at TIMESTAMP,
                updated_at TIMESTAMP, completed_at TIMESTAMP, abandoned_at TIMESTAMP)
            """);
        jdbc.execute("CREATE TABLE quest_task_submission (id BIGINT PRIMARY KEY, assignment_id BIGINT)");
        jdbc.execute("""
            CREATE TABLE quest_notification (
                member_id BIGINT, notification_type VARCHAR(48), title VARCHAR(160),
                content VARCHAR(1000), action_url VARCHAR(512), business_type VARCHAR(64), business_id VARCHAR(128))
            """);
        jdbc.execute("""
            CREATE TABLE quest_audit_log (
                actor_member_id BIGINT, action VARCHAR(64), target_type VARCHAR(64),
                target_id VARCHAR(128), result VARCHAR(24), detail_json VARCHAR(2000))
            """);
        jdbc.update("INSERT INTO quest_member VALUES (1, '成员甲', 'a@example.com', 'L0')");
        jdbc.update("INSERT INTO quest_member VALUES (2, '成员乙', 'b@example.com', 'L1')");
        jdbc.update("INSERT INTO quest_task VALUES (10, 'first-task', '首个任务')");
        jdbc.update("""
            INSERT INTO quest_task_assignment
                (id, task_id, member_id, status, source, claimed_at, due_at, updated_at)
            VALUES (100, 10, 1, 'IN_PROGRESS', 'CLAIMED', DATEADD('DAY', -4, CURRENT_TIMESTAMP),
                DATEADD('DAY', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP)
            """);
        jdbc.update("INSERT INTO quest_task_submission VALUES (200, 100)");
        audit = new AuditService(jdbc, new ObjectMapper());
        service = new AdminWorkflowService(jdbc, new ObjectMapper(), audit);
    }

    @Test
    void overdueProgressIsVisibleAndRestartPreservesHistory() {
        List<Map<String, Object>> progress = service.memberProgress(admin);
        assertThat(progress).hasSize(2);
        assertThat(((Number) progress.getFirst().get("overdue")).intValue()).isEqualTo(1);
        assertThat(((Number) progress.getFirst().get("total")).intValue()).isEqualTo(1);

        Map<String, Object> page = service.assignments(admin, 1L, null, "OVERDUE", 1, 20);
        assertThat(((Number) page.get("total")).intValue()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) page.get("items");
        assertThat(items.getFirst().get("status")).isEqualTo("OVERDUE");

        LocalDateTime nextDueAt = LocalDateTime.now().plusDays(7);
        service.restartAssignment(admin, 100, new RestartAssignmentRequest(nextDueAt, "成员需要补充时间"));
        assertThat(jdbc.queryForObject("SELECT status FROM quest_task_assignment WHERE id = 100", String.class))
            .isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_task_submission WHERE assignment_id = 100", Integer.class))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_notification WHERE member_id = 1", Integer.class))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_audit_log WHERE action = 'TASK_ASSIGNMENT_RESTART'", Integer.class))
            .isEqualTo(1);
        assertThat(service.assignments(admin, 1L, null, "OVERDUE", 1, 20).get("total")).isEqualTo(0L);
    }

    @Test
    void allStatusIncludesOtherStatusesWithinTheSelectedMember() {
        jdbc.update("""
            INSERT INTO quest_task_assignment
                (id, task_id, member_id, status, source, claimed_at, updated_at)
            VALUES (101, 10, 1, 'PASSED', 'CLAIMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                   (102, 10, 2, 'PASSED', 'CLAIMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);

        Map<String, Object> allForMember = service.assignments(admin, 1L, null, "ALL", 1, 20);
        assertThat(((Number) allForMember.get("total")).intValue()).isEqualTo(2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) allForMember.get("items");
        assertThat(items).extracting(item -> item.get("status")).containsExactlyInAnyOrder("OVERDUE", "PASSED");
        assertThat(((Number) service.assignments(admin, 1L, null, "OVERDUE", 1, 20).get("total")).intValue()).isEqualTo(1);
        assertThat(((Number) service.assignments(admin, null, null, "ALL", 1, 20).get("total")).intValue()).isEqualTo(3);
    }

    @Test
    void restartRejectsNonOverdueAssignmentsAndMembersWithoutPermission() {
        CurrentMember member = new CurrentMember(1L, "成员甲", null, "ACTIVE", "L0", 0,
            true, true, List.of("MEMBER"), List.of());
        RestartAssignmentRequest request = new RestartAssignmentRequest(LocalDateTime.now().plusDays(3), "重新开始");
        assertThatThrownBy(() -> service.restartAssignment(member, 100, request))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("无权");

        jdbc.update("UPDATE quest_task_assignment SET status = 'PASSED' WHERE id = 100");
        assertThatThrownBy(() -> service.restartAssignment(admin, 100, request))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("只有逾期任务");
    }
}
