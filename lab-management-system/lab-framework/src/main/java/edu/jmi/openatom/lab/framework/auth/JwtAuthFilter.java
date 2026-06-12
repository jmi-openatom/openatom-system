package edu.jmi.openatom.lab.framework.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.lab.common.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements HandlerInterceptor {
  private static final Set<String> PUBLIC_PREFIXES =
      Set.of("/api/health", "/api/auth/oauth", "/api/auth/dev-login");

  private final LabJwtService jwtService;
  private final ObjectMapper objectMapper;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request.getRequestURI())) {
      return true;
    }

    String token = resolveToken(request);
    LabPrincipal principal = token == null ? null : jwtService.parse(token);
    if (principal == null) {
      writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.error(Result.UNAUTHORIZED_CODE, "请先登录"));
      return false;
    }
    if (request.getRequestURI().startsWith("/api/admin") && !principal.isAdmin()) {
      writeJson(response, HttpServletResponse.SC_FORBIDDEN, Result.error(Result.FORBIDDEN_CODE, "缺少实验室后台权限"));
      return false;
    }
    LabSecurityContext.set(principal);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    LabSecurityContext.clear();
  }

  private boolean isPublic(String uri) {
    return PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
  }

  private String resolveToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      return authorization.substring(7);
    }
    String custom = request.getHeader("jmilab");
    return custom == null || custom.isBlank() ? null : custom;
  }

  private void writeJson(HttpServletResponse response, int status, Result<?> body) throws Exception {
    response.setStatus(status);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
