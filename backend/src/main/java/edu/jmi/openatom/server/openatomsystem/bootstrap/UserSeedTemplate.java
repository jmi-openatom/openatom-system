package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;

/**
 * 用户种子模板
 *
 * <p>定义系统预置用户的模板数据, 提供管理员账号的默认信息
 */
public record UserSeedTemplate(
    String username,
    String rawPassword,
    String realName,
    String studentId,
    UserStatus status,
    String phone,
    String email) {

  public static UserSeedTemplate admin() {
    return new UserSeedTemplate(
        "admin", "admin123456", "系统管理员", "admin", UserStatus.ACTIVE, null, null);
  }
}
