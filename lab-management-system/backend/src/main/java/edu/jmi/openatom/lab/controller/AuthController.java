package edu.jmi.openatom.lab.controller;

import edu.jmi.openatom.lab.common.ApiResponse;
import edu.jmi.openatom.lab.domain.LabModels.CmsLoginRequest;
import edu.jmi.openatom.lab.domain.LabModels.SessionResponse;
import edu.jmi.openatom.lab.service.LabStore;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final LabStore labStore;

  public AuthController(LabStore labStore) {
    this.labStore = labStore;
  }

  @GetMapping("/login-url")
  public ApiResponse<Map<String, String>> loginUrl(@RequestParam String redirectUri) {
    return ApiResponse.ok(Map.of("url", labStore.cmsAuthorizeUrl(redirectUri)));
  }

  @PostMapping("/cms-callback")
  public ApiResponse<SessionResponse> cmsCallback(@Valid @RequestBody CmsLoginRequest request) {
    return ApiResponse.ok(labStore.loginFromCms(request));
  }
}
