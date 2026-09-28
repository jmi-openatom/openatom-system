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

class LeaderboardServiceTest {
    private GrowthService growthService;
    private AdminWorkflowService adminService;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:quest_leaderboard;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP TABLE IF EXISTS quest_member");
        jdbc.execute("""
            CREATE TABLE quest_member (
                id BIGINT PRIMARY KEY,
                nickname VARCHAR(64),
                avatar_url VARCHAR(512),
                current_level VARCHAR(16),
                total_points INT,
                status VARCHAR(24),
                leaderboard_visible BOOLEAN DEFAULT TRUE
            )
            """);
        jdbc.update("INSERT INTO quest_member VALUES (1, '甲', null, 'L2', 150, 'ACTIVE', TRUE)");
        jdbc.update("INSERT INTO quest_member VALUES (2, '乙', null, 'L1', 100, 'ACTIVE', TRUE)");
        jdbc.update("INSERT INTO quest_member VALUES (3, '丙', null, 'L1', 100, 'ACTIVE', TRUE)");
        jdbc.update("INSERT INTO quest_member VALUES (4, '丁', null, 'L3', 999, 'DISABLED', TRUE)");
        growthService = new GrowthService(jdbc, mock(AuditService.class));
        adminService = new AdminWorkflowService(jdbc, new ObjectMapper(), mock(AuditService.class));
    }

    @Test
    void leaderboardRanksAllVisibleActiveLevelsAndAdminCanHideMember() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> before = (List<Map<String, Object>>) growthService.leaderboard().get("members");
        assertThat(before).extracting(row -> row.get("id")).containsExactly(1L, 2L, 3L);
        assertThat(before).extracting(row -> row.get("rank")).containsExactly(1, 2, 2);

        CurrentMember actor = new CurrentMember(9L, "管理员", null, "ACTIVE", "L0", 0,
            true, true, List.of("ADMIN"), List.of("member:manage"));
        adminService.updateLeaderboardVisibility(actor, 1, false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hidden = (List<Map<String, Object>>) growthService.leaderboard().get("members");
        assertThat(hidden).extracting(row -> row.get("id")).containsExactly(2L, 3L);
        assertThat(hidden).extracting(row -> row.get("rank")).containsExactly(1, 1);

        adminService.updateLeaderboardVisibility(actor, 1, true);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> restored = (List<Map<String, Object>>) growthService.leaderboard().get("members");
        assertThat(restored).hasSize(3);
    }

    @Test
    void memberWithoutManagementPermissionCannotChangeVisibility() {
        CurrentMember actor = new CurrentMember(2L, "乙", null, "ACTIVE", "L1", 100,
            true, true, List.of("MEMBER"), List.of());
        assertThatThrownBy(() -> adminService.updateLeaderboardVisibility(actor, 1, false))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("无权");
    }
}
