package edu.jmi.openatom.mail.web;

import edu.jmi.openatom.mail.config.MailProperties;
import edu.jmi.openatom.mail.domain.ProvisionRequest;
import edu.jmi.openatom.mail.domain.ProvisionResponse;
import edu.jmi.openatom.mail.oauth.MailSession;
import edu.jmi.openatom.mail.oauth.OAuthClient;
import edu.jmi.openatom.mail.oauth.OAuthFlowState;
import edu.jmi.openatom.mail.service.MailboxProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class OAuthBffController {
  public static final String MAIL_SESSION = "mailIdentity";
  private static final String OAUTH_FLOW = "oauthFlow";
  private final SecureRandom secureRandom = new SecureRandom();
  private final MailProperties properties;
  private final OAuthClient oauthClient;
  private final MailboxProvisioningService provisioningService;

  public OAuthBffController(
      MailProperties properties,
      OAuthClient oauthClient,
      MailboxProvisioningService provisioningService) {
    this.properties = properties;
    this.oauthClient = oauthClient;
    this.provisioningService = provisioningService;
  }

  @GetMapping("/oauth/login")
  public void login(HttpSession session, HttpServletResponse response) throws IOException {
    String state = random(32);
    String nonce = random(32);
    String verifier = random(64);
    session.setAttribute(OAUTH_FLOW, new OAuthFlowState(state, nonce, verifier));
    String challenge = base64Url(sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
    String target =
        UriComponentsBuilder.fromUriString(properties.getOauth().getAuthorizationUrl())
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.getOauth().getClientId())
            .queryParam("redirect_uri", properties.getOauth().getRedirectUri())
            .queryParam("scope", "openid profile email mail offline_access")
            .queryParam("state", state)
            .queryParam("nonce", nonce)
            .queryParam("code_challenge", challenge)
            .queryParam("code_challenge_method", "S256")
            .build()
            .encode()
            .toUriString();
    response.sendRedirect(target);
  }

  @GetMapping("/oauth/callback")
  public void callback(
      String code,
      String state,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    HttpSession session = request.getSession(false);
    OAuthFlowState flow =
        session == null ? null : (OAuthFlowState) session.getAttribute(OAUTH_FLOW);
    if (flow == null || !constantTimeEquals(flow.state(), state) || code == null || code.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_oauth_state");
    }
    session.removeAttribute(OAUTH_FLOW);
    OAuthClient.TokenSet tokens = oauthClient.exchangeCode(code, flow.verifier(), flow.nonce());
    long userId;
    try {
      userId = Long.parseLong(tokens.sub());
    } catch (NumberFormatException exception) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_numeric_subject");
    }
    String eventId = "jit-" + HexFormat.of().formatHex(sha256(
        (tokens.sub() + ":" + tokens.identityEventId()).getBytes(StandardCharsets.UTF_8))).substring(0, 48);
    ProvisionResponse mailbox =
        provisioningService.provision(
            new ProvisionRequest(
                eventId,
                "USER_CREATED",
                tokens.sub(),
                userId,
                tokens.sub(),
                tokens.displayName(),
                "ACTIVE"));
    request.changeSessionId();
    session.setAttribute(
        MAIL_SESSION,
        new MailSession(
            tokens.sub(),
            userId,
            tokens.displayName(),
            mailbox.address(),
            mailbox.status(),
            null,
            tokens.accessToken(),
            tokens.refreshToken(),
            tokens.expiresAt(),
            random(32),
            tokens.roles()));
    response.sendRedirect("/");
  }

  @GetMapping("/session")
  public SessionView session(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    MailSession session =
        httpSession == null ? null : (MailSession) httpSession.getAttribute(MAIL_SESSION);
    if (session == null) {
      return new SessionView(false, null, null, null, null);
    }
    return new SessionView(
        true,
        session.displayName(),
        session.address(),
        session.mailboxStatus(),
        session.csrfToken());
  }

  @PostMapping("/logout")
  public Map<String, Boolean> logout(
      HttpServletRequest request,
      @RequestHeader(value = "X-Mail-CSRF", required = false) String csrf) {
    HttpSession session = request.getSession(false);
    MailSession identity =
        session == null ? null : (MailSession) session.getAttribute(MAIL_SESSION);
    if (identity == null || !constantTimeEquals(identity.csrfToken(), csrf)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_csrf_token");
    }
    session.invalidate();
    return Map.of("loggedOut", true);
  }

  static boolean constantTimeEquals(String expected, String supplied) {
    if (expected == null || supplied == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
  }

  private String random(int bytes) {
    byte[] value = new byte[bytes];
    secureRandom.nextBytes(value);
    return base64Url(value);
  }

  private byte[] sha256(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (Exception exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private String base64Url(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  public record SessionView(
      boolean authenticated, String displayName, String address, String status, String csrfToken) {}
}