package edu.jmi.openatom.mail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends outbound mail through the Resend Email API.
 *
 * <p>The relay domain (mailer.jmi-openatom.cn) is verified in Resend, so the
 * From address must use it; callers pass the resolved relay sender.
 */
@Component
public class ResendClient {

  private static final String ENDPOINT = "https://api.resend.com/emails";
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String apiKey;

  public ResendClient(@Value("${mail.resend.api-key:}") String apiKey, ObjectMapper objectMapper) {
    this.apiKey = apiKey;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  public boolean isConfigured() {
    return apiKey != null && apiKey.startsWith("re_");
  }

  public record Attachment(String filename, String contentType, byte[] content) {}

  public record Result(String id, int status, String detail) {}

  /** Sends an email; returns the Resend message id on success. */
  public Result send(
      String from, List<String> to, String subject, String text, String html, List<Attachment> attachments) {
    if (!isConfigured()) {
      throw new IllegalStateException("resend_not_configured");
    }
    ObjectNode body = objectMapper.createObjectNode();
    body.put("from", from);
    ArrayNode toArray = body.putArray("to");
    to.forEach(toArray::add);
    body.put("subject", subject == null ? "" : subject);
    if (text != null && !text.isBlank()) {
      body.put("text", text);
    }
    if (html != null && !html.isBlank()) {
      body.put("html", html);
    }
    if (attachments != null && !attachments.isEmpty()) {
      ArrayNode attachmentNodes = body.putArray("attachments");
      for (Attachment attachment : attachments) {
        ObjectNode node = attachmentNodes.addObject();
        node.put("filename", attachment.filename());
        node.put("content", Base64.getEncoder().encodeToString(attachment.content()));
        if (attachment.contentType() != null && !attachment.contentType().isBlank()) {
          node.put("contentType", attachment.contentType());
        }
      }
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(ENDPOINT))
              .timeout(Duration.ofSeconds(30))
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
              .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JsonNode parsed = objectMapper.readTree(response.body());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return new Result(parsed.path("id").asText(), response.statusCode(), null);
      }
      String detail = parsed.path("message").asText("");
      if (detail.isBlank()) {
        detail = response.body();
      }
      return new Result(null, response.statusCode(), detail);
    } catch (IOException exception) {
      throw new StalwartClientException("resend_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new StalwartClientException("resend_transport_interrupted", exception);
    }
  }
}
