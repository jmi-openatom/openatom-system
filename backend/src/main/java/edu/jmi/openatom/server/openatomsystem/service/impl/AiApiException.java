package edu.jmi.openatom.server.openatomsystem.service.impl;

/** DeepSeek 调用异常（分类便于上层提示与重试决策）。 */
public class AiApiException extends RuntimeException {
  private final Kind kind;

  public AiApiException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public AiApiException(Kind kind, String message, Throwable cause) {
    super(message, cause);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  public enum Kind {
    /** 请求超时（含连接超时） */
    TIMEOUT,
    /** 触发限流（HTTP 429） */
    RATE_LIMITED,
    /** 网络错误（DNS/连接拒绝/中断） */
    NETWORK,
    /** 上游返回错误状态码或内容异常 */
    API_ERROR
  }
}