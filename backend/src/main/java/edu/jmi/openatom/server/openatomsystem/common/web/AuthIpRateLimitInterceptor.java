package edu.jmi.openatom.server.openatomsystem.common.web;

import edu.jmi.openatom.server.openatomsystem.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证/敏感接口 IP 限流器。
 *
 * <p>以客户端 IP 为维度在 Redis 中计数，对登录、图形验证码、发送验证码等
 * 易被机器人刷的公开接口做限流，默认每分钟最多 30 次。
 */
@Component
@RequiredArgsConstructor
public class AuthIpRateLimitInterceptor implements HandlerInterceptor {
  private static final String KEY_PREFIX = "openatom:rate-limit:auth-ip:";
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final StringRedisTemplate stringRedisTemplate;
  private final ClientIpResolver clientIpResolver;

  @Value("${app.rate-limit.auth-ip.max-per-minute:30}")
  private long maxPerMinute;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String ip = clientIpResolver.resolve(request);
    String key = KEY_PREFIX + normalizeIp(ip);
    Long count = stringRedisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      stringRedisTemplate.expire(key, WINDOW);
    }
    if (count != null && count > maxPerMinute) {
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