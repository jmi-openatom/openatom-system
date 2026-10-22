package edu.jmi.openatom.server.openatomsystem.exception;

/**
 * 业务层显式的“未登录”信号。
 *
 * <p>历史写法是 {@code Result.error(401, ...)} 并以 HTTP 200 返回，导致中间层网关
 * （Nginx {@code proxy_cache_valid 200}）会把“请先登录”的响应当成成功响应缓存下来，
 * 后续访客只能拿到这份错误体。改为抛出本异常后，全局异常处理会返回真正的 HTTP 401，
 * 任何按状态码判断的缓存层都不会保存它。
 */
public class NotLoggedInException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NotLoggedInException(String message) {
    super(message == null || message.isBlank() ? "未登录或会话已过期" : message);
  }
}
