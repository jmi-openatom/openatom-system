package edu.jmi.openatom.server.openatomsystem.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import edu.jmi.openatom.server.openatomsystem.config.MailOutboxProperties;
import edu.jmi.openatom.server.openatomsystem.entity.MailboxOutboxEvent;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MailboxProvisioningClientTest {
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendsAuthenticatedIdempotentEventWithoutReadingResponseBody() throws Exception {
    AtomicReference<String> authorization = new AtomicReference<>();
    AtomicReference<String> idempotencyKey = new AtomicReference<>();
    AtomicReference<String> requestBody = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/provision",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();

    MailboxProvisioningClient.DeliveryResult result = client().deliver(event());

    assertThat(result.delivered()).isTrue();
    assertThat(authorization).hasValue("Bearer internal-secret");
    assertThat(idempotencyKey).hasValue("evt-1");
    JsonNode sent = new ObjectMapper().readTree(requestBody.get());
    assertThat(sent.get("sub").asText()).isEqualTo("42");
    assertThat(sent.get("eventId").asText()).isEqualTo("evt-1");
    assertThat(sent.get("eventType").asText()).isEqualTo("USER_CREATED");
  }

  @Test
  void classifiesDuplicateAsSuccessAndServerErrorsAsRetryable() throws Exception {
    assertThat(deliverWithStatus(409).delivered()).isTrue();
    MailboxProvisioningClient.DeliveryResult unavailable = deliverWithStatus(503);
    assertThat(unavailable.retryable()).isTrue();
    assertThat(unavailable.reason()).isEqualTo("http_503");
  }

  @Test
  void classifiesInvalidRequestAsPermanentFailure() throws Exception {
    MailboxProvisioningClient.DeliveryResult result = deliverWithStatus(422);
    assertThat(result.delivered()).isFalse();
    assertThat(result.retryable()).isFalse();
    assertThat(result.reason()).isEqualTo("http_422");
  }

  private MailboxProvisioningClient.DeliveryResult deliverWithStatus(int status) throws Exception {
    if (server != null) {
      server.stop(0);
    }
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/provision",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
        });
    server.start();
    return client().deliver(event());
  }

  private MailboxProvisioningClient client() {
    MailOutboxProperties properties = new MailOutboxProperties();
    properties.setProvisionUrl(
        "http://127.0.0.1:" + server.getAddress().getPort() + "/internal/provision");
    properties.setServiceToken("internal-secret");
    return new MailboxProvisioningClient(properties, new ObjectMapper(), HttpClient.newHttpClient());
  }

  private MailboxOutboxEvent event() {
    return MailboxOutboxEvent.builder()
        .id(1L)
        .eventId("evt-1")
        .eventType("USER_CREATED")
        .aggregateId("42")
        .payloadJson("{\"sub\":\"42\",\"displayName\":\"张三\"}")
        .retryCount(0)
        .build();
  }
}
