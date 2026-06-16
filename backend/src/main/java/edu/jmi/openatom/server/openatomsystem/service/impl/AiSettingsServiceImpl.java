package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAiSettingsDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SystemSetting;
import edu.jmi.openatom.server.openatomsystem.mapper.SystemSettingMapper;
import edu.jmi.openatom.server.openatomsystem.service.AiSettingsService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiSettingsServiceImpl implements AiSettingsService {
  private static final String ENABLED_KEY = "ai.deepseek.enabled";
  private static final String BASE_URL_KEY = "ai.deepseek.base_url";
  private static final String API_KEY_KEY = "ai.deepseek.api_key";
  private static final String MODEL_KEY = "ai.deepseek.model";
  private static final String TIMEOUT_KEY = "ai.deepseek.timeout_seconds";

  private final SystemSettingMapper systemSettingMapper;

  @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}")
  private String defaultBaseUrl;

  @Value("${app.ai.deepseek.api-key:}")
  private String defaultApiKey;

  @Value("${app.ai.deepseek.model:deepseek-chat}")
  private String defaultModel;

  @Value("${app.ai.deepseek.timeout-seconds:30}")
  private Integer defaultTimeoutSeconds;

  @Override
  public Result<Map<String, Object>> settings() {
    return Result.success(maskedSettings(runtimeSettings()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> update(RequestAiSettingsDTO request) {
    if (request == null) request = new RequestAiSettingsDTO();
    if (request.getEnabled() != null) {
      save(ENABLED_KEY, String.valueOf(request.getEnabled()), "是否启用 DeepSeek AI");
    }
    if (notBlank(request.getBaseUrl())) {
      save(BASE_URL_KEY, request.getBaseUrl().trim(), "DeepSeek API Base URL");
    }
    if (notBlank(request.getModel())) {
      save(MODEL_KEY, request.getModel().trim(), "DeepSeek 模型名称");
    }
    if (request.getTimeoutSeconds() != null) {
      int timeout = Math.max(5, Math.min(request.getTimeoutSeconds(), 180));
      save(TIMEOUT_KEY, String.valueOf(timeout), "DeepSeek 超时时间（秒）");
    }
    if (notBlank(request.getApiKey())) {
      save(API_KEY_KEY, request.getApiKey().trim(), "DeepSeek API Key");
    }
    return Result.success(maskedSettings(runtimeSettings()), "AI 配置已保存");
  }

  @Override
  public RuntimeSettings runtimeSettings() {
    String baseUrl = value(BASE_URL_KEY, defaultBaseUrl);
    String apiKey = value(API_KEY_KEY, defaultApiKey);
    String model = value(MODEL_KEY, defaultModel);
    int timeout = parseInt(value(TIMEOUT_KEY, String.valueOf(defaultTimeoutSeconds)), 30);
    boolean hasApiKey = notBlank(apiKey);
    boolean enabled = Boolean.parseBoolean(value(ENABLED_KEY, String.valueOf(hasApiKey)));
    return new RuntimeSettings(enabled, baseUrl, apiKey, model, timeout, hasApiKey);
  }

  private Map<String, Object> maskedSettings(RuntimeSettings settings) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("enabled", settings.enabled());
    map.put("baseUrl", settings.baseUrl());
    map.put("model", settings.model());
    map.put("timeoutSeconds", settings.timeoutSeconds());
    map.put("hasApiKey", settings.hasApiKey());
    map.put("apiKeyMasked", mask(settings.apiKey()));
    return map;
  }

  private void save(String key, String value, String description) {
    SystemSetting setting =
        SystemSetting.builder().settingKey(key).settingValue(value).description(description).build();
    if (systemSettingMapper.selectById(key) == null) {
      systemSettingMapper.insert(setting);
    } else {
      systemSettingMapper.updateById(setting);
    }
  }

  private String value(String key, String fallback) {
    SystemSetting setting = systemSettingMapper.selectById(key);
    if (setting == null || !notBlank(setting.getSettingValue())) return fallback;
    return setting.getSettingValue();
  }

  private String mask(String value) {
    if (!notBlank(value)) return "";
    if (value.length() <= 8) return "****";
    return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
  }

  private int parseInt(String value, int fallback) {
    try {
      return Math.max(5, Integer.parseInt(value));
    } catch (Exception e) {
      return fallback;
    }
  }

  private boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
