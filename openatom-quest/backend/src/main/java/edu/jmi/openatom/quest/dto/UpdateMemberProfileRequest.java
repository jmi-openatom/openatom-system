package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateMemberProfileRequest(
    @NotBlank @Size(max = 64) String nickname,
    @Size(max = 512) String avatarUrl,
    @Email @Size(max = 191) String email,
    @Size(max = 128) String school,
    @Size(max = 128) String college,
    @Size(max = 128) String major,
    @Size(max = 32) String grade,
    @Size(max = 50) List<@Size(max = 64) String> skills,
    @Size(max = 512) String codeProfileUrl,
    @Min(0) @Max(168) Integer weeklyHours,
    @Size(max = 1000) String bio,
    @Size(max = 20) List<Long> directionIds,
    @Min(0) Integer totalPoints
) {
}
