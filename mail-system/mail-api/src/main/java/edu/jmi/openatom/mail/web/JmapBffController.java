package edu.jmi.openatom.mail.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.jmi.openatom.mail.oauth.MailSession;
import edu.jmi.openatom.mail.oauth.OAuthClient;
import edu.jmi.openatom.mail.service.ResendClient;
import edu.jmi.openatom.mail.service.UserJmapClient;
import edu.jmi.openatom.mail.service.StalwartClientException;
import edu.jmi.openatom.mail.service.MalwareScanUnavailableException;
import edu.jmi.openatom.mail.service.MalwareScanner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class JmapBffController {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(JmapBffController.class);

  private static final String MAIL_CAPABILITY = "urn:ietf:params:jmap:mail";
  private static final String UPLOADED_ATTACHMENTS = "mail.uploaded.attachments";
  private static final int MAX_REQUEST_BYTES = 25 * 1024 * 1024;
  private static final int MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024;
  private static final int MAX_ATTACHMENTS = 10;
  private static final int MAX_RECIPIENTS = 25;
  private static final int MAX_SUBMISSIONS_PER_MINUTE = 20;
  private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
  private static final Set<String> ALLOWED_METHODS =
      Set.of(
          "Mailbox/get",
          "Email/query",
          "Email/get",
          "Email/set",
          "EmailSubmission/set",
          "Identity/get",
          "Identity/set",
          "Thread/get",
          "SearchSnippet/get");
  private static final Set<String> BLOCKED_ATTACHMENT_EXTENSIONS =
      Set.of(
          "app", "bat", "cmd", "com", "dll", "dmg", "exe", "html", "htm", "iso",
          "jar", "js", "mjs", "msi", "pkg", "ps1", "scr", "sh", "svg", "vbs");
  private static final Set<String> BLOCKED_ATTACHMENT_TYPES =
      Set.of(
          "application/javascript",
          "application/x-dosexec",
          "application/x-msdownload",
          "image/svg+xml",
          "text/html",
          "text/javascript");
  private final UserJmapClient jmapClient;
  private final OAuthClient oauthClient;
  private final ObjectMapper objectMapper;
  private final MalwareScanner malwareScanner;
  private final ResendClient resendClient;
  private final ConcurrentHashMap<String, SubmissionWindow> submissionWindows =
      new ConcurrentHashMap<>();

  @Autowired
  public JmapBffController(
      UserJmapClient jmapClient,
      OAuthClient oauthClient,
      ObjectMapper objectMapper,
      MalwareScanner malwareScanner,
      ResendClient resendClient) {
    this.jmapClient = jmapClient;
    this.oauthClient = oauthClient;
    this.objectMapper = objectMapper;
    this.malwareScanner = malwareScanner;
    this.resendClient = resendClient;
  }

  /** Kept for unit tests; the Resend sender is absent (mail is not sent). */
  JmapBffController(
      UserJmapClient jmapClient,
      OAuthClient oauthClient,
      ObjectMapper objectMapper,
      MalwareScanner malwareScanner) {
    this(jmapClient, oauthClient, objectMapper, malwareScanner, null);
  }

  @PostMapping(value = "/jmap", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> jmap(
      @RequestBody JsonNode payload,
      @RequestHeader(value = "X-Mail-CSRF", required = false) String csrf,
      HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    MailSession session = requireSession(httpSession, csrf);
    session = refreshIfNeeded(httpSession, session, false);
    session = bindMailAccount(httpSession, session);
    boolean sendsMail =
        validateMethodCalls(
            payload,
            session.mailAccountId(),
            session.address(),
            attachmentRegistry(httpSession));
    if (sendsMail) {
      enforceSubmissionRate(session.sub());
      if (resendClient.isConfigured()) {
        UserJmapClient.Response response = submitViaResend(payload, session, httpSession);
        return ResponseEntity.status(response.status())
            .contentType(MediaType.APPLICATION_JSON)
            .body(response.body());
      }
    }
    UserJmapClient.Response response = jmapClient.forward(payload, session.accessToken());
    log.info("jmap forward: {} -> {}", methodNameSummary(payload), safePreviewBody(response.body(), 500));
    if (response.status() == 401 && session.refreshToken() != null) {
      session = refreshIfNeeded(httpSession, session, true);
      response = jmapClient.forward(payload, session.accessToken());
    }
    return ResponseEntity.status(response.status())
        .contentType(MediaType.APPLICATION_JSON)
        .body(response.body());
  }

  /**
   * Handles EmailSubmission/set through the Resend Email API instead of
   * Stalwart's SMTP delivery: creates the draft in Stalwart, reads it back,
   * sends via Resend, then destroys the draft and fabricates the JMAP
   * response the web client expects.
   */
  private UserJmapClient.Response submitViaResend(
      JsonNode payload, MailSession session, HttpSession httpSession) {
    try {
      JsonNode calls = payload.path("methodCalls");
      JsonNode emailSet = null;
      JsonNode submission = null;
      JsonNode otherCalls = objectMapper.createArrayNode();
      for (JsonNode call : calls) {
        String method = call.path(0).asText();
        if ("Email/set".equals(method)) {
          emailSet = call;
        } else if ("EmailSubmission/set".equals(method)) {
          submission = call;
        } else {
          ((com.fasterxml.jackson.databind.node.ArrayNode) otherCalls).add(call);
        }
      }
      if (submission == null) {
        UserJmapClient.Response fallback = jmapClient.forward(payload, session.accessToken());
        return fallback.status() == 401 && session.refreshToken() != null
            ? jmapClient.forward(payload, session.accessToken())
            : fallback;
      }
      ObjectNode mailAccount = objectMapper.createObjectNode();
      mailAccount.put("accountId", session.mailAccountId());
      // 1) Forward Email/set (create the draft in Stalwart)
      String emailId = null;
      if (emailSet != null && !emailSet.isNull()) {
        UserJmapClient.Response setResponse = jmapClient.forward(
            singleCall(emailSet), session.accessToken());
        log.info("submitViaResend: Email/set status={} body={}", setResponse.status(), setResponse.body());
        JsonNode setBody = objectMapper.readTree(setResponse.body());
        JsonNode created = setBody.path("methodResponses").path(0).path(1).path("created");
        String clientId = emailSet.path(1).path("create").fieldNames().next();
        emailId = created.path(clientId).path("id").asText(null);
        if (emailId == null) {
          return setResponse;
        }
      }
      // 2) Read the draft content from Stalwart
      ObjectNode getArgs = objectMapper.createObjectNode();
      getArgs.put("accountId", session.mailAccountId());
      getArgs.set("ids", objectMapper.createArrayNode().add(emailId));
      ObjectNode getCall = objectMapper.createObjectNode();
      getCall.set("using", objectMapper.createArrayNode()
          .add("urn:ietf:params:jmap:core")
          .add("urn:ietf:params:jmap:mail")
          .add("urn:ietf:params:jmap:submission"));
      getCall.putArray("methodCalls").add(objectMapper.createArrayNode()
          .add("Email/get").add(getArgs).add("draft-get"));
      UserJmapClient.Response getResponse = jmapClient.forward(getCall, session.accessToken());
      log.info("submitViaResend: Email/get status={} body={}", getResponse.status(), getResponse.body());
      JsonNode getBody = objectMapper.readTree(getResponse.body());
      JsonNode email = getBody.path("methodResponses").path(0).path(1).path("list").path(0);
      String fromAddress = email.path("from").path(0).path("email").asText(session.address());
      String subject = email.path("subject").asText("");
      String text = plainText(email);
      JsonNode toNode = email.path("to");
      java.util.List<String> to = new java.util.ArrayList<>();
      for (JsonNode recipient : toNode) {
        to.add(recipient.path("email").asText());
      }
      if (to.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "recipient_limit_exceeded");
      }
      // 3) Send through Resend
      ResendClient.Result result = resendClient.send(fromAddress, to, subject, text, java.util.List.of());
      if (result.id() == null || result.id().isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "resend_rejected_" + result.status() + ":" + result.detail());
      }
      // 4) Destroy the draft
      if (emailId != null) {
        ObjectNode destroyArgs = objectMapper.createObjectNode();
        destroyArgs.put("accountId", session.mailAccountId());
        destroyArgs.set("destroy", objectMapper.createArrayNode().add(emailId));
        jmapClient.forward(
            singleCall(objectMapper.createArrayNode().add("Email/set").add(destroyArgs).add("destroy")),
            session.accessToken());
      }
      // 5) Build the response the web client expects
      ObjectNode createdEntry = objectMapper.createObjectNode();
      createdEntry.put("id", result.id());
      ObjectNode submissionResult = objectMapper.createObjectNode();
      submissionResult.put("accountId", session.mailAccountId());
      submissionResult.put("newState", "r");
      ObjectNode created = submissionResult.putObject("created");
      created.set("send", createdEntry);
      ObjectNode methodResponses = objectMapper.createObjectNode();
      methodResponses.putArray("methodResponses")
          .add(objectMapper.createArrayNode().add("EmailSubmission/set").add(submissionResult).add("submit"));
      return new UserJmapClient.Response(200, objectMapper.writeValueAsString(methodResponses));
    } catch (ResponseStatusException exception) {
      return new UserJmapClient.Response(exception.getStatusCode().value(),
          "{\"methodResponses\":[\"error\",{\"type\":\"unknown\"}]}");
    } catch (Exception exception) {
      return new UserJmapClient.Response(500,
          "{\"methodResponses\":[\"error\",{\"type\":\"serverFail\"}]}");
    }
  }

  private String plainText(JsonNode email) {
    JsonNode bodyValues = email.path("bodyValues");
    for (JsonNode part : email.path("textBody")) {
      String partId = part.path("partId").asText();
      if (bodyValues.has(partId)) {
        return bodyValues.path(partId).path("value").asText("");
      }
    }
    return email.path("preview").asText("");
  }

  private JsonNode singleCall(JsonNode call) {
    ObjectNode wrapper = objectMapper.createObjectNode();
    wrapper.set("using", objectMapper.createArrayNode()
        .add("urn:ietf:params:jmap:core")
        .add("urn:ietf:params:jmap:mail")
        .add("urn:ietf:params:jmap:submission"));
    wrapper.putArray("methodCalls").add(call);
    return wrapper;
  }


  @GetMapping("/jmap/session")
  public ResponseEntity<String> jmapSession(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    MailSession session = requireSession(httpSession, null, false);
    session = refreshIfNeeded(httpSession, session, false);
    UserJmapClient.Response response = jmapClient.session(session.accessToken());
    log.info("jmapSession: status={} body={}", response.status(), response.body());
    if (response.status() == 401 && session.refreshToken() != null) {
      session = refreshIfNeeded(httpSession, session, true);
      response = jmapClient.session(session.accessToken());
      log.info("jmapSession(refreshed): status={} body={}", response.status(), response.body());
    }
    if (response.status() < 200 || response.status() >= 300) {
      return ResponseEntity.status(response.status())
          .contentType(MediaType.APPLICATION_JSON)
          .body(response.body());
    }
    ObjectNode sanitized = parseSession(response.body());
    String accountId = primaryMailAccount(sanitized);
    httpSession.setAttribute(
        OAuthBffController.MAIL_SESSION, session.withMailAccountId(accountId));
    sanitized.put("apiUrl", "/api/jmap");
    sanitized.putNull("uploadUrl");
    sanitized.putNull("downloadUrl");
    sanitized.putNull("eventSourceUrl");
    return ResponseEntity.status(response.status())
        .contentType(MediaType.APPLICATION_JSON)
        .body(writeJson(sanitized));
  }

  @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> uploadAttachment(
      @RequestPart("file") MultipartFile file,
      @RequestHeader(value = "X-Mail-CSRF", required = false) String csrf,
      HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    MailSession session = requireSession(httpSession, csrf);
    session = refreshIfNeeded(httpSession, session, false);
    session = bindMailAccount(httpSession, session);
    UploadDescriptor upload = validateUpload(file, attachmentRegistry(httpSession));
    byte[] content;
    try {
      content = file.getBytes();
    } catch (IOException exception) {
      throw new StalwartClientException("attachment_read_error", exception);
    }
    requireCleanAttachment(content);
    UserJmapClient.Response response =
        jmapClient.upload(session.mailAccountId(), upload.contentType(), content, session.accessToken());
    if (response.status() == 401 && session.refreshToken() != null) {
      session = refreshIfNeeded(httpSession, session, true);
      response =
          jmapClient.upload(
              session.mailAccountId(), upload.contentType(), content, session.accessToken());
    }
    if (response.status() < 200 || response.status() >= 300) {
      return ResponseEntity.status(response.status()).build();
    }
    String blobId = uploadedBlobId(response.body(), session.mailAccountId());
    UploadedAttachment attachment =
        new UploadedAttachment(blobId, upload.name(), upload.contentType(), upload.size());
    Map<String, UploadedAttachment> registry = attachmentRegistry(httpSession);
    registry.put(blobId, attachment);
    httpSession.setAttribute(UPLOADED_ATTACHMENTS, registry);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            Map.of(
                "blobId", attachment.blobId(),
                "name", attachment.name(),
                "type", attachment.contentType(),
                "size", attachment.size()));
  }

  @DeleteMapping("/attachments/{blobId}")
  public ResponseEntity<Void> forgetUploadedAttachment(
      @PathVariable String blobId,
      @RequestHeader(value = "X-Mail-CSRF", required = false) String csrf,
      HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    requireSession(httpSession, csrf);
    Map<String, UploadedAttachment> registry = attachmentRegistry(httpSession);
    registry.remove(blobId);
    httpSession.setAttribute(UPLOADED_ATTACHMENTS, registry);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/attachments/{blobId}")
  public ResponseEntity<byte[]> downloadAttachment(
      @PathVariable String blobId,
      @RequestParam(value = "name", required = false) String requestedName,
      HttpServletRequest request) {
    validateBlobId(blobId);
    HttpSession httpSession = request.getSession(false);
    MailSession session = requireSession(httpSession, null, false);
    session = refreshIfNeeded(httpSession, session, false);
    session = bindMailAccount(httpSession, session);
    UserJmapClient.BinaryResponse response =
        jmapClient.download(session.mailAccountId(), blobId, session.accessToken());
    if (response.status() == 401 && session.refreshToken() != null) {
      session = refreshIfNeeded(httpSession, session, true);
      response = jmapClient.download(session.mailAccountId(), blobId, session.accessToken());
    }
    if (response.status() < 200 || response.status() >= 300) {
      return ResponseEntity.status(response.status()).build();
    }
    requireCleanAttachment(response.body());
    String filename = sanitizeFilename(requestedName);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(response.body().length)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(response.body());
  }

  private void requireCleanAttachment(byte[] content) {
    try {
      if (malwareScanner.scan(content) == MalwareScanner.ScanResult.INFECTED) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, "attachment_malware_detected");
      }
    } catch (MalwareScanUnavailableException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "attachment_scan_unavailable");
    }
  }

  private MailSession requireSession(HttpSession httpSession, String csrf) {
    return requireSession(httpSession, csrf, true);
  }

  private MailSession requireSession(HttpSession httpSession, String csrf, boolean requireCsrf) {
    MailSession session =
        httpSession == null
            ? null
            : (MailSession) httpSession.getAttribute(OAuthBffController.MAIL_SESSION);
    if (session == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login_required");
    }
    if (!"ACTIVE".equals(session.mailboxStatus())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "mailbox_not_active");
    }
    if (requireCsrf && !OAuthBffController.constantTimeEquals(session.csrfToken(), csrf)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_csrf_token");
    }
    return session;
  }

  boolean validateMethodCalls(JsonNode payload) {
    return validateMethodCalls(payload, null, null, Map.of());
  }

  boolean validateMethodCalls(JsonNode payload, String accountId, String senderAddress) {
    return validateMethodCalls(payload, accountId, senderAddress, Map.of());
  }

  boolean validateMethodCalls(
      JsonNode payload,
      String accountId,
      String senderAddress,
      Map<String, UploadedAttachment> uploadedAttachments) {
    if (payload.toString().getBytes(StandardCharsets.UTF_8).length > MAX_REQUEST_BYTES) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "message_too_large");
    }
    JsonNode calls = payload.path("methodCalls");
    if (!calls.isArray() || calls.isEmpty() || calls.size() > 20) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_method_calls");
    }
    boolean sendsMail = false;
    for (JsonNode call : calls) {
      String method = call.isArray() && !call.isEmpty() ? call.get(0).asText() : "";
      if (!ALLOWED_METHODS.contains(method)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "jmap_method_not_allowed");
      }
      JsonNode arguments = call.path(1);
      if (accountId != null && !accountId.equals(arguments.path("accountId").asText())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "mail_account_mismatch");
      }
      if ("Email/set".equals(method)) {
        validateDrafts(arguments.path("create"), senderAddress, uploadedAttachments);
      }
      sendsMail |= "EmailSubmission/set".equals(method);
    }
    return sendsMail;
  }

  private void validateDrafts(
      JsonNode creates,
      String senderAddress,
      Map<String, UploadedAttachment> uploadedAttachments) {
    if (!creates.isObject()) {
      return;
    }
    Iterator<Map.Entry<String, JsonNode>> drafts = creates.fields();
    while (drafts.hasNext()) {
      JsonNode draft = drafts.next().getValue();
      validateDraftAttachments(draft.path("attachments"), uploadedAttachments);
      int recipients =
          draft.path("to").size() + draft.path("cc").size() + draft.path("bcc").size();
      if (recipients == 0 || recipients > MAX_RECIPIENTS) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, "recipient_limit_exceeded");
      }
      if (senderAddress != null) {
        JsonNode from = draft.path("from");
        if (!from.isArray() || from.size() != 1) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "sender_address_mismatch");
        }
        String fromEmail = from.get(0).path("email").asText();
        // Sending is also allowed from the Resend relay domain with the same
        // local part (mailer.jmi-openatom.cn).
        String relaySender =
            senderAddress.replaceFirst("@.*$", "@mailer.jmi-openatom.cn");
        if (!senderAddress.equalsIgnoreCase(fromEmail)
            && !relaySender.equalsIgnoreCase(fromEmail)) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "sender_address_mismatch");
        }
      }
    }
  }

  private void validateDraftAttachments(
      JsonNode attachments, Map<String, UploadedAttachment> uploadedAttachments) {
    if (attachments.isMissingNode() || attachments.isNull()) {
      return;
    }
    if (!attachments.isArray() || attachments.size() > MAX_ATTACHMENTS) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "attachment_count_exceeded");
    }
    long totalSize = 0;
    for (JsonNode candidate : attachments) {
      String blobId = candidate.path("blobId").asText();
      UploadedAttachment uploaded = uploadedAttachments.get(blobId);
      if (uploaded == null
          || !uploaded.name().equals(candidate.path("name").asText())
          || !uploaded.contentType().equals(candidate.path("type").asText())
          || uploaded.size() != candidate.path("size").asLong(-1)) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, "attachment_not_uploaded");
      }
      totalSize += uploaded.size();
    }
    if (totalSize > MAX_ATTACHMENT_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE, "attachments_total_too_large");
    }
  }

  private UploadDescriptor validateUpload(
      MultipartFile file, Map<String, UploadedAttachment> registry) {
    if (file.isEmpty() || file.getSize() <= 0) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "attachment_empty");
    }
    if (file.getSize() > MAX_ATTACHMENT_BYTES) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "attachment_too_large");
    }
    if (registry.size() >= MAX_ATTACHMENTS) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "attachment_count_exceeded");
    }
    long currentSize = registry.values().stream().mapToLong(UploadedAttachment::size).sum();
    if (currentSize + file.getSize() > MAX_ATTACHMENT_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE, "attachments_total_too_large");
    }
    String name = sanitizeFilename(file.getOriginalFilename());
    String extension = extensionOf(name);
    String contentType = normalizeContentType(file.getContentType());
    if (BLOCKED_ATTACHMENT_EXTENSIONS.contains(extension)
        || BLOCKED_ATTACHMENT_TYPES.contains(contentType)) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "attachment_type_not_allowed");
    }
    return new UploadDescriptor(name, contentType, file.getSize());
  }

  private String uploadedBlobId(String body, String accountId) {
    try {
      JsonNode response = objectMapper.readTree(body);
      String blobId = response.path("blobId").asText();
      String responseAccountId = response.path("accountId").asText();
      validateBlobId(blobId);
      if (!responseAccountId.isBlank() && !accountId.equals(responseAccountId)) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upload_account_mismatch");
      }
      return blobId;
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_upload_response");
    }
  }

  private void validateBlobId(String blobId) {
    if (blobId == null || !blobId.matches("^[A-Za-z0-9._~-]{1,1024}$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_blob_id");
    }
  }

  private String normalizeContentType(String value) {
    if (value == null || value.isBlank()) {
      return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
    String normalized = value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    try {
      MediaType.parseMediaType(normalized);
      return normalized;
    } catch (Exception exception) {
      return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
  }

  private String sanitizeFilename(String value) {
    String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
        .replace('\\', '/');
    normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
        .replaceAll("[\\p{Cntrl}]", "")
        .trim();
    if (normalized.isBlank() || ".".equals(normalized) || "..".equals(normalized)) {
      normalized = "attachment.bin";
    }
    return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
  }

  private String extensionOf(String filename) {
    int separator = filename.lastIndexOf('.');
    return separator < 0 ? "" : filename.substring(separator + 1).toLowerCase(Locale.ROOT);
  }

  @SuppressWarnings("unchecked")
  private Map<String, UploadedAttachment> attachmentRegistry(HttpSession httpSession) {
    Object value = httpSession.getAttribute(UPLOADED_ATTACHMENTS);
    if (value instanceof Map<?, ?> map) {
      LinkedHashMap<String, UploadedAttachment> safe = new LinkedHashMap<>();
      map.forEach(
          (key, attachment) -> {
            if (key instanceof String stringKey && attachment instanceof UploadedAttachment uploaded) {
              safe.put(stringKey, uploaded);
            }
          });
      return safe;
    }
    return new LinkedHashMap<>();
  }

  private MailSession bindMailAccount(HttpSession httpSession, MailSession session) {
    if (session.mailAccountId() != null && !session.mailAccountId().isBlank()) {
      return session;
    }
    UserJmapClient.Response response = jmapClient.session(session.accessToken());
    if (response.status() < 200 || response.status() >= 300) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "jmap_session_unavailable");
    }
    String accountId = primaryMailAccount(parseSession(response.body()));
    MailSession updated = session.withMailAccountId(accountId);
    httpSession.setAttribute(OAuthBffController.MAIL_SESSION, updated);
    return updated;
  }

  private ObjectNode parseSession(String body) {
    try {
      JsonNode parsed = objectMapper.readTree(body);
      if (!parsed.isObject()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_jmap_session");
      }
      return (ObjectNode) parsed;
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_jmap_session");
    }
  }

  private String primaryMailAccount(JsonNode session) {
    String accountId = session.path("primaryAccounts").path(MAIL_CAPABILITY).asText();
    if (accountId.isBlank() || !session.path("accounts").has(accountId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "mail_account_not_available");
    }
    return accountId;
  }

  private String writeJson(JsonNode value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_jmap_session");
    }
  }

  void enforceSubmissionRate(String subject) {
    Instant now = Instant.now();
    SubmissionWindow window =
        submissionWindows.compute(
            subject,
            (ignored, current) -> {
              if (current == null || current.startedAt().plus(RATE_WINDOW).isBefore(now)) {
                return new SubmissionWindow(now, 1);
              }
              return new SubmissionWindow(current.startedAt(), current.count() + 1);
            });
    if (window.count() > MAX_SUBMISSIONS_PER_MINUTE) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "send_rate_exceeded");
    }
  }

  private MailSession refreshIfNeeded(
      HttpSession httpSession, MailSession session, boolean force) {
    if (!force && session.accessTokenExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
      return session;
    }
    if (session.refreshToken() == null || session.refreshToken().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "oauth_session_expired");
    }
    OAuthClient.TokenSet refreshed = oauthClient.refresh(session.refreshToken());
    MailSession updated =
        session.withTokens(
            refreshed.accessToken(), refreshed.refreshToken(), refreshed.expiresAt());
    httpSession.setAttribute(OAuthBffController.MAIL_SESSION, updated);
    return updated;
  }


  private String methodNameSummary(JsonNode payload) {
    StringBuilder names = new StringBuilder();
    for (JsonNode call : payload.path("methodCalls")) {
      if (names.length() > 0) names.append(",");
      names.append(call.path(0).asText("?"));
    }
    return names.toString();
  }

  private String safePreviewBody(String body, int max) {
    if (body == null) return "null";
    return body.length() <= max ? body : body.substring(0, max) + "...(" + body.length() + "B)";
  }

  private record SubmissionWindow(Instant startedAt, int count) {}
  private record UploadDescriptor(String name, String contentType, long size) {}

  record UploadedAttachment(String blobId, String name, String contentType, long size)
      implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
  }
}