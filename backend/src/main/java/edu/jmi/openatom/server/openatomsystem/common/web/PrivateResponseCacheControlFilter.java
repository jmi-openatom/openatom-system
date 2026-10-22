package edu.jmi.openatom.server.openatomsystem.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为“按登录用户返回”的前台接口补上 {@code Cache-Control: no-store}。
 *
 * <p>背景：{@code /site/**} 下既有真正公开的接口（社团主页、活动列表），也有返回
 * 当前登录用户资料的接口（激活页、入会进度、积分、请假单、博客收藏状态等）。
 * 网关曾对整个 {@code /api/v1/site/} 前缀做 30 秒共享缓存，缓存键不含用户身份，
 * 导致一个用户的个人资料被另一个用户看到。
 *
 * <p>网关侧已改为精确白名单，但这里再兜一层：只要请求带登录凭证，响应就不允许被
 * 任何共享缓存（Nginx proxy_cache、CDN、浏览器）保存。控制器若已显式声明
 * {@code Cache-Control}，以控制器为准。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class PrivateResponseCacheControlFilter extends OncePerRequestFilter {

  private static final String TOKEN_NAME = "jmiopenatom";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = relativePath(request);
    if (path == null) {
      return true;
    }
    return !(path.startsWith("/site/") || path.startsWith("/public/"));
  }

  /**
   * 解析出与业务映射一致的应用内路径。
   *
   * <p>{@code getRequestURI()} 含 context-path（生产为 {@code /api/v1}），而
   * {@code getServletPath()} 在部分容器/测试环境下为空，因此两者都做兼容处理，
   * 保证过滤器在不同部署形态下判断一致。
   */
  private String relativePath(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    if (servletPath != null && !servletPath.isBlank()) {
      return servletPath;
    }
    String uri = request.getRequestURI();
    if (uri == null) {
      return null;
    }
    String context = request.getContextPath();
    String relative =
        context == null || context.isBlank() || !uri.startsWith(context)
            ? uri
            : uri.substring(context.length());
    // 兜底：context-path 未生效但路径仍带 /api/v1 前缀时，同样还原成应用内路径。
    return relative.startsWith("/api/v1/") ? relative.substring("/api/v1".length()) : relative;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (carriesCredentials(request)) {
      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
      response.setHeader("Pragma", "no-cache");
    }
    filterChain.doFilter(request, response);
  }

  private boolean carriesCredentials(HttpServletRequest request) {
    if (isPresent(request.getHeader(TOKEN_NAME))) {
      return true;
    }
    if (isPresent(request.getHeader("Authorization"))) {
      return true;
    }
    if (request.getCookies() != null) {
      for (var cookie : request.getCookies()) {
        if (TOKEN_NAME.equals(cookie.getName()) && isPresent(cookie.getValue())) {
          return true;
        }
      }
    }
    // 老版本前端在 query 上透传 token（如静态资源 / 回调链接），同样视为登录态。
    // 用 queryString 判断而不是 getParameter，避免在多部分表单上传时提前解析请求体。
    String query = request.getQueryString();
    return query != null && query.contains(TOKEN_NAME + "=");
  }

  private boolean isPresent(String value) {
    return value != null && !value.isBlank();
  }
}
