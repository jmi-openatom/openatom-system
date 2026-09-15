package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.TaskWorkflowService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final TaskWorkflowService workflowService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ApiResponse.ok(workflowService.notifications(member, unreadOnly));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> read(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long notificationId
    ) {
        workflowService.markNotificationRead(member, notificationId);
        return ApiResponse.ok();
    }
}
