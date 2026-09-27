package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.jmi.openatom.quest.dto.SubmitSiteExplorationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SiteExplorationServiceTest {
    private SiteExplorationService service;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:site_exploration;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbcTemplate.execute("DROP TABLE IF EXISTS quest_site_exploration_flag");
        jdbcTemplate.execute("""
            CREATE TABLE quest_site_exploration_flag (
                member_id BIGINT NOT NULL,
                page_key VARCHAR(24) NOT NULL,
                flag_value VARCHAR(64) NOT NULL UNIQUE,
                PRIMARY KEY (member_id, page_key)
            )
            """);
        service = new SiteExplorationService(jdbcTemplate);
    }

    @Test
    void flagsStayStablePerMemberAndCannotBeSubmittedByAnotherMember() {
        String firstAbout = service.flagFor(1, "about");
        String firstRegulations = service.flagFor(1, "regulations");
        String firstActivities = service.flagFor(1, "activities");
        String secondAbout = service.flagFor(2, "about");

        assertThat(firstAbout).startsWith("OA{ABOUT-").isEqualTo(service.flagFor(1, "about"));
        assertThat(secondAbout).isNotEqualTo(firstAbout);
        assertThatThrownBy(() -> service.verify(2, new SubmitSiteExplorationRequest(
            firstAbout, firstRegulations, firstActivities, "我想先参加一次开源活动")))
            .hasMessageContaining("关于我们");
        service.verify(1, new SubmitSiteExplorationRequest(
            firstAbout, firstRegulations, firstActivities, "我想先参加一次开源活动"));
        assertThatThrownBy(() -> service.flagFor(1, "unknown"))
            .hasMessageContaining("探索页面不存在");
    }
}
