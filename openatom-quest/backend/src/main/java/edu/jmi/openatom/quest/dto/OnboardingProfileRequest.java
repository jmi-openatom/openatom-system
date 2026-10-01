package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OnboardingProfileRequest(
    @Size(max = 64) String nickname,
    Boolean conductAgreed,
    @Size(max = 20) List<@NotNull Long> directionIds,
    @Size(max = 50) List<@Size(max = 64) String> skills,
    @Size(max = 512) String codeProfileUrl,
    @Min(0) @Max(168) Integer weeklyHours,
    @Size(max = 1000) String bio
) {
}
