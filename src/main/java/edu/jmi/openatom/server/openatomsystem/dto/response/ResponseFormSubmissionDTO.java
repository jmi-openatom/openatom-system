package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFormSubmissionDTO {
  private Integer id;
  private Integer campaignId;
  private Integer clubId;
  private Integer userId;
  private String submitterName;
  private String submitterAccount;
  private String contact;
  private Map<String, Object> formData;
  private Timestamp createdAt;
}
