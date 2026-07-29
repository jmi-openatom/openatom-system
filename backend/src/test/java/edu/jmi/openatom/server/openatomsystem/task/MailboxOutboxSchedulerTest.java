package edu.jmi.openatom.server.openatomsystem.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.config.MailOutboxProperties;
import edu.jmi.openatom.server.openatomsystem.entity.MailboxOutboxEvent;
import edu.jmi.openatom.server.openatomsystem.mapper.MailboxOutboxEventMapper;
import edu.jmi.openatom.server.openatomsystem.service.MailboxProvisioningClient;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MailboxOutboxSchedulerTest {
  private static final Instant NOW = Instant.parse("2026-07-30T00:00:00Z");

  @Mock private MailboxOutboxEventMapper mapper;
  @Mock private MailboxProvisioningClient client;
  private MailOutboxProperties properties;
  private MailboxOutboxScheduler scheduler;

  @BeforeEach
  void setUp() {
    properties = new MailOutboxProperties();
    properties.setProvisionUrl("http://mail-api/internal/provision");
    properties.setServiceToken("secret");
    scheduler =
        new MailboxOutboxScheduler(
            mapper, client, properties, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void marksSuccessfulDeliveryProcessed() throws Exception {
    MailboxOutboxEvent event = event(0);
    when(client.deliver(event))
        .thenReturn(new MailboxProvisioningClient.DeliveryResult(true, false, null, 204));

    scheduler.deliverClaimed(event, NOW);

    verify(mapper).markProcessed(1L);
    verify(mapper, never()).markRetry(any(), any(Integer.class), any(), any());
  }

  @Test
  void retriesTemporaryFailureWithExponentialBackoff() throws Exception {
    MailboxOutboxEvent event = event(2);
    when(client.deliver(event))
        .thenReturn(
            new MailboxProvisioningClient.DeliveryResult(false, true, "http_503", 0));

    scheduler.deliverClaimed(event, NOW);

    verify(mapper)
        .markRetry(1L, 3, Timestamp.from(NOW.plusSeconds(20)), "http_503");
    assertThat(scheduler.backoff(10)).isEqualTo(Duration.ofMinutes(42).plusSeconds(40));
    assertThat(scheduler.backoff(20)).isEqualTo(Duration.ofHours(1));
  }

  @Test
  void transportErrorsNeverPersistExceptionText() throws Exception {
    MailboxOutboxEvent event = event(0);
    when(client.deliver(event)).thenThrow(new IOException("token=must-not-be-logged"));

    scheduler.deliverClaimed(event, NOW);

    verify(mapper)
        .markRetry(eq(1L), eq(1), eq(Timestamp.from(NOW.plusSeconds(5))), eq("transport_error"));
  }

  @Test
  void stopsAfterConfiguredRetryLimit() throws Exception {
    properties.setMaxRetries(3);
    MailboxOutboxEvent event = event(2);
    when(client.deliver(event))
        .thenReturn(
            new MailboxProvisioningClient.DeliveryResult(false, true, "http_503", 0));

    scheduler.deliverClaimed(event, NOW);

    verify(mapper).markFailed(1L, 3, "http_503");
  }

  @Test
  void queuesDailyReconciliationWhenMailIntegrationIsEnabled() {
    when(mapper.enqueueDailyReconciliation()).thenReturn(2);

    scheduler.enqueueDailyReconciliation();

    verify(mapper).enqueueDailyReconciliation();
  }

  private MailboxOutboxEvent event(int retryCount) {
    return MailboxOutboxEvent.builder()
        .id(1L)
        .eventId("evt-1")
        .eventType("USER_CREATED")
        .payloadJson("{\"sub\":\"42\"}")
        .retryCount(retryCount)
        .build();
  }
}
