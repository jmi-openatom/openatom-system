package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.GrowthService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/growth")
@RequiredArgsConstructor
public class GrowthProfileController {
    private final GrowthService growthService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(
        @RequestAttribute(MemberAccessInterceptor.CURRENT_MEMBER) CurrentMember member
    ) {
        return ApiResponse.ok(growthService.memberGrowth(member));
    }
}
