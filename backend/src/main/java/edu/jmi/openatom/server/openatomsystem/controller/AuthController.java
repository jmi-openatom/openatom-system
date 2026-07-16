package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.*;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCurrentUserVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLoginVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseGroupJoinTokenVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseGroupJoinVerifyVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseQqBindTokenVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseTokenIntrospectionVO;
import edu.jmi.openatom.server.openatomsystem.service.AuthCenterService;
import edu.jmi.openatom.server.openatomsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
  private final AuthCenterService authCenterService;

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

  @PostMapping("/google-login")
  public Result<ResponseLoginVO> googleLogin(
      @Valid @RequestBody RequestGoogleLoginDTO requestGoogleLoginDTO) {
    return authService.googleLogin(requestGoogleLoginDTO);
  }

  @PostMapping("/google-bind")
  @SaCheckPermission("auth:me")
  public Result<String> bindGoogle(
      @Valid @RequestBody RequestGoogleLoginDTO requestGoogleLoginDTO) {
    return authService.bindGoogle(requestGoogleLoginDTO);
  }

  @GetMapping("/github-login-url")
  public Result<java.util.Map<String, String>> githubLoginUrl(
      @RequestParam(required = false) String redirect) {
    return authService.githubLoginUrl(redirect);
  }

  @GetMapping("/github-bind-url")
  @SaCheckPermission("auth:me")
  public Result<java.util.Map<String, String>> githubBindUrl() {
    return authService.githubBindUrl();
  }

  @PostMapping("/github-callback")
  public Result<java.util.Map<String, Object>> githubCallback(
      @Valid @RequestBody RequestGithubCallbackDTO request) {
    return authService.githubCallback(request);
  }

  @GetMapping("/gitee-login-url")
  public Result<java.util.Map<String, String>> giteeLoginUrl(@RequestParam(required = false) String redirect) {
    return authService.giteeLoginUrl(redirect);
  }

  @GetMapping("/gitee-bind-url")
  @SaCheckPermission("auth:me")
  public Result<java.util.Map<String, String>> giteeBindUrl() {
    return authService.giteeBindUrl();
  }

  @PostMapping("/gitee-callback")
  public Result<java.util.Map<String, Object>> giteeCallback(
      @Valid @RequestBody RequestGithubCallbackDTO request) {
    return authService.giteeCallback(request);
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

  @PostMapping("/qq-bind-token")
  public Result<ResponseQqBindTokenVO> createQqBindToken() {
    return authService.createQqBindToken();
  }

  @PostMapping("/qq-bind/confirm")
  public Result<String> confirmQqBind(
      @Valid @RequestBody RequestConfirmQqBindDTO requestConfirmQqBindDTO) {
    return authService.confirmQqBind(requestConfirmQqBindDTO);
  }

  @GetMapping("/qq-bind/status")
  public Result<Boolean> qqBindStatus(@RequestParam String qqOpenid) {
    return authService.isQqBound(qqOpenid);
  }

  @PostMapping("/qq-bind")
  public Result<String> confirmQqBindCompat(
      @Valid @RequestBody RequestConfirmQqBindDTO requestConfirmQqBindDTO) {
    return authService.confirmQqBind(requestConfirmQqBindDTO);
  }

  @DeleteMapping("/qq-bind")
  public Result<String> unbindQq() {
    return authService.unbindQq();
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

  @PostMapping("/introspect")
  public Result<ResponseTokenIntrospectionVO> introspect(@RequestParam(required = false) String token) {
    return authCenterService.introspect(token);
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
   * 更新激活页开关
   *
   * @param enabled 是否启用激活页引导流程
   * @return 更新后的激活页开关状态
   */
  @PatchMapping("/activation-enabled")
  public Result<Boolean> updateActivationEnabled(@RequestParam Boolean enabled) {
    return authService.updateActivationEnabled(enabled);
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

  /** Uploads the current user's avatar. */
  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SaCheckPermission("auth:me")
  public Result<User> uploadAvatar(@RequestParam("file") MultipartFile file) {
    String avatarBaseUrl =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/files/avatars/")
            .toUriString();
    return authService.updateAvatar(file, avatarBaseUrl);
  }

  /** Clears the current user's avatar and falls back to the generated surname avatar. */
  @DeleteMapping("/avatar")
  @SaCheckPermission("auth:me")
  public Result<User> removeAvatar() {
    return authService.removeAvatar();
  }

  /** Updates the current user's own profile (real name, contact, college info). */
  @PatchMapping("/profile")
  @SaCheckPermission("auth:me")
  public Result<User> updateProfile(@Valid @RequestBody RequestUpdateProfileDTO requestUpdateProfileDTO) {
    return authService.updateProfile(requestUpdateProfileDTO);
  }

  /** Marks the current user's onboarding (club introduction) as completed. */
  @PostMapping("/onboarding/complete")
  @SaCheckPermission("auth:me")
  public Result<User> completeOnboarding() {
    return authService.completeOnboarding();
  }

  /** Activates the current user's account after they acknowledge club membership. */
  @PostMapping("/activate")
  @SaCheckPermission("auth:me")
  public Result<User> activate() {
    return authService.activate();
  }

  /** Verifies a group join token and returns user info for the bot to set group card. */
  @GetMapping("/group-join/verify")
  public Result<ResponseGroupJoinVerifyVO> verifyGroupJoinToken(@RequestParam String token) {
    return authService.verifyGroupJoinToken(token);
  }

  /** 机器人审批通过后调用，标记用户已入群，并在提供 QQ 时同步完成账号绑定。 */
  @GetMapping("/group-join/confirm")
  public Result<String> confirmGroupJoin(
      @RequestParam String token, @RequestParam(required = false) String qqOpenid) {
    return authService.confirmGroupJoin(token, qqOpenid);
  }

  /** Generates a one-time token for QQ group join verification. */
  @PostMapping("/group-join-token")
  @SaCheckPermission("auth:me")
  public Result<ResponseGroupJoinTokenVO> createGroupJoinToken() {
    return authService.createGroupJoinToken();
  }
}
