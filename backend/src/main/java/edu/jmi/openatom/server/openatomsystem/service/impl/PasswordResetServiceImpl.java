package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPasswordResetDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPasswordResetSendCodeDTO;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.CaptchaService;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import edu.jmi.openatom.server.openatomsystem.service.PasswordResetService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 找回密码服务实现
 *
 * <p>验证码存 Redis（5 分钟过期），同一账号 60 秒限发一次，验证码最多错误 5 次；
 * 账号不存在时不发送邮件但返回成功（防枚举）；重置成功后作废该用户的会话和刷新令牌
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
  private static final String CODE_KEY_PREFIX = "openatom:pwd-reset:code:";
  private static final String SEND_LIMIT_KEY_PREFIX = "openatom:pwd-reset:send-limit:";
  private static final String ATTEMPT_KEY_PREFIX = "openatom:pwd-reset:attempt:";
  private static final String REFRESH_TOKEN_KEY_PREFIX = "openatom:refresh:token:";
  private static final String REFRESH_USER_KEY_PREFIX = "openatom:refresh:user:";
  private static final long CODE_TTL_SECONDS = 5 * 60L;
  private static final long SEND_LIMIT_TTL_SECONDS = 60L;
  private static final int MAX_ATTEMPTS = 5;
  private static final int CODE_LENGTH = 6;

  private final UserMapper userMapper;
  private final PasswordService passwordService;
  private final MailBroadcastPlanner mailBroadcastPlanner;
  private final StringRedisTemplate redisTemplate;
  private final CaptchaService captchaService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public Result<String> sendCode(RequestPasswordResetSendCodeDTO request) {
    String account = normalize(request == null ? null : request.getAccount());
    if (account == null) {
      return Result.error("请输入账号或邮箱");
    }
    if (request.getCaptchaId() == null
        || request.getCaptchaId().isBlank()
        || request.getCaptchaValue() == null) {
      return Result.error("请先完成滑块验证");
    }
    if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaValue())) {
      return Result.error("滑块验证未通过，请重试");
    }
    Boolean acquired =
        redisTemplate
            .opsForValue()
            .setIfAbsent(
                SEND_LIMIT_KEY_PREFIX + account,
                "1",
                Duration.ofSeconds(SEND_LIMIT_TTL_SECONDS));
    if (acquired == null || !acquired) {
      return Result.error("发送过于频繁，请 1 分钟后再试");
    }
    User user = findUser(account);
    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
      log.info("password reset code requested for account without email, ignored");
      return Result.success("验证码已发送，请查收邮件");
    }
    String code = generateCode();
    redisTemplate
        .opsForValue()
        .set(CODE_KEY_PREFIX + user.getId(), code, Duration.ofSeconds(CODE_TTL_SECONDS));
    redisTemplate.delete(ATTEMPT_KEY_PREFIX + user.getId());
    mailBroadcastPlanner.enqueueUserMailHtml(
        "password_reset_" + user.getId() + "_" + System.currentTimeMillis(),
        user.getId(),
        "【开放原子开源社团】重置密码验证码",
        buildCodeMailHtml(code));
    return Result.success("验证码已发送，请查收邮件");
  }

  @Override
  public Result<String> reset(RequestPasswordResetDTO request) {
    String account = normalize(request == null ? null : request.getAccount());
    String code = request == null ? null : request.getCode();
    String newPassword = request == null ? null : request.getNewPassword();
    if (account == null) {
      return Result.error("请输入账号或邮箱");
    }
    if (code == null || code.isBlank()) {
      return Result.error("请输入验证码");
    }
    if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
      return Result.error("密码长度必须在8到72个字符之间");
    }
    User user = findUser(account);
    if (user == null) {
      return Result.error("验证码无效或已过期");
    }
    String key = CODE_KEY_PREFIX + user.getId();
    String stored = redisTemplate.opsForValue().get(key);
    if (stored == null) {
      return Result.error("验证码无效或已过期");
    }
    String attemptKey = ATTEMPT_KEY_PREFIX + user.getId();
    Long attempts = redisTemplate.opsForValue().increment(attemptKey);
    redisTemplate.expire(attemptKey, Duration.ofSeconds(CODE_TTL_SECONDS));
    if (attempts != null && attempts > MAX_ATTEMPTS) {
      redisTemplate.delete(key);
      return Result.error("验证码错误次数过多，请重新获取");
    }
    if (!MessageDigest.isEqual(
        stored.getBytes(StandardCharsets.UTF_8), code.trim().getBytes(StandardCharsets.UTF_8))) {
      return Result.error("验证码错误");
    }
    user.setPassword(passwordService.encode(newPassword));
    userMapper.updateById(user);
    redisTemplate.delete(key);
    redisTemplate.delete(attemptKey);
    invalidateSessions(user.getId());
    log.info("password reset completed: userId={}", user.getId());
    return Result.success("密码重置成功，请使用新密码登录");
  }

  private User findUser(String account) {
    User user = userMapper.selectByStudentIdOrUserName(account);
    if (user != null) {
      return user;
    }
    return userMapper.selectByEmail(account);
  }

  private String generateCode() {
    StringBuilder builder = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      builder.append(secureRandom.nextInt(10));
    }
    return builder.toString();
  }

  private String buildCodeMailHtml(String code) {
    return """
        <div style="max-width:520px;margin:0 auto;padding:24px;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;color:#1f2937;line-height:1.6;">
          <h2 style="font-size:20px;margin:0 0 16px;">重置密码</h2>
          <p style="margin:0 0 16px;">同学，你好：</p>
          <p style="margin:0 0 16px;">你正在申请重置登录密码，本次验证码为：</p>
          <p style="margin:0 0 16px;font-size:28px;font-weight:700;letter-spacing:6px;color:#2563eb;">%s</p>
          <p style="margin:0 0 16px;">验证码 5 分钟内有效，请勿将验证码泄露给他人。</p>
          <p style="margin:0 0 16px;">如果不是你本人操作，请忽略此邮件，你的账号仍然是安全的。</p>
          <p style="margin:0;color:#6b7280;font-size:13px;">—— 开放原子开源社团</p>
        </div>
        """
        .formatted(code);
  }

  private void invalidateSessions(int userId) {
    try {
      StpUtil.logout(userId);
    } catch (Exception exception) {
      log.warn("password reset: session logout failed for userId={}: {}", userId,
          exception.getMessage());
    }
    try {
      SaTokenDao tokenDao = SaManager.getSaTokenDao();
      String userKey = REFRESH_USER_KEY_PREFIX + userId;
      String refreshToken = tokenDao.get(userKey);
      if (refreshToken != null) {
        tokenDao.delete(REFRESH_TOKEN_KEY_PREFIX + refreshToken);
      }
      tokenDao.delete(userKey);
    } catch (Exception exception) {
      log.warn("password reset: refresh token cleanup failed for userId={}: {}", userId,
          exception.getMessage());
    }
  }

  private String normalize(String account) {
    if (account == null) {
      return null;
    }
    String trimmed = account.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}