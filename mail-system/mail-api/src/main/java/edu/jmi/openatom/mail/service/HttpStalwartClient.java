package edu.jmi.openatom.mail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.jmi.openatom.mail.config.MailProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Least-privilege Stalwart account automation through its JMAP extension API. */
@Component
public class HttpStalwartClient implements StalwartClient {
  private static final String CORE = "urn:ietf:params:jmap:core";
  private static final String STALWART = "urn:stalwart:jmap";
  private final MailProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public HttpStalwartClient(MailProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  HttpStalwartClient(
      MailProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  @Override
  public String ensureAccount(
      String oauthSub, String displayName, List<String> aliases, long quotaBytes) {
    String existing = queryAccountId(oauthSub);
    if (existing != null) {
      updateAliases(existing, aliases);
      setEnabled(existing, true);
      return existing;
    }
    ObjectNode account = objectMapper.createObjectNode();
    account.put("@type", "User");
    account.put("name", oauthSub);
    account.put("domainId", properties.getStalwart().getDomainId());
    account.put("description", displayName == null ? "OpenAtom user " + oauthSub : displayName);
    account.set("credentials", objectMapper.createObjectNode());
    account.set("memberGroupIds", objectMapper.createObjectNode());
    account.set("roles", typed("User"));
    account.set("permissions", typed("Inherit"));
    ObjectNode quotas = objectMapper.createObjectNode();
    quotas.put("maxDiskQuota", quotaBytes);
    account.set("quotas", quotas);
    account.set("aliases", aliasNodes(aliases));
    account.set("encryptionAtRest", typed("Disabled"));

    ObjectNode arguments = objectMapper.createObjectNode();
    ObjectNode create = objectMapper.createObjectNode();
    create.set("mailbox", account);
    arguments.set("create", create);
    JsonNode response = call("x:Account/set", arguments);
    JsonNode created = response.path("created").path("mailbox").path("id");
    if (!created.isTextual()) {
      throw new StalwartClientException("stalwart_account_create_failed");
    }
    return created.asText();
  }

  @Override
  public void setEnabled(String accountId, boolean enabled) {
    ObjectNode patch = objectMapper.createObjectNode();
    if (enabled) {
      patch.set("permissions", typed("Inherit"));
    } else {
      ObjectNode permissions = typed("Replace");
      permissions.set("enabledPermissions", objectMapper.createObjectNode());
      permissions.set("disabledPermissions", objectMapper.createObjectNode());
      patch.set("permissions", permissions);
    }
    update(accountId, patch);
  }

  @Override
  public void updateAliases(String accountId, List<String> aliases) {
    ObjectNode patch = objectMapper.createObjectNode();
    patch.set("aliases", aliasNodes(aliases));
    update(accountId, patch);
  }

  private String queryAccountId(String oauthSub) {
    ObjectNode filter = objectMapper.createObjectNode();
    filter.put("name", oauthSub);
    ObjectNode arguments = objectMapper.createObjectNode();
    arguments.set("filter", filter);
    arguments.put("limit", 1);
    JsonNode response = call("x:Account/query", arguments);
    JsonNode ids = response.path("ids");
    return ids.isArray() && !ids.isEmpty() ? ids.get(0).asText() : null;
  }

  private void update(String id, ObjectNode patch) {
    ObjectNode update = objectMapper.createObjectNode();
    update.set(id, patch);
    ObjectNode arguments = objectMapper.createObjectNode();
    arguments.set("update", update);
    JsonNode response = call("x:Account/set", arguments);
    if (response.path("notUpdated").has(id)) {
      throw new StalwartClientException("stalwart_account_update_failed");
    }
  }

  private JsonNode call(String method, ObjectNode arguments) {
    ObjectNode requestBody = objectMapper.createObjectNode();
    var using = objectMapper.createArrayNode().add(CORE).add(STALWART);
    var methodCall = objectMapper.createArrayNode().add(method).add(arguments).add("c1");
    requestBody.set("using", using);
    requestBody.set("methodCalls", objectMapper.createArrayNode().add(methodCall));
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(properties.getStalwart().getApiUrl()))
              .timeout(Duration.ofSeconds(10))
              .header("Authorization", "Bearer " + properties.getStalwart().getApiToken())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new StalwartClientException("stalwart_http_" + response.statusCode());
      }
      JsonNode methodResponses = objectMapper.readTree(response.body()).path("methodResponses");
      if (!methodResponses.isArray() || methodResponses.isEmpty()) {
        throw new StalwartClientException("stalwart_invalid_response");
      }
      JsonNode responseTuple = methodResponses.get(0);
      if ("error".equals(responseTuple.path(0).asText())) {
        throw new StalwartClientException("stalwart_method_error");
      }
      return responseTuple.path(1);
    } catch (IOException exception) {
      throw new StalwartClientException("stalwart_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new StalwartClientException("stalwart_transport_interrupted", exception);
    }
  }

  private ObjectNode typed(String type) {
    ObjectNode value = objectMapper.createObjectNode();
    value.put("@type", type);
    return value;
  }

  private ObjectNode aliasNodes(List<String> aliases) {
    ObjectNode result = objectMapper.createObjectNode();
    int index = 0;
    for (String address : aliases) {
      int separator = address.lastIndexOf('@');
      if (separator <= 0 || separator == address.length() - 1) {
        throw new StalwartClientException("invalid_mailbox_alias");
      }
      ObjectNode alias = objectMapper.createObjectNode();
      alias.put("name", address.substring(0, separator));
      alias.put("domainId", properties.getStalwart().getDomainId());
      alias.put("enabled", true);
      result.set(Integer.toString(index++), alias);
    }
    return result;
  }
}
