package edu.jmi.openatom.server.openatomsystem.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.config.MailOutboxProperties;
import edu.jmi.openatom.server.openatomsystem.entity.MailboxOutboxEvent;
import edu.jmi.openatom.server.openatomsystem.mapper.MailboxOutboxEventMapper;
import edu.jmi.openatom.server.openatomsystem.service.MailboxProvisioningClient;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reliably forwards committed user changes to the standalone mail control plane. */
@Slf4j
@Component
public class MailboxOutboxScheduler {
  private final MailboxOutboxEventMapper mapper;
  private final MailboxProvisioningClient client;
  private final MailOutboxProperties properties;
  private final Clock clock;

  public MailboxOutboxScheduler(
      MailboxOutboxEventMapper mapper,
      MailboxProvisioningClient client,
      MailOutboxProperties properties) {
    this(mapper, client, properties, Clock.systemUTC());
  }

  MailboxOutboxScheduler(
      MailboxOutboxEventMapper mapper,
      MailboxProvisioningClient client,
      MailOutboxProperties properties,
      Clock clock) {
    this.mapper = mapper;
    this.client = client;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${app.mail.outbox.fixed-delay:PT5S}")
  public void forwardCommittedEvents() {
    if (!properties.enabled()) {
      return;
    }
    Instant now = clock.instant();
    mapper.recoverStale(Timestamp.from(now.minus(properties.normalizedStaleAfter())));
    List<MailboxOutboxEvent> events =
        mapper.selectList(
            new LambdaQueryWrapper<MailboxOutboxEvent>()
                .and(q -> q.eq(MailboxOutboxEvent::getStatus, "PENDING")
                    .or()
                    .eq(MailboxOutboxEvent::getStatus, "RETRY"))
                .and(q -> q.isNull(MailboxOutboxEvent::getNextRetryAt)
                    .or()
                    .le(MailboxOutboxEvent::getNextRetryAt, Timestamp.from(now)))
                .orderByAsc(MailboxOutboxEvent::getId)
                .last("LIMIT " + properties.normalizedBatchSize()));
    for (MailboxOutboxEvent event : events) {
      if (mapper.claim(event.getId()) == 1) {
        deliverClaimed(event, now);
      }
    }
  }

  @Scheduled(
      initialDelayString = "${app.mail.outbox.reconcile-initial-delay:PT15S}",
      fixedDelayString = "${app.mail.outbox.reconcile-fixed-delay:PT1H}")
  public void enqueueDailyReconciliation() {
    if (!properties.enabled()) {
      return;
    }
    int inserted = mapper.enqueueDailyReconciliation();
    if (inserted > 0) {
      log.info("Queued {} mailbox reconciliation event(s)", inserted);
    }
  }

  void deliverClaimed(MailboxOutboxEvent event, Instant now) {
    try {
      MailboxProvisioningClient.DeliveryResult result = client.deliver(event);
      if (result.delivered()) {
        mapper.markProcessed(event.getId());
      } else if (result.retryable()) {
        retryOrFail(event, now, result.reason());
      } else {
        mapper.markFailed(event.getId(), nextRetryCount(event), result.reason());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      retryOrFail(event, now, "transport_interrupted");
    } catch (IOException | RuntimeException exception) {
      retryOrFail(event, now, "transport_error");
      log.warn("Mailbox provisioning transport failed for eventId={}", event.getEventId());
    }
  }

  private void retryOrFail(MailboxOutboxEvent event, Instant now, String reason) {
    int retryCount = nextRetryCount(event);
    if (retryCount >= properties.normalizedMaxRetries()) {
      mapper.markFailed(event.getId(), retryCount, reason);
      return;
    }
    mapper.markRetry(
        event.getId(), retryCount, Timestamp.from(now.plus(backoff(retryCount))), reason);
  }

  Duration backoff(int retryCount) {
    Duration base = properties.normalizedBaseBackoff();
    Duration maximum = properties.normalizedMaxBackoff();
    int shift = Math.min(Math.max(0, retryCount - 1), 30);
    long multiplier = 1L << shift;
    try {
      Duration calculated = base.multipliedBy(multiplier);
      return calculated.compareTo(maximum) > 0 ? maximum : calculated;
    } catch (ArithmeticException exception) {
      return maximum;
    }
  }

  private int nextRetryCount(MailboxOutboxEvent event) {
    return (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
  }
}
