package edu.jmi.openatom.lab.framework.auth;

public final class LabSecurityContext {
  private static final ThreadLocal<LabPrincipal> CURRENT = new ThreadLocal<>();

  private LabSecurityContext() {}

  public static void set(LabPrincipal principal) {
    CURRENT.set(principal);
  }

  public static LabPrincipal require() {
    LabPrincipal principal = CURRENT.get();
    if (principal == null) {
      throw new IllegalStateException("未登录");
    }
    return principal;
  }

  public static Long userId() {
    return require().userId();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
