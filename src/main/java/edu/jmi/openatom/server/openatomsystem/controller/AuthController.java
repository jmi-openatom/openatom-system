package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestLogoutDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestRefreshTokenDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseCurrentUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseLoginDTO;
import edu.jmi.openatom.server.openatomsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public ApiResponse<String> register(@Valid @RequestBody RequestRegisterDTO requestRegisterDTO) {
    return authService.register(requestRegisterDTO);
  }

  @PostMapping("/login")
  public ApiResponse<ResponseLoginDTO> login(@Valid @RequestBody RequestLoginDTO requestLoginDTO) {
    return authService.login(requestLoginDTO);
  }

  @PostMapping("/refresh-token")
  public ApiResponse<ResponseLoginDTO> refresh(
      @Valid @RequestBody RequestRefreshTokenDTO requestRefreshTokenDTO) {
    return authService.refreshToken(
        requestRefreshTokenDTO == null ? null : requestRefreshTokenDTO.getRefreshToken());
  }

  @PostMapping("/logout")
  @SaCheckPermission("auth:logout")
  public ApiResponse<String> logout(@RequestBody(required = false) RequestLogoutDTO requestLogoutDTO) {
    return authService.logout(requestLogoutDTO == null ? null : requestLogoutDTO.getRefreshToken());
  }

  @GetMapping("/me")
  @SaCheckPermission("auth:me")
  public ApiResponse<ResponseCurrentUserDTO> me() {
    return authService.getCurrentUserInfo();
  }

  @PatchMapping("/register-enabled")
  public ApiResponse<Boolean> updateRegisterEnabled(@RequestParam Boolean enabled) {
    return authService.updateRegisterEnabled(enabled);
  }
}
