package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.jmi.openatom.server.openatomsystem.vo.ResponseCaptchaVO;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CaptchaServiceImplTest {
  private static final String CAPTCHA_KEY_PREFIX = "openatom:captcha:slider:";

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOps;
  private CaptchaServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    service = new CaptchaServiceImpl(redisTemplate);
  }

  @Test
  void generateReturnsImagesAndStoresTargetX() {
    ResponseCaptchaVO captcha = service.generate();

    assertNotNull(captcha.getCaptchaId());
    assertTrue(captcha.getBackgroundBase64().startsWith("data:image/png;base64,"));
    assertTrue(captcha.getPieceBase64().startsWith("data:image/png;base64,"));
    assertTrue(captcha.getPieceY() > 0);
    ArgumentCaptor<String> targetCaptor = ArgumentCaptor.forClass(String.class);
    verify(valueOps).set(eq(CAPTCHA_KEY_PREFIX + captcha.getCaptchaId()), targetCaptor.capture(),
        any(Duration.class));
    assertTrue(Integer.parseInt(targetCaptor.getValue()) > 0);
  }

  @Test
  void verifyAcceptsPositionWithinTolerance() {
    when(valueOps.get(anyString())).thenReturn("120");

    assertTrue(service.verify("id-1", 120));
    assertTrue(service.verify("id-1", 124));
    assertTrue(service.verify("id-1", 116));
  }

  @Test
  void verifyRejectsPositionOutsideTolerance() {
    when(valueOps.get(anyString())).thenReturn("120");

    assertFalse(service.verify("id-1", 130));
    assertFalse(service.verify("id-1", 110));
  }

  @Test
  void verifyFailsForUnknownOrBlankCaptcha() {
    when(valueOps.get(anyString())).thenReturn(null);

    assertFalse(service.verify("id-1", 120));
    assertFalse(service.verify("", 120));
    assertFalse(service.verify("id-1", -5));
  }

  @Test
  void verifyConsumesCaptchaAfterAttempt() {
    when(valueOps.get(anyString())).thenReturn("80");

    assertTrue(service.verify("id-1", 80));
    verify(redisTemplate).delete(CAPTCHA_KEY_PREFIX + "id-1");
  }
}