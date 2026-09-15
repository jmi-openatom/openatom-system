package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateOnboardingRequest(
    @Min(1) @Max(7) int step,
    @Size(max = 2000) String skillNote,
    @Size(max = 32) String assessment
) {
}
