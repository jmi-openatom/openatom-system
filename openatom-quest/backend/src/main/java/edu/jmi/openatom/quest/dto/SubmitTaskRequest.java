package edu.jmi.openatom.quest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitTaskRequest(
    @NotBlank String completionNote,
    @Size(max = 512) String repositoryUrl,
    @Size(max = 512) String pullRequestUrl,
    @Size(max = 512) String demoUrl,
    @Size(max = 512) String videoUrl,
    String problemsAndLearning,
    boolean aiUsed,
    String aiUsageDetail
) {
    @AssertTrue(message = "使用 AI 工具时必须说明参与范围")
    public boolean isAiUsageValid() {
        return !aiUsed || (aiUsageDetail != null && !aiUsageDetail.isBlank());
    }
}
