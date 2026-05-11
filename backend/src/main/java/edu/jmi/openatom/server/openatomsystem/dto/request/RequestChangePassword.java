package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码请求
 *
 * <p>用于用户修改自己的登录密码, 携带旧密码oldPassword和新密码newPassword
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestChangePassword {
  private String oldPassword;
  private String newPassword;
}
