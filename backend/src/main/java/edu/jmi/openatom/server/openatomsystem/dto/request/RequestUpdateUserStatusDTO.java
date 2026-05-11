package edu.jmi.openatom.server.openatomsystem.dto.request;

import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户状态请求
 *
 * <p>用于更新系统用户的状态, 携带目标状态status（UserStatus枚举）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateUserStatusDTO {
  private UserStatus status;
}
