package edu.jmi.openatom.mail.web;

import edu.jmi.openatom.mail.domain.ProvisionRequest;
import edu.jmi.openatom.mail.domain.ProvisionResponse;
import edu.jmi.openatom.mail.service.MailboxProvisioningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
  private final InternalTokenVerifier tokenVerifier;
  private final MailboxProvisioningService service;

  public InternalMailboxController(
      InternalTokenVerifier tokenVerifier, MailboxProvisioningService service) {
    this.tokenVerifier = tokenVerifier;
    this.service = service;
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
}
