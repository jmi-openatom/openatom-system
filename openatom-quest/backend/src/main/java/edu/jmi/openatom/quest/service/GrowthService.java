package edu.jmi.openatom.quest.service;

import edu.jmi.openatom.quest.model.CurrentMember;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrowthService {
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    public List<Map<String, Object>> stages() {
        return jdbcTemplate.queryForList("""
            SELECT id, stage_key AS stageKey, name, objective
            FROM quest_growth_stage ORDER BY stage_key
            """);
    }

    public Map<String, Object> memberGrowth(CurrentMember member) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("level", member.currentLevel());
        result.put("points", member.totalPoints());
        result.put("rules", jdbcTemplate.queryForList("""
            SELECT level_key AS levelKey, name, minimum_points AS minimumPoints, sort_order AS sortOrder
            FROM quest_level_rule WHERE status = 'ACTIVE' ORDER BY sort_order
            """));
        result.put("ledger", jdbcTemplate.queryForList("""
            SELECT id, amount, balance_after AS balanceAfter, source_type AS sourceType,
                   source_id AS sourceId, reason, created_at AS createdAt
            FROM quest_point_ledger WHERE member_id = ? ORDER BY created_at DESC LIMIT 100
            """, member.id()));
        result.put("routes", jdbcTemplate.queryForList("""
            SELECT route.id, route.name, member_route.status, stage.stage_key AS currentStage,
                   member_route.enrolled_at AS enrolledAt, member_route.completed_at AS completedAt
            FROM quest_member_route member_route
            JOIN quest_growth_route route ON route.id = member_route.route_id
            LEFT JOIN quest_growth_stage stage ON stage.id = member_route.current_stage_id
            WHERE member_route.member_id = ? ORDER BY member_route.updated_at DESC
            """, member.id()));
        return result;
    }

    public List<Map<String, Object>> recommendedRoutes(CurrentMember member) {
        return jdbcTemplate.queryForList("""
            SELECT r.id, r.route_key AS routeKey, r.name, r.description,
                   d.id AS directionId, d.name AS directionName,
                   COUNT(DISTINCT rs.stage_id) AS stageCount,
                   COUNT(DISTINCT t.id) AS taskCount,
                   mr.id AS enrollmentId, COALESCE(mr.status, 'NOT_ENROLLED') AS memberStatus
            FROM quest_growth_route r
            JOIN quest_technical_direction d ON d.id = r.direction_id
            JOIN quest_member_direction md ON md.direction_id = r.direction_id AND md.member_id = ?
            LEFT JOIN quest_route_stage rs ON rs.route_id = r.id
            LEFT JOIN quest_task t ON t.route_id = r.id AND t.status = 'PUBLISHED'
            LEFT JOIN quest_member_route mr ON mr.route_id = r.id AND mr.member_id = ?
            WHERE r.status = 'PUBLISHED'
            GROUP BY r.id, r.route_key, r.name, r.description, d.id, d.name, mr.id, mr.status
            ORDER BY (mr.status = 'ACTIVE') DESC, d.sort_order, r.name
            """, member.id(), member.id());
    }

    public Map<String, Object> routeDetail(CurrentMember member, long routeId) {
        List<Map<String, Object>> routes = jdbcTemplate.queryForList("""
            SELECT r.id, r.route_key AS routeKey, r.name, r.description,
                   d.name AS directionName, COALESCE(mr.status, 'NOT_ENROLLED') AS memberStatus
            FROM quest_growth_route r
            JOIN quest_technical_direction d ON d.id = r.direction_id
            LEFT JOIN quest_member_route mr ON mr.route_id = r.id AND mr.member_id = ?
            WHERE r.id = ? AND r.status = 'PUBLISHED'
            """, member.id(), routeId);
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("成长路线不存在或尚未发布");
        }
        Map<String, Object> result = new LinkedHashMap<>(routes.getFirst());
        result.put("stages", jdbcTemplate.queryForList("""
            SELECT s.id, s.stage_key AS stageKey, s.name, s.objective, rs.sort_order AS sortOrder,
                   rs.required_task_count AS requiredTaskCount,
                   COUNT(DISTINCT t.id) AS publishedTasks,
                   SUM(CASE WHEN a.status = 'PASSED' THEN 1 ELSE 0 END) AS passedTasks
            FROM quest_route_stage rs
            JOIN quest_growth_stage s ON s.id = rs.stage_id
            LEFT JOIN quest_task t ON t.route_id = rs.route_id AND t.stage_id = s.id AND t.status = 'PUBLISHED'
            LEFT JOIN quest_task_assignment a ON a.task_id = t.id AND a.member_id = ?
            WHERE rs.route_id = ?
            GROUP BY s.id, s.stage_key, s.name, s.objective, rs.sort_order, rs.required_task_count
            ORDER BY rs.sort_order
            """, member.id(), routeId));
        return result;
    }

    @Transactional
    public Map<String, Object> enroll(CurrentMember member, long routeId) {
        Long firstStage = jdbcTemplate.query("""
            SELECT rs.stage_id FROM quest_route_stage rs
            JOIN quest_growth_route r ON r.id = rs.route_id
            WHERE r.id = ? AND r.status = 'PUBLISHED'
            ORDER BY rs.sort_order LIMIT 1
            """, rs -> rs.next() ? rs.getLong(1) : null, routeId);
        if (firstStage == null) {
            throw new IllegalArgumentException("成长路线不存在、未发布或未配置阶段");
        }
        try {
            jdbcTemplate.update("""
                INSERT INTO quest_member_route (member_id, route_id, status, current_stage_id)
                VALUES (?, ?, 'ACTIVE', ?)
                """, member.id(), routeId, firstStage);
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("你已经加入该成长路线");
        }
        Long enrollmentId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_member_route WHERE member_id = ? AND route_id = ?",
            Long.class,
            member.id(),
            routeId
        );
        auditService.record(member.id(), "ROUTE_ENROLL", "MEMBER_ROUTE", enrollmentId, Map.of("routeId", routeId));
        return Map.of("enrollmentId", enrollmentId, "status", "ACTIVE");
    }
}
