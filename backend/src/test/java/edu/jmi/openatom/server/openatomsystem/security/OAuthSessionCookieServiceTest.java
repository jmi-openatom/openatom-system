package edu.jmi.openatom.server.openatomsystem.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OAuthSessionCookieServiceTest {

  @Test
  void centralLoginCookieIsHttpOnlySecureAndScopedToIssuerContext() {
    OAuthSessionCookieService service = new OAuthSessionCookieService("jmiopenatom");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("/api/v1");
    request.addHeader("X-Forwarded-Proto", "https");
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.write(request, response, "signed-token", 3600);

    String cookie = response.getHeader("Set-Cookie");
    assertTrue(cookie.contains("jmiopenatom=signed-token"));
    assertTrue(cookie.contains("Path=/api/v1"));
    assertTrue(cookie.contains("Max-Age=3600"));
    assertTrue(cookie.contains("Secure"));
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Lax"));
  }
}
