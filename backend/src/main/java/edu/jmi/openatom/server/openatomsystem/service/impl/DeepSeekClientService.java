package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.entity.AiCallLog;
import edu.jmi.openatom.server.openatomsystem.mapper.AiCallLogMapper;
import edu.jmi.openatom.server.openatomsystem.service.AiSettingsService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** DeepSeek 服务端代理。未配置 API Key 时返回本地兜底内容，便于开发环境跑通链路。 */
@Service
@RequiredArgsConstructor
public class DeepSeekClientService {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AiCallLogMapper aiCallLogMapper;
  private final AiSettingsService aiSettingsService;

  public String chat(String scene, String systemPrompt, List<Map<String, String>> messages) {
    long start = System.currentTimeMillis();
    AiSettingsService.RuntimeSettings settings = aiSettingsService.runtimeSettings();
    String requestSummary = truncate(messages.isEmpty() ? "" : messages.getLast().get("content"), 900);
    try {
      String response =
          !settings.enabled() || !settings.hasApiKey()
              ? fallback(scene, messages)
              : callDeepSeek(settings, systemPrompt, messages);
      log(settings, scene, requestSummary, truncate(response, 900), "success", null, System.currentTimeMillis() - start);
      return response;
    } catch (Exception e) {
      log(settings, scene, requestSummary, null, "failed", truncate(e.getMessage(), 900), System.currentTimeMillis() - start);
      return fallback(scene, messages);
    }
  }

  private String callDeepSeek(
      AiSettingsService.RuntimeSettings settings,
      String systemPrompt,
      List<Map<String, String>> messages)
      throws IOException, InterruptedException {
    List<Map<String, String>> payloadMessages = new ArrayList<>();
    payloadMessages.add(Map.of("role", "system", "content", systemPrompt));
    payloadMessages.addAll(messages);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", settings.model());
    payload.put("messages", payloadMessages);
    payload.put("temperature", 0.4);
    payload.put("stream", false);
    String body = OBJECT_MAPPER.writeValueAsString(payload);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(settings.baseUrl().replaceAll("/+$", "") + "/chat/completions"))
            .timeout(Duration.ofSeconds(Math.max(5, settings.timeoutSeconds())))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + settings.apiKey())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
            .send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("DeepSeek 调用失败: HTTP " + response.statusCode());
    }
    Map<String, Object> responseBody =
        OBJECT_MAPPER.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
    Object choices = responseBody.get("choices");
    if (choices instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
      Object message = first.get("message");
      if (message instanceof Map<?, ?> messageMap && messageMap.get("content") != null) {
        return String.valueOf(messageMap.get("content"));
      }
    }
    throw new IOException("DeepSeek 响应格式不正确");
  }

  private String fallback(String scene, List<Map<String, String>> messages) {
    String latest =
        messages.isEmpty() || messages.getLast().get("content") == null
            ? "活动需求"
            : messages.getLast().get("content");
    if ("activity_plan".equals(scene) || "activity_plan_revision".equals(scene)) {
      return """
          # 活动策划案

          ## 一、活动名称
          待确认活动

          ## 二、活动背景
          根据当前需求：%s。本活动旨在围绕社团建设和成员参与，形成一次可执行、可复盘的校园活动。

          ## 三、活动目的与意义
          1. 增强参与者对社团方向和项目内容的了解。
          2. 提升成员之间的交流协作。
          3. 为后续招新、项目实践或社团运营沉淀活动经验。

          ## 四、活动时间
          待补充。

          ## 五、活动地点
          待补充。

          ## 六、活动对象
          待补充。

          ## 七、活动流程
          1. 签到入场与物料发放。
          2. 主持人开场并介绍活动背景。
          3. 社团项目展示或主题分享。
          4. 分组互动与任务体验。
          5. 总结交流与合影留念。

          ## 八、人员分工
          - 总负责人：待补充。
          - 签到引导：志愿者若干。
          - 现场执行：活动工作人员若干。
          - 宣传记录：宣传负责人或志愿者。

          ## 九、志愿者需求
          建议招募 4-8 名志愿者，负责签到、引导、秩序维护、拍摄记录和物资整理。

          ## 十、经费预算
          待补充。若无预算，可注明“本活动暂无经费支出”。

          ## 十一、安全预案
          活动前检查场地、设备和动线；活动中安排专人维护秩序；如遇突发情况，及时联系指导老师或学校相关部门。

          ## 十二、预期效果
          参与者能够了解活动主题和社团项目，提升参与意愿，并形成可用于后续宣传和复盘的活动材料。
          """.formatted(latest);
    }
    return """
        {
          "summary": "已收到你的活动需求：%s",
          "suggestions": [
            "建议先明确活动对象、活动时间、地点和预计人数。",
            "活动形式可以采用主题介绍、互动体验、分组任务和总结交流组合。",
            "如果需要生成申请材料，请提前准备活动负责人、联系方式、志愿者人数和预算信息。"
          ],
          "questions": [
            "活动预计什么时候举办？",
            "活动地点是否已经确定？",
            "预计参与人数和志愿者人数分别是多少？",
            "主办单位、负责人和联系电话是什么？"
          ],
          "missingFields": ["activityDate", "location", "expectedParticipants", "principalName", "principalPhone"]
        }
        """.formatted(latest.replace("\"", "\\\""));
  }

  private void log(
      AiSettingsService.RuntimeSettings settings,
      String scene,
      String requestSummary,
      String responseSummary,
      String status,
      String errorMessage,
      long durationMs) {
    Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    aiCallLogMapper.insert(
        AiCallLog.builder()
            .userId(userId)
            .scene(scene)
            .provider("deepseek")
            .model(settings.model())
            .promptVersion("v1")
            .requestSummary(requestSummary)
            .responseSummary(responseSummary)
            .status(status)
            .errorMessage(errorMessage)
            .durationMs(durationMs)
            .build());
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) return value;
    return value.substring(0, maxLength);
  }
}
