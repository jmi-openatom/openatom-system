package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateMembershipDTO {
  private Integer departmentId;
  private Integer positionId;
  private String status;
  private Boolean featured;
  private Integer sortOrder;
}
