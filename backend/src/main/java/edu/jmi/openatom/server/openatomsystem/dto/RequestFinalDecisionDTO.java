package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 终审决策请求
 *
 * <p>用于对申请作出最终录取决策, 包含决策结果decision, 分配部门ID departmentId, 分配岗位ID positionId和审批意见comment
 */
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
