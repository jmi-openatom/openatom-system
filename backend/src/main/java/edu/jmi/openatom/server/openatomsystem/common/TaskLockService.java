package edu.jmi.openatom.server.openatomsystem.common;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 定时任务分布式锁。
 *
 * <p>backend 多副本部署时，非幂等的 @Scheduled 任务会重复执行。
 * 通过 Redis SET NX EX 保证同一时刻只有一个实例执行，拿不到锁直接跳过等待下一轮。
 */
@Component
@RequiredArgsConstructor
public class TaskLockService {
  private static final String KEY_PREFIX = "openatom:task-lock:";

  private final StringRedisTemplate redisTemplate;

  public boolean tryLock(String name, Duration ttl) {
    Boolean acquired =
        redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, "1", ttl);
    return Boolean.TRUE.equals(acquired);
  }

  public void unlock(String name) {
    redisTemplate.delete(KEY_PREFIX + name);
  }
}