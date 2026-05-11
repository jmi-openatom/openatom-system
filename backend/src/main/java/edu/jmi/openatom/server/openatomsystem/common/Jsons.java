package edu.jmi.openatom.server.openatomsystem.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 *
 * <p>封装 Jackson ObjectMapper, 提供对象序列化, JSON 解析等静态方法
 */
public final class Jsons {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private Jsons() {}

  public static String stringify(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String stringValue) {
      return stringValue;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("JSON序列化失败");
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> parseObject(String value) {
    if (value == null || value.isBlank()) {
      return Map.of();
    }
    try {
      return OBJECT_MAPPER.readValue(value, Map.class);
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  public static List<Map<String, Object>> parseListOfObjects(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    try {
      return OBJECT_MAPPER.readValue(value, new TypeReference<List<Map<String, Object>>>() {});
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }
}
