package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskOwnerRequest(@NotNull @Min(1) Long ownerMemberId) {
}
