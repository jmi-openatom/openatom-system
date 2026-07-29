package edu.jmi.openatom.mail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.mail.config.MailProperties;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class UserJmapClient {
  private static final int MAX_DOWNLOAD_BYTES = 25 * 1024 * 1024;
  private final MailProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public UserJmapClient(MailProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public Response forward(JsonNode payload, String accessToken) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(properties.getStalwart().getJmapUrl()))
              .timeout(Duration.ofSeconds(20))
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), response.body());
    } catch (IOException exception) {
      throw new StalwartClientException("jmap_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new StalwartClientException("jmap_transport_interrupted", exception);
    }
  }

  public Response session(String accessToken) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(properties.getStalwart().getSessionUrl()))
              .timeout(Duration.ofSeconds(10))
              .header("Authorization", "Bearer " + accessToken)
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), response.body());
    } catch (IOException exception) {
      throw new StalwartClientException("jmap_session_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new StalwartClientException("jmap_session_transport_interrupted", exception);
    }
  }

  public Response upload(
      String accountId, String contentType, byte[] content, String accessToken) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(jmapEndpoint("upload", accountId))
              .timeout(Duration.ofSeconds(60))
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", contentType)
              .POST(HttpRequest.BodyPublishers.ofByteArray(content))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), response.body());
    } catch (IOException exception) {
      throw new StalwartClientException("jmap_upload_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new StalwartClientException("jmap_upload_transport_interrupted", exception);
    }
  }

  public BinaryResponse download(String accountId, String blobId, String accessToken) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(jmapEndpoint("download", accountId, blobId, "attachment"))
              .timeout(Duration.ofSeconds(60))
              .header("Authorization", "Bearer " + accessToken)
              .GET()
              .build();
      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream body = response.body()) {
        return new BinaryResponse(response.statusCode(), readLimited(body));
      }
    } catch (AttachmentTooLargeException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new StalwartClientException("jmap_download_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new StalwartClientException("jmap_download_transport_interrupted", exception);
    }
  }

  private URI jmapEndpoint(String... segments) {
    URI base = URI.create(properties.getStalwart().getJmapUrl());
    StringBuilder path = new StringBuilder("/jmap");
    for (String segment : segments) {
      path.append('/').append(encodePathSegment(segment));
    }
    try {
      return new URI(base.getScheme(), null, base.getHost(), base.getPort(), path.toString(), null, null);
    } catch (Exception exception) {
      throw new IllegalStateException("invalid_jmap_endpoint", exception);
    }
  }

  private String encodePathSegment(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private byte[] readLimited(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[16 * 1024];
    int total = 0;
    int read;
    while ((read = input.read(buffer)) != -1) {
      total += read;
      if (total > MAX_DOWNLOAD_BYTES) {
        throw new AttachmentTooLargeException();
      }
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  public record Response(int status, String body) {}
  public record BinaryResponse(int status, byte[] body) {}
}
