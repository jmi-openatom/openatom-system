package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建岗位请求
 *
 * <p>用于创建社团岗位, 包含岗位名称name, 所属部门ID departmentId, 最大人数maxCount和关联角色ID列表roleIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreatePositionDTO {
  @NotBlank(message = "岗位名称不能为空")
  private String name;

  private Integer departmentId;

  @Min(value = 1, message = "岗位最大人数必须大于0")
  private Integer maxCount;

  private List<Integer> roleIds;
}
