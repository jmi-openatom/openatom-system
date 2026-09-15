package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.TaskWorkflowService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TaskWorkflowService workflowService;

    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(workflowService.dashboard(member));
    }
}
