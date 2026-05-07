package edu.jmi.openatom.server.openatomsystem.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerAdvice {

  @ExceptionHandler(NotLoginException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotLoginException(NotLoginException e) {
    return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或会话已过期"));
  }

  @ExceptionHandler(NotRoleException.class)
  public ApiResponse<Void> handleNotRoleException(NotRoleException e) {
    return ApiResponse.error(403, "缺少所需角色");
  }

  @ExceptionHandler(NotPermissionException.class)
  public ApiResponse<Void> handleNotPermissionException(NotPermissionException e) {
    return ApiResponse.error(403, "缺少所需权限");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
    return ApiResponse.error(400, e.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException e) {
    return ApiResponse.error(404, "请求路径不存在");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ApiResponse<Void> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    log.warn("Invalid request body", e);
    return ApiResponse.error(400, "请求体格式错误，请检查 JSON 格式");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Void> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
    return ApiResponse.error(400, message.isBlank() ? "请求参数不合法" : message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
    String message =
        e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .distinct()
            .collect(Collectors.joining("; "));
    return ApiResponse.error(400, message.isBlank() ? "请求参数不合法" : message);
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ApiResponse.error(500, "服务器内部错误");
  }
}
