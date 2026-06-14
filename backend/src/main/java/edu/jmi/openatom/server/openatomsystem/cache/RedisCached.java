package edu.jmi.openatom.server.openatomsystem.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 方法级缓存注解.
 *
 * <p>适合用于读多写少、短时间内不会频繁变化的查询方法. 写方法应配套使用
 * {@link RedisCacheEvict} 清理对应缓存域.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCached {
  /** 缓存域名, 会参与 Redis key 前缀. */
  String cacheName();

  /** SpEL key 表达式, 支持 #p0/#a0/#arg0 和真实参数名. 留空时使用方法参数摘要. */
  String key() default "";

  /** TTL 秒数. 小于等于 0 时使用 app.cache.redis.default-ttl. */
  long ttlSeconds() default 0L;

  /** 是否缓存 null 返回值. 默认为 false. */
  boolean cacheNull() default false;

  /** 当返回值是 Result 且 code 非 0 时是否也缓存. 默认为 false. */
  boolean cacheErrors() default false;
}
