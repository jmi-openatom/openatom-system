package edu.jmi.openatom.lab.framework.auth;

import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.framework.entity.LabUser;

public final class LabUserViews {
  private LabUserViews() {}

  public static LabDtos.LabUserView toView(LabUser user) {
    if (user == null) {
      return null;
    }
    return new LabDtos.LabUserView(
        user.getId(),
        user.getClubUserId(),
        user.getUsername(),
        user.getNickname(),
        user.getAvatarUrl(),
        user.getEmail(),
        user.getPhone(),
        user.getLabRole(),
        user.getReputationScore(),
        user.getLastLoginAt());
  }
}
