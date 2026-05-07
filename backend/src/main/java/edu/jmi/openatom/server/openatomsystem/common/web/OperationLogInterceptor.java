package edu.jmi.openatom.server.openatomsystem.common.web;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import edu.jmi.openatom.server.openatomsystem.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {
  private static final String START_TIME_ATTRIBUTE = "operationLogStartTime";
  private static final int MODULE_MAX_LENGTH = 50;
  private static final int ACTION_MAX_LENGTH = 120;
  private static final int TARGET_ID_MAX_LENGTH = 80;

  private final OperationLogMapper operationLogMapper;
  private final ClientIpResolver clientIpResolver;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
      operationLogMapper.insert(
          OperationLog.builder()
              .operatorId(resolveOperatorId())
              .module(resolveModule(request))
              .action(resolveAction(request))
              .targetId(resolveTargetId(request))
              .content(buildContent(request, response, ex))
              .build());
    } catch (Exception logException) {
      log.warn("Failed to record operation log", logException);
    }
  }

  private Integer resolveOperatorId() {
    try {
      return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    } catch (Exception ignored) {
      return null;
    }
  }

  private String resolveModule(HttpServletRequest request) {
    String path = getRequestPath(request);
    String[] segments =
        Arrays.stream(path.split("/")).filter(segment -> !segment.isBlank()).toArray(String[]::new);
    return truncate(segments.length == 0 ? "root" : segments[0], MODULE_MAX_LENGTH);
  }

  private String resolveAction(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String pathPattern = pattern == null ? getRequestPath(request) : String.valueOf(pattern);
    return truncate(request.getMethod() + " " + pathPattern, ACTION_MAX_LENGTH);
  }

  private String resolveTargetId(HttpServletRequest request) {
    String[] segments =
        Arrays.stream(getRequestPath(request).split("/"))
            .filter(segment -> !segment.isBlank())
            .toArray(String[]::new);
    for (int i = segments.length - 1; i >= 0; i--) {
      if (segments[i].matches("\\d+")) {
        return truncate(segments[i], TARGET_ID_MAX_LENGTH);
      }
    }
    return null;
  }

  private String buildContent(
      HttpServletRequest request, HttpServletResponse response, Exception exception) {
    long durationMs = 0L;
    Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
    if (startTime instanceof Long start) {
      durationMs = System.currentTimeMillis() - start;
    }

    StringBuilder content =
        new StringBuilder()
            .append("method=")
            .append(request.getMethod())
            .append(", ip=")
            .append(clientIpResolver.resolve(request))
            .append(", uri=")
            .append(request.getRequestURI())
            .append(", status=")
            .append(response.getStatus())
            .append(", durationMs=")
            .append(durationMs);

    String queryString = request.getQueryString();
    if (queryString != null && !queryString.isBlank()) {
      content.append(", query=").append(maskSensitiveQuery(queryString));
    }
    if (exception != null) {
      content
          .append(", exception=")
          .append(exception.getClass().getSimpleName())
          .append(": ")
          .append(exception.getMessage());
    }
    return content.toString();
  }

  private String maskSensitiveQuery(String queryString) {
    return Arrays.stream(queryString.split("&"))
        .map(this::maskSensitiveQueryPair)
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }

  private String maskSensitiveQueryPair(String pair) {
    int separatorIndex = pair.indexOf('=');
    String name = separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
    if (isSensitiveName(name)) {
      return separatorIndex < 0 ? name : name + "=******";
    }
    return pair;
  }

  private boolean isSensitiveName(String name) {
    String normalized = name.toLowerCase(Locale.ROOT);
    return normalized.contains("password")
        || normalized.contains("token")
        || normalized.contains("secret")
        || normalized.contains("credential");
  }

  private String getRequestPath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    String uri = request.getRequestURI();
    if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
      return uri.substring(contextPath.length());
    }
    return uri;
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, Math.max(0, maxLength - 3)) + "...";
  }
}
