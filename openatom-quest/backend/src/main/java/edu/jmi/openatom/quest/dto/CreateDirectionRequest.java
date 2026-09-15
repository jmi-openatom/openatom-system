package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDirectionRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,62}") String directionKey,
    @NotBlank @Size(max = 120) String name,
    @NotNull @Min(0) Integer sortOrder
) {
}
