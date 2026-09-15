package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.dto.AssignTaskRequest;
import edu.jmi.openatom.quest.dto.CreateRouteRequest;
import edu.jmi.openatom.quest.dto.CreateTaskRequest;
import edu.jmi.openatom.quest.dto.CreateDirectionRequest;
import edu.jmi.openatom.quest.dto.CreateAnnouncementRequest;
import edu.jmi.openatom.quest.dto.UpdateDirectionRequest;
import edu.jmi.openatom.quest.dto.UpdateLevelRuleRequest;
import edu.jmi.openatom.quest.dto.UpdateMemberRolesRequest;
import edu.jmi.openatom.quest.dto.UpdateMemberStatusRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.AdminWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminWorkflowService workflowService;

    @GetMapping("/announcements")
    public ApiResponse<List<Map<String, Object>>> announcements(@RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member) {
        return ApiResponse.ok(workflowService.announcements(member));
    }

    @PostMapping("/announcements")
    public ApiResponse<Map<String, Object>> createAnnouncement(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        return ApiResponse.ok(workflowService.createAnnouncement(member, request));
    }

    @PostMapping("/announcements/{announcementId}/publish")
    public ApiResponse<Void> publishAnnouncement(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long announcementId
    ) {
        workflowService.publishAnnouncement(member, announcementId);
        return ApiResponse.ok();
    }

    @GetMapping("/directions")
    public ApiResponse<List<Map<String, Object>>> directions(@RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member) {
        return ApiResponse.ok(workflowService.directions(member));
    }

    @PostMapping("/directions")
    public ApiResponse<Map<String, Object>> createDirection(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @Valid @RequestBody CreateDirectionRequest request
    ) {
        return ApiResponse.ok(workflowService.createDirection(member, request));
    }

    @PatchMapping("/directions/{directionId}")
    public ApiResponse<Void> updateDirection(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long directionId,
        @Valid @RequestBody UpdateDirectionRequest request
    ) {
        workflowService.updateDirection(member, directionId, request);
        return ApiResponse.ok();
    }

    @GetMapping("/level-rules")
    public ApiResponse<List<Map<String, Object>>> levelRules(@RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member) {
        return ApiResponse.ok(workflowService.levelRules(member));
    }

    @PatchMapping("/level-rules/{levelKey}")
    public ApiResponse<Void> updateLevelRule(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable String levelKey,
        @Valid @RequestBody UpdateLevelRuleRequest request
    ) {
        workflowService.updateLevelRule(member, levelKey, request);
        return ApiResponse.ok();
    }

    @GetMapping("/routes")
    public ApiResponse<List<Map<String, Object>>> routes(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.routes(member));
    }

    @PostMapping("/routes")
    public ApiResponse<Map<String, Object>> createRoute(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @Valid @RequestBody CreateRouteRequest request
    ) {
        return ApiResponse.ok(workflowService.createRoute(member, request));
    }

    @PostMapping("/routes/{routeId}/publish")
    public ApiResponse<Void> publishRoute(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long routeId
    ) {
        workflowService.publishRoute(member, routeId);
        return ApiResponse.ok();
    }

    @PostMapping("/routes/{routeId}/archive")
    public ApiResponse<Void> archiveRoute(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long routeId
    ) {
        workflowService.archiveRoute(member, routeId);
        return ApiResponse.ok();
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.tasks(member));
    }

    @GetMapping("/task-options")
    public ApiResponse<Map<String, Object>> taskOptions(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.taskOptions(member));
    }

    @PostMapping("/tasks")
    public ApiResponse<Map<String, Object>> createTask(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        return ApiResponse.ok(workflowService.createTask(member, request));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ApiResponse<Void> changeTaskStatus(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long taskId,
        @RequestParam String status
    ) {
        workflowService.changeTaskStatus(member, taskId, status);
        return ApiResponse.ok();
    }

    @PostMapping("/tasks/{taskId}/assignments")
    public ApiResponse<Map<String, Object>> assignTask(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long taskId,
        @Valid @RequestBody AssignTaskRequest request
    ) {
        return ApiResponse.ok(workflowService.assignTask(member, taskId, request));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.stats(member));
    }

    @GetMapping("/members")
    public ApiResponse<List<Map<String, Object>>> members(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.members(member));
    }

    @PatchMapping("/members/{memberId}/status")
    public ApiResponse<Void> updateMemberStatus(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long memberId,
        @Valid @RequestBody UpdateMemberStatusRequest request
    ) {
        workflowService.updateMemberStatus(member, memberId, request);
        return ApiResponse.ok();
    }

    @PatchMapping("/members/{memberId}/roles")
    public ApiResponse<Void> updateMemberRoles(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long memberId,
        @Valid @RequestBody UpdateMemberRolesRequest request
    ) {
        workflowService.updateMemberRoles(member, memberId, request);
        return ApiResponse.ok();
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<Map<String, Object>>> auditLogs(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.auditLogs(member));
    }
}
