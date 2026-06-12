package edu.jmi.openatom.lab.admin.controller;

import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.common.web.Result;
import edu.jmi.openatom.lab.framework.auth.LabSecurityContext;
import edu.jmi.openatom.lab.framework.oauth.CmsOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
  private final CmsOAuthService cmsOAuthService;

  @GetMapping("/oauth/url")
  public Result<LabDtos.AuthUrlResponse> oauthUrl() {
    return Result.success(cmsOAuthService.authorizeUrl());
  }

  @PostMapping("/oauth/callback")
  public Result<LabDtos.LoginResponse> oauthCallback(@Valid @RequestBody LabDtos.AuthCallbackRequest request) {
    return Result.success(cmsOAuthService.callback(request.code()));
  }

  @PostMapping("/dev-login")
  public Result<LabDtos.LoginResponse> devLogin(@Valid @RequestBody LabDtos.DevLoginRequest request) {
    return Result.success(cmsOAuthService.devLogin(request));
  }

  @GetMapping("/me")
  public Result<LabDtos.LabUserView> me() {
    return Result.success(cmsOAuthService.me(LabSecurityContext.userId()));
  }
}
