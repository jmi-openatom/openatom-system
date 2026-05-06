package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateMembershipDTO {
  @NotNull(message = "userId不能为空")
  private Integer userId;

  @NotNull(message = "clubId不能为空")
  private Integer clubId;

  private Integer departmentId;
  private Integer positionId;
  private String status;
  private Boolean featured;
  private Integer sortOrder;
}
