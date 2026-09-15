package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

public record ReviewSubmissionRequest(
    @NotBlank @Pattern(regexp = "PASSED|REVISION_REQUIRED|FAILED") String result,
    @NotBlank String comment,
    Map<String, Integer> scores,
    List<String> requiredChanges,
    List<String> suggestions,
    List<String> strengths,
    @NotNull Boolean resubmissionAllowed,
    Boolean excellent
) {
    @AssertTrue(message = "退回或不通过时必须填写可执行的修改意见")
    public boolean isRevisionDetailValid() {
        return "PASSED".equals(result)
            || (requiredChanges != null && requiredChanges.stream().anyMatch(value -> value != null && !value.isBlank()));
    }
}
