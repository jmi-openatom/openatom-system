package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RestartAssignmentRequest(
    @NotNull LocalDateTime dueAt,
    @NotBlank @Size(max = 500) String reason
) {
}
