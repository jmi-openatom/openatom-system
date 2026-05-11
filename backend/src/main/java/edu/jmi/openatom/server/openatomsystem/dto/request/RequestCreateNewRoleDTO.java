package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建角色请求
 *
 * <p>用于创建新角色, 包含角色名称name, 唯一编码code, 数据权限范围dataScope和描述description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateNewRoleDTO {
  @NotBlank(message = "角色名称不能为空")
  @Size(max = 64, message = "角色名称长度不能超过64个字符")
  private String name;

  @NotBlank(message = "角色编码不能为空")
  @Pattern(regexp = "^[a-z][a-z0-9:_-]{1,63}$", message = "角色编码只能包含小写字母、数字、冒号、下划线和连字符，并以字母开头")
  private String code;

  private String dataScope;

  @Size(max = 255, message = "描述长度不能超过255个字符")
  private String description;
}
