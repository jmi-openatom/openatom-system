package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record CreateTaskRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9-]{2,64}") String taskKey,
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 500) String summary,
    Long directionId,
    Long routeId,
    Long stageId,
    @NotBlank @Pattern(regexp = "ONBOARDING|LEARNING|CHALLENGE|COLLABORATION|REAL_PROJECT|LIMITED_EVENT") String taskType,
    @NotBlank @Pattern(regexp = "ENTRY|BEGINNER|INTERMEDIATE|ADVANCED") String difficulty,
    @NotEmpty List<@NotBlank String> learningObjectives,
    @NotNull @Min(1) Integer estimatedMinutes,
    @NotBlank @Pattern(regexp = "NONE|FIXED|AFTER_CLAIM") String deadlineType,
    LocalDateTime fixedDeadline,
    @Min(1) Integer durationHours,
    @NotBlank String instructions,
    List<String> resources,
    @NotBlank String submissionRequirements,
    @NotBlank String acceptanceCriteria,
    @NotNull @Min(0) Integer points,
    Long ownerMemberId,
    List<String> faq,
    @Min(1) Integer submissionLimit,
    @Min(1) Integer capacity,
    Boolean requiredInStage,
    List<Long> prerequisiteTaskIds
) {
}
