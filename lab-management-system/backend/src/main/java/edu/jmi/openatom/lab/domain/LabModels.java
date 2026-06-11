package edu.jmi.openatom.lab.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class LabModels {
  private LabModels() {}

  public record LabUser(
      Long id,
      Long clubUserId,
      String username,
      String realName,
      int labRole,
      int reputationScore,
      boolean active,
      Instant createdAt) {}

  public record LabProblem(
      Long id,
      String title,
      String descriptionMd,
      String difficulty,
      int timeLimitMs,
      int memoryLimitMb,
      String status,
      LocalDate publishDate,
      List<LabProblemCase> cases,
      Map<String, String> standardSolutions,
      Instant createdAt) {}

  public record LabProblemCase(
      Long id, Long problemId, String inputData, String expectedOutput, boolean sampleFlag, int sortOrder) {}

  public record LabSubmission(
      Long id,
      Long problemId,
      Long labUserId,
      String language,
      String status,
      String judgeMessage,
      int passedCaseCount,
      int totalCaseCount,
      Instant createdAt) {}

  public record LabCheckinLog(
      Long id,
      Long labUserId,
      LocalDate checkinDate,
      int attendanceStatus,
      String attendanceStatusText,
      String sourceType,
      Long sourceId,
      int localScoreChange,
      int clubScoreChange,
      int clubSyncStatus,
      String clubSyncStatusText,
      String remark,
      Instant createdAt) {}

  public record LabNotice(
      Long id,
      Long receiverLabUserId,
      String title,
      String content,
      String type,
      boolean readFlag,
      Instant createdAt) {}

  public record LabSettings(
      LocalTime standardCheckinTime,
      int lateMinutes,
      int normalClubPoints,
      int latePenalty,
      int absentPenalty) {}

  public record Dashboard(
      long activeMemberCount,
      long problemCount,
      long todaySubmissionCount,
      long todayAcceptedCount,
      long todayCheckedInCount,
      long pendingClubSyncCount,
      LocalDate today) {}

  public record CmsLoginRequest(
      @NotNull Long clubUserId,
      @NotBlank String username,
      String realName,
      @NotNull Boolean isLabMember) {}

  public record SessionResponse(String token, LabUser user) {}

  public record SubmissionRequest(@NotBlank String language, @NotBlank String code) {}

  public record LabUserRequest(
      @NotNull Long clubUserId,
      @NotBlank String username,
      String realName,
      Integer labRole,
      Boolean active) {}

  public record ProblemRequest(
      @NotBlank String title,
      @NotBlank String descriptionMd,
      String difficulty,
      Integer timeLimitMs,
      Integer memoryLimitMb,
      String status,
      LocalDate publishDate,
      List<LabProblemCase> cases,
      Map<String, String> standardSolutions) {}

  public record NoticeRequest(Long receiverLabUserId, @NotBlank String title, @NotBlank String content, String type) {}

  public record SettingsRequest(
      LocalTime standardCheckinTime,
      Integer lateMinutes,
      Integer normalClubPoints,
      Integer latePenalty,
      Integer absentPenalty) {}
}
