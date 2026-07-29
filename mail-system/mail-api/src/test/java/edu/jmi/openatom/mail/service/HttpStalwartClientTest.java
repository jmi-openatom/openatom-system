package edu.jmi.openatom.mail.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.jmi.openatom.mail.config.MailProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpStalwartClientTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void createsAccountUsingStalwartMapAndSetEncoding() throws Exception {
    List<JsonNode> requests = new ArrayList<>();
    List<String> authorization = new ArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api",
        exchange -> {
          requests.add(objectMapper.readTree(exchange.getRequestBody()));
          authorization.add(exchange.getRequestHeaders().getFirst("Authorization"));
          int call = requests.size();
          String response =
              call == 1
                  ? "{\"methodResponses\":[[\"x:Account/query\",{\"ids\":[]},\"c1\"]]}"
                  : "{\"methodResponses\":[[\"x:Account/set\",{\"created\":{\"mailbox\":{\"id\":\"account-1\"}}},\"c1\"]]}";
          respond(exchange, response);
        });
    server.start();
    try {
      MailProperties properties = properties(server.getAddress().getPort());
      HttpStalwartClient client = new HttpStalwartClient(properties, objectMapper);

      String accountId =
          client.ensureAccount(
              "oauth-sub-42", "张三", List.of("zhangsan@jmi-openatom.cn"), 1_048_576);

      assertThat(accountId).isEqualTo("account-1");
      assertThat(requests).hasSize(2);
      assertThat(authorization).containsOnly("Bearer API_test_token_123456789");
      JsonNode account =
          requests.get(1).path("methodCalls").get(0).get(1).path("create").path("mailbox");
      assertThat(account.path("credentials").isObject()).isTrue();
      assertThat(account.path("memberGroupIds").isObject()).isTrue();
      assertThat(account.path("aliases").isObject()).isTrue();
      assertThat(account.path("aliases").path("0").path("name").asText())
          .isEqualTo("zhangsan");
      assertThat(account.path("aliases").path("0").path("domainId").asText())
          .isEqualTo("domain-1");
      assertThat(account.path("quotas").path("maxDiskQuota").asLong()).isEqualTo(1_048_576);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void suspendsAccountWithEmptyReplacePermissionSets() throws Exception {
    List<JsonNode> requests = new ArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api",
        exchange -> {
          requests.add(objectMapper.readTree(exchange.getRequestBody()));
          respond(
              exchange,
              "{\"methodResponses\":[[\"x:Account/set\",{\"updated\":{\"account-1\":null}},\"c1\"]]}");
        });
    server.start();
    try {
      HttpStalwartClient client =
          new HttpStalwartClient(properties(server.getAddress().getPort()), objectMapper);

      client.setEnabled("account-1", false);

      JsonNode permissions =
          requests.get(0).path("methodCalls").get(0).get(1)
              .path("update").path("account-1").path("permissions");
      assertThat(permissions.path("@type").asText()).isEqualTo("Replace");
      assertThat(permissions.path("enabledPermissions").isObject()).isTrue();
      assertThat(permissions.path("disabledPermissions").isObject()).isTrue();
    } finally {
      server.stop(0);
    }
  }

  private MailProperties properties(int port) {
    MailProperties properties = new MailProperties();
    properties.getStalwart().setApiUrl("http://127.0.0.1:" + port + "/api");
    properties.getStalwart().setApiToken("API_test_token_123456789");
    properties.getStalwart().setDomainId("domain-1");
    return properties;
  }

  private void respond(HttpExchange exchange, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
