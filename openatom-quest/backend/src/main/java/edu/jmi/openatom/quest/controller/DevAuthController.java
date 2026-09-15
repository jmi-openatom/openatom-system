package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {
    private static final String MEMBER_ID = "member.id";
    private final JdbcTemplate jdbcTemplate;

    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    @Transactional
    public ApiResponse<Map<String, Object>> login(
        @RequestParam(defaultValue = "MEMBER") String role,
        HttpSession session
    ) {
        if (!List.of("MEMBER", "MENTOR", "PROJECT_OWNER", "ADMIN").contains(role)) {
            throw new IllegalArgumentException("不支持的开发角色");
        }
        String subject = "quest-dev-" + role.toLowerCase();
        Long memberId = jdbcTemplate.query("""
            SELECT member_id FROM quest_oauth_identity WHERE provider = 'dev' AND subject = ?
            """, resultSet -> resultSet.next() ? resultSet.getLong(1) : null, subject);
        if (memberId == null) {
            jdbcTemplate.update("""
                INSERT INTO quest_member (
                    nickname, school, college, major, grade, skills_json, weekly_hours, bio,
                    conduct_agreed_at, profile_completed_at, onboarding_completed_at, status
                ) VALUES (?, '本地开发学校', '开源学院', '软件工程', '2026级', '[\"Git\",\"Vue\",\"Java\"]', 8,
                          '仅用于本地界面与业务验收', NOW(3), NOW(3), NOW(3), 'ACTIVE')
                """, "本地" + role);
            memberId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jdbcTemplate.update("""
                INSERT INTO quest_oauth_identity
                    (member_id, provider, subject, display_name, first_login_at, last_login_at)
                VALUES (?, 'dev', ?, ?, ?, ?)
                """, memberId, subject, "本地" + role, LocalDateTime.now(), LocalDateTime.now());
            jdbcTemplate.update("INSERT INTO quest_onboarding_progress (member_id, current_step, completed_steps_json) VALUES (?, 7, '[1,2,3,4,5,6,7]')", memberId);
            jdbcTemplate.update("""
                INSERT INTO quest_member_direction (member_id, direction_id, is_primary)
                SELECT ?, id, TRUE FROM quest_technical_direction WHERE direction_key = 'frontend'
                """, memberId);
        }
        jdbcTemplate.update("""
            INSERT IGNORE INTO quest_member_role (member_id, role_id)
            SELECT ?, id FROM quest_role WHERE role_key IN ('MEMBER', ?)
            """, memberId, role);
        session.setAttribute(MEMBER_ID, memberId);
        return ApiResponse.ok(Map.of("memberId", memberId, "role", role));
    }
}
