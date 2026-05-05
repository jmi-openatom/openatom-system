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
public class RequestAssignPositionDTO {
  @NotNull(message = "positionId不能为空")
  private Integer positionId;

  private String effectiveAt;
}
