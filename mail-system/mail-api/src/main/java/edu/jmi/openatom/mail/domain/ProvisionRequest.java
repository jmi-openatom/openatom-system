package edu.jmi.openatom.mail.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProvisionRequest(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @NotBlank String sub,
    @NotNull Long userId,
    String username,
    String displayName,
    @NotBlank String status) {}
