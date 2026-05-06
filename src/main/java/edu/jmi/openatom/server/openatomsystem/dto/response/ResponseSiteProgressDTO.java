package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseSiteProgressDTO {
  private List<ApplicationProgress> applications;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApplicationProgress {
    private Integer id;
    private Integer clubId;
    private String clubName;
    private Integer campaignId;
    private String campaignName;
    private String applicantName;
    private String preferredDepartment;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<ApprovalRecordProgress> approvalRecords;
    private List<InterviewProgress> interviews;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InterviewProgress {
    private Integer id;
    private Integer round;
    private Timestamp scheduledStartAt;
    private Timestamp scheduledEndAt;
    private String location;
    private String mode;
    private String status;
    private String comment;
    private String suggestion;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApprovalRecordProgress {
    private String node;
    private String action;
    private String comment;
    private Timestamp createdAt;
  }
}
