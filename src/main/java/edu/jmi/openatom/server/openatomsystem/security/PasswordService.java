package edu.jmi.openatom.server.openatomsystem.security;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
