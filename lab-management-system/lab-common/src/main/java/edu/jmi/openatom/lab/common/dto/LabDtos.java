package edu.jmi.openatom.lab.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LabDtos {
  private LabDtos() {}

  public record AuthUrlResponse(String authorizeUrl, String state) {}

  public record AuthCallbackRequest(@NotBlank String code, @NotBlank String state) {}

  public record DevLoginRequest(
      @NotNull Long clubUserId, String username, String nickname, Integer labRole) {}

  public record LabUserView(
      Long id,
      Long clubUserId,
      String username,
      String nickname,
      String avatarUrl,
      String email,
      String phone,
      Integer labRole,
      Integer reputationScore,
      LocalDateTime lastLoginAt) {}

  public record LoginResponse(String token, LocalDateTime expiresAt, LabUserView user) {}

  public record TestCaseView(
      Long id, String inputText, String expectedOutput, Boolean sampleCase, Integer sortOrder) {}

  public record ProblemView(
      Long id,
      LocalDate problemDate,
      String title,
      String slug,
      String difficulty,
      String descriptionMarkdown,
      Integer timeLimitMs,
      Integer memoryLimitMb,
      String status,
      Boolean aiGenerated,
      List<TestCaseView> sampleCases) {}

  public record SubmitCodeRequest(@NotNull Long problemId, @NotBlank String language, @NotBlank String code) {}

  public record SubmissionView(
      Long id,
      Long userId,
      Long problemId,
      String language,
      String judgeStatus,
      Integer scorePassed,
      Integer totalCases,
      Integer runtimeMs,
      Integer memoryKb,
      String errorMessage,
      LocalDateTime submittedAt,
      LocalDateTime judgedAt) {}

  public record CheckinView(
      Long id,
      Long userId,
      LocalDate checkinDate,
      Integer attendanceStatus,
      String source,
      LocalDateTime checkinAt,
      Integer localScoreChange,
      Integer clubScoreChange,
      Integer clubSyncStatus,
      String syncError) {}

  public record ScoreOverview(Integer reputationScore, Long acceptedCount, Long checkinCount) {}

  public record NotificationView(
      Long id,
      Long userId,
      String noticeType,
      String title,
      String content,
      LocalDateTime readAt,
      LocalDateTime createdAt) {}

  public record AdminProblemRequest(
      @NotNull LocalDate problemDate,
      @NotBlank String title,
      String difficulty,
      @NotBlank String descriptionMarkdown,
      Integer timeLimitMs,
      Integer memoryLimitMb,
      List<TestCaseInput> testCases,
      String solutionCpp,
      String solutionJava,
      String solutionPython) {}

  public record TestCaseInput(
      @NotBlank String inputText, @NotBlank String expectedOutput, Boolean sampleCase, Integer sortOrder) {}

  public record ScoreboardRow(Long userId, String nickname, String username, Integer labRole, Integer reputationScore) {}

  public record MonitorStatus(
      Boolean aiEnabled,
      Boolean sandboxConfigured,
      String mqExchange,
      String scoreRoutingKey,
      Integer pendingSubmissions) {}

  public record LabUserOption(
      Long id, String username, String nickname, String phone, String email, Integer labRole, Integer reputationScore) {}

  public record CheckInGroupRequest(@NotBlank String name, List<Long> userIds) {}

  public record CheckInTargetsRequest(List<Long> userIds) {}

  public record CreateCheckInSessionRequest(
      Long groupId,
      @NotBlank String title,
      String location,
      String startAt,
      String endAt,
      String status,
      Integer checkinPoints,
      Integer checkinWindowMinutes,
      Integer lateAfterMinutes,
      Integer latePenaltyPoints,
      Integer absentPenaltyPoints,
      List<Long> targetUserIds) {}

  public record CheckInScanRequest(@NotBlank String token) {}

  public record CheckInRecordStatusRequest(@NotBlank String status) {}

  public record EveningStudyScheduleRequest(
      @NotNull Long groupId,
      @NotBlank String title,
      String location,
      @NotBlank String startTime,
      @NotBlank String endTime,
      Integer checkinPoints,
      Integer checkinWindowMinutes,
      Integer lateAfterMinutes,
      Integer latePenaltyPoints,
      Integer absentPenaltyPoints,
      Boolean enabled) {}

  public record CheckInGroupView(Long id, String name, Integer memberCount, List<Long> userIds) {}

  public record CheckInRecordView(
      Long userId,
      String username,
      String nickname,
      String phone,
      String status,
      LocalDateTime checkinAt,
      Long exclusionSourceId,
      String exclusionReason) {}

  public record CheckInSessionView(
      Long id,
      Long groupId,
      Long scheduleId,
      String sessionType,
      String attendanceDate,
      String title,
      String groupName,
      String location,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime checkinDeadlineAt,
      String status,
      String qrPayload,
      Integer checkinPoints,
      Integer checkinWindowMinutes,
      Integer lateAfterMinutes,
      Integer latePenaltyPoints,
      Integer absentPenaltyPoints,
      Integer targetCount,
      Integer signedCount,
      Integer checkedCount,
      Integer lateCount,
      Integer absentCount,
      Integer pendingCount,
      Integer excusedCount,
      List<Long> targetUserIds,
      List<Long> excusedUserIds) {}

  public record EveningStudyScheduleView(
      Long id,
      Long groupId,
      String groupName,
      Integer memberCount,
      String title,
      String location,
      String startTime,
      String endTime,
      Integer checkinPoints,
      Integer checkinWindowMinutes,
      Integer lateAfterMinutes,
      Integer latePenaltyPoints,
      Integer absentPenaltyPoints,
      Boolean enabled,
      Long todaySessionId,
      Integer todayTargetCount,
      Integer todaySignedCount,
      Integer todayCheckedCount,
      Integer todayLateCount,
      Integer todayAbsentCount,
      Integer todayPendingCount,
      Integer todayExcusedCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record EveningStudyTodayView(
      String date,
      Integer sessionCount,
      Integer targetCount,
      Integer signedCount,
      Integer checkedCount,
      Integer lateCount,
      Integer absentCount,
      Integer pendingCount,
      Integer excusedCount,
      List<CheckInSessionView> sessions) {}
}
