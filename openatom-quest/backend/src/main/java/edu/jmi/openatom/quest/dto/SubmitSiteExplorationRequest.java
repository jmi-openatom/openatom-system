package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitSiteExplorationRequest(
    @NotBlank @Size(max = 64) String aboutFlag,
    @NotBlank @Size(max = 64) String regulationsFlag,
    @NotBlank @Size(max = 64) String activitiesFlag,
    @NotBlank @Size(max = 1000) String reflection
) {
}
