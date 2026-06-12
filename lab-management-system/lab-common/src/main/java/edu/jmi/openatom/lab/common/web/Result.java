package edu.jmi.openatom.lab.common.web;

import java.io.Serializable;

public record Result<T>(Integer code, String message, T data, String traceId) implements Serializable {
  public static final int SUCCESS_CODE = 0;
  public static final int ERROR_CODE = 50000;
  public static final int UNAUTHORIZED_CODE = 40100;
  public static final int FORBIDDEN_CODE = 40300;
  private static final String DEFAULT_TRACE_ID = "defaultTraceId";

  public static <T> Result<T> success() {
    return new Result<>(SUCCESS_CODE, "success", null, DEFAULT_TRACE_ID);
  }

  public static <T> Result<T> success(T data) {
    return new Result<>(SUCCESS_CODE, "success", data, DEFAULT_TRACE_ID);
  }

  public static <T> Result<T> success(T data, String message) {
    return new Result<>(SUCCESS_CODE, message, data, DEFAULT_TRACE_ID);
  }

  public static <T> Result<T> error(String message) {
    return new Result<>(ERROR_CODE, message, null, DEFAULT_TRACE_ID);
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null, DEFAULT_TRACE_ID);
  }
}
