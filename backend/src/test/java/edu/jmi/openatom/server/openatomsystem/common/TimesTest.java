package edu.jmi.openatom.server.openatomsystem.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimesTest {
  @Test
  void parsesOffsetlessDateTimesInBusinessZone() {
    assertThat(Times.parseTimestamp("2026-05-18T00:00").toInstant())
        .isEqualTo(Instant.parse("2026-05-17T16:00:00Z"));
  }

  @Test
  void preservesExplicitOffsets() {
    assertThat(Times.parseTimestamp("2026-05-18T00:00:00+08:00").toInstant())
        .isEqualTo(Instant.parse("2026-05-17T16:00:00Z"));
  }
}
