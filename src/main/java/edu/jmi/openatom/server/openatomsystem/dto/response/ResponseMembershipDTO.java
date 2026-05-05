package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMembershipDTO {
  private Integer id;
  private Integer userId;
  private String userName;
  private String realName;
  private Integer clubId;
  private String clubName;
  private Integer departmentId;
  private String departmentName;
  private Integer positionId;
  private String positionName;
  private String status;
  private Boolean featured;
  private Integer sortOrder;
  private Timestamp joinedAt;
  private Timestamp leftAt;
}
