package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;

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
