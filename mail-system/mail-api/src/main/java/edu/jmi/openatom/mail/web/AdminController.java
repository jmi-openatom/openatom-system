package edu.jmi.openatom.mail.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.mail.domain.MailboxAccount;
import edu.jmi.openatom.mail.oauth.MailSession;
import edu.jmi.openatom.mail.repository.MailboxRepository;
import edu.jmi.openatom.mail.service.MailboxProvisioningService;
import edu.jmi.openatom.mail.service.MainSiteUsersClient;
import edu.jmi.openatom.mail.service.ResendClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Administrator surface for the mail system (user list, suspend, Resend stats). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final MailboxRepository repository;
  private final MailboxProvisioningService service;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final String resendApiKey;
  private final MainSiteUsersClient mainSiteUsersClient;
  private final ResendClient resendClient;
  private final edu.jmi.openatom.mail.config.MailProperties mailProperties;

  public AdminController(
      MailboxRepository repository,
      MailboxProvisioningService service,
      ObjectMapper objectMapper,
      @Value("${mail.resend.api-key:}") String resendApiKey,
      MainSiteUsersClient mainSiteUsersClient,
      ResendClient resendClient,
      edu.jmi.openatom.mail.config.MailProperties mailProperties) {
    this.repository = repository;
    this.service = service;
    this.objectMapper = objectMapper;
    this.resendApiKey = resendApiKey;
    this.mainSiteUsersClient = mainSiteUsersClient;
    this.resendClient = resendClient;
    this.mailProperties = mailProperties;
  }

  @GetMapping("/mailboxes")
  public MailboxPage mailboxes(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "id") String sort,
      @RequestParam(defaultValue = "desc") String order,
      HttpServletRequest request) {
    requireAdmin(request);
    List<MailboxAccount> all = repository.findAll();
    // search
    if (keyword != null && !keyword.isBlank()) {
      String term = keyword.trim().toLowerCase();
      all =
          all.stream()
              .filter(
                  a ->
                      (a.displayName() != null && a.displayName().toLowerCase().contains(term))
                          || (a.primaryAddress() != null
                              && a.primaryAddress().toLowerCase().contains(term))
                          || (a.oauthSub() != null && a.oauthSub().toLowerCase().contains(term)))
              .toList();
    }
    // sort
    java.util.Comparator<MailboxAccount> comparator = comparatorFor(sort);
    if ("asc".equalsIgnoreCase(order)) {
      all = all.stream().sorted(comparator).toList();
    } else {
      all = all.stream().sorted(comparator.reversed()).toList();
    }
    // paginate
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(1, pageSize));
    int total = all.size();
    int from = Math.min((safePage - 1) * safeSize, total);
    int to = Math.min(from + safeSize, total);
    List<MailboxView> rows =
        all.subList(from, to).stream().map(MailboxView::from).toList();
    return new MailboxPage(rows, total, safePage, safeSize);
  }

  private java.util.Comparator<MailboxAccount> comparatorFor(String sort) {
    return switch (sort == null ? "id" : sort) {
      case "displayName" ->
          java.util.Comparator.comparing(
              MailboxAccount::displayName,
              java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
      case "address" ->
          java.util.Comparator.comparing(
              MailboxAccount::primaryAddress,
              java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
      case "status" ->
          java.util.Comparator.comparing(
              MailboxAccount::status,
              java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
      default -> java.util.Comparator.comparingLong(MailboxAccount::id);
    };
  }

  @PostMapping("/mailboxes/{id}/suspend")
  public Map<String, String> suspend(
      @PathVariable long id,
      @RequestBody SuspendRequest body,
      HttpServletRequest request) {
    requireAdmin(request);
    MailboxAccount target =
        repository.findAll().stream()
            .filter(item -> item.id() == id)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mailbox_not_found"));
    service.setSuspended(target.oauthSub(), body.suspended());
    return Map.of("status", body.suspended() ? "SUSPENDED" : "ACTIVE");
  }

  @GetMapping("/stats")
  public Map<String, Object> stats(HttpServletRequest request) {
    requireAdmin(request);
    List<MailboxAccount> all = repository.findAll();
    long active = all.stream().filter(a -> "ACTIVE".equals(a.status())).count();
    return Map.of("total", (long) all.size(), "active", active, "resend", resendStats());
  }

  private Map<String, Object> resendStats() {
    if (resendApiKey == null || !resendApiKey.startsWith("re_")) {
      return Map.of("configured", false);
    }
    try {
      HttpRequest req =
          HttpRequest.newBuilder(URI.create("https://api.resend.com/domains"))
              .timeout(Duration.ofSeconds(10))
              .header("Authorization", "Bearer " + resendApiKey)
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      JsonNode body = objectMapper.readTree(response.body());
      boolean verified = false;
      String domain = "";
      String region = "";
      for (JsonNode item : body.path("data")) {
        if ("verified".equals(item.path("status").asText())) {
          verified = true;
          domain = item.path("name").asText();
          region = item.path("region").asText();
          break;
        }
      }
      return Map.of(
          "configured", true,
          "verified", verified,
          "domain", domain,
          "region", region);
    } catch (IOException | InterruptedException exception) {
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Map.of("configured", true, "error", "resend_api_unreachable");
    }
  }

  /** Main-site users that have an external (non @jmi-openatom.cn) email, for broadcast recipients. */
  @GetMapping("/external-recipients")
  public MainSiteUsersClient.RecipientPage externalRecipients(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "") String keyword,
      HttpServletRequest request) {
    requireAdmin(request);
    try {
      return mainSiteUsersClient.recipients(page, pageSize, keyword);
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "main_site_unreachable");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "main_site_unreachable");
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "main_site_not_configured");
    }
  }

  /** Sends a bulk email to a set of external recipients through the Resend relay. */
  @PostMapping("/broadcast")
  public Map<String, Object> broadcast(
      @RequestBody BroadcastRequest body, HttpServletRequest request) {
    requireAdmin(request);
    if (body.recipients() == null || body.recipients().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipients_required");
    }
    if (body.recipients().size() > mailProperties.getBroadcastMaxRecipients()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "too_many_recipients");
    }
    List<String> recipients =
        body.recipients().stream()
            .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    if (recipients.isEmpty() || recipients.stream().anyMatch(value -> !isEmail(value))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_recipients");
    }
    String subject = body.subject() == null ? "" : body.subject().trim();
    String text = body.textBody() == null ? "" : body.textBody().trim();
    String html = body.htmlBody() == null ? "" : body.htmlBody().trim();
    if (subject.isBlank() || (text.isBlank() && html.isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content_required");
    }
    if (!resendClient.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "resend_not_configured");
    }
    ResendClient.Result result =
        resendClient.send(
            mailProperties.getBroadcastFrom(), recipients, subject, text, html, List.of());
    if (result.id() == null || result.id().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "resend_rejected_" + result.status() + ":" + result.detail());
    }
    return Map.of("id", result.id(), "recipients", recipients.size());
  }

  private boolean isEmail(String value) {
    return value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  }

  private void requireAdmin(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    MailSession session =
        httpSession == null
            ? null
            : (MailSession) httpSession.getAttribute(OAuthBffController.MAIL_SESSION);
    if (session == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login_required");
    }
    if (!session.isAdmin()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin_required");
    }
  }

  public record SuspendRequest(boolean suspended) {}

  public record BroadcastRequest(List<String> recipients, String subject, String htmlBody, String textBody) {}

  public record MailboxPage(List<MailboxView> rows, int total, int page, int pageSize) {}

  public record MailboxView(
      long id,
      String sub,
      long userId,
      String displayName,
      String address,
      String mailDomain,
      String status,
      String provisionStatus,
      String lastEventId) {
    static MailboxView from(MailboxAccount account) {
      return new MailboxView(
          account.id(),
          account.oauthSub(),
          account.userId(),
          account.displayName(),
          account.primaryAddress(),
          account.mailDomain(),
          account.status(),
          account.provisionStatus(),
          account.lastEventId());
    }
  }
}