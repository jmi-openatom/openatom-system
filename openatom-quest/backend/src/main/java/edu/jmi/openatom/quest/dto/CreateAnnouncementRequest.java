package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateAnnouncementRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 10000) String content,
    LocalDateTime expiresAt
) {
}
