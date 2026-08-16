package edu.jmi.openatom.mail.service;

import edu.jmi.openatom.mail.config.MailProperties;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Sends broadcast mail through the Resend relay in batches (Resend caps recipients per message). */
@Service
public class BroadcastSenderService {
  private static final int BATCH_SIZE = 50;

  private final ResendClient resendClient;
  private final MailProperties properties;

  public BroadcastSenderService(ResendClient resendClient, MailProperties properties) {
    this.resendClient = resendClient;
    this.properties = properties;
  }

  public record SendResult(int recipients, int batches, List<String> ids) {}

  /** Validates, dedupes and sends; empty recipient lists succeed with zero sends. */
  public SendResult send(List<String> recipientInput, String subject, String html, String text) {
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
      return new SendResult(0, 0, List.of());
    }
    if (!resendClient.isConfigured()) {
      throw new IllegalStateException("resend_not_configured");
    }
    List<String> ordered = new ArrayList<>(emails);
    List<String> ids = new ArrayList<>();
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
    return new SendResult(ordered.size(), ids.size(), ids);
  }
}
