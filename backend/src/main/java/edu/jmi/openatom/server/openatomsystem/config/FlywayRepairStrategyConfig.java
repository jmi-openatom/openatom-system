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
  private static final String V15_CHECKSUM_MISMATCH = "Migration checksum mismatch for migration version 15";

  @Value("${app.flyway.auto-repair-checksum-mismatch:true}")
  private boolean autoRepairChecksumMismatch;

  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
      try {
        flyway.migrate();
      } catch (FlywayValidateException ex) {
        if (!autoRepairChecksumMismatch || !isKnownChecksumMismatch(ex)) {
          throw ex;
        }
        log.warn("Detected known Flyway checksum mismatch. Running flyway repair once before migration.");
        flyway.repair();
        flyway.migrate();
      }
    };
  }

  private boolean isKnownChecksumMismatch(FlywayValidateException ex) {
    String message = ex.getMessage();
    return message != null
        && (message.contains(V3_CHECKSUM_MISMATCH) || message.contains(V15_CHECKSUM_MISMATCH));
  }
}
