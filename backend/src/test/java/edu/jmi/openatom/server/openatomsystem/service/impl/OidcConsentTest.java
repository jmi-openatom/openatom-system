package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.entity.OauthAuthorizationCode;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthAuthorizationCodeMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthClientMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.OidcSigningKeyProvider;
import edu.jmi.openatom.server.openatomsystem.security.OidcUserSession;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class OidcConsentTest {
  private OidcServiceImpl service;
  private OauthAuthorizationCodeMapper codes;
  private ValueOperations<String, String> values;
  private AtomicReference<String> pending;
  private OidcUserSession session;

  @BeforeEach
  void setUp() {
    OauthClientMapper clients = mock(OauthClientMapper.class);
    codes = mock(OauthAuthorizationCodeMapper.class);
    UserMapper users = mock(UserMapper.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> mockedValues = mock(ValueOperations.class);
    values = mockedValues;
    pending = new AtomicReference<>();
    session = mock(OidcUserSession.class);
    when(session.isLogin()).thenReturn(true);
    when(session.userId()).thenReturn(12);
    when(session.tokenValue()).thenReturn("login-token");
    when(redis.opsForValue()).thenReturn(values);
    doAnswer(invocation -> {
      pending.set(invocation.getArgument(1));
      return null;
    }).when(values).set(anyString(), anyString(), any(Duration.class));
    when(values.get(anyString())).thenAnswer(invocation -> pending.get());
    when(values.getAndDelete(anyString())).thenAnswer(invocation -> pending.getAndSet(null));
    when(clients.selectByClientId("example-client")).thenReturn(OauthClient.builder()
        .clientId("example-client")
        .clientName("Example App")
        .redirectUris("https://app.example.test/callback")
        .scopes("openid profile")
        .grantTypes("authorization_code refresh_token")
        .enabled(true)
        .build());
    when(users.selectById(12)).thenReturn(User.builder().id(12).userName("student").realName("测试成员").build());
    service = new OidcServiceImpl(clients, codes, users, new PasswordService(),
        OidcSigningKeyProvider.ephemeral("consent-test"), session, redis, new ObjectMapper());
    ReflectionTestUtils.setField(service, "configuredIssuer", "https://oauth.example.test/api/v1");
    ReflectionTestUtils.setField(service, "mainSiteBaseUrl", "https://www.example.test");
  }

  @Test
  void authorizationCodeRequiresExplicitApprovalAndCannotBeReplayed() {
    String requestId = beginConsent();
    verify(codes, never()).insert(any(OauthAuthorizationCode.class));

    ResponseEntity<Map<String, Object>> details = service.consentRequest(requestId, consentRequest());
    assertEquals("Example App", details.getBody().get("client_name"));
    assertEquals("测试成员", details.getBody().get("account_name"));

    ResponseEntity<Map<String, Object>> approved = service.decideConsent(requestId, true, consentRequest());
    assertEquals(HttpStatus.OK, approved.getStatusCode());
    assertTrue(String.valueOf(approved.getBody().get("redirect_url")).contains("code="));
    verify(codes).insert(any(OauthAuthorizationCode.class));
    assertEquals(HttpStatus.BAD_REQUEST, service.decideConsent(requestId, true, consentRequest()).getStatusCode());
  }

  @Test
  void refusalReturnsAccessDeniedWithoutCode() {
    String requestId = beginConsent();
    ResponseEntity<Map<String, Object>> refused = service.decideConsent(requestId, false, consentRequest());
    assertTrue(String.valueOf(refused.getBody().get("redirect_url")).contains("error=access_denied"));
    verify(codes, never()).insert(any(OauthAuthorizationCode.class));
  }

  @Test
  void consentDecisionRequiresExplicitTokenHeader() {
    String requestId = beginConsent();
    assertEquals(HttpStatus.UNAUTHORIZED,
        service.decideConsent(requestId, true, new MockHttpServletRequest()).getStatusCode());
    verify(codes, never()).insert(any(OauthAuthorizationCode.class));
  }

  @Test
  void anotherAccountCannotApproveThePendingRequest() {
    String requestId = beginConsent();
    when(session.userId()).thenReturn(99);
    assertEquals(HttpStatus.FORBIDDEN, service.decideConsent(requestId, true, consentRequest()).getStatusCode());
    verify(codes, never()).insert(any(OauthAuthorizationCode.class));
  }

  private String beginConsent() {
    ResponseEntity<Void> started = service.authorize("code", "example-client",
        "https://app.example.test/callback", "openid profile", "state-1", "nonce-1",
        "challenge", "S256", new MockHttpServletRequest());
    assertEquals(HttpStatus.FOUND, started.getStatusCode());
    String location = started.getHeaders().getLocation().toString();
    assertTrue(location.startsWith("https://www.example.test/oauth/consent?request_id="));
    return location.substring(location.indexOf("request_id=") + "request_id=".length());
  }

  private MockHttpServletRequest consentRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("jmiopenatom", "login-token");
    return request;
  }

}
