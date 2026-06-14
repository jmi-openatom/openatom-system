package edu.jmi.openatom.server.openatomsystem.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Redis 注解缓存配置. */
@Component
@ConfigurationProperties(prefix = "app.cache.redis")
public class RedisCacheProperties {
  private boolean enabled = true;
  private String keyPrefix = "openatom:cache";
  private Duration defaultTtl = Duration.ofMinutes(10);
  private int scanBatchSize = 500;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public Duration getDefaultTtl() {
    return defaultTtl;
  }

  public void setDefaultTtl(Duration defaultTtl) {
    this.defaultTtl = defaultTtl;
  }

  public int getScanBatchSize() {
    return scanBatchSize;
  }

  public void setScanBatchSize(int scanBatchSize) {
    this.scanBatchSize = scanBatchSize;
  }

  public String normalizedKeyPrefix() {
    String prefix = keyPrefix == null || keyPrefix.isBlank() ? "openatom:cache" : keyPrefix.trim();
    while (prefix.endsWith(":")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    return prefix;
  }

  public int normalizedScanBatchSize() {
    return scanBatchSize <= 0 ? 500 : scanBatchSize;
  }

  public Duration normalizedDefaultTtl() {
    return defaultTtl == null || defaultTtl.isZero() || defaultTtl.isNegative()
        ? Duration.ofMinutes(10)
        : defaultTtl;
  }
}
