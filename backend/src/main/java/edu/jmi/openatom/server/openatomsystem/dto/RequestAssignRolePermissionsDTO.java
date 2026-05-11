package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色权限分配请求
 *
 * <p>用于为角色批量分配权限, 携带权限ID列表permissionIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAssignRolePermissionsDTO {
  @NotNull(message = "permissionIds不能为空")
  private List<Integer> permissionIds;
}
