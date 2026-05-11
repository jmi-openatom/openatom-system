package edu.jmi.openatom.server.openatomsystem.common;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 时间工具类
 *
 * <p>提供时间戳解析和获取当前时间戳等静态方法
 */
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
