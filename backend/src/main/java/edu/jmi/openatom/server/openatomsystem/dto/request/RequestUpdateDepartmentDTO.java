package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新部门请求
 *
 * <p>用于更新部门信息, 包含部门名称name, 描述description和负责人用户ID managerUserId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateDepartmentDTO {
  @Size(max = 64, message = "部门名称长度不能超过64个字符")
  private String name;

  @Size(max = 500, message = "部门描述长度不能超过500个字符")
  private String description;

  private Integer managerUserId;
}
