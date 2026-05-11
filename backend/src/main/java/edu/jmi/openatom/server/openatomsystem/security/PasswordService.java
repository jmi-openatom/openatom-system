package edu.jmi.openatom.server.openatomsystem.security;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密服务
 *
 * <p>提供密码的加密, 匹配验证以及密码加密级别升级检测等功能, 支持 BCrypt 和 SHA-256 两种加密方式
 */
@Component
public class PasswordService {
  private static final String BCRYPT_PREFIX = "$2";

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public String encode(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  public boolean matches(String rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
      return false;
    }
    if (isBcrypt(encodedPassword)) {
      return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    return SaSecureUtil.sha256(rawPassword).equals(encodedPassword);
  }

  public boolean shouldUpgrade(String encodedPassword) {
    return encodedPassword == null || !isBcrypt(encodedPassword);
  }

  private boolean isBcrypt(String encodedPassword) {
    return encodedPassword.startsWith(BCRYPT_PREFIX);
  }
}
