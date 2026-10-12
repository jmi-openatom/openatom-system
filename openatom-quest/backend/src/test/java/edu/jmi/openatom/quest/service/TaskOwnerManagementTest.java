package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.model.CurrentMember;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TaskOwnerManagementTest {
    private JdbcTemplate jdbc;
    private AdminWorkflowService service;
    private TaskWorkflowService taskService;
    private final CurrentMember admin = actor(2000L, "ADMIN", List.of("task:manage", "submission:review"));
    private final CurrentMember originalOwner = actor(1000L, "PROJECT_OWNER", List.of("task:manage", "submission:review"));
    private final CurrentMember newOwner = actor(1001L, "PROJECT_OWNER", List.of("task:manage", "submission:review"));

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:quest_task_owner;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE quest_member (id BIGINT PRIMARY KEY, nickname VARCHAR(64), status VARCHAR(24))");
        jdbc.execute("CREATE TABLE quest_member_role (member_id BIGINT, role_id BIGINT)");
        jdbc.execute("CREATE TABLE quest_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE quest_permission (id BIGINT PRIMARY KEY, permission_key VARCHAR(64))");
        jdbc.execute("CREATE TABLE quest_task (id BIGINT PRIMARY KEY, title VARCHAR(160), owner_member_id BIGINT, status VARCHAR(24))");
        jdbc.execute("CREATE TABLE quest_task_assignment (id BIGINT PRIMARY KEY, task_id BIGINT, member_id BIGINT, status VARCHAR(24))");
        jdbc.execute("""
            CREATE TABLE quest_task_submission (
                id BIGINT PRIMARY KEY, assignment_id BIGINT, status VARCHAR(24), version_no INT,
                submitted_at TIMESTAMP, completion_note VARCHAR(1000), repository_url VARCHAR(512),
                pull_request_url VARCHAR(512), demo_url VARCHAR(512), video_url VARCHAR(512),
                problems_and_learning VARCHAR(1000), ai_used BOOLEAN, ai_usage_detail VARCHAR(1000))
            """);
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
        jdbc.update("""
            INSERT INTO quest_member VALUES (1, '普通成员', 'ACTIVE'), (1000, '原负责人', 'ACTIVE'),
                (1001, '新负责人', 'ACTIVE'), (1002, '导师', 'ACTIVE'),
                (1003, '禁用负责人', 'DISABLED'), (2000, '管理员', 'ACTIVE')
            """);
        jdbc.update("INSERT INTO quest_permission VALUES (1, 'submission:review')");
        jdbc.update("INSERT INTO quest_role_permission VALUES (2, 1), (3, 1), (4, 1)");
        jdbc.update("INSERT INTO quest_member_role VALUES (1, 1), (1000, 3), (1001, 3), (1002, 2), (1003, 3), (2000, 4)");
        jdbc.update("""
            INSERT INTO quest_task VALUES (10, '已有提交的任务', 1000, 'PUBLISHED'),
                (11, '草稿任务', 1000, 'DRAFT'), (12, '下架任务', 1000, 'OFFLINE'),
                (13, '归档任务', 1000, 'ARCHIVED')
            """);
        jdbc.update("INSERT INTO quest_task_assignment VALUES (100, 10, 1, 'PENDING_REVIEW')");
        jdbc.update("""
            INSERT INTO quest_task_submission (id, assignment_id, status, version_no, submitted_at, completion_note)
            VALUES (200, 100, 'SUBMITTED', 2, CURRENT_TIMESTAMP, '已有成果')
            """);
        ObjectMapper mapper = new ObjectMapper();
        AuditService audit = new AuditService(jdbc, mapper);
        service = new AdminWorkflowService(jdbc, mapper, audit);
        taskService = new TaskWorkflowService(jdbc, mapper, audit, null);
    }

    @Test
    void changingOwnerTransfersReviewScopeWithoutChangingMemberWork() {
        Map<String, Object> assignment = jdbc.queryForMap("SELECT * FROM quest_task_assignment WHERE id = 100");
        Map<String, Object> submission = jdbc.queryForMap("SELECT * FROM quest_task_submission WHERE id = 200");
        assertThat(taskService.reviewQueue(originalOwner)).hasSize(1);
        assertThat(taskService.reviewQueue(newOwner)).isEmpty();

        service.updateTaskOwner(admin, 10, 1001);

        assertThat(jdbc.queryForObject("SELECT owner_member_id FROM quest_task WHERE id = 10", Long.class)).isEqualTo(1001L);
        assertThat(taskService.reviewQueue(originalOwner)).isEmpty();
        assertThat(taskService.reviewQueue(newOwner)).hasSize(1);
        assertThat(jdbc.queryForMap("SELECT * FROM quest_task_assignment WHERE id = 100")).isEqualTo(assignment);
        assertThat(jdbc.queryForMap("SELECT * FROM quest_task_submission WHERE id = 200")).isEqualTo(submission);
        assertThatThrownBy(() -> service.assignmentOptions(originalOwner, 10)).hasMessageContaining("自己负责");
        assertThat(service.assignmentOptions(newOwner, 10)).isNotEmpty();
        assertThat(jdbc.queryForMap("SELECT member_id, notification_type, action_url FROM quest_notification"))
            .containsEntry("member_id", 1001L).containsEntry("notification_type", "TASK_OWNER_ASSIGNED")
            .containsEntry("action_url", "/reviews");
        assertThat(jdbc.queryForObject("SELECT detail_json FROM quest_audit_log WHERE action = 'TASK_OWNER_CHANGE'", String.class))
            .contains("\"previousOwnerMemberId\":1000", "\"ownerMemberId\":1001");
    }

    @Test
    void administratorCanAssignDraftAndOfflineTasksToMentorsAndAdmins() {
        service.updateTaskOwner(admin, 11, 1002);
        service.updateTaskOwner(admin, 12, 2000);
        assertThat(jdbc.queryForObject("SELECT owner_member_id FROM quest_task WHERE id = 11", Long.class)).isEqualTo(1002L);
        assertThat(jdbc.queryForObject("SELECT owner_member_id FROM quest_task WHERE id = 12", Long.class)).isEqualTo(2000L);
    }

    @Test
    void projectOwnerCannotTransferOwnershipEvenOfOwnTask() {
        assertThatThrownBy(() -> service.updateTaskOwner(originalOwner, 10, 1001)).hasMessageContaining("只有管理员");
        assertThatThrownBy(() -> service.updateTaskOwner(actor(1L, "MEMBER", List.of()), 10, 1001)).hasMessageContaining("无权");
        assertThat(jdbc.queryForObject("SELECT owner_member_id FROM quest_task WHERE id = 10", Long.class)).isEqualTo(1000L);
    }

    @Test
    void administratorWithoutTaskManagementPermissionCannotChangeOwner() {
        assertThatThrownBy(() -> service.updateTaskOwner(actor(2000L, "ADMIN", List.of()), 10, 1001))
            .hasMessageContaining("无权");
    }

    @Test
    void ordinaryDisabledAndMissingMembersCannotBeOwners() {
        for (long memberId : List.of(1L, 1003L, 9999L)) {
            assertThatThrownBy(() -> service.updateTaskOwner(admin, 10, memberId))
                .hasMessageContaining("启用中的导师、项目负责人或管理员");
        }
        assertThat(jdbc.queryForObject("SELECT owner_member_id FROM quest_task WHERE id = 10", Long.class)).isEqualTo(1000L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_notification", Integer.class)).isZero();
    }

    @Test
    void archivedAndMissingTasksRejectOwnershipChanges() {
        assertThatThrownBy(() -> service.updateTaskOwner(admin, 13, 1001)).hasMessageContaining("归档");
        assertThatThrownBy(() -> service.updateTaskOwner(admin, 9999, 1001)).hasMessageContaining("不存在");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_audit_log", Integer.class)).isZero();
    }

    @Test
    void assigningCurrentOwnerDoesNotCreateDuplicateNotificationsOrAuditLogs() {
        service.updateTaskOwner(admin, 10, 1000);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_notification", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_audit_log", Integer.class)).isZero();
    }

    private static CurrentMember actor(long id, String role, List<String> permissions) {
        return new CurrentMember(id, "验收用户", null, "ACTIVE", "L0", 0, true, true, List.of(role), permissions);
    }
}
