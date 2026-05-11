package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色分配请求
 *
 * <p>用于为用户批量分配角色, 携带角色ID列表roleIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAssignUserRolesDTO {
  @NotNull(message = "roleIds不能为空")
  private List<Integer> roleIds;
}
