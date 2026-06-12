package edu.jmi.openatom.lab.framework.auth;

public record LabPrincipal(Long userId, Long clubUserId, String username, String nickname, Integer labRole) {
  public boolean isAdmin() {
    return labRole != null && labRole >= 1;
  }
}
