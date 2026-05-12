package edu.jmi.openatom.server.openatomsystem.common.web;

import edu.jmi.openatom.server.openatomsystem.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 入会申请提交限流器。
 *
 * <p>以客户端 IP 为维度在 Redis 中计数，默认限制为 1 秒内最多 5 次 POST /applications。
 */
@Component
@RequiredArgsConstructor
public class ApplicationSubmitRateLimitInterceptor implements HandlerInterceptor {
  private static final String KEY_PREFIX = "openatom:rate-limit:applications:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ClientIpResolver clientIpResolver;

  @Value("${app.rate-limit.application-submit.max-per-second:5}")
  private long maxPerSecond;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!HttpMethod.POST.matches(request.getMethod())) {
      return true;
    }

    String ip = clientIpResolver.resolve(request);
    String key = KEY_PREFIX + normalizeIp(ip);
    Long count = stringRedisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      stringRedisTemplate.expire(key, Duration.ofSeconds(1));
    }
    if (count != null && count > maxPerSecond) {
      throw new RateLimitExceededException("访问过于频繁，请稍后再试");
    }
    return true;
  }

  private String normalizeIp(String ip) {
    if (ip == null || ip.isBlank()) {
      return "unknown";
    }
    return ip.replaceAll("[^0-9A-Za-z:.\\-]", "_");
  }
}
