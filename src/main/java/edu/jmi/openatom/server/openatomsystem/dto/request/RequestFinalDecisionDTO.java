package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestFinalDecisionDTO {
  @NotBlank(message = "终审决策不能为空")
  private String decision;

  private Integer departmentId;
  private Integer positionId;
  private String comment;
}
