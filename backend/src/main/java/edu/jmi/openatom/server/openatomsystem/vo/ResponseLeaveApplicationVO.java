package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseLeaveApplicationVO {
  private Integer id;
  private Integer userId;
  private String userName;
  private String realName;
  private String studentId;
  private String title;
  private String reason;
  private Timestamp startAt;
  private Timestamp endAt;
  private List<Map<String, Object>> attachments;
  private String status;
  private Integer reviewerId;
  private String reviewerName;
  private String reviewComment;
  private Timestamp reviewedAt;
  private Timestamp createdAt;
  private List<Map<String, Object>> approvalFlow;
}
