package edu.jmi.openatom.server.openatomsystem.dto;

import java.io.Serializable;
import lombok.Getter;

/**
 * 统一API响应
 *
 * <p>封装API响应的状态码, 消息提示, 返回数据及追踪ID, 提供静态工厂方法用于快速构建成功或失败响应
 */
@Getter
public class ApiResponse<T> implements Serializable {
  public static final int SUCCESS_CODE = 0;
  public static final String SUCCESS_MESSAGE = "success";
  public static final int ERROR_CODE = 50000;
  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_TRACE_ID = "defaultTraceId";

  private final Integer code;
  private final String message;
  private final T data;
  private final String traceId;

  // 私有化构造方法，强制使用静态工厂方法
  private ApiResponse(Integer code, String message, T data, String traceId) {
    this.code = code;
    this.message = message;
    this.data = data;
    this.traceId = (traceId == null || traceId.isEmpty()) ? DEFAULT_TRACE_ID : traceId;
  }

  // --- 成功返回的方法 ---

  public static <T> ApiResponse<T> success() {
    return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null, DEFAULT_TRACE_ID);
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, DEFAULT_TRACE_ID);
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(SUCCESS_CODE, message, data, DEFAULT_TRACE_ID);
  }

  // --- 失败返回的方法 ---

  public static <T> ApiResponse<T> error() {
    return new ApiResponse<>(ERROR_CODE, "操作失败", null, DEFAULT_TRACE_ID);
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(ERROR_CODE, message, null, DEFAULT_TRACE_ID);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null, DEFAULT_TRACE_ID);
  }

  public static <T> ApiResponse<T> error(int code, String message, String traceId) {
    return new ApiResponse<>(code, message, null, traceId);
  }
}
