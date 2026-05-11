package edu.jmi.openatom.server.openatomsystem.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import edu.jmi.openatom.server.openatomsystem.common.Result;
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

/**
 * 全局异常处理
 *
 * <p>使用 @RestControllerAdvice 统一处理全局异常，包括未登录, 无权限,
 * 参数校验失败, 资源不存在等异常, 返回统一的 ApiResponse 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerAdvice {

  @ExceptionHandler(NotLoginException.class)
  public ResponseEntity<Result<Void>> handleNotLoginException(NotLoginException e) {
    return ResponseEntity.status(401).body(Result.error(401, "未登录或会话已过期"));
  }

  @ExceptionHandler(NotRoleException.class)
  public Result<Void> handleNotRoleException(NotRoleException e) {
    return Result.error(403, "缺少所需角色");
  }

  @ExceptionHandler(NotPermissionException.class)
  public Result<Void> handleNotPermissionException(NotPermissionException e) {
    return Result.error(403, "缺少所需权限");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
    return Result.error(400, e.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
    return Result.error(404, "请求路径不存在");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public Result<Void> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    log.warn("Invalid request body", e);
    return Result.error(400, "请求体格式错误，请检查 JSON 格式");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<Void> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
    return Result.error(400, message.isBlank() ? "请求参数不合法" : message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
    String message =
        e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .distinct()
            .collect(Collectors.joining("; "));
    return Result.error(400, message.isBlank() ? "请求参数不合法" : message);
  }

  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return Result.error(500, "服务器内部错误");
  }
}
