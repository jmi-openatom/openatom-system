package edu.jmi.openatom.mail.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.mail.oauth.MailSession;
import edu.jmi.openatom.mail.oauth.OAuthClient;
import edu.jmi.openatom.mail.service.MalwareScanner;
import edu.jmi.openatom.mail.service.MalwareScanUnavailableException;
import edu.jmi.openatom.mail.service.ResendClient;
import edu.jmi.openatom.mail.service.UserJmapClient;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class JmapBffControllerTest {
  private final ObjectMapper json = new ObjectMapper();
  private final MalwareScanner cleanScanner = content -> MalwareScanner.ScanResult.CLEAN;
  private final JmapBffController controller =
      new JmapBffController(null, null, json, cleanScanner);

  @Test
  void acceptsAllowedReadMethods() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/get",{"ids":["1"]},"c1"]]}
        """);

    assertThat(controller.validateMethodCalls(payload)).isFalse();
  }

  @Test
  void rejectsUnlistedJmapMethod() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Blob/copy",{},"c1"]]}
        """);

    assertThatThrownBy(() -> controller.validateMethodCalls(payload))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("jmap_method_not_allowed");
  }

  @Test
  void rejectsAttachmentsThatDidNotPassThroughTheUploadPipeline() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/set",{"create":{"draft":{"to":[{"email":"a@example.com"}],"attachments":[{"blobId":"x"}]}}},"c1"]]}
        """);

    assertThatThrownBy(() -> controller.validateMethodCalls(payload))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_not_uploaded");
  }

  @Test
  void acceptsRegisteredAttachmentWithUntamperedMetadata() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/set",{"accountId":"own-account","create":{"draft":{"to":[{"email":"a@example.com"}],"from":[{"email":"me@example.com"}],"attachments":[{"blobId":"blob-1","name":"report.pdf","type":"application/pdf","size":1024}]}}},"c1"]]}
        """);
    Map<String, JmapBffController.UploadedAttachment> uploaded =
        Map.of(
            "blob-1",
            new JmapBffController.UploadedAttachment(
                "blob-1", "report.pdf", "application/pdf", 1024));

    assertThat(
            controller.validateMethodCalls(
                payload, "own-account", "me@example.com", uploaded))
        .isFalse();
  }

  @Test
  void rejectsTamperedAttachmentMetadata() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/set",{"accountId":"own-account","create":{"draft":{"to":[{"email":"a@example.com"}],"from":[{"email":"me@example.com"}],"attachments":[{"blobId":"blob-1","name":"renamed.exe","type":"application/pdf","size":1024}]}}},"c1"]]}
        """);
    Map<String, JmapBffController.UploadedAttachment> uploaded =
        Map.of(
            "blob-1",
            new JmapBffController.UploadedAttachment(
                "blob-1", "report.pdf", "application/pdf", 1024));

    assertThatThrownBy(
            () ->
                controller.validateMethodCalls(
                    payload, "own-account", "me@example.com", uploaded))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_not_uploaded");
  }

  @Test
  void requiresAtLeastOneAndAtMostTwentyFiveRecipients() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/set",{"create":{"draft":{"subject":"no recipients"}}},"c1"]]}
        """);

    assertThatThrownBy(() -> controller.validateMethodCalls(payload))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("recipient_limit_exceeded");

    StringBuilder recipients = new StringBuilder();
    for (int index = 0; index < 26; index++) {
      if (index > 0) recipients.append(',');
      recipients.append("{\"email\":\"user").append(index).append("@example.com\"}");
    }
    JsonNode tooMany = json.readTree(
        "{\"methodCalls\":[[\"Email/set\",{\"create\":{\"draft\":{\"to\":["
            + recipients
            + "]}}},\"c1\"]]}");
    assertThatThrownBy(() -> controller.validateMethodCalls(tooMany))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("recipient_limit_exceeded");
  }

  @Test
  void limitsEachUserToTwentySubmissionsPerMinute() {
    for (int attempt = 0; attempt < 20; attempt++) {
      controller.enforceSubmissionRate("user-42");
    }

    assertThatThrownBy(() -> controller.enforceSubmissionRate("user-42"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("send_rate_exceeded");
    controller.enforceSubmissionRate("another-user");
  }

  @Test
  void rejectsAnotherUsersAccountId() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/get",{"accountId":"victim","ids":["1"]},"c1"]]}
        """);

    assertThatThrownBy(
            () -> controller.validateMethodCalls(payload, "own-account", "me@example.com"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("mail_account_mismatch");
  }

  @Test
  void rejectsSpoofedFromAddress() throws Exception {
    JsonNode payload = json.readTree("""
        {"methodCalls":[["Email/set",{"accountId":"own-account","create":{"draft":{"to":[{"email":"a@example.com"}],"from":[{"email":"admin@example.com"}]}}},"c1"]]}
        """);

    assertThatThrownBy(
            () -> controller.validateMethodCalls(payload, "own-account", "me@example.com"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("sender_address_mismatch");
  }


  @Test
  void sendsSubmissionThroughResendWhenConfigured() throws Exception {
    UserJmapClient client = mock(UserJmapClient.class);
    ResendClient resend = mock(ResendClient.class);
    JmapBffController sendController =
        new JmapBffController(client, mock(OAuthClient.class), json, cleanScanner, resend);
    when(resend.isConfigured()).thenReturn(true);
    when(resend.send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(new ResendClient.Result("resend-message-1", 200, null));
    when(client.forward(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(
            new UserJmapClient.Response(
                200,
                "{\"methodResponses\":[[\"Email/set\",{\"accountId\":\"account-1\",\"created\":{\"draft\":{\"id\":\"email-1\"}}},\"c1\"]]}"),
            new UserJmapClient.Response(
                200,
                "{\"methodResponses\":[[\"Email/get\",{\"accountId\":\"account-1\",\"list\":[{\"id\":\"email-1\",\"from\":[{\"email\":\"ceshiyonghu@mailer.jmi-openatom.cn\"}],\"to\":[{\"email\":\"a@example.com\"}],\"subject\":\"hello\",\"textBody\":[{\"partId\":\"body\",\"type\":\"text/plain\"}],\"bodyValues\":{\"body\":{\"value\":\"world\"}}}]},\"c1\"]]}"),
            new UserJmapClient.Response(
                200,
                "{\"methodResponses\":[[\"Email/set\",{\"accountId\":\"account-1\",\"destroyed\":[\"email-1\"]},\"c1\"]]}"));
    MockHttpServletRequest request = authenticatedRequest();
    JsonNode payload = json.readTree("""
        {"methodCalls":[
          ["Email/set",{"accountId":"account-1","create":{"draft":{"to":[{"email":"a@example.com"}],"from":[{"email":"ceshiyonghu@mailer.jmi-openatom.cn"}],"subject":"hello","textBody":[{"partId":"body","type":"text/plain"}],"bodyValues":{"body":{"value":"world"}}}}},"c1"],
          ["EmailSubmission/set",{"accountId":"account-1","create":{"send":{"emailId":"#draft","identityId":"id-1"}},"onSuccessDestroyEmail":["#draft"]},"c2"]
        ]}
        """);

    var response = sendController.jmap(payload, "csrf-token", request);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("resend-message-1");
    verify(resend).send(
        org.mockito.ArgumentMatchers.eq("ceshiyonghu@mailer.jmi-openatom.cn"),
        org.mockito.ArgumentMatchers.eq(java.util.List.of("a@example.com")),
        org.mockito.ArgumentMatchers.eq("hello"),
        org.mockito.ArgumentMatchers.eq("world"),
        org.mockito.ArgumentMatchers.anyList());
  }

  @Test
  void fallsBackToStalwartSubmissionWhenResendNotConfigured() throws Exception {
    UserJmapClient client = mock(UserJmapClient.class);
    ResendClient resend = mock(ResendClient.class);
    JmapBffController sendController =
        new JmapBffController(client, mock(OAuthClient.class), json, cleanScanner, resend);
    when(resend.isConfigured()).thenReturn(false);
    when(client.forward(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new UserJmapClient.Response(200, "{\"methodResponses\":[]}"));
    MockHttpServletRequest request = authenticatedRequest();
    JsonNode payload = json.readTree("""
        {"methodCalls":[["EmailSubmission/set",{"accountId":"account-1","create":{}},"c1"]]}
        """);

    var response = sendController.jmap(payload, "csrf-token", request);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(client).forward(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("access-token"));
    verify(resend, org.mockito.Mockito.never()).send(
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyList());
  }

  @Test
  void uploadsAttachmentThroughUsersJmapTokenAndRegistersMetadata() {
    UserJmapClient client = mock(UserJmapClient.class);
    JmapBffController uploadController =
        new JmapBffController(client, mock(OAuthClient.class), json, cleanScanner);
    MockHttpServletRequest request = authenticatedRequest();
    MockMultipartFile file =
        new MockMultipartFile("file", "report.pdf", "application/pdf", "test".getBytes());
    when(client.upload("account-1", "application/pdf", "test".getBytes(), "access-token"))
        .thenReturn(
            new UserJmapClient.Response(
                201,
                "{\"accountId\":\"account-1\",\"blobId\":\"blob-1\",\"type\":\"application/pdf\",\"size\":4}"));

    var response = uploadController.uploadAttachment(file, "csrf-token", request);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).containsEntry("blobId", "blob-1");
    verify(client).upload("account-1", "application/pdf", "test".getBytes(), "access-token");
  }

  @Test
  void rejectsOversizedAndActiveAttachmentsBeforeUpload() {
    JmapBffController uploadController =
        new JmapBffController(
            mock(UserJmapClient.class), mock(OAuthClient.class), json, cleanScanner);

    assertThatThrownBy(
            () ->
                uploadController.uploadAttachment(
                    new MockMultipartFile(
                        "file",
                        "large.pdf",
                        "application/pdf",
                        new byte[20 * 1024 * 1024 + 1]),
                    "csrf-token",
                    authenticatedRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_too_large");

    assertThatThrownBy(
            () ->
                uploadController.uploadAttachment(
                    new MockMultipartFile(
                        "file", "payload.html", "text/html", "<script/>".getBytes()),
                    "csrf-token",
                    authenticatedRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_type_not_allowed");
  }

  @Test
  void downloadsAsNonSniffableAttachmentInsteadOfInlinePreview() {
    UserJmapClient client = mock(UserJmapClient.class);
    JmapBffController downloadController =
        new JmapBffController(client, mock(OAuthClient.class), json, cleanScanner);
    byte[] content = "content".getBytes();
    when(client.download("account-1", "blob-1", "access-token"))
        .thenReturn(new UserJmapClient.BinaryResponse(200, content));

    var response =
        downloadController.downloadAttachment("blob-1", "report.html", authenticatedRequest());

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .startsWith("attachment;")
        .contains("report.html");
    assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
  }

  @Test
  void rejectsMalwareAndFailsClosedWhenScannerIsUnavailable() {
    UserJmapClient client = mock(UserJmapClient.class);
    MalwareScanner infectedScanner = content -> MalwareScanner.ScanResult.INFECTED;
    JmapBffController infectedController =
        new JmapBffController(client, mock(OAuthClient.class), json, infectedScanner);
    MockMultipartFile file =
        new MockMultipartFile("file", "report.pdf", "application/pdf", "payload".getBytes());

    assertThatThrownBy(
            () -> infectedController.uploadAttachment(file, "csrf-token", authenticatedRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_malware_detected");

    MalwareScanner unavailableScanner =
        content -> {
          throw new MalwareScanUnavailableException("offline");
        };
    JmapBffController unavailableController =
        new JmapBffController(client, mock(OAuthClient.class), json, unavailableScanner);
    assertThatThrownBy(
            () -> unavailableController.uploadAttachment(file, "csrf-token", authenticatedRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_scan_unavailable");
  }

  @Test
  void rescansDownloadedAttachmentsAndFailsClosed() {
    UserJmapClient client = mock(UserJmapClient.class);
    when(client.download("account-1", "blob-1", "access-token"))
        .thenReturn(new UserJmapClient.BinaryResponse(200, "payload".getBytes()));

    JmapBffController infectedController =
        new JmapBffController(
            client,
            mock(OAuthClient.class),
            json,
            content -> MalwareScanner.ScanResult.INFECTED);
    assertThatThrownBy(
            () ->
                infectedController.downloadAttachment(
                    "blob-1", "report.pdf", authenticatedRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_malware_detected");

    JmapBffController unavailableController =
        new JmapBffController(
            client,
            mock(OAuthClient.class),
            json,
            content -> {
              throw new MalwareScanUnavailableException("offline");
            });
    assertThatThrownBy(
            () ->
                unavailableController.downloadAttachment(
                    "blob-1", "report.pdf", authenticatedRequest()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("attachment_scan_unavailable");
  }

  private MockHttpServletRequest authenticatedRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = (MockHttpSession) request.getSession(true);
    session.setAttribute(
        OAuthBffController.MAIL_SESSION,
        new MailSession(
            "user-42",
            42L,
            "测试用户",
            "ceshiyonghu@example.com",
            "ACTIVE",
            "account-1",
            "access-token",
            "refresh-token",
            Instant.now().plusSeconds(600),
            "csrf-token"));
    return request;
  }
}