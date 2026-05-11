package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求
 *
 * <p>用于新用户注册账号, 包含用户名username, 密码password, 真实姓名realName, 手机号phone, 邮箱email和学号studentId
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestRegisterDTO {
  @Size(max = 32, message = "学号长度不能超过32个字符")
  private String studentId;

  @NotBlank(message = "用户名不能为空")
  @Size(max = 64, message = "用户名长度不能超过64个字符")
  private String username;

  @NotBlank(message = "密码不能为空")
  @Size(min = 8, max = 72, message = "密码长度必须在8到72个字符之间")
  private String password;

  @Size(max = 64, message = "真实姓名长度不能超过64个字符")
  private String realName;

  @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
  private String phone;

  @Email(message = "邮箱格式不正确")
  @Size(max = 128, message = "邮箱长度不能超过128个字符")
  private String email;
}
