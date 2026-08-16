package edu.jmi.openatom.mail.web;

import edu.jmi.openatom.mail.domain.ProvisionRequest;
import edu.jmi.openatom.mail.domain.ProvisionResponse;
import edu.jmi.openatom.mail.repository.MailboxRepository;
import edu.jmi.openatom.mail.service.BroadcastEmailTemplate;
import edu.jmi.openatom.mail.service.BroadcastSenderService;
import edu.jmi.openatom.mail.service.MailboxProvisioningService;
import edu.jmi.openatom.mail.service.MainSiteUsersClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/mailboxes")
public class InternalMailboxController {
  private static final Logger log = LoggerFactory.getLogger(InternalMailboxController.class);

  private final InternalTokenVerifier tokenVerifier;
  private final MailboxProvisioningService service;
  private final MailboxRepository repository;
  private final MainSiteUsersClient mainSiteUsersClient;
  private final BroadcastSenderService broadcastSenderService;

  public InternalMailboxController(
      InternalTokenVerifier tokenVerifier,
      MailboxProvisioningService service,
      MailboxRepository repository,
      MainSiteUsersClient mainSiteUsersClient,
      BroadcastSenderService broadcastSenderService) {
    this.tokenVerifier = tokenVerifier;
    this.service = service;
    this.repository = repository;
    this.mainSiteUsersClient = mainSiteUsersClient;
    this.broadcastSenderService = broadcastSenderService;
  }

  @PostMapping("/provision")
  public ProvisionResponse provision(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody ProvisionRequest request) {
    authorize(authorization);
    if (!idempotencyKey.equals(request.eventId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idempotency_key_mismatch");
    }
    if (!request.sub().matches("^[A-Za-z0-9._-]{1,64}$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_subject");
    }
    return service.provision(request);
  }

  /**
   * Sends a branded broadcast mail. Without {@code recipients} it targets every
   * active mail-system account plus main-site users with an external email;
   * with {@code recipients} it sends to exactly those addresses.
   */
  @PostMapping("/broadcast")
  public Map<String, Object> broadcast(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody InternalBroadcastRequest request) {
    authorize(authorization);
    String subject = request.subject() == null ? "" : request.subject().trim();
    String text = request.textBody() == null ? "" : request.textBody().trim();
    String html = request.htmlBody() == null ? "" : request.htmlBody().trim();
    if (subject.isBlank() || (text.isBlank() && html.isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content_required");
    }
    if (html.length() > 200_000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content_too_large");
    }
    List<String> recipients = request.recipients();
    if (recipients == null || recipients.isEmpty()) {
      recipients = resolveAllRecipients();
      log.info("internal broadcast (kind={}) resolved {} recipient(s)", request.kind(), recipients.size());
    } else {
      recipients = new ArrayList<>(new LinkedHashSet<>(recipients));
    }
    String wrappedHtml = BroadcastEmailTemplate.wrap(html, subject);
    BroadcastSenderService.SendResult result;
    try {
      result = broadcastSenderService.send(
          recipients, subject, wrappedHtml, text, "auto",
          request.kind() == null ? "auto" : request.kind());
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "resend_not_configured");
    }
    log.info("internal broadcast (kind={}) sent to {} recipient(s) in {} batch(es)",
        request.kind(), result.recipients(), result.batches());
    return Map.of("sent", result.recipients(), "batches", result.batches());
  }

  private List<String> resolveAllRecipients() {
    Set<String> emails = new LinkedHashSet<>();
    repository.findAll().stream()
        .filter(account -> "ACTIVE".equals(account.status()))
        .map(account -> account.primaryAddress())
        .filter(address -> address != null && !address.isBlank())
        .map(address -> address.toLowerCase())
        .forEach(emails::add);
    try {
      mainSiteUsersClient
          .recipients(1, 500, "")
          .rows()
          .forEach(recipient -> emails.add(recipient.email().toLowerCase()));
    } catch (Exception exception) {
      log.warn("internal broadcast: main-site recipients unavailable: {}",
          exception.getMessage());
    }
    return new ArrayList<>(emails);
  }

  @GetMapping("/{sub}")
  public ProvisionResponse status(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable String sub) {
    authorize(authorization);
    return service.status(sub);
  }

  @PostMapping("/{sub}/correct-primary")
  public ProvisionResponse correct(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable String sub,
      @Valid @RequestBody CorrectPrimaryRequest request) {
    authorize(authorization);
    return service.correctPrimaryAddress(sub, request.localPart());
  }

  private void authorize(String authorization) {
    if (!tokenVerifier.accepts(authorization)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_service_token");
    }
  }

  public record CorrectPrimaryRequest(@NotBlank String localPart) {}

  public record InternalBroadcastRequest(
      String kind, String subject, String htmlBody, String textBody, List<String> recipients) {}
}
