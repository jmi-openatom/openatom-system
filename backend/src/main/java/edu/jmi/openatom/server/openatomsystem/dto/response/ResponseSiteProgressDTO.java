package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewFeedback;

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
    private List<InterviewProgress> interviews;
    private List<ApprovalRecord> approvalRecords;
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
    private List<InterviewFeedback> feedbacks;
  }
}
