package edu.jmi.openatom.quest.controller;

import edu.jmi.openatom.quest.common.ApiResponse;
import edu.jmi.openatom.quest.config.MemberAccessInterceptor;
import edu.jmi.openatom.quest.dto.UpdateOnboardingRequest;
import edu.jmi.openatom.quest.dto.UpdateProfileRequest;
import edu.jmi.openatom.quest.entity.OnboardingProgress;
import edu.jmi.openatom.quest.model.CurrentMember;
import edu.jmi.openatom.quest.service.OnboardingService;
import edu.jmi.openatom.quest.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberProfileController {
    private final ProfileService profileService;
    private final OnboardingService onboardingService;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(HttpServletRequest request) {
        return ApiResponse.ok(profileService.getProfile(currentMember(request).id()));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest servletRequest) {
        profileService.updateProfile(currentMember(servletRequest).id(), request);
        return ApiResponse.ok();
    }

    @GetMapping("/onboarding")
    public ApiResponse<OnboardingProgress> getOnboarding(HttpServletRequest request) {
        return ApiResponse.ok(onboardingService.get(currentMember(request).id()));
    }

    @PutMapping("/onboarding")
    public ApiResponse<OnboardingProgress> updateOnboarding(
        @Valid @RequestBody UpdateOnboardingRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(onboardingService.completeStep(currentMember(servletRequest).id(), request));
    }

    private CurrentMember currentMember(HttpServletRequest request) {
        return (CurrentMember) request.getAttribute(MemberAccessInterceptor.CURRENT_MEMBER);
    }
}
