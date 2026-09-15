package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.dto.SubmitTaskRequest;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.TaskWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {
    private final TaskWorkflowService workflowService;

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> catalog(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @RequestParam(required = false) Long directionId,
        @RequestParam(required = false) Long stageId
    ) {
        return ApiResponse.ok(workflowService.catalog(member, directionId, stageId));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> detail(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long taskId
    ) {
        return ApiResponse.ok(workflowService.detail(member, taskId));
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ApiResponse<Map<String, Object>> claim(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long taskId
    ) {
        return ApiResponse.ok(workflowService.claim(member, taskId));
    }

    @GetMapping("/assignments/me")
    public ApiResponse<List<Map<String, Object>>> myAssignments(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.myAssignments(member));
    }

    @PostMapping("/assignments/{assignmentId}/abandon")
    public ApiResponse<Void> abandon(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long assignmentId
    ) {
        workflowService.abandon(member, assignmentId);
        return ApiResponse.ok();
    }

    @PostMapping("/assignments/{assignmentId}/submissions")
    public ApiResponse<Map<String, Object>> submit(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long assignmentId,
        @Valid @RequestBody SubmitTaskRequest request
    ) {
        return ApiResponse.ok(workflowService.submit(member, assignmentId, request));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ApiResponse<List<Map<String, Object>>> history(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long assignmentId
    ) {
        return ApiResponse.ok(workflowService.submissionHistory(member, assignmentId));
    }
}
