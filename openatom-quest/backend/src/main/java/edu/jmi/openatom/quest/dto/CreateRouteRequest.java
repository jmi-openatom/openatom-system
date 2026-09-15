package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateRouteRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9-]{2,64}") String routeKey,
    @NotBlank @Size(max = 128) String name,
    @Size(max = 1000) String description,
    @NotNull Long directionId,
    @NotEmpty List<@Pattern(regexp = "L[0-4]") String> stageKeys
) {
}
