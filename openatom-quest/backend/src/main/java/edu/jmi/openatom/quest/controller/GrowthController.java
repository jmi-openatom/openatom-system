package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.GrowthService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class GrowthController {
    private final GrowthService growthService;

    @GetMapping("/stages")
    public ApiResponse<List<Map<String, Object>>> stages() {
        return ApiResponse.ok(growthService.stages());
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> routes(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(growthService.recommendedRoutes(member));
    }

    @GetMapping("/{routeId}")
    public ApiResponse<Map<String, Object>> detail(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long routeId
    ) {
        return ApiResponse.ok(growthService.routeDetail(member, routeId));
    }

    @PostMapping("/{routeId}/enroll")
    public ApiResponse<Map<String, Object>> enroll(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member,
        @PathVariable long routeId
    ) {
        return ApiResponse.ok(growthService.enroll(member, routeId));
    }
}
