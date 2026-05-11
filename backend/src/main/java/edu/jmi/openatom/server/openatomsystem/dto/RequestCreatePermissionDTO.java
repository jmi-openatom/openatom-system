package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建权限请求
 *
 * <p>用于创建系统权限项, 包含权限名称name, 唯一编码code, 类型type, 关联路径path和请求方法method
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreatePermissionDTO {
  @NotBlank(message = "权限名称不能为空")
  @Size(max = 64, message = "权限名称长度不能超过64个字符")
  private String name;

  @NotBlank(message = "权限编码不能为空")
  @Pattern(regexp = "^[a-z][a-z0-9:_-]{1,127}$", message = "权限编码只能包含小写字母、数字、冒号、下划线和连字符，并以字母开头")
  private String code;

  @NotBlank(message = "权限类型不能为空")
  private String type;

  @Size(max = 255, message = "路径长度不能超过255个字符")
  private String path;

  @Pattern(regexp = "^$|^(GET|POST|PUT|PATCH|DELETE)$", message = "请求方法不正确")
  private String method;
}
