package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.AssignTaskRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ProjectOwnerAssignmentTest {
    private JdbcTemplate jdbc;
    private AdminWorkflowService service;
    private final CurrentMember owner = actor(1000L, "PROJECT_OWNER", List.of("task:manage"));
    private final CurrentMember admin = actor(2000L, "ADMIN", List.of("task:manage"));

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:quest_owner_assignment;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE quest_member (id BIGINT PRIMARY KEY, nickname VARCHAR(64), status VARCHAR(24))");
        jdbc.execute("""
            CREATE TABLE quest_task (
                id BIGINT PRIMARY KEY, title VARCHAR(160), owner_member_id BIGINT,
                status VARCHAR(24), deadline_type VARCHAR(24) DEFAULT 'NONE',
                fixed_deadline TIMESTAMP, duration_hours INT, capacity INT)
            """);
        jdbc.execute("""
            CREATE TABLE quest_task_assignment (
                id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT, member_id BIGINT, assigned_by BIGINT,
                source VARCHAR(24), status VARCHAR(24), claimed_at TIMESTAMP, due_at TIMESTAMP,
                UNIQUE (task_id, member_id))
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
            INSERT INTO quest_member VALUES (1, '成员甲', 'ACTIVE'), (2, '成员乙', 'ACTIVE'),
                (3, '禁用成员', 'DISABLED'), (1000, '负责人', 'ACTIVE'), (2000, '管理员', 'ACTIVE')
            """);
        jdbc.update("""
            INSERT INTO quest_task (id, title, owner_member_id, status) VALUES
                (10, '我的已发布任务', 1000, 'PUBLISHED'),
                (11, '其他人的任务', 2000, 'PUBLISHED'),
                (12, '我的草稿', 1000, 'DRAFT')
            """);
        ObjectMapper mapper = new ObjectMapper();
        service = new AdminWorkflowService(jdbc, mapper, new AuditService(jdbc, mapper));
    }

    @Test
    void ownerWithIdOutsideLongCacheCanAssignOwnTaskAndNotifyMember() {
        Map<String, Object> result = service.assignTask(owner, 10, new AssignTaskRequest(1L));

        assertThat(result).containsEntry("status", "IN_PROGRESS");
        assertThat(jdbc.queryForMap("SELECT member_id, assigned_by, source FROM quest_task_assignment WHERE id = ?",
            result.get("assignmentId")))
            .containsEntry("member_id", 1L).containsEntry("assigned_by", 1000L).containsEntry("source", "ASSIGNED");
        assertThat(jdbc.queryForMap("SELECT member_id, content, notification_type FROM quest_notification"))
            .containsEntry("member_id", 1L).containsEntry("notification_type", "TASK_ASSIGNED")
            .containsEntry("content", "已为你分配任务「我的已发布任务」。");
        assertThat(jdbc.queryForObject("SELECT actor_member_id FROM quest_audit_log WHERE action = 'TASK_ASSIGN'", Long.class))
            .isEqualTo(1000L);
    }

    @Test
    void optionsOnlyExposeActiveUnassignedMemberIdsAndNicknames() {
        service.assignTask(owner, 10, new AssignTaskRequest(1L));
        jdbc.update("UPDATE quest_task_assignment SET status = 'ABANDONED' WHERE member_id = 1");

        List<Map<String, Object>> options = service.assignmentOptions(owner, 10);
        assertThat(options).extracting(item -> item.get("id")).containsExactlyInAnyOrder(2L, 1000L, 2000L);
        assertThat(options).allSatisfy(item -> assertThat(item).containsOnlyKeys("id", "nickname"));
    }

    @Test
    void ownerCannotReadOptionsOrAssignTasksOwnedByOthers() {
        assertThatThrownBy(() -> service.assignmentOptions(owner, 11)).hasMessageContaining("自己负责");
        assertThatThrownBy(() -> service.assignTask(owner, 11, new AssignTaskRequest(1L)))
            .hasMessageContaining("自己负责");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_task_assignment", Integer.class)).isZero();
    }

    @Test
    void ordinaryMemberCannotReadOptionsOrAssignEvenIfNamedAsOwner() {
        CurrentMember member = actor(1000L, "MEMBER", List.of("task:claim"));
        assertThatThrownBy(() -> service.assignmentOptions(member, 10)).hasMessageContaining("无权");
        assertThatThrownBy(() -> service.assignTask(member, 10, new AssignTaskRequest(1L)))
            .hasMessageContaining("无权");
    }

    @Test
    void administratorCanStillAssignTasksOwnedByOthers() {
        assertThat(service.assignmentOptions(admin, 10)).isNotEmpty();
        assertThat(service.assignTask(admin, 10, new AssignTaskRequest(1L))).containsEntry("status", "IN_PROGRESS");
    }

    @Test
    void assignmentStillRejectsDisabledMembersDraftTasksAndDuplicates() {
        assertThatThrownBy(() -> service.assignTask(owner, 10, new AssignTaskRequest(3L)))
            .hasMessageContaining("启用中的成员");
        assertThatThrownBy(() -> service.assignTask(owner, 12, new AssignTaskRequest(1L)))
            .hasMessageContaining("已发布");
        service.assignTask(owner, 10, new AssignTaskRequest(1L));
        assertThatThrownBy(() -> service.assignTask(owner, 10, new AssignTaskRequest(1L)))
            .hasMessageContaining("已经拥有");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_notification", Integer.class)).isEqualTo(1);
    }

    @Test
    void ownerAssignmentRespectsCapacityAndRelativeDeadline() {
        jdbc.update("UPDATE quest_task SET capacity = 1, deadline_type = 'AFTER_CLAIM', duration_hours = 48 WHERE id = 10");
        assertThat(service.assignTask(owner, 10, new AssignTaskRequest(1L)).get("dueAt")).isNotNull();
        assertThat(jdbc.queryForObject(
            "SELECT DATEDIFF('HOUR', claimed_at, due_at) FROM quest_task_assignment", Integer.class)).isEqualTo(48);
        assertThatThrownBy(() -> service.assignTask(owner, 10, new AssignTaskRequest(2L)))
            .hasMessageContaining("名额已满");
    }

    private static CurrentMember actor(long id, String role, List<String> permissions) {
        return new CurrentMember(id, "验收用户", null, "ACTIVE", "L0", 0, true, true, List.of(role), permissions);
    }
}
