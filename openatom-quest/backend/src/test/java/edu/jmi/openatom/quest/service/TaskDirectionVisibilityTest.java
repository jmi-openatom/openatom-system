package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.model.CurrentMember;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TaskDirectionVisibilityTest {
    private JdbcTemplate jdbc;
    private TaskWorkflowService service;
    private final CurrentMember frontendMember = member(1L);
    private final CurrentMember backendMember = member(2L);
    private final CurrentMember unconfiguredMember = member(3L);

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:quest_task_directions;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE quest_member (id BIGINT PRIMARY KEY, nickname VARCHAR(64))");
        jdbc.execute("CREATE TABLE quest_technical_direction (id BIGINT PRIMARY KEY, name VARCHAR(96))");
        jdbc.execute("""
            CREATE TABLE quest_member_direction (
                member_id BIGINT, direction_id BIGINT, is_primary BOOLEAN,
                PRIMARY KEY (member_id, direction_id))
            """);
        jdbc.execute("CREATE TABLE quest_growth_stage (id BIGINT PRIMARY KEY, stage_key VARCHAR(16), name VARCHAR(64))");
        jdbc.execute("""
            CREATE TABLE quest_task (
                id BIGINT PRIMARY KEY, task_key VARCHAR(64), title VARCHAR(160), summary VARCHAR(500),
                task_type VARCHAR(24) DEFAULT 'LEARNING', difficulty VARCHAR(24) DEFAULT 'ENTRY',
                direction_id BIGINT, stage_id BIGINT, estimated_minutes INT DEFAULT 30, points INT DEFAULT 10,
                learning_objectives_json VARCHAR(2000) DEFAULT '[]', deadline_type VARCHAR(24) DEFAULT 'NONE',
                fixed_deadline TIMESTAMP, duration_hours INT, instructions VARCHAR(2000), resources_json VARCHAR(2000),
                submission_requirements VARCHAR(2000), acceptance_criteria VARCHAR(2000), faq_json VARCHAR(2000),
                submission_limit INT, capacity INT, required_in_stage BOOLEAN DEFAULT TRUE,
                owner_member_id BIGINT DEFAULT 9, status VARCHAR(24) DEFAULT 'PUBLISHED',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """);
        jdbc.execute("""
            CREATE TABLE quest_task_assignment (
                id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT, member_id BIGINT,
                source VARCHAR(24), status VARCHAR(24), claimed_at TIMESTAMP, due_at TIMESTAMP,
                UNIQUE (task_id, member_id))
            """);
        jdbc.execute("CREATE TABLE quest_task_prerequisite (task_id BIGINT, prerequisite_task_id BIGINT)");
        jdbc.update("INSERT INTO quest_member VALUES (1, '前端成员'), (2, '后端成员'), (3, '未选方向成员'), (9, '任务负责人')");
        jdbc.update("INSERT INTO quest_technical_direction VALUES (10, '前端'), (20, '后端'), (30, '设计')");
        jdbc.update("INSERT INTO quest_member_direction VALUES (1, 10, TRUE), (2, 20, TRUE)");
        jdbc.update("INSERT INTO quest_growth_stage VALUES (100, 'L0', '新人报到'), (101, 'L1', '开源入门')");
        jdbc.update("""
            INSERT INTO quest_task (id, task_key, title, direction_id, stage_id) VALUES
                (1000, 'common', '通用任务', NULL, 100),
                (1010, 'frontend-l0', '前端 L0', 10, 100),
                (1011, 'frontend-l1', '前端 L1', 10, 101),
                (1020, 'backend-l0', '后端 L0', 20, 100),
                (1030, 'design-l0', '设计 L0', 30, 100),
                (1040, 'frontend-draft', '未发布任务', 10, 100)
            """);
        jdbc.update("UPDATE quest_task SET status = 'DRAFT' WHERE id = 1040");
        service = new TaskWorkflowService(jdbc, new ObjectMapper(), mock(AuditService.class), mock(SiteExplorationService.class));
    }

    @Test
    void catalogIsScopedToEachMembersSelectionAndIncludesCommonTasks() {
        assertThat(taskIds(service.catalog(frontendMember, null, null)))
            .containsExactlyInAnyOrder(1000L, 1010L, 1011L);
        assertThat(taskIds(service.catalog(backendMember, null, null)))
            .containsExactlyInAnyOrder(1000L, 1020L);
    }

    @Test
    void allSelectedDirectionsIncludingSecondaryDirectionAreVisibleWithoutDuplicates() {
        jdbc.update("INSERT INTO quest_member_direction VALUES (1, 30, FALSE)");
        assertThat(taskIds(service.catalog(frontendMember, null, null)))
            .containsExactlyInAnyOrder(1000L, 1010L, 1011L, 1030L);
        assertThat(service.detail(frontendMember, 1030)).containsEntry("id", 1030L);
        assertThat(service.claim(frontendMember, 1030)).containsEntry("status", "IN_PROGRESS");
    }

    @Test
    void optionalFiltersCanOnlyNarrowTheMembersCatalog() {
        assertThat(service.catalog(frontendMember, 20L, null)).isEmpty();
        assertThat(service.catalog(frontendMember, 20L, 100L)).isEmpty();
        assertThat(taskIds(service.catalog(frontendMember, 10L, 101L))).containsExactly(1011L);
        assertThat(taskIds(service.catalog(frontendMember, null, 100L)))
            .containsExactlyInAnyOrder(1000L, 1010L);
    }

    @Test
    void noSelectedDirectionOnlyAllowsCommonTasks() {
        assertThat(taskIds(service.catalog(unconfiguredMember, null, null))).containsExactly(1000L);
        assertThat(service.detail(unconfiguredMember, 1000)).containsEntry("id", 1000L);
        assertThat(service.claim(unconfiguredMember, 1000)).containsEntry("status", "IN_PROGRESS");
        assertThatThrownBy(() -> service.detail(unconfiguredMember, 1010))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.claim(unconfiguredMember, 1010))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void directDetailAndClaimRequestsCannotAccessOtherDirections() {
        assertThat(service.detail(frontendMember, 1010)).containsEntry("id", 1010L);
        assertThat(service.detail(frontendMember, 1000)).containsEntry("id", 1000L);
        for (long taskId : List.of(1020L, 1030L, 1040L, 9999L)) {
            assertThatThrownBy(() -> service.detail(frontendMember, taskId))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.claim(frontendMember, taskId))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quest_task_assignment", Integer.class)).isZero();
        assertThat(service.claim(frontendMember, 1010)).containsEntry("status", "IN_PROGRESS");
    }

    @Test
    void changingSelectionTakesEffectWithoutReplacingTheCurrentSession() {
        jdbc.update("UPDATE quest_member_direction SET direction_id = 20 WHERE member_id = 1");
        assertThat(taskIds(service.catalog(frontendMember, null, null)))
            .containsExactlyInAnyOrder(1000L, 1020L);
        assertThatThrownBy(() -> service.detail(frontendMember, 1010))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.claim(frontendMember, 1010))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.detail(frontendMember, 1020)).containsEntry("id", 1020L);
        assertThat(service.claim(frontendMember, 1020)).containsEntry("status", "IN_PROGRESS");
    }

    @Test
    void previousOrAssignedTasksRemainAccessibleOnlyToTheirAssigneeAndDoNotExpandCatalog() {
        jdbc.update("""
            INSERT INTO quest_task_assignment (task_id, member_id, source, status)
            VALUES (1030, 1, 'ASSIGNED', 'IN_PROGRESS'), (1040, 1, 'CLAIMED', 'PASSED')
            """);
        assertThat(taskIds(service.catalog(frontendMember, null, null)))
            .containsExactlyInAnyOrder(1000L, 1010L, 1011L);
        assertThat(service.detail(frontendMember, 1030)).containsEntry("memberStatus", "IN_PROGRESS");
        assertThat(service.detail(frontendMember, 1040)).containsEntry("memberStatus", "PASSED");
        assertThatThrownBy(() -> service.detail(backendMember, 1030))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.detail(backendMember, 1040))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void administratorUsesTheSamePersonalCatalogScope() {
        CurrentMember admin = new CurrentMember(1L, "管理员", null, "ACTIVE", "L0", 0,
            true, true, List.of("ADMIN"), List.of("task:claim", "task:manage", "stats:global"));
        assertThat(taskIds(service.catalog(admin, null, null)))
            .containsExactlyInAnyOrder(1000L, 1010L, 1011L);
        assertThatThrownBy(() -> service.claim(admin, 1020))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private List<Long> taskIds(List<Map<String, Object>> tasks) {
        return tasks.stream().map(task -> ((Number) task.get("id")).longValue()).toList();
    }

    private static CurrentMember member(long id) {
        return new CurrentMember(id, "成员", null, "ACTIVE", "L0", 0,
            true, true, List.of("MEMBER"), List.of("task:read", "task:claim", "task:submit"));
    }
}
