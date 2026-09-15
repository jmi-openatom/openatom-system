package edu.jmi.openatom.quest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.AssignTaskRequest;
import edu.jmi.openatom.quest.dto.CreateRouteRequest;
import edu.jmi.openatom.quest.dto.CreateTaskRequest;
import edu.jmi.openatom.quest.dto.CreateDirectionRequest;
import edu.jmi.openatom.quest.dto.CreateAnnouncementRequest;
import edu.jmi.openatom.quest.dto.UpdateDirectionRequest;
import edu.jmi.openatom.quest.dto.UpdateLevelRuleRequest;
import edu.jmi.openatom.quest.dto.UpdateMemberRolesRequest;
import edu.jmi.openatom.quest.dto.UpdateMemberProfileRequest;
import edu.jmi.openatom.quest.dto.UpdateMemberStatusRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminWorkflowService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public List<Map<String, Object>> directions(CurrentMember actor) {
        requirePermission(actor, "route:manage");
        return jdbcTemplate.queryForList("""
            SELECT id, direction_key AS directionKey, name, sort_order AS sortOrder, status, created_at AS createdAt
            FROM quest_technical_direction ORDER BY sort_order, id
            """);
    }

    public List<Map<String, Object>> announcements(CurrentMember actor) {
        requirePermission(actor, "stats:global");
        return jdbcTemplate.queryForList("""
            SELECT id, title, content, status, published_at AS publishedAt, expires_at AS expiresAt, created_at AS createdAt
            FROM quest_announcement ORDER BY created_at DESC LIMIT 100
            """);
    }

    @Transactional
    public Map<String, Object> createAnnouncement(CurrentMember actor, CreateAnnouncementRequest request) {
        requirePermission(actor, "stats:global");
        jdbcTemplate.update("INSERT INTO quest_announcement (title, content, expires_at) VALUES (?, ?, ?)",
            request.title().trim(), request.content().trim(), request.expiresAt());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditService.record(actor.id(), "ANNOUNCEMENT_CREATE", "ANNOUNCEMENT", id, Map.of("title", request.title()));
        return Map.of("id", id, "status", "DRAFT");
    }

    @Transactional
    public void publishAnnouncement(CurrentMember actor, long announcementId) {
        requirePermission(actor, "stats:global");
        int updated = jdbcTemplate.update("""
            UPDATE quest_announcement SET status = 'PUBLISHED', published_by = ?, published_at = NOW(3)
            WHERE id = ? AND status = 'DRAFT'
            """, actor.id(), announcementId);
        if (updated == 0) throw new IllegalArgumentException("公告不存在或已经发布");
        jdbcTemplate.update("""
            INSERT INTO quest_notification (member_id, notification_type, title, content, action_url, business_type, business_id)
            SELECT member.id, 'SYSTEM_ANNOUNCEMENT', announcement.title, LEFT(announcement.content, 1000),
                   '/dashboard', 'ANNOUNCEMENT', CAST(announcement.id AS CHAR)
            FROM quest_announcement announcement
            JOIN quest_member member ON member.status = 'ACTIVE'
            WHERE announcement.id = ?
            """, announcementId);
        auditService.record(actor.id(), "ANNOUNCEMENT_PUBLISH", "ANNOUNCEMENT", announcementId, Map.of());
    }

    @Transactional
    public Map<String, Object> createDirection(CurrentMember actor, CreateDirectionRequest request) {
        requirePermission(actor, "route:manage");
        try {
            jdbcTemplate.update("INSERT INTO quest_technical_direction (direction_key, name, sort_order) VALUES (?, ?, ?)",
                request.directionKey(), request.name().trim(), request.sortOrder());
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("技术方向标识已存在");
        }
        Long id = jdbcTemplate.queryForObject("SELECT id FROM quest_technical_direction WHERE direction_key = ?", Long.class, request.directionKey());
        auditService.record(actor.id(), "DIRECTION_CREATE", "TECHNICAL_DIRECTION", id, Map.of("key", request.directionKey()));
        return Map.of("id", id, "status", "ACTIVE");
    }

    @Transactional
    public void updateDirection(CurrentMember actor, long directionId, UpdateDirectionRequest request) {
        requirePermission(actor, "route:manage");
        int updated = jdbcTemplate.update("UPDATE quest_technical_direction SET name = ?, sort_order = ?, status = ? WHERE id = ?",
            request.name().trim(), request.sortOrder(), request.status(), directionId);
        if (updated == 0) throw new IllegalArgumentException("技术方向不存在");
        auditService.record(actor.id(), "DIRECTION_UPDATE", "TECHNICAL_DIRECTION", directionId, Map.of("status", request.status()));
    }

    public List<Map<String, Object>> levelRules(CurrentMember actor) {
        requirePermission(actor, "stats:global");
        return jdbcTemplate.queryForList("""
            SELECT level_key AS levelKey, name, minimum_points AS minimumPoints,
                   sort_order AS sortOrder, status
            FROM quest_level_rule ORDER BY sort_order, minimum_points
            """);
    }

    @Transactional
    public void updateLevelRule(CurrentMember actor, String levelKey, UpdateLevelRuleRequest request) {
        requirePermission(actor, "stats:global");
        int updated = jdbcTemplate.update("UPDATE quest_level_rule SET name = ?, minimum_points = ?, status = ? WHERE level_key = ?",
            request.name().trim(), request.minimumPoints(), request.status(), levelKey);
        if (updated == 0) throw new IllegalArgumentException("等级规则不存在");
        auditService.record(actor.id(), "LEVEL_RULE_UPDATE", "LEVEL_RULE", levelKey, Map.of("minimumPoints", request.minimumPoints(), "status", request.status()));
    }

    @Transactional
    public Map<String, Object> createRoute(CurrentMember actor, CreateRouteRequest request) {
        requirePermission(actor, "route:manage");
        Integer activeDirection = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM quest_technical_direction WHERE id = ? AND status = 'ACTIVE'", Integer.class, request.directionId());
        if (activeDirection == null || activeDirection == 0) {
            throw new IllegalArgumentException("请选择启用中的技术方向");
        }
        if (request.stageKeys().stream().distinct().count() != request.stageKeys().size()) {
            throw new IllegalArgumentException("成长阶段不能重复");
        }
        try {
            jdbcTemplate.update("""
                INSERT INTO quest_growth_route (route_key, name, description, direction_id, status, created_by)
                VALUES (?, ?, ?, ?, 'DRAFT', ?)
                """, request.routeKey(), request.name(), request.description(), request.directionId(), actor.id());
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("成长路线标识已存在");
        }
        Long routeId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_growth_route WHERE route_key = ?",
            Long.class,
            request.routeKey()
        );
        int order = 1;
        for (String stageKey : request.stageKeys()) {
            int inserted = jdbcTemplate.update("""
                INSERT INTO quest_route_stage (route_id, stage_id, sort_order, required_task_count, optional_task_count)
                SELECT ?, id, ?, 0, 0 FROM quest_growth_stage WHERE stage_key = ?
                """, routeId, order++, stageKey);
            if (inserted != 1) {
                throw new IllegalArgumentException("成长阶段不存在：" + stageKey);
            }
        }
        auditService.record(actor.id(), "ROUTE_CREATE", "GROWTH_ROUTE", routeId, Map.of("routeKey", request.routeKey()));
        return Map.of("id", routeId, "status", "DRAFT");
    }

    public void publishRoute(CurrentMember actor, long routeId) {
        requirePermission(actor, "route:manage");
        int updated = jdbcTemplate.update("""
            UPDATE quest_growth_route SET status = 'PUBLISHED', published_at = COALESCE(published_at, NOW(3))
            WHERE id = ? AND status IN ('DRAFT', 'PUBLISHED')
            """, routeId);
        if (updated == 0) {
            throw new IllegalArgumentException("路线不存在或已归档");
        }
        auditService.record(actor.id(), "ROUTE_PUBLISH", "GROWTH_ROUTE", routeId, Map.of());
    }

    public void archiveRoute(CurrentMember actor, long routeId) {
        requirePermission(actor, "route:manage");
        int updated = jdbcTemplate.update("""
            UPDATE quest_growth_route SET status = 'ARCHIVED', archived_at = COALESCE(archived_at, NOW(3))
            WHERE id = ? AND status <> 'ARCHIVED'
            """, routeId);
        if (updated == 0) throw new IllegalArgumentException("路线不存在或已经归档");
        auditService.record(actor.id(), "ROUTE_ARCHIVE", "GROWTH_ROUTE", routeId, Map.of());
    }

    public List<Map<String, Object>> routes(CurrentMember actor) {
        requirePermission(actor, "route:manage");
        return jdbcTemplate.queryForList("""
            SELECT r.id, r.route_key AS routeKey, r.name, r.description, r.status,
                   d.id AS directionId, d.name AS directionName, COUNT(rs.stage_id) AS stageCount,
                   r.published_at AS publishedAt, r.updated_at AS updatedAt
            FROM quest_growth_route r
            JOIN quest_technical_direction d ON d.id = r.direction_id
            LEFT JOIN quest_route_stage rs ON rs.route_id = r.id
            GROUP BY r.id, r.route_key, r.name, r.description, r.status, d.id, d.name, r.published_at, r.updated_at
            ORDER BY r.updated_at DESC
            """);
    }

    @Transactional
    public Map<String, Object> createTask(CurrentMember actor, CreateTaskRequest request) {
        requirePermission(actor, "task:manage");
        validateDeadline(request);
        if (request.routeId() != null) {
            Integer relation = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM quest_growth_route route
                JOIN quest_route_stage stage ON stage.route_id = route.id
                WHERE route.id = ? AND route.direction_id = ? AND stage.stage_id = ? AND route.status <> 'ARCHIVED'
                """, Integer.class, request.routeId(), request.directionId(), request.stageId());
            if (relation == null || relation == 0) {
                throw new IllegalArgumentException("任务的技术方向、路线和成长阶段不匹配");
            }
        }
        long ownerId = request.ownerMemberId() == null ? actor.id() : request.ownerMemberId();
        if (!actor.roles().contains("ADMIN") && ownerId != actor.id()) {
            throw new IllegalArgumentException("只能为自己创建负责的任务");
        }
        Integer qualifiedOwner = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT member.id)
            FROM quest_member member
            JOIN quest_member_role member_role ON member_role.member_id = member.id
            JOIN quest_role_permission role_permission ON role_permission.role_id = member_role.role_id
            JOIN quest_permission permission ON permission.id = role_permission.permission_id
            WHERE member.id = ? AND member.status = 'ACTIVE'
              AND permission.permission_key = 'submission:review'
            """, Integer.class, ownerId);
        if (qualifiedOwner == null || qualifiedOwner == 0) {
            throw new IllegalArgumentException("任务负责人必须是启用中的导师、项目负责人或管理员");
        }
        try {
            jdbcTemplate.update("""
                INSERT INTO quest_task (
                    task_key, title, summary, direction_id, route_id, stage_id, task_type, difficulty,
                    learning_objectives_json, estimated_minutes, deadline_type, fixed_deadline, duration_hours,
                    instructions, resources_json, submission_requirements, acceptance_criteria, points,
                    owner_member_id, faq_json, submission_limit, capacity, assignment_mode,
                    required_in_stage, status, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'INDIVIDUAL', ?, 'DRAFT', ?)
                """,
                request.taskKey(), request.title(), request.summary(), request.directionId(), request.routeId(), request.stageId(),
                request.taskType(), request.difficulty(), json(request.learningObjectives()), request.estimatedMinutes(),
                request.deadlineType(), request.fixedDeadline(), request.durationHours(), request.instructions(),
                json(request.resources()), request.submissionRequirements(), request.acceptanceCriteria(), request.points(),
                ownerId, json(request.faq()), request.submissionLimit(), request.capacity(),
                request.requiredInStage() == null || request.requiredInStage(), actor.id()
            );
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("任务标识已存在，或关联数据无效");
        }
        Long taskId = jdbcTemplate.queryForObject("SELECT id FROM quest_task WHERE task_key = ?", Long.class, request.taskKey());
        if (request.prerequisiteTaskIds() != null) {
            for (Long prerequisiteId : request.prerequisiteTaskIds().stream().distinct().toList()) {
                if (prerequisiteId.equals(taskId)) {
                    throw new IllegalArgumentException("任务不能依赖自身");
                }
                int inserted = jdbcTemplate.update("""
                    INSERT INTO quest_task_prerequisite (task_id, prerequisite_task_id)
                    SELECT ?, id FROM quest_task WHERE id = ?
                    """, taskId, prerequisiteId);
                if (inserted != 1) {
                    throw new IllegalArgumentException("前置任务不存在：" + prerequisiteId);
                }
            }
        }
        auditService.record(actor.id(), "TASK_CREATE", "TASK", taskId, Map.of("taskKey", request.taskKey()));
        return Map.of("id", taskId, "status", "DRAFT");
    }

    public List<Map<String, Object>> tasks(CurrentMember actor) {
        requirePermission(actor, "task:manage");
        String scope = actor.roles().contains("ADMIN") ? "" : " WHERE t.owner_member_id = ?";
        Object[] args = actor.roles().contains("ADMIN") ? new Object[]{} : new Object[]{actor.id()};
        return jdbcTemplate.queryForList("""
            SELECT t.id, t.task_key AS taskKey, t.title, t.task_type AS taskType, t.difficulty,
                   t.status, t.points, t.capacity, t.owner_member_id AS ownerMemberId,
                   owner.nickname AS ownerName, d.name AS directionName, s.stage_key AS stageKey,
                   COUNT(a.id) AS assignmentCount,
                   SUM(a.status = 'PASSED') AS passedCount,
                   t.updated_at AS updatedAt
            FROM quest_task t
            JOIN quest_member owner ON owner.id = t.owner_member_id
            LEFT JOIN quest_technical_direction d ON d.id = t.direction_id
            LEFT JOIN quest_growth_stage s ON s.id = t.stage_id
            LEFT JOIN quest_task_assignment a ON a.task_id = t.id
            """ + scope + " GROUP BY t.id, t.task_key, t.title, t.task_type, t.difficulty, t.status, t.points, t.capacity, t.owner_member_id, owner.nickname, d.name, s.stage_key, t.updated_at ORDER BY t.updated_at DESC", args);
    }

    public Map<String, Object> taskOptions(CurrentMember actor) {
        requirePermission(actor, "task:manage");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routes", jdbcTemplate.queryForList("""
            SELECT route.id, route.route_key AS routeKey, route.name,
                   route.direction_id AS directionId, direction.name AS directionName
            FROM quest_growth_route route
            JOIN quest_technical_direction direction ON direction.id = route.direction_id
            WHERE route.status = 'PUBLISHED' AND direction.status = 'ACTIVE'
            ORDER BY direction.sort_order, route.name
            """));
        result.put("stages", jdbcTemplate.queryForList("""
            SELECT route_stage.route_id AS routeId, stage.id, stage.stage_key AS stageKey, stage.name
            FROM quest_route_stage route_stage
            JOIN quest_growth_stage stage ON stage.id = route_stage.stage_id
            JOIN quest_growth_route route ON route.id = route_stage.route_id
            WHERE route.status = 'PUBLISHED'
            ORDER BY route_stage.route_id, route_stage.sort_order
            """));
        return result;
    }

    @Transactional
    public void changeTaskStatus(CurrentMember actor, long taskId, String status) {
        requirePermission(actor, "task:manage");
        requireTaskOwner(actor, taskId);
        if (!List.of("DRAFT", "PUBLISHED", "OFFLINE", "ARCHIVED").contains(status)) {
            throw new IllegalArgumentException("无效的任务状态");
        }
        int updated = jdbcTemplate.update("""
            UPDATE quest_task
            SET status = ?, published_at = CASE WHEN ? = 'PUBLISHED' THEN COALESCE(published_at, NOW(3)) ELSE published_at END,
                archived_at = CASE WHEN ? = 'ARCHIVED' THEN COALESCE(archived_at, NOW(3)) ELSE archived_at END
            WHERE id = ? AND (? = 'ARCHIVED' OR status <> 'ARCHIVED')
            """, status, status, status, taskId, status);
        if (updated == 0) {
            throw new IllegalArgumentException("任务不存在，或归档任务不能恢复");
        }
        auditService.record(actor.id(), "TASK_STATUS_CHANGE", "TASK", taskId, Map.of("status", status));
    }

    @Transactional
    public Map<String, Object> assignTask(CurrentMember actor, long taskId, AssignTaskRequest request) {
        requirePermission(actor, "task:manage");
        requireTaskOwner(actor, taskId);
        Integer activeMember = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM quest_member WHERE id = ? AND status = 'ACTIVE'", Integer.class, request.memberId());
        if (activeMember == null || activeMember == 0) {
            throw new IllegalArgumentException("只能向启用中的成员分配任务");
        }
        List<Map<String, Object>> taskRows = jdbcTemplate.queryForList("""
            SELECT deadline_type, fixed_deadline, duration_hours, capacity
            FROM quest_task WHERE id = ? AND status = 'PUBLISHED' FOR UPDATE
            """, taskId);
        if (taskRows.isEmpty()) {
            throw new IllegalArgumentException("只能分配已发布的任务");
        }
        Number capacity = (Number) taskRows.getFirst().get("capacity");
        if (capacity != null) {
            Integer activeAssignments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_task_assignment WHERE task_id = ? AND status <> 'ABANDONED'",
                Integer.class,
                taskId
            );
            if (activeAssignments != null && activeAssignments >= capacity.intValue()) {
                throw new IllegalStateException("该任务分配名额已满");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueAt = assignmentDueAt(taskRows.getFirst(), now);
        try {
            jdbcTemplate.update("""
                INSERT INTO quest_task_assignment (task_id, member_id, assigned_by, source, status, claimed_at, due_at)
                VALUES (?, ?, ?, 'ASSIGNED', 'IN_PROGRESS', ?, ?)
                """, taskId, request.memberId(), actor.id(), now, dueAt);
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("成员不存在或已经拥有该任务");
        }
        Long assignmentId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_task_assignment WHERE task_id = ? AND member_id = ?",
            Long.class,
            taskId,
            request.memberId()
        );
        jdbcTemplate.update("""
            INSERT INTO quest_notification (member_id, notification_type, title, content, action_url, business_type, business_id)
            SELECT ?, 'TASK_ASSIGNED', '收到新任务', CONCAT('管理员为你分配了任务「', title, '」。'),
                   CONCAT('/tasks/', id), 'TASK', CAST(id AS CHAR)
            FROM quest_task WHERE id = ?
            """, request.memberId(), taskId);
        auditService.record(actor.id(), "TASK_ASSIGN", "TASK_ASSIGNMENT", assignmentId, Map.of("taskId", taskId, "memberId", request.memberId()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignmentId", assignmentId);
        result.put("status", "IN_PROGRESS");
        result.put("dueAt", dueAt);
        return result;
    }

    public Map<String, Object> stats(CurrentMember actor) {
        requirePermission(actor, "stats:global");
        return Map.of(
            "oauth", jdbcTemplate.queryForMap("""
                SELECT SUM(action = 'OAUTH_LOGIN_START') AS attempts,
                       SUM(action = 'OAUTH_LOGIN_SUCCESS') AS successes,
                       SUM(action IN ('OAUTH_LOGIN_FAILED', 'OAUTH_CALLBACK_REJECTED', 'OAUTH_LOGIN_BLOCKED')) AS failures,
                       ROUND(100 * SUM(action = 'OAUTH_LOGIN_SUCCESS') /
                           NULLIF(SUM(action = 'OAUTH_LOGIN_SUCCESS') + SUM(action IN ('OAUTH_LOGIN_FAILED', 'OAUTH_CALLBACK_REJECTED', 'OAUTH_LOGIN_BLOCKED')), 0), 1) AS successRate
                FROM quest_audit_log
                WHERE action LIKE 'OAUTH_%' AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                """),
            "members", jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS total,
                       SUM(status = 'ACTIVE') AS active,
                       SUM(created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)) AS newIn30Days
                FROM quest_member
                """),
            "tasks", jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS assignments,
                       SUM(status = 'PASSED') AS passed,
                       SUM(status = 'OVERDUE') AS overdue,
                       ROUND(100 * SUM(status = 'PASSED') / NULLIF(COUNT(*), 0), 1) AS completionRate
                FROM quest_task_assignment
                """),
            "reviews", jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS reviewed,
                       ROUND(AVG(TIMESTAMPDIFF(HOUR, s.submitted_at, r.reviewed_at)), 1) AS averageHours,
                       (SELECT COUNT(*) FROM quest_task_submission WHERE status IN ('SUBMITTED', 'REVIEWING')) AS pending
                FROM quest_review_record r JOIN quest_task_submission s ON s.id = r.submission_id
                """),
            "directions", jdbcTemplate.queryForList("""
                SELECT d.name, COUNT(md.member_id) AS memberCount
                FROM quest_technical_direction d
                LEFT JOIN quest_member_direction md ON md.direction_id = d.id
                GROUP BY d.id, d.name ORDER BY d.sort_order
                """)
        );
    }

    public List<Map<String, Object>> members(CurrentMember actor) {
        requirePermission(actor, "member:manage");
        List<Map<String, Object>> members = jdbcTemplate.queryForList("""
            SELECT m.id, m.nickname, m.avatar_url AS avatarUrl, m.email, m.school, m.college, m.major,
                   m.grade, m.skills_json AS skillsJson, m.code_profile_url AS codeProfileUrl,
                   m.weekly_hours AS weeklyHours, m.bio,
                   m.status, m.current_level AS currentLevel, m.total_points AS totalPoints,
                   m.profile_completed_at AS profileCompletedAt, m.onboarding_completed_at AS onboardingCompletedAt,
                   GROUP_CONCAT(DISTINCT r.role_key ORDER BY r.role_key SEPARATOR ',') AS roles,
                   GROUP_CONCAT(DISTINCT d.name ORDER BY d.sort_order SEPARATOR ',') AS directions,
                   GROUP_CONCAT(DISTINCT d.id ORDER BY d.sort_order SEPARATOR ',') AS directionIdCsv,
                   m.created_at AS createdAt
            FROM quest_member m
            LEFT JOIN quest_member_role mr ON mr.member_id = m.id
            LEFT JOIN quest_role r ON r.id = mr.role_id
            LEFT JOIN quest_member_direction md ON md.member_id = m.id
            LEFT JOIN quest_technical_direction d ON d.id = md.direction_id
            GROUP BY m.id, m.nickname, m.avatar_url, m.email, m.school, m.college, m.major, m.grade,
                     m.skills_json, m.code_profile_url, m.weekly_hours, m.bio, m.status,
                     m.current_level, m.total_points, m.profile_completed_at, m.onboarding_completed_at, m.created_at
            ORDER BY m.created_at DESC
            LIMIT 500
            """);
        members.forEach(item -> {
            item.put("skills", readStringList(item.remove("skillsJson")));
            Object directionIds = item.remove("directionIdCsv");
            item.put("directionIds", directionIds == null || String.valueOf(directionIds).isBlank()
                ? List.of()
                : java.util.Arrays.stream(String.valueOf(directionIds).split(",")).map(Long::valueOf).toList());
        });
        return members;
    }

    @Transactional
    public void updateMemberProfile(CurrentMember actor, long memberId, UpdateMemberProfileRequest request) {
        requirePermission(actor, "member:manage");
        List<Map<String, Object>> memberRows = jdbcTemplate.queryForList(
            "SELECT total_points FROM quest_member WHERE id = ? FOR UPDATE", memberId);
        if (memberRows.isEmpty()) throw new IllegalArgumentException("成员不存在");
        int previousPoints = ((Number) memberRows.getFirst().get("total_points")).intValue();
        int totalPoints = request.totalPoints() == null ? previousPoints : request.totalPoints();
        String level = jdbcTemplate.query("""
            SELECT level_key FROM quest_level_rule
            WHERE status = 'ACTIVE' AND minimum_points <= ?
            ORDER BY minimum_points DESC LIMIT 1
            """, resultSet -> resultSet.next() ? resultSet.getString(1) : "L0", totalPoints);

        List<Long> directionIds = request.directionIds() == null
            ? List.of()
            : request.directionIds().stream().distinct().toList();
        if (!directionIds.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(directionIds.size(), "?"));
            Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_technical_direction WHERE status = 'ACTIVE' AND id IN (" + placeholders + ")",
                Integer.class,
                directionIds.toArray());
            if (activeCount == null || activeCount != directionIds.size()) {
                throw new IllegalArgumentException("包含不存在或已归档的技术方向");
            }
        }

        jdbcTemplate.update("""
            UPDATE quest_member
            SET nickname = ?, avatar_url = ?, email = ?, school = ?, college = ?, major = ?, grade = ?,
                skills_json = ?, code_profile_url = ?, weekly_hours = ?, bio = ?, total_points = ?, current_level = ?
            WHERE id = ?
            """,
            request.nickname().trim(), trimToNull(request.avatarUrl()), trimToNull(request.email()),
            trimToNull(request.school()), trimToNull(request.college()), trimToNull(request.major()),
            trimToNull(request.grade()), json(request.skills()), trimToNull(request.codeProfileUrl()),
            request.weeklyHours(), trimToNull(request.bio()), totalPoints, level, memberId);
        int pointDelta = totalPoints - previousPoints;
        if (pointDelta != 0) {
            jdbcTemplate.update("""
                INSERT INTO quest_point_ledger
                    (member_id, amount, balance_after, source_type, source_id, idempotency_key, reason, adjusted_by)
                VALUES (?, ?, ?, 'ADMIN_ADJUST', ?, ?, '管理员在成员管理中调整积分', ?)
                """, memberId, pointDelta, totalPoints, String.valueOf(memberId),
                "admin-adjust:" + java.util.UUID.randomUUID(), actor.id());
        }
        jdbcTemplate.update("DELETE FROM quest_member_direction WHERE member_id = ?", memberId);
        for (int index = 0; index < directionIds.size(); index++) {
            jdbcTemplate.update(
                "INSERT INTO quest_member_direction (member_id, direction_id, is_primary) VALUES (?, ?, ?)",
                memberId, directionIds.get(index), index == 0);
        }
        auditService.record(actor.id(), "MEMBER_PROFILE_UPDATE", "MEMBER", memberId,
            Map.of("directionCount", directionIds.size(), "totalPoints", totalPoints, "level", level));
    }

    @Transactional
    public void updateMemberStatus(CurrentMember actor, long memberId, UpdateMemberStatusRequest request) {
        requirePermission(actor, "member:manage");
        if (actor.id() == memberId && "DISABLED".equals(request.status())) {
            throw new IllegalArgumentException("不能禁用自己的账号");
        }
        int updated = jdbcTemplate.update("UPDATE quest_member SET status = ? WHERE id = ?", request.status(), memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("成员不存在");
        }
        auditService.record(actor.id(), "MEMBER_STATUS_CHANGE", "MEMBER", memberId, Map.of("status", request.status(), "reason", request.reason()));
    }

    @Transactional
    public void updateMemberRoles(CurrentMember actor, long memberId, UpdateMemberRolesRequest request) {
        requirePermission(actor, "role:manage");
        Set<String> allowed = Set.of("MEMBER", "MENTOR", "PROJECT_OWNER", "ADMIN");
        if (!allowed.containsAll(request.roles())) {
            throw new IllegalArgumentException("包含不支持的角色");
        }
        if (actor.id() == memberId && !request.roles().contains("ADMIN")) {
            throw new IllegalArgumentException("不能移除自己的管理员角色");
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_member WHERE id = ?", Integer.class, memberId);
        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("成员不存在");
        }
        jdbcTemplate.update("DELETE FROM quest_member_role WHERE member_id = ?", memberId);
        for (String role : request.roles()) {
            jdbcTemplate.update("""
                INSERT INTO quest_member_role (member_id, role_id, assigned_by)
                SELECT ?, id, ? FROM quest_role WHERE role_key = ?
                """, memberId, actor.id(), role);
        }
        auditService.record(actor.id(), "MEMBER_ROLES_CHANGE", "MEMBER", memberId, Map.of("roles", request.roles()));
    }

    public List<Map<String, Object>> auditLogs(CurrentMember actor) {
        requirePermission(actor, "audit:read");
        return jdbcTemplate.queryForList("""
            SELECT log.id, log.action, log.target_type AS targetType, log.target_id AS targetId,
                   log.result, log.detail_json AS detail, log.created_at AS createdAt,
                   actor.id AS actorId, actor.nickname AS actorName
            FROM quest_audit_log log
            LEFT JOIN quest_member actor ON actor.id = log.actor_member_id
            ORDER BY log.created_at DESC LIMIT 200
            """);
    }

    private void validateDeadline(CreateTaskRequest request) {
        if ("FIXED".equals(request.deadlineType()) && request.fixedDeadline() == null) {
            throw new IllegalArgumentException("固定截止任务必须填写截止时间");
        }
        if ("AFTER_CLAIM".equals(request.deadlineType()) && request.durationHours() == null) {
            throw new IllegalArgumentException("领取后倒计时任务必须填写持续小时数");
        }
    }

    private LocalDateTime assignmentDueAt(Map<String, Object> task, LocalDateTime now) {
        if ("FIXED".equals(task.get("deadline_type"))) {
            Object value = task.get("fixed_deadline");
            return value instanceof java.sql.Timestamp timestamp ? timestamp.toLocalDateTime() : (LocalDateTime) value;
        }
        if ("AFTER_CLAIM".equals(task.get("deadline_type"))) {
            return now.plusHours(((Number) task.get("duration_hours")).longValue());
        }
        return null;
    }

    private void requirePermission(CurrentMember member, String permission) {
        if (!member.permissions().contains(permission)) {
            throw new IllegalArgumentException("无权执行此操作");
        }
    }

    private void requireTaskOwner(CurrentMember actor, long taskId) {
        List<Long> owners = jdbcTemplate.queryForList(
            "SELECT owner_member_id FROM quest_task WHERE id = ? FOR UPDATE", Long.class, taskId);
        if (owners.isEmpty()) throw new IllegalArgumentException("任务不存在");
        if (!actor.roles().contains("ADMIN") && owners.getFirst() != actor.id()) {
            throw new IllegalArgumentException("只能管理自己负责的任务");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据序列化失败", exception);
        }
    }

    private List<String> readStringList(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return List.of();
        try {
            return objectMapper.readValue(
                String.valueOf(value),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("成员技能数据损坏", exception);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
