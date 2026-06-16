package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAiSettingsDTO;
import java.util.Map;

public interface AiSettingsService {
  Result<Map<String, Object>> settings();

  Result<Map<String, Object>> update(RequestAiSettingsDTO request);

  RuntimeSettings runtimeSettings();

  record RuntimeSettings(
      boolean enabled,
      String baseUrl,
      String apiKey,
      String model,
      int timeoutSeconds,
      boolean hasApiKey) {}
}
