package edu.jmi.openatom.mail.web;

import edu.jmi.openatom.mail.domain.ProvisionResponse;
import edu.jmi.openatom.mail.oauth.MailSession;
import edu.jmi.openatom.mail.service.MailboxProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** User-facing mailbox self-service: status and first-login activation. */
@RestController
@RequestMapping("/api/mailbox")
public class MailboxSelfController {
  private final MailboxProvisioningService service;

  public MailboxSelfController(MailboxProvisioningService service) {
    this.service = service;
  }

  @GetMapping("/status")
  public Map<String, Object> status(HttpServletRequest request) {
    MailSession session = requireSession(request);
    ProvisionResponse status = service.status(session.sub());
    java.util.Map<String, Object> result = new java.util.HashMap<>();
    result.put("status", status.status());
    result.put("provisionStatus", status.provisionStatus());
    result.put("address", status.address() == null ? "" : status.address());
    result.put("displayName", session.displayName());
    result.put("isAdmin", session.isAdmin());
    return result;
  }

  @PostMapping("/activate")
  public Map<String, Object> activate(
      @RequestHeader(value = "X-Mail-CSRF", required = false) String csrf,
      @Valid @RequestBody ActivateRequest body,
      HttpServletRequest request) {
    MailSession session = requireSession(request, csrf);
    ProvisionResponse result;
    if (body.usePinyin() != null && body.usePinyin()) {
      result = service.activateWithPinyin(session.sub(), session.displayName());
    } else {
      if (body.localPart() == null || body.localPart().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "local_part_required");
      }
      result = service.correctPrimaryAddress(session.sub(), body.localPart());
    }
    // Keep the server-side session in sync so the reloaded client can
    // bootstrap the mail context with the newly assigned address.
    if (result.address() != null && !result.address().isBlank()) {
      MailSession updated =
          session.withMailAccountId(null).withAddress(result.address()).withMailboxStatus(result.status());
      HttpSession httpSession = request.getSession(false);
      if (httpSession != null) {
        httpSession.setAttribute(OAuthBffController.MAIL_SESSION, updated);
      }
    }
    return provisionResult(result);
  }

  private Map<String, Object> provisionResult(ProvisionResponse result) {
    java.util.Map<String, Object> map = new java.util.HashMap<>();
    map.put("status", result.status());
    map.put("provisionStatus", result.provisionStatus());
    map.put("address", result.address() == null ? "" : result.address());
    return map;
  }

  private MailSession requireSession(HttpServletRequest request) {
    return requireSession(request, null);
  }

  private MailSession requireSession(HttpServletRequest request, String csrf) {
    HttpSession httpSession = request.getSession(false);
    MailSession session =
        httpSession == null
            ? null
            : (MailSession) httpSession.getAttribute(OAuthBffController.MAIL_SESSION);
    if (session == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login_required");
    }
    if (csrf != null && !OAuthBffController.constantTimeEquals(session.csrfToken(), csrf)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_csrf_token");
    }
    return session;
  }

  public record ActivateRequest(Boolean usePinyin, String localPart) {}
}