package edu.jmi.openatom.server.openatomsystem.exception;

/** 请求频率超过限制时抛出的业务异常。 */
public class RateLimitExceededException extends RuntimeException {
  public RateLimitExceededException(String message) {
    super(message);
  }
}
