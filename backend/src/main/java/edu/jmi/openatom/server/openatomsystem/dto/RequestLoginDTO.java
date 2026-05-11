package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求
 *
 * <p>用于用户登录系统, 携带用户名username和密码password
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestLoginDTO {
  @NotBlank(message = "用户名不能为空")
  private String username;

  @NotBlank(message = "密码不能为空")
  private String password;
}
