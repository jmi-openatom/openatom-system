package edu.jmi.openatom.server.openatomsystem.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClubExpenseVO(
    Long id,
    Integer clubId,
    String clubName,
    LocalDate paidOn,
    String title,
    String category,
    BigDecimal amount,
    String handledBy,
    String paymentMethod,
    Integer activityId,
    String activityTitle,
    String note,
    String status,
    String voidReason,
    Integer createdBy,
    Integer updatedBy,
    Integer voidedBy,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime voidedAt,
    Integer attachmentCount,
    List<Attachment> attachments) {

  public record Attachment(
      Long id, String originalName, String mimeType, Long fileSize, LocalDateTime createdAt) {}

  public record History(
      Long id,
      String action,
      String beforeJson,
      String afterJson,
      String reason,
      Integer operatorId,
      String operatorName,
      LocalDateTime createdAt) {}

  public record Summary(
      Long count, BigDecimal totalAmount, List<CategoryTotal> byCategory, List<MonthTotal> byMonth) {}

  public record CategoryTotal(String category, Long count, BigDecimal amount) {}

  public record MonthTotal(String month, Long count, BigDecimal amount) {}

  public record ClubOption(Integer id, String name) {}

  public record ActivityOption(Integer id, String title) {}
}
