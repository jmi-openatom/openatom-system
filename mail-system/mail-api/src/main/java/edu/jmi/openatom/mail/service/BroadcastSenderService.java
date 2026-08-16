package edu.jmi.openatom.mail.service;

import edu.jmi.openatom.mail.config.MailProperties;
import edu.jmi.openatom.mail.repository.BroadcastLogRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Sends broadcast mail through the Resend relay in batches (Resend caps recipients per message). */
@Service
public class BroadcastSenderService {
  private static final Logger log = LoggerFactory.getLogger(BroadcastSenderService.class);
  private static final int BATCH_SIZE = 50;

  private final ResendClient resendClient;
  private final MailProperties properties;
  private final BroadcastLogRepository logRepository;

  public BroadcastSenderService(
      ResendClient resendClient,
      MailProperties properties,
      BroadcastLogRepository logRepository) {
    this.resendClient = resendClient;
    this.properties = properties;
    this.logRepository = logRepository;
  }

  public record SendResult(int recipients, int batches, List<String> ids) {}

  /** Validates, dedupes and sends; empty recipient lists succeed with zero sends. */
  public SendResult send(
      List<String> recipientInput,
      String subject,
      String html,
      String text,
      String source,
      String kind) {
    Set<String> emails = new LinkedHashSet<>();
    if (recipientInput != null) {
      for (String value : recipientInput) {
        String email = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
          continue;
        }
        emails.add(email);
      }
    }
    if (emails.isEmpty()) {
      logRepository.record(source, kind, subject, properties.getBroadcastFrom(), 0, 0, null,
          "sent", "no recipients");
      return new SendResult(0, 0, List.of());
    }
    if (!resendClient.isConfigured()) {
      throw new IllegalStateException("resend_not_configured");
    }
    List<String> ordered = new ArrayList<>(emails);
    List<String> ids = new ArrayList<>();
    try {
      for (int from = 0; from < ordered.size(); from += BATCH_SIZE) {
        List<String> batch = ordered.subList(from, Math.min(from + BATCH_SIZE, ordered.size()));
        ResendClient.Result result =
            resendClient.send(
                properties.getBroadcastFrom(),
                batch,
                subject,
                text,
                html,
                List.of());
        if (result.id() == null || result.id().isBlank()) {
          throw new StalwartClientException(
              "resend_rejected_" + result.status() + ":" + result.detail());
        }
        ids.add(result.id());
      }
      logRepository.record(source, kind, subject, properties.getBroadcastFrom(),
          ordered.size(), ids.size(), String.join(",", ids), "sent", null);
      return new SendResult(ordered.size(), ids.size(), ids);
    } catch (RuntimeException exception) {
      log.warn("broadcast send failed: subject={} recipients={}: {}",
          subject, ordered.size(), exception.getMessage());
      logRepository.record(source, kind, subject, properties.getBroadcastFrom(),
          ordered.size(), ids.size(), String.join(",", ids), "failed",
          truncate(exception.getMessage(), 500));
      throw exception;
    }
  }

  private String truncate(String value, int max) {
    if (value == null) return null;
    return value.length() <= max ? value : value.substring(0, max);
  }
}
