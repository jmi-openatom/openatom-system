package edu.jmi.openatom.server.openatomsystem.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Redis 方法级缓存清理注解. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheEvict {
  /** 要清理的缓存域名. */
  String[] cacheNames();

  /** SpEL key 表达式. 留空时按缓存域清理全部条目. */
  String key() default "";

  /** 是否清理缓存域下全部条目. */
  boolean allEntries() default true;

  /** 是否在方法调用前清理. 默认在成功返回后清理, 有事务时延迟到提交后. */
  boolean beforeInvocation() default false;

  /** 当返回值是 Result 且 code 非 0 时是否也清理. 默认为 false. */
  boolean evictOnErrorResult() default false;
}
