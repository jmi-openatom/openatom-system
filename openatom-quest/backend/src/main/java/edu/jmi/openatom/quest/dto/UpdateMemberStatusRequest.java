package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberStatusRequest(
    @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status,
    @NotBlank @Size(max = 500) String reason
) {
}
