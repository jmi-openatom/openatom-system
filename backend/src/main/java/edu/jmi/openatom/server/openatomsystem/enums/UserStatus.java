package edu.jmi.openatom.server.openatomsystem.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 用户状态枚举 */
@Getter
@AllArgsConstructor
public enum UserStatus {
  ACTIVE(1, "active"),
  DISABLED(0, "disabled"),
  LOCKED(2, "locked");

  @EnumValue private final Integer value;
  private final String description;

  @JsonValue
  public String jsonValue() {
    return description;
  }

  @JsonCreator
  public static UserStatus fromValue(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    if ("enabled".equalsIgnoreCase(value)) {
      return ACTIVE;
    }
    return Arrays.stream(values())
        .filter(
            status ->
                status.name().equalsIgnoreCase(value)
                    || status.description.equalsIgnoreCase(value)
                    || String.valueOf(status.value).equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("未知用户状态: " + value));
  }
}
