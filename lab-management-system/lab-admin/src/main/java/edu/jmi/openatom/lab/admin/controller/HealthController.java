package edu.jmi.openatom.lab.admin.controller;

import edu.jmi.openatom.lab.common.web.Result;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  @GetMapping("/api/health")
  public Result<Map<String, String>> health() {
    return Result.success(Map.of("status", "ok", "service", "lab-management-system"));
  }
}
