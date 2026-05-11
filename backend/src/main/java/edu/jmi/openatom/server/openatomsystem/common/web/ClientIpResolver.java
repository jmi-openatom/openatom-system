package edu.jmi.openatom.server.openatomsystem.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 客户端 IP 解析器
 *
 * <p>从 HTTP 请求中解析客户端真实 IP 地址, 支持 X-Forwarded-For, X-Real-IP, Forwarded 等代理头信息
 */
@Component
public class ClientIpResolver {

  public String resolve(HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    String forwardedFor = firstHeaderValue(request, "X-Forwarded-For");
    if (forwardedFor != null) {
      return forwardedFor;
    }

    String realIp = cleanHeaderValue(request.getHeader("X-Real-IP"));
    if (realIp != null) {
      return realIp;
    }

    String forwarded = request.getHeader("Forwarded");
    if (forwarded != null && !forwarded.isBlank()) {
      for (String part : forwarded.split(";")) {
        String trimmed = part.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("for=")) {
          return trimmed.substring(4).replace("\"", "");
        }
      }
    }

    return request.getRemoteAddr();
  }

  private String firstHeaderValue(HttpServletRequest request, String headerName) {
    String headerValue = request.getHeader(headerName);
    if (headerValue == null || headerValue.isBlank()) {
      return null;
    }
    return cleanHeaderValue(headerValue.split(",")[0]);
  }

  private String cleanHeaderValue(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
