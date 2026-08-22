package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送找回密码验证码请求
 *
 * <p>携带账号（用户名/学号/邮箱）申请发送找回密码验证码邮件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPasswordResetSendCodeDTO {
  @NotBlank(message = "账号不能为空")
  @Size(max = 128, message = "账号长度不能超过128个字符")
  private String account;
}