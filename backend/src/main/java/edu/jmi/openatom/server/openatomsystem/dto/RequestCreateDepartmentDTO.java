package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建部门请求
 *
 * <p>用于在社团下创建新部门, 包含部门名称name, 描述description和负责人用户ID managerUserId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateDepartmentDTO {
  @NotBlank(message = "部门名称不能为空")
  @Size(max = 64, message = "部门名称长度不能超过64个字符")
  private String name;

  @Size(max = 500, message = "部门描述长度不能超过500个字符")
  private String description;

  private Integer managerUserId;
}
