package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseApplicationDTO {
  private Integer id;
  private Integer campaignId;
  private String campaignName;
  private Integer clubId;
  private String clubName;
  private Integer userId;
  private String applicantName;
  private String studentId;
  private Integer firstChoiceDepartmentId;
  private String firstChoiceDepartmentName;
  private Integer secondChoiceDepartmentId;
  private String secondChoiceDepartmentName;
  private String preferredDepartment;
  private String status;
  private Map<String, Object> profile;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
