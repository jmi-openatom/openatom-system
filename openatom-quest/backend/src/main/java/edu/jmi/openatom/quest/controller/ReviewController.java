package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.dto.ReviewSubmissionRequest;
import edu.jmi.openatom.quest.dto.CreateAppealRequest;
import edu.jmi.openatom.quest.dto.ResolveAppealRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final TaskWorkflowService workflowService;

    @GetMapping("/queue")
    public ApiResponse<List<Map<String, Object>>> queue(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.reviewQueue(member));
    }

    @PostMapping("/submissions/{submissionId}")
    public ApiResponse<Map<String, Object>> review(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long submissionId,
        @Valid @RequestBody ReviewSubmissionRequest request
    ) {
        return ApiResponse.ok(workflowService.review(member, submissionId, request));
    }

    @PostMapping("/{reviewId}/appeals")
    public ApiResponse<Map<String, Object>> createAppeal(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long reviewId,
        @Valid @RequestBody CreateAppealRequest request
    ) {
        return ApiResponse.ok(workflowService.createAppeal(member, reviewId, request));
    }

    @GetMapping("/appeals/queue")
    public ApiResponse<List<Map<String, Object>>> appealQueue(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.appealQueue(member));
    }

    @PostMapping("/appeals/{appealId}/resolve")
    public ApiResponse<Void> resolveAppeal(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long appealId,
        @Valid @RequestBody ResolveAppealRequest request
    ) {
        workflowService.resolveAppeal(member, appealId, request);
        return ApiResponse.ok();
    }
}
