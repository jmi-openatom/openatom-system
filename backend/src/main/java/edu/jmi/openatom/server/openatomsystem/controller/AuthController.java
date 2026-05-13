package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.*;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCurrentUserVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLoginVO;
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

/**
 * 认证授权控制器
 *
 * <p>提供注册, 登录, 登出, 令牌刷新及密码修改等接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  /**
   * 用户注册
   *
   * @param requestRegisterDTO 注册请求
   * @return 操作结果
   */
  @PostMapping("/register")
  public Result<String> register(@Valid @RequestBody RequestRegisterDTO requestRegisterDTO) {
    return authService.register(requestRegisterDTO);
  }

  /**
   * 用户登录
   *
   * @param requestLoginDTO 登录请求
   * @return 登录响应
   */
  @PostMapping("/login")
  public Result<ResponseLoginVO> login(@Valid @RequestBody RequestLoginDTO requestLoginDTO) {
    return authService.login(requestLoginDTO);
  }

  @PostMapping("/miniapp-login")
  public Result<ResponseLoginVO> miniappLogin(
      @Valid @RequestBody RequestMiniappLoginDTO requestMiniappLoginDTO) {
    return authService.miniappLogin(requestMiniappLoginDTO);
  }

  @PostMapping("/miniapp-bind")
  public Result<String> bindMiniapp(
      @Valid @RequestBody RequestMiniappLoginDTO requestMiniappLoginDTO) {
    return authService.bindMiniapp(requestMiniappLoginDTO);
  }

  /**
   * 刷新令牌
   *
   * @param requestRefreshTokenDTO 刷新令牌请求
   * @return 新的登录响应
   */
  @PostMapping("/refresh-token")
  public Result<ResponseLoginVO> refresh(
      @Valid @RequestBody RequestRefreshTokenDTO requestRefreshTokenDTO) {
    return authService.refreshToken(
        requestRefreshTokenDTO == null ? null : requestRefreshTokenDTO.getRefreshToken());
  }

  /**
   * 用户登出
   *
   * @param requestLogoutDTO 登出请求
   * @return 操作结果
   */
  @PostMapping("/logout")
  @SaCheckPermission("auth:logout")
  public Result<String> logout(
      @RequestBody(required = false) RequestLogoutDTO requestLogoutDTO) {
    return authService.logout(requestLogoutDTO == null ? null : requestLogoutDTO.getRefreshToken());
  }

  /**
   * 获取当前用户信息
   *
   * @return 当前用户信息
   */
  @GetMapping("/me")
  @SaCheckPermission("auth:me")
  public Result<ResponseCurrentUserVO> me() {
    return authService.getCurrentUserInfo();
  }

  /**
   * 更新注册开关
   *
   * @param enabled 是否开启注册
   * @return 更新后的注册开关状态
   */
  @PatchMapping("/register-enabled")
  public Result<Boolean> updateRegisterEnabled(@RequestParam Boolean enabled) {
    return authService.updateRegisterEnabled(enabled);
  }

  /**
   * 修改密码
   *
   * @param requestChangePassword 修改密码请求
   * @return 操作结果
   */
  @PatchMapping("/password")
  public Result<String> changePassword(
      @RequestBody RequestChangePasswordDTO requestChangePassword) {
    return authService.changePassword(requestChangePassword);
  }
}
