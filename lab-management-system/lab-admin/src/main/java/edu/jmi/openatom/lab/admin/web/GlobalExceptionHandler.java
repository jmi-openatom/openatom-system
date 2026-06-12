package edu.jmi.openatom.lab.admin.web;

import edu.jmi.openatom.lab.common.web.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> badRequest(IllegalArgumentException ex) {
    return Result.error(40000, ex.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> validation(Exception ex) {
    return Result.error(40000, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Result<Void> unauthorized(IllegalStateException ex) {
    return Result.error(Result.UNAUTHORIZED_CODE, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<Void> serverError(Exception ex) {
    return Result.error(ex.getMessage() == null ? "系统异常" : ex.getMessage());
  }
}
