package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDataOpenApplicationVO {
  private Integer id;
  private String appName;
  private String applicantName;
  private String applicantContact;
  private String organization;
  private String purpose;
  private String endpointScope;
  private String status;
  private String apiKey;
  private String reviewComment;
  private Integer reviewedBy;
  private String reviewerName;
  private Timestamp reviewedAt;
  private Timestamp lastUsedAt;
  private Integer callCount;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
