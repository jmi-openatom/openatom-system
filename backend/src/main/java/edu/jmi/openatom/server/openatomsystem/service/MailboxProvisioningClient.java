package edu.jmi.openatom.server.openatomsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.jmi.openatom.server.openatomsystem.config.MailOutboxProperties;
import edu.jmi.openatom.server.openatomsystem.entity.MailboxOutboxEvent;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Minimal internal HTTP client. It deliberately never reads or logs response bodies. */
@Component
public class MailboxProvisioningClient {
  private final MailOutboxProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public MailboxProvisioningClient(MailOutboxProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newBuilder().build());
  }

  MailboxProvisioningClient(
      MailOutboxProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  public DeliveryResult deliver(MailboxOutboxEvent event) throws IOException, InterruptedException {
    ObjectNode payload = (ObjectNode) objectMapper.readTree(event.getPayloadJson());
    payload.put("eventId", event.getEventId());
    payload.put("eventType", event.getEventType());

    HttpRequest request =
        HttpRequest.newBuilder(properties.provisionUri())
            .timeout(properties.normalizedRequestTimeout())
            .header("Authorization", "Bearer " + properties.getServiceToken())
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", event.getEventId())
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
    HttpResponse<Void> response =
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    int status = response.statusCode();
    if ((status >= 200 && status < 300) || status == 409) {
      return DeliveryResult.success(status);
    }
    if (status == 408 || status == 425 || status == 429 || status >= 500) {
      return DeliveryResult.retry("http_" + status);
    }
    return DeliveryResult.permanentFailure("http_" + status);
  }

  public record DeliveryResult(boolean delivered, boolean retryable, String reason, int statusCode) {
    static DeliveryResult success(int status) {
      return new DeliveryResult(true, false, null, status);
    }

    static DeliveryResult retry(String reason) {
      return new DeliveryResult(false, true, reason, 0);
    }

    static DeliveryResult permanentFailure(String reason) {
      return new DeliveryResult(false, false, reason, 0);
    }
  }

  /** Queries a user's mailbox status; returns null when the mail system is unreachable. */
  public MailboxStatus queryStatus(int userId) {
    if (!properties.enabled()) {
      return null;
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder(properties.queryStatusUri(String.valueOf(userId)))
              .timeout(properties.normalizedRequestTimeout())
              .header("Authorization", "Bearer " + properties.getServiceToken())
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return null;
      }
      JsonNode body = objectMapper.readTree(response.body());
      return new MailboxStatus(
          body.path("address").asText(""),
          body.path("status").asText(""),
          body.path("provisionStatus").asText(""));
    } catch (IOException | InterruptedException exception) {
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return null;
    }
  }

  public record MailboxStatus(String address, String status, String provisionStatus) {}
}
