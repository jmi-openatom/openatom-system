package edu.jmi.openatom.server.openatomsystem.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.ErrorCode;
import org.flywaydb.core.api.ErrorDetails;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.test.util.ReflectionTestUtils;

class FlywayRepairStrategyConfigTest {

  @Test
  void repairsFailedV52MigrationOnceBeforeRetrying() {
    FlywayRepairStrategyConfig config = enabledConfig();
    Flyway flyway = mock(Flyway.class);
    FlywayValidateException failure =
        validationFailure("Detected failed migration to version 52 (add mailbox outbox)");
    when(flyway.migrate()).thenThrow(failure).thenReturn(null);

    FlywayMigrationStrategy strategy = config.flywayMigrationStrategy();
    strategy.migrate(flyway);

    InOrder calls = inOrder(flyway);
    calls.verify(flyway).migrate();
    calls.verify(flyway).repair();
    calls.verify(flyway).migrate();
  }

  @Test
  void doesNotRepairUnknownValidationFailure() {
    FlywayRepairStrategyConfig config = enabledConfig();
    Flyway flyway = mock(Flyway.class);
    FlywayValidateException failure = validationFailure("Detected failed migration to version 51");
    when(flyway.migrate()).thenThrow(failure);

    FlywayMigrationStrategy strategy = config.flywayMigrationStrategy();

    assertThatThrownBy(() -> strategy.migrate(flyway)).isSameAs(failure);
    verify(flyway, never()).repair();
  }

  private FlywayRepairStrategyConfig enabledConfig() {
    FlywayRepairStrategyConfig config = new FlywayRepairStrategyConfig();
    ReflectionTestUtils.setField(config, "autoRepairChecksumMismatch", true);
    return config;
  }

  private FlywayValidateException validationFailure(String message) {
    return new FlywayValidateException(
        new ErrorDetails(ErrorCode.FAILED_VERSIONED_MIGRATION, message), message);
  }
}
