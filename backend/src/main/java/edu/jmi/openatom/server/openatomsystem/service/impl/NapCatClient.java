package edu.jmi.openatom.server.openatomsystem.service.impl;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class NapCatClient {
  private final RestTemplate restTemplate;

  @Value("${app.napcat.base-url:}")
  private String baseUrl;

  @Value("${app.napcat.access-token:}")
  private String accessToken;

  public NapCatClient(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate =
        restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(15))
            .build();
  }

  public NapCatResponse getLoginInfo() {
    return post("get_login_info", Map.of());
  }

  public NapCatResponse getGroupList() {
    return post("get_group_list", Map.of());
  }

  public NapCatResponse getGroupMemberList(String groupId) {
    return post("get_group_member_list", Map.of("group_id", groupId, "no_cache", false));
  }

  public NapCatResponse muteMember(String groupId, String userId, int durationSeconds) {
    return post(
        "set_group_ban",
        Map.of("group_id", groupId, "user_id", userId, "duration", durationSeconds));
  }

  public NapCatResponse muteAll(String groupId, boolean enabled) {
    return post("set_group_whole_ban", Map.of("group_id", groupId, "enable", enabled));
  }

  public NapCatResponse sendGroupMessage(String groupId, String message) {
    return post("send_group_msg", Map.of("group_id", groupId, "message", message));
  }

  public NapCatResponse handleGroupRequest(String flag, boolean approve, String reason) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("flag", flag);
    body.put("sub_type", "add");
    body.put("approve", approve);
    if (reason != null && !reason.isBlank()) {
      body.put("reason", reason);
    }
    return post("set_group_add_request", body);
  }

  public NapCatResponse post(String action, Map<String, ?> body) {
    String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
    if (normalizedBaseUrl.isBlank()) {
      return NapCatResponse.failed("未配置 NapCat HTTP API 地址");
    }
    String normalizedAction = action.startsWith("/") ? action.substring(1) : action;
    String url = normalizedBaseUrl + "/" + normalizedAction;
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      if (accessToken != null && !accessToken.isBlank()) {
        headers.setBearerAuth(accessToken.trim());
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> response =
          restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
      return NapCatResponse.from(response);
    } catch (RestClientException ex) {
      log.warn("NapCat action failed: {}", action, ex);
      return NapCatResponse.failed(ex.getMessage());
    }
  }

  private String normalizeBaseUrl(String value) {
    String text = value == null ? "" : value.trim();
    while (text.endsWith("/")) {
      text = text.substring(0, text.length() - 1);
    }
    return text;
  }

  public record NapCatResponse(boolean ok, Object data, String message, Integer retcode) {
    static NapCatResponse failed(String message) {
      return new NapCatResponse(false, null, message == null ? "NapCat 调用失败" : message, null);
    }

    static NapCatResponse from(Map<String, Object> body) {
      if (body == null) {
        return failed("NapCat 返回为空");
      }
      Object retcodeValue = body.get("retcode");
      Integer retcode = retcodeValue instanceof Number number ? number.intValue() : null;
      String status = String.valueOf(body.getOrDefault("status", ""));
      boolean ok = retcode == null ? !"failed".equalsIgnoreCase(status) : retcode == 0;
      Object data = body.containsKey("data") ? body.get("data") : body;
      String message =
          String.valueOf(
              body.getOrDefault(
                  "message", body.getOrDefault("wording", ok ? "success" : "NapCat 调用失败")));
      return new NapCatResponse(ok, data, message, retcode);
    }
  }
}
