package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验验证码并重置密码请求
 *
 * <p>携带账号、邮箱验证码和新密码完成密码重置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPasswordResetDTO {
  @NotBlank(message = "账号不能为空")
  @Size(max = 128, message = "账号长度不能超过128个字符")
  private String account;

  @NotBlank(message = "验证码不能为空")
  @Size(max = 6, message = "验证码格式不正确")
  private String code;

  @NotBlank(message = "新密码不能为空")
  @Size(min = 8, max = 72, message = "密码长度必须在8到72个字符之间")
  private String newPassword;
}