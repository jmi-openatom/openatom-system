package edu.jmi.openatom.quest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.jmi.openatom.quest.dto.CreateRouteRequest;
import edu.jmi.openatom.quest.dto.CreateTaskRequest;
import edu.jmi.openatom.quest.dto.AssignTaskRequest;
import edu.jmi.openatom.quest.dto.CreateAppealRequest;
import edu.jmi.openatom.quest.dto.ReviewSubmissionRequest;
import edu.jmi.openatom.quest.dto.ResolveAppealRequest;
import edu.jmi.openatom.quest.dto.SubmitTaskRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.AdminWorkflowService;
import edu.jmi.openatom.quest.service.AssignmentLifecycleService;
import edu.jmi.openatom.quest.service.GrowthService;
import edu.jmi.openatom.quest.service.TaskWorkflowService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "quest.integration", matches = "true")
class TaskWorkflowIntegrationTests {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AdminWorkflowService adminWorkflowService;
    @Autowired private TaskWorkflowService taskWorkflowService;
    @Autowired private GrowthService growthService;
    @Autowired private AssignmentLifecycleService assignmentLifecycleService;

    @Test
    void completeWorkflowKeepsVersionsAndAwardsPointsOnlyOnce() {
        long adminId = member("验收管理员");
        long mentorId = member("验收导师");
        long memberId = member("验收成员");
        grantRole(adminId, "ADMIN");
        grantRole(mentorId, "MENTOR");
        grantRole(memberId, "MEMBER");
        CurrentMember admin = current(adminId, List.of("ADMIN"), List.of("route:manage", "task:manage", "stats:global", "submission:review"));
        CurrentMember mentor = current(mentorId, List.of("MENTOR"), List.of("submission:review"));
        CurrentMember member = current(memberId, List.of("MEMBER"), List.of("task:read", "task:claim", "task:submit"));
        long directionId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_technical_direction WHERE direction_key = 'frontend'", Long.class);
        long l0 = jdbcTemplate.queryForObject("SELECT id FROM quest_growth_stage WHERE stage_key = 'L0'", Long.class);
        long l1 = jdbcTemplate.queryForObject("SELECT id FROM quest_growth_stage WHERE stage_key = 'L1'", Long.class);

        long routeId = number(adminWorkflowService.createRoute(admin, new CreateRouteRequest(
            "acceptance-route", "验收成长路线", "验证任务闭环", directionId, List.of("L0", "L1")
        )).get("id"));
        adminWorkflowService.publishRoute(admin, routeId);
        growthService.enroll(member, routeId);

        long firstTaskId = createTask(admin, mentorId, directionId, routeId, l0, "acceptance-first", List.of(), 120);
        long secondTaskId = createTask(admin, mentorId, directionId, routeId, l1, "acceptance-second", List.of(firstTaskId), 50);
        adminWorkflowService.changeTaskStatus(admin, firstTaskId, "PUBLISHED");
        adminWorkflowService.changeTaskStatus(admin, secondTaskId, "PUBLISHED");
        CurrentMember unrelatedOwner = current(memberId, List.of("PROJECT_OWNER"), List.of("task:manage"));
        assertThatThrownBy(() -> adminWorkflowService.changeTaskStatus(unrelatedOwner, firstTaskId, "OFFLINE"))
            .hasMessageContaining("自己负责");
        assertThatThrownBy(() -> adminWorkflowService.assignTask(unrelatedOwner, firstTaskId, new AssignTaskRequest(memberId)))
            .hasMessageContaining("自己负责");
        assertThat(adminWorkflowService.levelRules(admin)).hasSize(6);

        assertThatThrownBy(() -> taskWorkflowService.claim(member, secondTaskId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("前置任务");

        long assignmentId = number(taskWorkflowService.claim(member, firstTaskId).get("assignmentId"));
        long firstSubmissionId = number(taskWorkflowService.submit(member, assignmentId, new SubmitTaskRequest(
            "完成第一版", "https://github.com/example/quest", null, null, null, "学会提交", false, null
        )).get("submissionId"));
        taskWorkflowService.review(mentor, firstSubmissionId, new ReviewSubmissionRequest(
            "REVISION_REQUIRED", "请补充验证说明", Map.of("completion", 70), List.of("补充可复现步骤"), List.of(), List.of("结构清晰"), true, false
        ));
        long reviewId = jdbcTemplate.queryForObject("SELECT id FROM quest_review_record WHERE submission_id = ?", Long.class, firstSubmissionId);
        long appealId = number(taskWorkflowService.createAppeal(member, reviewId, new CreateAppealRequest("我已经提供验证步骤，希望由其他导师复核" )).get("id"));
        assertThatThrownBy(() -> taskWorkflowService.resolveAppeal(mentor, appealId, new ResolveAppealRequest("UPHELD", "原审核意见合理")))
            .hasMessageContaining("原审核人");
        taskWorkflowService.resolveAppeal(admin, appealId, new ResolveAppealRequest("OVERTURNED", "允许成员补充说明后重新提交"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM quest_review_appeal WHERE id = ?", String.class, appealId)).isEqualTo("OVERTURNED");

        long secondSubmissionId = number(taskWorkflowService.submit(member, assignmentId, new SubmitTaskRequest(
            "完成第二版并补充验证", "https://github.com/example/quest", "https://github.com/example/quest/pull/1", null, null,
            "理解了可复现验证", true, "使用 AI 整理检查清单，逐项人工验证"
        )).get("submissionId"));
        taskWorkflowService.review(mentor, secondSubmissionId, new ReviewSubmissionRequest(
            "PASSED", "验收通过", Map.of("completion", 100), List.of(), List.of("继续保持"), List.of("验证完整"), true, true
        ));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM quest_task_assignment WHERE id = ?", String.class, assignmentId)).isEqualTo("PASSED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_task_submission WHERE assignment_id = ?", Integer.class, assignmentId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_review_record r JOIN quest_task_submission s ON s.id = r.submission_id WHERE s.assignment_id = ?", Integer.class, assignmentId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT total_points FROM quest_member WHERE id = ?", Integer.class, memberId)).isEqualTo(120);
        assertThat(jdbcTemplate.queryForObject("SELECT current_level FROM quest_member WHERE id = ?", String.class, memberId)).isEqualTo("L1");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_point_ledger WHERE member_id = ?", Integer.class, memberId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT current_stage_id FROM quest_member_route WHERE member_id = ? AND route_id = ?", Long.class, memberId, routeId)).isEqualTo(l1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_notification WHERE member_id = ?", Integer.class, memberId)).isGreaterThanOrEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_audit_log WHERE actor_member_id IN (?, ?, ?)", Integer.class, adminId, mentorId, memberId)).isGreaterThanOrEqualTo(8);
        assertThat(taskWorkflowService.claim(member, secondTaskId)).containsEntry("status", "IN_PROGRESS");
        long assignedMemberId = member("验收被分配成员");
        Map<String, Object> directAssignment = adminWorkflowService.assignTask(admin, secondTaskId, new AssignTaskRequest(assignedMemberId));
        assertThat(directAssignment.get("dueAt")).isNotNull();
        long directAssignmentId = number(directAssignment.get("assignmentId"));
        jdbcTemplate.update("UPDATE quest_task_assignment SET due_at = DATE_SUB(NOW(3), INTERVAL 1 MINUTE) WHERE id = ?", directAssignmentId);
        assignmentLifecycleService.reconcile();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM quest_task_assignment WHERE id = ?", String.class, directAssignmentId)).isEqualTo("OVERDUE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_notification WHERE member_id = ? AND notification_type = 'TASK_OVERDUE'", Integer.class, assignedMemberId)).isEqualTo(1);
        CurrentMember assignedMember = current(assignedMemberId, List.of("MEMBER"), List.of("task:read", "task:submit"));
        assertThatThrownBy(() -> taskWorkflowService.abandon(member, directAssignmentId))
            .hasMessageContaining("领取记录不存在");
        taskWorkflowService.abandon(assignedMember, directAssignmentId);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM quest_task_assignment WHERE id = ?", String.class, directAssignmentId)).isEqualTo("ABANDONED");
        adminWorkflowService.changeTaskStatus(admin, firstTaskId, "ARCHIVED");
        assertThat(taskWorkflowService.detail(member, firstTaskId)).containsEntry("id", firstTaskId);
        assertThat(taskWorkflowService.submissionHistory(member, assignmentId)).hasSize(2);
        assertThatThrownBy(() -> taskWorkflowService.review(mentor, secondSubmissionId, new ReviewSubmissionRequest(
            "PASSED", "重复审核", Map.of(), List.of(), List.of(), List.of(), true, false
        ))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolvingOldAppealPreservesNewPassedVersionAndFullResolution() {
        long adminId = member("申诉管理员"), mentorId = member("申诉导师"), memberId = member("申诉成员");
        grantRole(adminId, "ADMIN");
        grantRole(mentorId, "MENTOR");
        grantRole(memberId, "MEMBER");
        CurrentMember admin = current(adminId, List.of("ADMIN"), List.of("route:manage", "task:manage", "submission:review"));
        CurrentMember mentor = current(mentorId, List.of("MENTOR"), List.of("submission:review"));
        CurrentMember learner = current(memberId, List.of("MEMBER"), List.of("task:claim", "task:submit"));
        long directionId = jdbcTemplate.queryForObject("SELECT id FROM quest_technical_direction WHERE direction_key = 'frontend'", Long.class);
        long stageId = jdbcTemplate.queryForObject("SELECT id FROM quest_growth_stage WHERE stage_key = 'L0'", Long.class);
        long routeId = number(adminWorkflowService.createRoute(admin, new CreateRouteRequest(
            "appeal-version-route", "申诉版本路线", "", directionId, List.of("L0"))).get("id"));
        long taskId = createTask(admin, mentorId, directionId, routeId, stageId, "appeal-version-task", List.of(), 120);
        adminWorkflowService.changeTaskStatus(admin, taskId, "PUBLISHED");
        long assignmentId = number(taskWorkflowService.claim(learner, taskId).get("assignmentId"));
        SubmitTaskRequest submission = new SubmitTaskRequest("完成说明", null, null, null, null, "总结", false, null);
        long firstId = number(taskWorkflowService.submit(learner, assignmentId, submission).get("submissionId"));
        taskWorkflowService.review(mentor, firstId, new ReviewSubmissionRequest(
            "REVISION_REQUIRED", "补充说明", Map.of(), List.of("补充步骤"), List.of(), List.of(), true, false));
        long reviewId = jdbcTemplate.queryForObject("SELECT id FROM quest_review_record WHERE submission_id = ?", Long.class, firstId);
        long appealId = number(taskWorkflowService.createAppeal(learner, reviewId, new CreateAppealRequest("请求复核旧版本" )).get("id"));
        long secondId = number(taskWorkflowService.submit(learner, assignmentId, submission).get("submissionId"));
        taskWorkflowService.review(mentor, secondId, new ReviewSubmissionRequest(
            "PASSED", "通过", Map.of(), List.of(), List.of(), List.of(), true, false));
        String resolution = "复核说明".repeat(400);
        taskWorkflowService.resolveAppeal(admin, appealId, new ResolveAppealRequest("OVERTURNED", resolution));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM quest_task_assignment WHERE id = ?", String.class, assignmentId)).isEqualTo("PASSED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM quest_task_submission WHERE id = ?", String.class, firstId)).isEqualTo("SUPERSEDED");
        assertThat(jdbcTemplate.queryForObject("SELECT resolution FROM quest_review_appeal WHERE id = ?", String.class, appealId)).isEqualTo(resolution);
        assertThat(jdbcTemplate.queryForObject("SELECT total_points FROM quest_member WHERE id = ?", Integer.class, memberId)).isEqualTo(120);
        assertThat(jdbcTemplate.queryForObject("SELECT CHAR_LENGTH(content) FROM quest_notification WHERE member_id = ? AND notification_type = 'APPEAL_RESOLVED'", Integer.class, memberId)).isEqualTo(1000);
    }

    @Test
    void taskOwnerMustHaveReviewPermission() {
        long adminId = member("负责人校验管理员");
        long ordinaryMemberId = member("普通负责人候选人");
        grantRole(adminId, "ADMIN");
        grantRole(ordinaryMemberId, "MEMBER");
        CurrentMember admin = current(adminId, List.of("ADMIN"), List.of("route:manage", "task:manage"));
        long directionId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_technical_direction WHERE direction_key = 'frontend'", Long.class);
        long stageId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_growth_stage WHERE stage_key = 'L0'", Long.class);
        long routeId = number(adminWorkflowService.createRoute(admin, new CreateRouteRequest(
            "owner-validation-route", "负责人校验路线", "", directionId, List.of("L0")
        )).get("id"));

        assertThatThrownBy(() -> createTask(
            admin, ordinaryMemberId, directionId, routeId, stageId, "invalid-task-owner", List.of(), 10
        )).hasMessageContaining("任务负责人");
    }

    @Test
    void projectOwnerCanManageOnlyOwnedTasks() {
        long adminId = member("范围校验管理员");
        long firstOwnerId = member("项目负责人甲");
        long secondOwnerId = member("项目负责人乙");
        grantRole(adminId, "ADMIN");
        grantRole(firstOwnerId, "PROJECT_OWNER");
        grantRole(secondOwnerId, "PROJECT_OWNER");
        CurrentMember admin = current(adminId, List.of("ADMIN"), List.of("route:manage", "task:manage"));
        CurrentMember firstOwner = current(firstOwnerId, List.of("PROJECT_OWNER"), List.of("task:manage", "submission:review"));
        long directionId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_technical_direction WHERE direction_key = 'frontend'", Long.class);
        long stageId = jdbcTemplate.queryForObject(
            "SELECT id FROM quest_growth_stage WHERE stage_key = 'L0'", Long.class);
        long routeId = number(adminWorkflowService.createRoute(admin, new CreateRouteRequest(
            "project-owner-route", "项目负责人路线", "", directionId, List.of("L0")
        )).get("id"));
        adminWorkflowService.publishRoute(admin, routeId);
        long firstTaskId = createTask(admin, firstOwnerId, directionId, routeId, stageId, "project-owner-first", List.of(), 20);
        long secondTaskId = createTask(admin, secondOwnerId, directionId, routeId, stageId, "project-owner-second", List.of(), 20);

        assertThat(adminWorkflowService.tasks(firstOwner))
            .extracting(item -> ((Number) item.get("id")).longValue())
            .containsExactly(firstTaskId);
        assertThat((List<?>) adminWorkflowService.taskOptions(firstOwner).get("routes")).isNotEmpty();
        assertThat((List<?>) adminWorkflowService.taskOptions(firstOwner).get("stages")).isNotEmpty();
        adminWorkflowService.changeTaskStatus(firstOwner, firstTaskId, "PUBLISHED");
        assertThatThrownBy(() -> adminWorkflowService.changeTaskStatus(firstOwner, secondTaskId, "PUBLISHED"))
            .hasMessageContaining("自己负责");

        long selfOwnedTaskId = number(adminWorkflowService.createTask(firstOwner, new CreateTaskRequest(
            "project-owner-self", "负责人自建任务", "验证项目负责人任务入口", directionId, routeId, stageId,
            "REAL_PROJECT", "BEGINNER", List.of("完成项目任务"), 60, "NONE", null, null,
            "按任务说明完成", List.of(), "提交成果说明", "成果可验证", 30, null,
            List.of(), 3, 10, true, List.of(firstTaskId)
        )).get("id"));
        assertThat(jdbcTemplate.queryForObject(
            "SELECT owner_member_id FROM quest_task WHERE id = ?", Long.class, selfOwnedTaskId
        )).isEqualTo(firstOwnerId);
    }

    private long createTask(CurrentMember admin, long mentorId, long directionId, long routeId, long stageId, String key, List<Long> prerequisites, int points) {
        return number(adminWorkflowService.createTask(admin, new CreateTaskRequest(
            key, "任务 " + key, "用于验收主流程", directionId, routeId, stageId,
            "LEARNING", "ENTRY", List.of("完成目标"), 30, "AFTER_CLAIM", null, 72,
            "按步骤完成任务", List.of("https://docs.jmi-openatom.cn"), "提交成果说明", "结果可复现",
            points, mentorId, List.of(), 3, 100, true, prerequisites
        )).get("id"));
    }

    private long member(String nickname) {
        jdbcTemplate.update("""
            INSERT INTO quest_member (nickname, status, current_level, total_points, profile_completed_at, onboarding_completed_at)
            VALUES (?, 'ACTIVE', 'L0', 0, NOW(3), NOW(3))
            """, nickname);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void grantRole(long memberId, String roleKey) {
        jdbcTemplate.update("""
            INSERT INTO quest_member_role (member_id, role_id)
            SELECT ?, id FROM quest_role WHERE role_key = ?
            """, memberId, roleKey);
    }

    private CurrentMember current(long id, List<String> roles, List<String> permissions) {
        return new CurrentMember(id, "验收用户", null, "ACTIVE", "L0", 0, true, true, roles, permissions);
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }
}
