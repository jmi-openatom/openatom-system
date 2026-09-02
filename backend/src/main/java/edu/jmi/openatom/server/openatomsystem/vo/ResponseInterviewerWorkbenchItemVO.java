package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseInterviewerWorkbenchItemVO {
  private Integer interviewId;
  private Integer applicationId;
  private Integer campaignId;
  private Integer clubId;
  private Integer sessionId;
  private String sessionName;
  private Integer roomId;
  private String roomName;
  private String location;
  private Integer queueNumber;
  private Timestamp scheduledStartAt;
  private Timestamp scheduledEndAt;
  private String interviewStatus;
  private String applicantName;
  private String studentId;
  private String college;
  private String major;
  private String grade;
  private String firstChoiceDepartmentName;
  private String secondChoiceDepartmentName;
  private Map<String, Object> profile;
  private ResponseInterviewEvaluationTemplateVO template;
  private Map<String, Object> ownFeedback;
  private Integer submittedCount;
  private Integer requiredCount;
  private List<Map<String, Object>> groupFeedbacks;
}
