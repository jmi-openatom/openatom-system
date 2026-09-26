package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestVoidClubExpenseDTO(
    @NotBlank(message = "请填写作废原因") @Size(max = 500) String reason,
    Integer version) {}
