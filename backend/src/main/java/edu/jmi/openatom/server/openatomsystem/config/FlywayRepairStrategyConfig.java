package edu.jmi.openatom.server.openatomsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FlywayRepairStrategyConfig {
  private static final String V3_CHECKSUM_MISMATCH = "Migration checksum mismatch for migration version 3";

  @Value("${app.flyway.auto-repair-v3-checksum-mismatch:true}")
  private boolean autoRepairV3ChecksumMismatch;

  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
      try {
        flyway.migrate();
      } catch (FlywayValidateException ex) {
        if (!autoRepairV3ChecksumMismatch || !isKnownV3ChecksumMismatch(ex)) {
          throw ex;
        }
        log.warn("Detected known Flyway V3 checksum mismatch. Running flyway repair once before migration.");
        flyway.repair();
        flyway.migrate();
      }
    };
  }

  private boolean isKnownV3ChecksumMismatch(FlywayValidateException ex) {
    String message = ex.getMessage();
    return message != null && message.contains(V3_CHECKSUM_MISMATCH);
  }
}
