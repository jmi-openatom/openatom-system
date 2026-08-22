package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPasswordResetDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPasswordResetSendCodeDTO;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.CaptchaService;
import edu.jmi.openatom.server.openatomsystem.service.MailBroadcastPlanner;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class PasswordResetServiceImplTest {
  private static final String CODE_KEY = "openatom:pwd-reset:code:7";
  private static final String ATTEMPT_KEY = "openatom:pwd-reset:attempt:7";
  private static final String CAPTCHA_ID = "captcha-1";

  private UserMapper userMapper;
  private PasswordService passwordService;
  private MailBroadcastPlanner mailBroadcastPlanner;
  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOps;
  private CaptchaService captchaService;
  private PasswordResetServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    userMapper = mock(UserMapper.class);
    passwordService = mock(PasswordService.class);
    mailBroadcastPlanner = mock(MailBroadcastPlanner.class);
    redisTemplate = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    captchaService = mock(CaptchaService.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(captchaService.verify(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(true);
    service =
        new PasswordResetServiceImpl(userMapper, passwordService, mailBroadcastPlanner,
            redisTemplate, captchaService);
  }

  private RequestPasswordResetSendCodeDTO sendCodeRequest(String account) {
    return RequestPasswordResetSendCodeDTO.builder()
        .account(account)
        .captchaId(CAPTCHA_ID)
        .captchaValue(120)
        .build();
  }

  @Test
  void sendCodeStoresCodeAndEnqueuesMailForExistingUser() {
    User user = User.builder().id(7).userName("member").email("member@example.com").build();
    when(userMapper.selectByStudentIdOrUserName("member")).thenReturn(user);
    when(valueOps.setIfAbsent(eq("openatom:pwd-reset:send-limit:member"), eq("1"), any(Duration.class)))
        .thenReturn(true);

    Result<String> result = service.sendCode(sendCodeRequest("member"));

    assertEquals(0, result.getCode());
    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(valueOps).set(eq(CODE_KEY), codeCaptor.capture(), any(Duration.class));
    assertTrue(codeCaptor.getValue().matches("\\d{6}"));
    verify(mailBroadcastPlanner)
        .enqueueUserMailHtml(anyString(), eq(7), anyString(), org.mockito.ArgumentMatchers.contains(codeCaptor.getValue()));
  }

  @Test
  void sendCodeRejectsWhenSliderCaptchaFails() {
    when(captchaService.verify(anyString(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(false);

    Result<String> result = service.sendCode(sendCodeRequest("member"));

    assertEquals(50000, result.getCode());
    assertEquals("滑块验证未通过，请重试", result.getMessage());
    verify(mailBroadcastPlanner, never()).enqueueUserMailHtml(any(), any(), any(), any());
  }

  @Test
  void sendCodeAlwaysSucceedsForUnknownAccountWithoutEnqueueing() {
    when(userMapper.selectByStudentIdOrUserName("nobody")).thenReturn(null);
    when(userMapper.selectByEmail("nobody")).thenReturn(null);
    when(valueOps.setIfAbsent(eq("openatom:pwd-reset:send-limit:nobody"), eq("1"), any(Duration.class)))
        .thenReturn(true);

    Result<String> result = service.sendCode(sendCodeRequest("nobody"));

    assertEquals(0, result.getCode());
    assertEquals("验证码已发送，请查收邮件", result.getData());
    verify(mailBroadcastPlanner, never()).enqueueUserMailHtml(any(), any(), any(), any());
  }

  @Test
  void sendCodeRejectsRequestsWithinOneMinute() {
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

    Result<String> result = service.sendCode(sendCodeRequest("member"));

    assertEquals(50000, result.getCode());
    assertEquals("发送过于频繁，请 1 分钟后再试", result.getMessage());
    verify(mailBroadcastPlanner, never()).enqueueUserMailHtml(any(), any(), any(), any());
  }

  @Test
  void sendCodeSkipsMailWhenUserHasNoEmail() {
    User user = User.builder().id(7).userName("member").build();
    when(userMapper.selectByStudentIdOrUserName("member")).thenReturn(user);
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

    Result<String> result = service.sendCode(sendCodeRequest("member"));

    assertEquals(0, result.getCode());
    verify(mailBroadcastPlanner, never()).enqueueUserMailHtml(any(), any(), any(), any());
  }

  @Test
  void resetUpdatesPasswordAndClearsCodeForValidRequest() {
    User user = User.builder().id(7).userName("member").email("member@example.com").build();
    when(userMapper.selectByStudentIdOrUserName("member")).thenReturn(user);
    when(valueOps.get(CODE_KEY)).thenReturn("123456");
    when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);
    when(passwordService.encode("new-password-123")).thenReturn("$2a$10$encoded");
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

    service.sendCode(sendCodeRequest("member"));
    Result<String> result =
        service.reset(
            RequestPasswordResetDTO.builder()
                .account("member")
                .code("123456")
                .newPassword("new-password-123")
                .build());

    assertEquals(0, result.getCode());
    verify(passwordService).encode("new-password-123");
    assertEquals("$2a$10$encoded", user.getPassword());
    verify(userMapper).updateById(user);
    verify(redisTemplate).delete(CODE_KEY);
    verify(redisTemplate, org.mockito.Mockito.times(2)).delete(ATTEMPT_KEY);
  }

  @Test
  void resetRejectsWrongCode() {
    User user = User.builder().id(7).userName("member").build();
    when(userMapper.selectByStudentIdOrUserName("member")).thenReturn(user);
    when(valueOps.get(CODE_KEY)).thenReturn("123456");
    when(valueOps.increment(ATTEMPT_KEY)).thenReturn(1L);

    Result<String> result =
        service.reset(
            RequestPasswordResetDTO.builder()
                .account("member")
                .code("000000")
                .newPassword("new-password-123")
                .build());

    assertEquals(50000, result.getCode());
    assertEquals("验证码错误", result.getMessage());
    verify(userMapper, never()).updateById(any());
  }

  @Test
  void resetRejectsExpiredOrMissingCode() {
    User user = User.builder().id(7).userName("member").build();
    when(userMapper.selectByStudentIdOrUserName("member")).thenReturn(user);
    when(valueOps.get(CODE_KEY)).thenReturn(null);

    Result<String> result =
        service.reset(
            RequestPasswordResetDTO.builder()
                .account("member")
                .code("123456")
                .newPassword("new-password-123")
                .build());

    assertEquals(50000, result.getCode());
    assertEquals("验证码无效或已过期", result.getMessage());
    verify(userMapper, never()).updateById(any());
  }

  @Test
  void resetClearsCodeAfterTooManyFailedAttempts() {
    User user = User.builder().id(7).userName("member").build();
    when(userMapper.selectByStudentIdOrUserName("member")).thenReturn(user);
    when(valueOps.get(CODE_KEY)).thenReturn("123456");
    when(valueOps.increment(ATTEMPT_KEY)).thenReturn(6L);

    Result<String> result =
        service.reset(
            RequestPasswordResetDTO.builder()
                .account("member")
                .code("000000")
                .newPassword("new-password-123")
                .build());

    assertEquals(50000, result.getCode());
    assertEquals("验证码错误次数过多，请重新获取", result.getMessage());
    verify(redisTemplate).delete(CODE_KEY);
    verify(userMapper, never()).updateById(any());
  }

  @Test
  void resetRejectsShortPassword() {
    Result<String> result =
        service.reset(
            RequestPasswordResetDTO.builder()
                .account("member")
                .code("123456")
                .newPassword("short")
                .build());

    assertEquals(50000, result.getCode());
    assertEquals("密码长度必须在8到72个字符之间", result.getMessage());
  }
}