package edu.jmi.openatom.quest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.dto.ReviewSubmissionRequest;
import edu.jmi.openatom.quest.dto.CreateAppealRequest;
import edu.jmi.openatom.quest.dto.ResolveAppealRequest;
import edu.jmi.openatom.quest.dto.SubmitTaskRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
public class TaskWorkflowService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public List<Map<String, Object>> catalog(CurrentMember member, Long directionId, Long stageId) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.task_key AS taskKey, t.title, t.summary, t.task_type AS taskType,
                   t.difficulty, t.estimated_minutes AS estimatedMinutes, t.points,
                   d.name AS directionName, s.stage_key AS stageKey, s.name AS stageName,
                   a.id AS assignmentId, COALESCE(a.status, 'NOT_STARTED') AS memberStatus,
                   CASE WHEN NOT EXISTS (
                       SELECT 1 FROM quest_task_prerequisite p
                       WHERE p.task_id = t.id AND NOT EXISTS (
                           SELECT 1 FROM quest_task_assignment pa
                           WHERE pa.task_id = p.prerequisite_task_id AND pa.member_id = ? AND pa.status = 'PASSED'
                       )
                   ) THEN TRUE ELSE FALSE END AS prerequisitesMet
            FROM quest_task t
            LEFT JOIN quest_technical_direction d ON d.id = t.direction_id
            LEFT JOIN quest_growth_stage s ON s.id = t.stage_id
            LEFT JOIN quest_task_assignment a ON a.task_id = t.id AND a.member_id = ?
            WHERE t.status = 'PUBLISHED'
            """);
        List<Object> args = new java.util.ArrayList<>(List.of(member.id(), member.id()));
        if (directionId != null) {
            sql.append(" AND t.direction_id = ?");
            args.add(directionId);
        }
        if (stageId != null) {
            sql.append(" AND t.stage_id = ?");
            args.add(stageId);
        }
        sql.append(" ORDER BY s.stage_key, t.required_in_stage DESC, t.created_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> detail(CurrentMember member, long taskId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT t.id, t.task_key AS taskKey, t.title, t.summary, t.task_type AS taskType,
                   t.difficulty, t.learning_objectives_json AS learningObjectives,
                   t.estimated_minutes AS estimatedMinutes, t.deadline_type AS deadlineType,
                   t.fixed_deadline AS fixedDeadline, t.duration_hours AS durationHours,
                   t.instructions, t.resources_json AS resources,
                   t.submission_requirements AS submissionRequirements,
                   t.acceptance_criteria AS acceptanceCriteria, t.points, t.faq_json AS faq,
                   t.submission_limit AS submissionLimit, t.capacity, t.required_in_stage AS requiredInStage,
                   d.name AS directionName, s.stage_key AS stageKey, s.name AS stageName,
                   owner.nickname AS ownerName, a.id AS assignmentId,
                   COALESCE(a.status, 'NOT_STARTED') AS memberStatus, a.due_at AS dueAt
            FROM quest_task t
            LEFT JOIN quest_technical_direction d ON d.id = t.direction_id
            LEFT JOIN quest_growth_stage s ON s.id = t.stage_id
            JOIN quest_member owner ON owner.id = t.owner_member_id
            LEFT JOIN quest_task_assignment a ON a.task_id = t.id AND a.member_id = ?
            WHERE t.id = ? AND (t.status = 'PUBLISHED' OR a.id IS NOT NULL)
            """, member.id(), taskId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("任务不存在或尚未发布");
        }
        Map<String, Object> result = new LinkedHashMap<>(rows.getFirst());
        result.put("prerequisites", jdbcTemplate.queryForList("""
            SELECT p.prerequisite_task_id AS id, required.title,
                   CASE WHEN a.status = 'PASSED' THEN TRUE ELSE FALSE END AS completed
            FROM quest_task_prerequisite p
            JOIN quest_task required ON required.id = p.prerequisite_task_id
            LEFT JOIN quest_task_assignment a ON a.task_id = required.id AND a.member_id = ?
            WHERE p.task_id = ? ORDER BY required.id
            """, member.id(), taskId));
        return result;
    }

    @Transactional
    public Map<String, Object> claim(CurrentMember member, long taskId) {
        requirePermission(member, "task:claim");
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
            "SELECT * FROM quest_task WHERE id = ? AND status = 'PUBLISHED' FOR UPDATE", taskId);
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("任务不存在或尚未发布");
        }
        Map<String, Object> task = tasks.getFirst();
        Integer unmet = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM quest_task_prerequisite p
            WHERE p.task_id = ? AND NOT EXISTS (
                SELECT 1 FROM quest_task_assignment a
                WHERE a.task_id = p.prerequisite_task_id AND a.member_id = ? AND a.status = 'PASSED'
            )
            """, Integer.class, taskId, member.id());
        if (unmet != null && unmet > 0) {
            throw new IllegalStateException("请先完成全部前置任务");
        }
        Number capacity = (Number) task.get("capacity");
        if (capacity != null) {
            Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_task_assignment WHERE task_id = ? AND status <> 'ABANDONED'",
                Integer.class,
                taskId
            );
            if (active != null && active >= capacity.intValue()) {
                throw new IllegalStateException("该任务领取名额已满");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueAt = dueAt(task, now);
        try {
            jdbcTemplate.update("""
                INSERT INTO quest_task_assignment (task_id, member_id, source, status, claimed_at, due_at)
                VALUES (?, ?, 'CLAIMED', 'IN_PROGRESS', ?, ?)
                """, taskId, member.id(), now, dueAt);
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("你已经领取过该任务");
        }
        Long assignmentId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_task_assignment WHERE task_id = ? AND member_id = ?",
            Long.class,
            taskId,
            member.id()
        );
        auditService.record(member.id(), "TASK_CLAIM", "TASK_ASSIGNMENT", assignmentId, Map.of("taskId", taskId));
        return Map.of("assignmentId", assignmentId, "status", "IN_PROGRESS", "dueAt", dueAt == null ? "" : dueAt.toString());
    }

    public List<Map<String, Object>> myAssignments(CurrentMember member) {
        return jdbcTemplate.queryForList("""
            SELECT a.id, a.task_id AS taskId, t.title, t.summary, t.difficulty, t.points,
                   a.status, a.claimed_at AS claimedAt, a.due_at AS dueAt,
                   (SELECT MAX(s.version_no) FROM quest_task_submission s WHERE s.assignment_id = a.id) AS latestVersion,
                   (SELECT r.comment FROM quest_review_record r
                    JOIN quest_task_submission s ON s.id = r.submission_id
                    WHERE s.assignment_id = a.id ORDER BY r.reviewed_at DESC LIMIT 1) AS latestFeedback
            FROM quest_task_assignment a
            JOIN quest_task t ON t.id = a.task_id
            WHERE a.member_id = ?
            ORDER BY FIELD(a.status, 'REVISION_REQUIRED', 'IN_PROGRESS', 'PENDING_REVIEW', 'OVERDUE', 'PASSED', 'ABANDONED'), a.updated_at DESC
            """, member.id());
    }

    @Transactional
    public void abandon(CurrentMember member, long assignmentId) {
        Map<String, Object> assignment = ownedAssignmentForUpdate(member.id(), assignmentId);
        if (!List.of("IN_PROGRESS", "REVISION_REQUIRED", "OVERDUE").contains(assignment.get("status"))) {
            throw new IllegalStateException("当前任务状态不能放弃");
        }
        jdbcTemplate.update("UPDATE quest_task_assignment SET status = 'ABANDONED', abandoned_at = NOW(3) WHERE id = ?", assignmentId);
        auditService.record(member.id(), "TASK_ABANDON", "TASK_ASSIGNMENT", assignmentId, Map.of());
    }

    @Transactional
    public Map<String, Object> submit(CurrentMember member, long assignmentId, SubmitTaskRequest request) {
        requirePermission(member, "task:submit");
        Map<String, Object> assignment = ownedAssignmentForUpdate(member.id(), assignmentId);
        String status = (String) assignment.get("status");
        if (!("IN_PROGRESS".equals(status) || "REVISION_REQUIRED".equals(status))) {
            throw new IllegalStateException("当前任务状态不能提交");
        }
        Number limit = (Number) assignment.get("submission_limit");
        Integer currentVersion = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(version_no), 0) FROM quest_task_submission WHERE assignment_id = ?",
            Integer.class,
            assignmentId
        );
        int version = (currentVersion == null ? 0 : currentVersion) + 1;
        if (limit != null && version > limit.intValue()) {
            throw new IllegalStateException("已达到该任务的提交次数上限");
        }
        jdbcTemplate.update(
            "UPDATE quest_task_submission SET status = 'SUPERSEDED' WHERE assignment_id = ? AND status = 'REVISION_REQUIRED'",
            assignmentId
        );
        jdbcTemplate.update("""
            INSERT INTO quest_task_submission (
                assignment_id, version_no, completion_note, repository_url, pull_request_url,
                demo_url, video_url, problems_and_learning, ai_used, ai_usage_detail, status, submitted_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', ?)
            """,
            assignmentId,
            version,
            request.completionNote(),
            blankToNull(request.repositoryUrl()),
            blankToNull(request.pullRequestUrl()),
            blankToNull(request.demoUrl()),
            blankToNull(request.videoUrl()),
            blankToNull(request.problemsAndLearning()),
            request.aiUsed(),
            blankToNull(request.aiUsageDetail()),
            LocalDateTime.now()
        );
        Long submissionId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_task_submission WHERE assignment_id = ? AND version_no = ?",
            Long.class,
            assignmentId,
            version
        );
        jdbcTemplate.update("UPDATE quest_task_assignment SET status = 'PENDING_REVIEW' WHERE id = ?", assignmentId);
        auditService.record(member.id(), "TASK_SUBMIT", "TASK_SUBMISSION", submissionId, Map.of("assignmentId", assignmentId, "version", version));
        return Map.of("submissionId", submissionId, "version", version, "status", "SUBMITTED");
    }

    public List<Map<String, Object>> submissionHistory(CurrentMember member, long assignmentId) {
        ownedAssignment(member.id(), assignmentId);
        return jdbcTemplate.queryForList("""
            SELECT s.id, s.version_no AS version, s.completion_note AS completionNote,
                   s.repository_url AS repositoryUrl, s.pull_request_url AS pullRequestUrl,
                   s.demo_url AS demoUrl, s.video_url AS videoUrl,
                   s.problems_and_learning AS problemsAndLearning,
                   s.ai_used AS aiUsed, s.ai_usage_detail AS aiUsageDetail,
                   s.status, s.submitted_at AS submittedAt,
                   r.id AS reviewId, r.result AS reviewResult, r.comment AS reviewComment,
                   r.score_json AS scores, r.required_changes_json AS requiredChanges,
                   r.suggestions_json AS suggestions, r.strengths_json AS strengths,
                   r.resubmission_allowed AS resubmissionAllowed, r.excellent, r.reviewed_at AS reviewedAt,
                   appeal.status AS appealStatus, appeal.reason AS appealReason,
                   appeal.resolution AS appealResolution
            FROM quest_task_submission s
            LEFT JOIN quest_review_record r ON r.submission_id = s.id
            LEFT JOIN quest_review_appeal appeal ON appeal.review_id = r.id AND appeal.member_id = ?
            WHERE s.assignment_id = ? ORDER BY s.version_no DESC, r.reviewed_at DESC
            """, member.id(), assignmentId);
    }

    @Transactional
    public Map<String, Object> createAppeal(CurrentMember member, long reviewId, CreateAppealRequest request) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT r.id, r.result, r.reviewer_member_id, a.id AS assignment_id
            FROM quest_review_record r
            JOIN quest_task_submission s ON s.id = r.submission_id
            JOIN quest_task_assignment a ON a.id = s.assignment_id
            WHERE r.id = ? AND a.member_id = ?
            """, reviewId, member.id());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("审核记录不存在或不属于当前成员");
        }
        if ("PASSED".equals(rows.getFirst().get("result"))) {
            throw new IllegalStateException("已通过的审核无需申诉");
        }
        try {
            jdbcTemplate.update(
                "INSERT INTO quest_review_appeal (review_id, member_id, reason) VALUES (?, ?, ?)",
                reviewId, member.id(), request.reason().trim()
            );
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("该审核结果已经发起过申诉");
        }
        Long appealId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_review_appeal WHERE review_id = ? AND member_id = ?", Long.class, reviewId, member.id());
        auditService.record(member.id(), "REVIEW_APPEAL_CREATE", "REVIEW_APPEAL", appealId, Map.of("reviewId", reviewId));
        return Map.of("id", appealId, "status", "PENDING");
    }

    public List<Map<String, Object>> appealQueue(CurrentMember reviewer) {
        requirePermission(reviewer, "submission:review");
        return jdbcTemplate.queryForList("""
            SELECT appeal.id, appeal.review_id AS reviewId, appeal.reason, appeal.status,
                   appeal.created_at AS createdAt, member.nickname AS memberName,
                   task.title AS taskTitle, review.comment AS originalComment,
                   original.nickname AS originalReviewer
            FROM quest_review_appeal appeal
            JOIN quest_review_record review ON review.id = appeal.review_id
            JOIN quest_member original ON original.id = review.reviewer_member_id
            JOIN quest_task_submission submission ON submission.id = review.submission_id
            JOIN quest_task_assignment assignment ON assignment.id = submission.assignment_id
            JOIN quest_task task ON task.id = assignment.task_id
            JOIN quest_member member ON member.id = appeal.member_id
            WHERE appeal.status = 'PENDING' AND review.reviewer_member_id <> ?
            ORDER BY appeal.created_at
            """, reviewer.id());
    }

    @Transactional
    public void resolveAppeal(CurrentMember resolver, long appealId, ResolveAppealRequest request) {
        requirePermission(resolver, "submission:review");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT appeal.id, appeal.status, appeal.member_id, review.reviewer_member_id,
                   submission.id AS submission_id, assignment.id AS assignment_id,
                   submission.version_no, assignment.status AS assignment_status
            FROM quest_review_appeal appeal
            JOIN quest_review_record review ON review.id = appeal.review_id
            JOIN quest_task_submission submission ON submission.id = review.submission_id
            JOIN quest_task_assignment assignment ON assignment.id = submission.assignment_id
            WHERE appeal.id = ? FOR UPDATE
            """, appealId);
        if (rows.isEmpty()) throw new IllegalArgumentException("申诉不存在");
        Map<String, Object> row = rows.getFirst();
        if (!"PENDING".equals(row.get("status"))) throw new IllegalStateException("申诉已经处理");
        if (((Number) row.get("reviewer_member_id")).longValue() == resolver.id()) {
            throw new IllegalArgumentException("原审核人不能处理该申诉");
        }
        jdbcTemplate.update("""
            UPDATE quest_review_appeal SET status = ?, resolved_by = ?, resolution = ?, resolved_at = NOW(3)
            WHERE id = ?
            """, request.status(), resolver.id(), request.resolution().trim(), appealId);
        Integer latestVersion = jdbcTemplate.queryForObject(
            "SELECT MAX(version_no) FROM quest_task_submission WHERE assignment_id = ?",
            Integer.class, row.get("assignment_id"));
        boolean isLatestVersion = latestVersion != null
            && latestVersion == ((Number) row.get("version_no")).intValue();
        if ("OVERTURNED".equals(request.status()) && isLatestVersion
            && !"PASSED".equals(row.get("assignment_status"))
            && !"PENDING_REVIEW".equals(row.get("assignment_status"))) {
            jdbcTemplate.update("UPDATE quest_task_submission SET status = 'REVISION_REQUIRED' WHERE id = ?", row.get("submission_id"));
            jdbcTemplate.update("UPDATE quest_task_assignment SET status = 'REVISION_REQUIRED' WHERE id = ?", row.get("assignment_id"));
        }
        createNotification(
            ((Number) row.get("member_id")).longValue(), "APPEAL_RESOLVED", "审核申诉已处理",
            request.resolution().trim(), "/assignments/" + row.get("assignment_id"), "REVIEW_APPEAL", appealId
        );
        auditService.record(resolver.id(), "REVIEW_APPEAL_RESOLVE", "REVIEW_APPEAL", appealId, Map.of("status", request.status()));
    }

    public List<Map<String, Object>> reviewQueue(CurrentMember reviewer) {
        requirePermission(reviewer, "submission:review");
        String scope = reviewer.roles().contains("ADMIN") ? "" : " AND t.owner_member_id = ?";
        Object[] args = reviewer.roles().contains("ADMIN") ? new Object[]{} : new Object[]{reviewer.id()};
        return jdbcTemplate.queryForList("""
            SELECT s.id AS submissionId, s.version_no AS version, s.submitted_at AS submittedAt,
                   s.completion_note AS completionNote, s.repository_url AS repositoryUrl,
                   s.pull_request_url AS pullRequestUrl, s.demo_url AS demoUrl, s.video_url AS videoUrl,
                   s.problems_and_learning AS problemsAndLearning, s.ai_used AS aiUsed,
                   s.ai_usage_detail AS aiUsageDetail,
                   a.id AS assignmentId, t.id AS taskId, t.title AS taskTitle,
                   member.id AS memberId, member.nickname AS memberName,
                   TIMESTAMPDIFF(HOUR, s.submitted_at, NOW()) AS waitingHours
            FROM quest_task_submission s
            JOIN quest_task_assignment a ON a.id = s.assignment_id
            JOIN quest_task t ON t.id = a.task_id
            JOIN quest_member member ON member.id = a.member_id
            WHERE s.status IN ('SUBMITTED', 'REVIEWING')
            """ + scope + " ORDER BY s.submitted_at", args);
    }

    @Transactional
    public Map<String, Object> review(CurrentMember reviewer, long submissionId, ReviewSubmissionRequest request) {
        requirePermission(reviewer, "submission:review");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT s.id, s.status AS submission_status, s.assignment_id, a.member_id, a.task_id,
                   t.title, t.points, t.owner_member_id
            FROM quest_task_submission s
            JOIN quest_task_assignment a ON a.id = s.assignment_id
            JOIN quest_task t ON t.id = a.task_id
            WHERE s.id = ? FOR UPDATE
            """, submissionId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("提交记录不存在");
        }
        Map<String, Object> row = rows.getFirst();
        long ownerId = ((Number) row.get("owner_member_id")).longValue();
        if (!reviewer.roles().contains("ADMIN") && ownerId != reviewer.id()) {
            throw new IllegalArgumentException("你只能审核自己负责的任务");
        }
        if (!("SUBMITTED".equals(row.get("submission_status")) || "REVIEWING".equals(row.get("submission_status")))) {
            throw new IllegalStateException("该版本已经完成审核");
        }
        boolean passed = "PASSED".equals(request.result());
        boolean revision = "REVISION_REQUIRED".equals(request.result());
        jdbcTemplate.update("""
            INSERT INTO quest_review_record (
                submission_id, reviewer_member_id, result, comment, score_json,
                required_changes_json, suggestions_json, strengths_json,
                resubmission_allowed, excellent, reviewed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            submissionId,
            reviewer.id(),
            request.result(),
            request.comment(),
            json(request.scores()),
            json(request.requiredChanges()),
            json(request.suggestions()),
            json(request.strengths()),
            request.resubmissionAllowed(),
            Boolean.TRUE.equals(request.excellent()),
            LocalDateTime.now()
        );
        jdbcTemplate.update("UPDATE quest_task_submission SET status = ? WHERE id = ?", request.result(), submissionId);
        String assignmentStatus = passed ? "PASSED" : (revision ? "REVISION_REQUIRED" : "ABANDONED");
        jdbcTemplate.update(
            "UPDATE quest_task_assignment SET status = ?, completed_at = CASE WHEN ? = 'PASSED' THEN NOW(3) ELSE completed_at END WHERE id = ?",
            assignmentStatus,
            assignmentStatus,
            row.get("assignment_id")
        );
        long memberId = ((Number) row.get("member_id")).longValue();
        if (passed) {
            grantTaskPoints(memberId, ((Number) row.get("points")).intValue(), ((Number) row.get("assignment_id")).longValue(), reviewer.id(), (String) row.get("title"));
            advanceRouteIfStageCompleted(memberId, ((Number) row.get("task_id")).longValue());
        }
        String notificationTitle = passed ? "任务审核通过" : (revision ? "任务需要修改" : "任务未通过");
        createNotification(
            memberId,
            "TASK_REVIEWED",
            notificationTitle,
            request.comment(),
            "/assignments/" + row.get("assignment_id"),
            "TASK_SUBMISSION",
            submissionId
        );
        auditService.record(reviewer.id(), "SUBMISSION_REVIEW", "TASK_SUBMISSION", submissionId, Map.of("result", request.result()));
        return Map.of("submissionId", submissionId, "result", request.result(), "assignmentStatus", assignmentStatus);
    }

    public List<Map<String, Object>> notifications(CurrentMember member, boolean unreadOnly) {
        return jdbcTemplate.queryForList("""
            SELECT id, notification_type AS type, title, content, action_url AS actionUrl,
                   read_at AS readAt, created_at AS createdAt
            FROM quest_notification WHERE member_id = ?
            """ + (unreadOnly ? " AND read_at IS NULL" : "") + " ORDER BY created_at DESC LIMIT 100", member.id());
    }

    public void markNotificationRead(CurrentMember member, long notificationId) {
        int updated = jdbcTemplate.update(
            "UPDATE quest_notification SET read_at = COALESCE(read_at, NOW(3)) WHERE id = ? AND member_id = ?",
            notificationId,
            member.id()
        );
        if (updated == 0) {
            throw new IllegalArgumentException("通知不存在");
        }
    }

    public Map<String, Object> dashboard(CurrentMember member) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("level", member.currentLevel());
        result.put("points", member.totalPoints());
        result.put("assignments", jdbcTemplate.queryForMap("""
            SELECT COUNT(*) AS total,
                   SUM(status = 'IN_PROGRESS') AS inProgress,
                   SUM(status = 'REVISION_REQUIRED') AS revisionRequired,
                   SUM(status = 'PENDING_REVIEW') AS pendingReview,
                   SUM(status = 'PASSED') AS passed
            FROM quest_task_assignment WHERE member_id = ?
            """, member.id()));
        result.put("upcoming", jdbcTemplate.queryForList("""
            SELECT a.id AS assignmentId, t.title, a.due_at AS dueAt
            FROM quest_task_assignment a JOIN quest_task t ON t.id = a.task_id
            WHERE a.member_id = ? AND a.status IN ('IN_PROGRESS', 'REVISION_REQUIRED')
              AND a.due_at BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)
            ORDER BY a.due_at LIMIT 5
            """, member.id()));
        result.put("feedback", jdbcTemplate.queryForList("""
            SELECT t.title, r.result, r.comment, r.reviewed_at AS reviewedAt, a.id AS assignmentId
            FROM quest_review_record r
            JOIN quest_task_submission s ON s.id = r.submission_id
            JOIN quest_task_assignment a ON a.id = s.assignment_id
            JOIN quest_task t ON t.id = a.task_id
            WHERE a.member_id = ? ORDER BY r.reviewed_at DESC LIMIT 5
            """, member.id()));
        result.put("unreadNotifications", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM quest_notification WHERE member_id = ? AND read_at IS NULL",
            Integer.class,
            member.id()
        ));
        result.put("announcements", jdbcTemplate.queryForList("""
            SELECT id, title, content, published_at AS publishedAt
            FROM quest_announcement
            WHERE status = 'PUBLISHED' AND (expires_at IS NULL OR expires_at > NOW(3))
            ORDER BY published_at DESC LIMIT 5
            """));
        return result;
    }

    private Map<String, Object> ownedAssignmentForUpdate(long memberId, long assignmentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT a.*, t.submission_limit FROM quest_task_assignment a
            JOIN quest_task t ON t.id = a.task_id
            WHERE a.id = ? AND a.member_id = ? FOR UPDATE
            """, assignmentId, memberId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("任务领取记录不存在");
        }
        return rows.getFirst();
    }

    private void ownedAssignment(long memberId, long assignmentId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM quest_task_assignment WHERE id = ? AND member_id = ?",
            Integer.class,
            assignmentId,
            memberId
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("任务领取记录不存在");
        }
    }

    private LocalDateTime dueAt(Map<String, Object> task, LocalDateTime now) {
        String deadlineType = (String) task.get("deadline_type");
        if ("FIXED".equals(deadlineType)) {
            Object value = task.get("fixed_deadline");
            return value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : (LocalDateTime) value;
        }
        if ("AFTER_CLAIM".equals(deadlineType)) {
            return now.plusHours(((Number) task.get("duration_hours")).longValue());
        }
        return null;
    }

    private void grantTaskPoints(long memberId, int points, long assignmentId, long reviewerId, String title) {
        if (points <= 0) {
            return;
        }
        Integer balance = jdbcTemplate.queryForObject(
            "SELECT total_points FROM quest_member WHERE id = ? FOR UPDATE",
            Integer.class,
            memberId
        );
        int nextBalance = (balance == null ? 0 : balance) + points;
        int inserted = jdbcTemplate.update("""
            INSERT IGNORE INTO quest_point_ledger
                (member_id, amount, balance_after, source_type, source_id, idempotency_key, reason, adjusted_by)
            VALUES (?, ?, ?, 'TASK_PASSED', ?, ?, ?, ?)
            """,
            memberId,
            points,
            nextBalance,
            String.valueOf(assignmentId),
            "task-passed:" + assignmentId,
            "完成任务：" + title,
            reviewerId
        );
        if (inserted == 1) {
            String previousLevel = jdbcTemplate.queryForObject(
                "SELECT current_level FROM quest_member WHERE id = ?",
                String.class,
                memberId
            );
            String nextLevel = jdbcTemplate.query("""
                SELECT level_key FROM quest_level_rule
                WHERE status = 'ACTIVE' AND minimum_points <= ?
                ORDER BY minimum_points DESC LIMIT 1
                """, resultSet -> resultSet.next() ? resultSet.getString(1) : "L0", nextBalance);
            jdbcTemplate.update("UPDATE quest_member SET total_points = ?, current_level = ? WHERE id = ?", nextBalance, nextLevel, memberId);
            createNotification(
                memberId,
                "POINTS_GRANTED",
                "获得 " + points + " 积分",
                "完成任务「" + title + "」后获得积分。",
                "/points",
                "TASK_ASSIGNMENT",
                assignmentId
            );
            if (!nextLevel.equals(previousLevel)) {
                createNotification(
                    memberId,
                    "LEVEL_UP",
                    "成长等级提升至 " + nextLevel,
                    "你的持续学习和真实贡献推动了等级提升。",
                    "/points",
                    "MEMBER",
                    memberId
                );
            }
        }
    }

    private void advanceRouteIfStageCompleted(long memberId, long taskId) {
        List<Map<String, Object>> contexts = jdbcTemplate.queryForList("""
            SELECT t.route_id, t.stage_id, rs.sort_order
            FROM quest_task t
            JOIN quest_member_route mr ON mr.route_id = t.route_id AND mr.member_id = ? AND mr.status = 'ACTIVE'
            JOIN quest_route_stage rs ON rs.route_id = t.route_id AND rs.stage_id = t.stage_id
            WHERE t.id = ?
            """, memberId, taskId);
        if (contexts.isEmpty()) {
            return;
        }
        Map<String, Object> context = contexts.getFirst();
        long routeId = ((Number) context.get("route_id")).longValue();
        long stageId = ((Number) context.get("stage_id")).longValue();
        Integer required = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM quest_task
            WHERE route_id = ? AND stage_id = ? AND required_in_stage = TRUE AND status = 'PUBLISHED'
            """, Integer.class, routeId, stageId);
        Integer passed = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM quest_task t
            JOIN quest_task_assignment a ON a.task_id = t.id AND a.member_id = ? AND a.status = 'PASSED'
            WHERE t.route_id = ? AND t.stage_id = ? AND t.required_in_stage = TRUE AND t.status = 'PUBLISHED'
            """, Integer.class, memberId, routeId, stageId);
        if (required == null || required == 0 || !required.equals(passed)) {
            return;
        }
        Long nextStageId = jdbcTemplate.query("""
            SELECT stage_id FROM quest_route_stage
            WHERE route_id = ? AND sort_order > ? ORDER BY sort_order LIMIT 1
            """, resultSet -> resultSet.next() ? resultSet.getLong(1) : null, routeId, context.get("sort_order"));
        if (nextStageId == null) {
            jdbcTemplate.update("""
                UPDATE quest_member_route SET status = 'COMPLETED', completed_at = NOW(3)
                WHERE member_id = ? AND route_id = ? AND status = 'ACTIVE'
                """, memberId, routeId);
            createNotification(memberId, "ROUTE_COMPLETED", "成长路线已完成", "恭喜你完成当前成长路线。", "/routes/" + routeId, "GROWTH_ROUTE", routeId);
        } else {
            jdbcTemplate.update("""
                UPDATE quest_member_route SET current_stage_id = ?
                WHERE member_id = ? AND route_id = ? AND current_stage_id = ?
                """, nextStageId, memberId, routeId, stageId);
            createNotification(memberId, "STAGE_COMPLETED", "成长阶段已完成", "下一阶段已经解锁。", "/routes/" + routeId, "GROWTH_ROUTE", routeId);
        }
    }

    private void createNotification(long memberId, String type, String title, String content, String actionUrl, String businessType, Object businessId) {
        String preview = content.codePointCount(0, content.length()) > 1000
            ? content.substring(0, content.offsetByCodePoints(0, 999)) + "…" : content;
        jdbcTemplate.update("""
            INSERT INTO quest_notification
                (member_id, notification_type, title, content, action_url, business_type, business_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, memberId, type, title, preview, actionUrl, businessType, String.valueOf(businessId));
    }

    private void requirePermission(CurrentMember member, String permission) {
        if (!member.permissions().contains(permission)) {
            throw new IllegalArgumentException("无权执行此操作");
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据序列化失败", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
