package edu.jmi.openatom.server.openatomsystem.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Writes the host-only, HttpOnly session cookie used by the central OAuth login page. */
@Component
public class OAuthSessionCookieService {
  private final String tokenName;

  public OAuthSessionCookieService(@Value("${sa-token.token-name:jmiopenatom}") String tokenName) {
    this.tokenName = tokenName;
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      String token,
      long expiresInSeconds) {
    ResponseCookie cookie =
        ResponseCookie.from(tokenName, token)
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path(cookiePath(request))
            .maxAge(Duration.ofSeconds(Math.max(60, expiresInSeconds)))
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void clear(HttpServletRequest request, HttpServletResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(tokenName, "")
            .httpOnly(true)
            .secure(isSecure(request))
            .sameSite("Lax")
            .path(cookiePath(request))
            .maxAge(Duration.ZERO)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private boolean isSecure(HttpServletRequest request) {
    String forwardedProto = request.getHeader("X-Forwarded-Proto");
    return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
  }

  private String cookiePath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    return contextPath == null || contextPath.isBlank() ? "/" : contextPath;
  }
}
