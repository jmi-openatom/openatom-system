package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import edu.jmi.openatom.server.openatomsystem.mapper.OauthAuthorizationCodeMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthClientMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.OidcSigningKeyProvider;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class OidcServiceImplTest {
  private static final String ISSUER = "https://oauth.example.test/api/v1";

  private OidcServiceImpl service;

  @BeforeEach
  void setUp() {
    OidcSigningKeyProvider keys = OidcSigningKeyProvider.ephemeral("test-key");
    service =
        new OidcServiceImpl(
            mock(OauthClientMapper.class),
            mock(OauthAuthorizationCodeMapper.class),
            mock(UserMapper.class),
            new PasswordService(),
            keys);
    ReflectionTestUtils.setField(service, "configuredIssuer", ISSUER);
    ReflectionTestUtils.setField(service, "resourceAudience", "stalwart");
  }

  @Test
  void discoveryAdvertisesOnlyAsymmetricSigningAndS256Pkce() {
    Map<String, Object> discovery = service.configuration(new MockHttpServletRequest());

    assertEquals(List.of("RS256"), discovery.get("id_token_signing_alg_values_supported"));
    assertEquals(List.of("S256"), discovery.get("code_challenge_methods_supported"));
    @SuppressWarnings("unchecked")
    List<String> scopes = (List<String>) discovery.get("scopes_supported");
    assertTrue(scopes.contains("mail"));
  }

  @Test
  void centralLoginUrlRedirectsToMainSiteLoginWithoutPropagatingLegacyTokenQuery() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setQueryString(
        "response_type=code&client_id=openatom-mail&jmiopenatom=secret-token&state=abc");

    String authorizeUrl = ReflectionTestUtils.invokeMethod(service, "authorizeUrl", request);
    String loginUrl = ReflectionTestUtils.invokeMethod(service, "loginUrl", authorizeUrl, request);

    assertFalse(authorizeUrl.contains("secret-token"));
    URI location = URI.create(loginUrl);
    assertEquals("www.jmi-openatom.cn", location.getHost());
    assertEquals("/login", location.getPath());
    assertTrue(loginUrl.contains("redirect="));
    assertFalse(loginUrl.contains("secret-token"));
    assertFalse(loginUrl.contains("/api/v1/oauth/login"));
  }

  @Test
  void jwksNeverContainsSymmetricOrPrivateKeyMaterial() {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> keys = (List<Map<String, Object>>) service.jwks().get("keys");

    assertEquals(1, keys.size());
    assertEquals("RSA", keys.getFirst().get("kty"));
    assertFalse(keys.getFirst().containsKey("k"));
    assertFalse(keys.getFirst().containsKey("d"));
  }
}