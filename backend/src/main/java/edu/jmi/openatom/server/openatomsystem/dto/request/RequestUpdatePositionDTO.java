package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新岗位请求
 *
 * <p>用于更新社团岗位信息, 包含岗位名称name, 所属部门ID departmentId, 最大人数maxCount和关联角色ID列表roleIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdatePositionDTO {
  private String name;
  private Integer departmentId;

  @Min(value = 1, message = "岗位最大人数必须大于0")
  private Integer maxCount;

  private List<Integer> roleIds;
}
