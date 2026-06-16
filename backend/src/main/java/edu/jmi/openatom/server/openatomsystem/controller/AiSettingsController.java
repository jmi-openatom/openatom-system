package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAiSettingsDTO;
import edu.jmi.openatom.server.openatomsystem.service.AiSettingsService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/settings")
public class AiSettingsController {
  private final AiSettingsService aiSettingsService;

  @GetMapping
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> settings() {
    return aiSettingsService.settings();
  }

  @PutMapping
  @SaCheckPermission("activity:create")
  public Result<Map<String, Object>> update(@RequestBody(required = false) RequestAiSettingsDTO request) {
    return aiSettingsService.update(request);
  }
}
