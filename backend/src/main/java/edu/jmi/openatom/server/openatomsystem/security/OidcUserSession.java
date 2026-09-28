package edu.jmi.openatom.server.openatomsystem.security;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/** Access to the authenticated main-site session during an OAuth authorization request. */
@Component
public class OidcUserSession {
  public boolean isLogin() {
    return StpUtil.isLogin();
  }

  public int userId() {
    return StpUtil.getLoginIdAsInt();
  }

  public String tokenValue() {
    return StpUtil.getTokenValue();
  }

  public void logout() {
    StpUtil.logout();
  }
}
