package edu.jmi.openatom.mail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.mail.config.MailProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Fetches main-site users (those with an external email address) so the mail
 * admin console can build recipient lists for broadcast emails. Calls the
 * main-site internal endpoint with the shared service token.
 */
@Component
public class MainSiteUsersClient {
  private final MailProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public MainSiteUsersClient(MailProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  MainSiteUsersClient(MailProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  public RecipientPage recipients(int page, int pageSize, String keyword)
      throws IOException, InterruptedException {
    URI uri = URI.create(resolvedUsersUrl() + "?page=" + page
        + "&pageSize=" + Math.min(500, Math.max(1, pageSize))
        + (keyword != null && !keyword.isBlank()
            ? "&keyword=" + URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8)
            : ""));
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + properties.getInternalServiceToken())
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(String.valueOf(response.statusCode()));
    }
    JsonNode body = objectMapper.readTree(response.body());
    JsonNode data = body.path("data");
    List<Recipient> rows = new ArrayList<>();
    for (JsonNode item : data.path("list")) {
      String email = item.path("email").asText("");
      if (email.isBlank()) {
        continue;
      }
      rows.add(
          new Recipient(
              item.path("userId").asLong(0),
              item.path("name").asText(""),
              email));
    }
    return new RecipientPage(
        rows,
        data.path("total").asLong(0),
        data.path("page").asLong(page),
        data.path("pageSize").asLong(pageSize));
  }

  /**
   * Resolves the main-site users URL leniently: blank config falls back to the
   * public API domain, a missing scheme gets https prefixed, and any path is
   * replaced with the fixed internal route so operators only need to provide
   * the host.
   */
  String resolvedUsersUrl() {
    String configured = properties.getMainSiteUsersUrl();
    String base = configured == null || configured.isBlank()
        ? DEFAULT_USERS_URL
        : configured.trim();
    if (!base.startsWith("http://") && !base.startsWith("https://")) {
      base = "https://" + base;
    }
    try {
      URI parsed = URI.create(base);
      String authority = parsed.getRawAuthority();
      if (authority == null || authority.isBlank()) {
        return DEFAULT_USERS_URL;
      }
      return parsed.getScheme() + "://" + authority + USERS_PATH;
    } catch (IllegalArgumentException exception) {
      return DEFAULT_USERS_URL;
    }
  }

  private static final String DEFAULT_USERS_URL =
      "https://api.jmi-openatom.cn/api/v1/internal/mail/users";
  private static final String USERS_PATH = "/api/v1/internal/mail/users";

  public record Recipient(long userId, String name, String email) {}

  public record RecipientPage(List<Recipient> rows, long total, long page, long pageSize) {}
}