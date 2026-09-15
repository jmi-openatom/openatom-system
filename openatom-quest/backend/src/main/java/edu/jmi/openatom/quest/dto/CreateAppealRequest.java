package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAppealRequest(
    @NotBlank @Size(max = 2000) String reason
) {
}
