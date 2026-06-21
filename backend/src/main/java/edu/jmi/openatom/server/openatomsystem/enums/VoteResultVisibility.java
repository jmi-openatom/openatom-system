package edu.jmi.openatom.server.openatomsystem.enums;

import java.util.Arrays;

/** 投票结果在前台的可见范围 */
public enum VoteResultVisibility {
  PUBLIC("public"),
  AFTER_VOTE("after_vote"),
  PRIVATE("private");

  private final String value;

  VoteResultVisibility(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public boolean isVisibleToSite(boolean voted, boolean closed) {
    return this == PUBLIC || (this == AFTER_VOTE && (voted || closed));
  }

  public boolean hidesSiteCounts() {
    return this == PRIVATE;
  }

  public static VoteResultVisibility fromValue(String value) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim().toLowerCase();
    return Arrays.stream(values())
        .filter(item -> item.value.equals(normalized))
        .findFirst()
        .orElse(null);
  }

  public static VoteResultVisibility fromLegacy(Boolean resultVisible) {
    return Boolean.FALSE.equals(resultVisible) ? AFTER_VOTE : PUBLIC;
  }
}
