package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateLevelRuleRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Min(0) Integer minimumPoints,
    @NotBlank @Pattern(regexp = "ACTIVE|ARCHIVED") String status
) {
}
