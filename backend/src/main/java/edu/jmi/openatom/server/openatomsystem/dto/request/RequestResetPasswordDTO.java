package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重置密码请求
 *
 * <p>用于管理员重置用户密码, 携带新密码newPassword
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResetPasswordDTO {
  @NotBlank(message = "新密码不能为空")
  @Size(min = 8, max = 72, message = "新密码长度必须在8到72个字符之间")
  private String newPassword;
}
