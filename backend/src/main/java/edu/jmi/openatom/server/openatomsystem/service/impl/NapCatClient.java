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
import org.springframework.web.client.HttpClientErrorException;
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

  public NapCatResponse getGroupMemberInfo(String groupId, String userId) {
    return post("get_group_member_info", Map.of("group_id", groupId, "user_id", userId, "no_cache", false));
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

  public NapCatResponse sendGroupNotice(String groupId, String content) {
    return post("_send_group_notice", Map.of("group_id", groupId, "content", content));
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
    } catch (HttpClientErrorException.Forbidden ex) {
      String responseBody = ex.getResponseBodyAsString();
      if (responseBody != null && responseBody.contains("token verify failed")) {
        return NapCatResponse.failed("NapCat 访问令牌不正确，请确认 APP_NAPCAT_ACCESS_TOKEN 与 NapCat HTTP 服务端 token 一致。");
      }
      return NapCatResponse.failed("NapCat HTTP 403：" + ex.getStatusText());
    } catch (HttpClientErrorException.NotFound ex) {
      String responseBody = ex.getResponseBodyAsString();
      if (responseBody != null && responseBody.contains("Cannot POST /" + normalizedAction)) {
        return NapCatResponse.failed(
            "当前 NapCat 地址不是 OneBot HTTP API 服务端，可能连到了 WebUI 或反向 WebSocket 端口。"
                + "请在 NapCat 启用 HTTP 服务端后设置 APP_NAPCAT_BASE_URL，例如 http://napcat:3000。");
      }
      return NapCatResponse.failed("NapCat HTTP 404：" + ex.getStatusText());
    } catch (RestClientException ex) {
      log.warn("NapCat action failed: {}", action, ex);
      String message = ex.getMessage();
      if (message != null && message.contains("Connection refused")) {
        return NapCatResponse.failed(
            "无法连接 NapCat HTTP 服务端，请确认 NapCat 网络配置里的 HTTP 服务器已启用，主机为 0.0.0.0，端口为 3000。");
      }
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
