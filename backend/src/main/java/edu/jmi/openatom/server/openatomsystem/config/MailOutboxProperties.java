package edu.jmi.openatom.server.openatomsystem.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for reliable user-to-mailbox provisioning events. */
@Component
@ConfigurationProperties(prefix = "app.mail.outbox")
public class MailOutboxProperties {
  private String provisionUrl;
  private String serviceToken;
  private int batchSize = 50;
  private int maxRetries = 12;
  private Duration requestTimeout = Duration.ofSeconds(10);
  private Duration baseBackoff = Duration.ofSeconds(5);
  private Duration maxBackoff = Duration.ofHours(1);
  private Duration staleAfter = Duration.ofMinutes(5);

  @PostConstruct
  void validate() {
    if (!enabled()) {
      return;
    }
    URI uri = URI.create(provisionUrl.trim());
    if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalStateException("app.mail.outbox.provision-url must use HTTP(S)");
    }
    if (serviceToken == null || serviceToken.isBlank()) {
      throw new IllegalStateException(
          "app.mail.outbox.service-token is required when mailbox provisioning is enabled");
    }
  }

  public boolean enabled() {
    return provisionUrl != null && !provisionUrl.isBlank();
  }

  public URI provisionUri() {
    return URI.create(provisionUrl.trim());
  }

  public int normalizedBatchSize() {
    return Math.max(1, Math.min(batchSize, 500));
  }

  public int normalizedMaxRetries() {
    return Math.max(1, maxRetries);
  }

  public Duration normalizedRequestTimeout() {
    return positive(requestTimeout, Duration.ofSeconds(10));
  }

  public Duration normalizedBaseBackoff() {
    return positive(baseBackoff, Duration.ofSeconds(5));
  }

  public Duration normalizedMaxBackoff() {
    return positive(maxBackoff, Duration.ofHours(1));
  }

  public Duration normalizedStaleAfter() {
    return positive(staleAfter, Duration.ofMinutes(5));
  }

  private Duration positive(Duration value, Duration fallback) {
    return value == null || value.isZero() || value.isNegative() ? fallback : value;
  }

  public String getProvisionUrl() { return provisionUrl; }
  public void setProvisionUrl(String provisionUrl) { this.provisionUrl = provisionUrl; }
  public String getServiceToken() { return serviceToken; }
  public void setServiceToken(String serviceToken) { this.serviceToken = serviceToken; }
  public int getBatchSize() { return batchSize; }
  public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
  public int getMaxRetries() { return maxRetries; }
  public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
  public Duration getRequestTimeout() { return requestTimeout; }
  public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
  public Duration getBaseBackoff() { return baseBackoff; }
  public void setBaseBackoff(Duration baseBackoff) { this.baseBackoff = baseBackoff; }
  public Duration getMaxBackoff() { return maxBackoff; }
  public void setMaxBackoff(Duration maxBackoff) { this.maxBackoff = maxBackoff; }
  public Duration getStaleAfter() { return staleAfter; }
  public void setStaleAfter(Duration staleAfter) { this.staleAfter = staleAfter; }
}
