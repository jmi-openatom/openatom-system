package edu.jmi.openatom.server.openatomsystem.common;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 时间工具类
 *
 * <p>提供时间戳解析和获取当前时间戳等静态方法
 */
public final class Times {
  public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

  private Times() {}

  public static Timestamp parseTimestamp(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    try {
      return Timestamp.from(OffsetDateTime.parse(normalized).toInstant());
    } catch (RuntimeException ignored) {
      return Timestamp.from(LocalDateTime.parse(normalized.replace(' ', 'T')).atZone(BUSINESS_ZONE).toInstant());
    }
  }

  public static Timestamp now() {
    return new Timestamp(System.currentTimeMillis());
  }
}
