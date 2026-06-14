package edu.jmi.openatom.server.openatomsystem.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Redis 注解缓存切面. */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisCacheAspect {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final RedisCacheProperties properties;
  private final ExpressionParser expressionParser = new SpelExpressionParser();
  private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  @Around("@annotation(redisCached)")
  public Object cache(ProceedingJoinPoint joinPoint, RedisCached redisCached) throws Throwable {
    if (!properties.isEnabled()) {
      return joinPoint.proceed();
    }

    String key = fullKey(redisCached.cacheName(), resolveKey(joinPoint, redisCached.key()));
    Method method = specificMethod(joinPoint);
    Object cachedValue = readCache(key, method);
    if (cachedValue != null) {
      return cachedValue;
    }

    Object result = joinPoint.proceed();
    if (shouldCache(result, redisCached)) {
      writeCache(key, result, ttl(redisCached));
    }
    return result;
  }

  @Around("@annotation(redisCacheEvict)")
  public Object evict(ProceedingJoinPoint joinPoint, RedisCacheEvict redisCacheEvict)
      throws Throwable {
    if (!properties.isEnabled()) {
      return joinPoint.proceed();
    }

    if (redisCacheEvict.beforeInvocation()) {
      runEviction(joinPoint, redisCacheEvict);
      return joinPoint.proceed();
    }

    Object result = joinPoint.proceed();
    if (shouldEvict(result, redisCacheEvict)) {
      runAfterCommit(() -> runEviction(joinPoint, redisCacheEvict));
    }
    return result;
  }

  private Object readCache(String key, Method method) {
    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null) {
        return null;
      }
      JavaType returnType = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
      return objectMapper.readValue(json, returnType);
    } catch (Exception e) {
      log.warn("Redis cache read failed, key={}", key, e);
      return null;
    }
  }

  private void writeCache(String key, Object value, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (Exception e) {
      log.warn("Redis cache write failed, key={}", key, e);
    }
  }

  private void runEviction(ProceedingJoinPoint joinPoint, RedisCacheEvict redisCacheEvict) {
    for (String cacheName : redisCacheEvict.cacheNames()) {
      if (cacheName == null || cacheName.isBlank()) {
        continue;
      }
      if (redisCacheEvict.allEntries() || redisCacheEvict.key().isBlank()) {
        evictByPattern(cacheName.trim());
      } else {
        String key = fullKey(cacheName.trim(), resolveKey(joinPoint, redisCacheEvict.key()));
        deleteKey(key);
      }
    }
  }

  private void deleteKey(String key) {
    try {
      redisTemplate.delete(key);
    } catch (RuntimeException e) {
      log.warn("Redis cache delete failed, key={}", key, e);
    }
  }

  private void evictByPattern(String cacheName) {
    String pattern = fullKey(cacheName, "*");
    try {
      redisTemplate.execute(
          (RedisCallback<Void>)
              connection -> {
                scanAndDelete(connection, pattern);
                return null;
              });
    } catch (RuntimeException e) {
      log.warn("Redis cache evict failed, pattern={}", pattern, e);
    }
  }

  private void scanAndDelete(RedisConnection connection, String pattern) {
    ScanOptions options =
        ScanOptions.scanOptions()
            .match(pattern)
            .count(properties.normalizedScanBatchSize())
            .build();
    List<byte[]> batch = new ArrayList<>(properties.normalizedScanBatchSize());
    try (Cursor<byte[]> cursor = connection.scan(options)) {
      while (cursor.hasNext()) {
        batch.add(cursor.next());
        if (batch.size() >= properties.normalizedScanBatchSize()) {
          unlinkBatch(connection, batch);
        }
      }
      unlinkBatch(connection, batch);
    }
  }

  private void unlinkBatch(RedisConnection connection, List<byte[]> keys) {
    if (keys.isEmpty()) {
      return;
    }
    byte[][] keyArray = keys.toArray(byte[][]::new);
    try {
      connection.unlink(keyArray);
    } catch (RuntimeException e) {
      connection.del(keyArray);
    }
    keys.clear();
  }

  private boolean shouldCache(Object value, RedisCached redisCached) {
    if (value == null) {
      return redisCached.cacheNull();
    }
    return redisCached.cacheErrors() || isSuccessOrPlainValue(value);
  }

  private boolean shouldEvict(Object value, RedisCacheEvict redisCacheEvict) {
    return redisCacheEvict.evictOnErrorResult() || isSuccessOrPlainValue(value);
  }

  private boolean isSuccessOrPlainValue(Object value) {
    if (value instanceof Result<?> result) {
      return Objects.equals(result.getCode(), Result.SUCCESS_CODE);
    }
    return true;
  }

  private Duration ttl(RedisCached redisCached) {
    return redisCached.ttlSeconds() > 0
        ? Duration.ofSeconds(redisCached.ttlSeconds())
        : properties.normalizedDefaultTtl();
  }

  private String fullKey(String cacheName, String key) {
    return properties.normalizedKeyPrefix() + ":" + cacheName + ":" + key;
  }

  private String resolveKey(ProceedingJoinPoint joinPoint, String expression) {
    if (expression != null && !expression.isBlank()) {
      Object value = expressionParser.parseExpression(expression).getValue(context(joinPoint));
      return value == null ? "null" : String.valueOf(value);
    }
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    return signature.getMethod().getName() + ":" + sha256(Arrays.deepToString(joinPoint.getArgs()));
  }

  private StandardEvaluationContext context(ProceedingJoinPoint joinPoint) {
    Method method = specificMethod(joinPoint);
    Object[] args = joinPoint.getArgs();
    StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());
    context.setVariable("methodName", method.getName());
    String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
    for (int i = 0; i < args.length; i++) {
      context.setVariable("p" + i, args[i]);
      context.setVariable("a" + i, args[i]);
      context.setVariable("arg" + i, args[i]);
      if (parameterNames != null && i < parameterNames.length) {
        context.setVariable(parameterNames[i], args[i]);
      }
    }
    return context;
  }

  private Method specificMethod(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    return AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
  }

  private void runAfterCommit(Runnable task) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      task.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            task.run();
          }
        });
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
