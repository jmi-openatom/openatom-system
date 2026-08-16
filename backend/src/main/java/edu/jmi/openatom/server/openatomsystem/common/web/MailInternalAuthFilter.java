package edu.jmi.openatom.server.openatomsystem.common.web;

import edu.jmi.openatom.server.openatomsystem.config.MailOutboxProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates machine-to-machine calls from the mail system on
 * {@code /internal/mail/**} using the shared service token, so those endpoints
 * do not require a user login.
 */
@Component
public class MailInternalAuthFilter extends OncePerRequestFilter {
  private final MailOutboxProperties properties;

  public MailInternalAuthFilter(MailOutboxProperties properties) {
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri == null || !uri.contains("/internal/mail/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String expected = "Bearer " + properties.getServiceToken();
    String actual = request.getHeader("Authorization");
    if (!constantTimeEquals(expected, actual == null ? "" : actual)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":401,\"message\":\"invalid_service_token\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static boolean constantTimeEquals(String expected, String actual) {
    byte[] left = expected.getBytes(StandardCharsets.UTF_8);
    byte[] right = actual.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(left, right);
  }
}