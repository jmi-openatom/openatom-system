package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolveAppealRequest(
    @NotBlank @Pattern(regexp = "UPHELD|OVERTURNED") String status,
    @NotBlank @Size(max = 2000) String resolution
) {
}
