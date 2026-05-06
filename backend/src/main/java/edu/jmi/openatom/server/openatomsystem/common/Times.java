package edu.jmi.openatom.server.openatomsystem.common;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public final class Times {
  private Times() {}

  public static Timestamp parseTimestamp(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Timestamp.from(OffsetDateTime.parse(value).toInstant());
    } catch (RuntimeException ignored) {
      return Timestamp.valueOf(LocalDateTime.parse(value));
    }
  }

  public static Timestamp now() {
    return new Timestamp(System.currentTimeMillis());
  }
}
