package edu.jmi.openatom.server.openatomsystem.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 回归保护：带登录凭证访问前台个人接口时，响应必须禁止共享缓存。
 *
 * <p>对应线上事故：网关对 /api/v1/site/ 做了 30 秒共享缓存，缓存键不含用户身份，
 * 使账号激活页返回了其他成员的资料。
 */
class PrivateResponseCacheControlFilterTest {

  private final PrivateResponseCacheControlFilter filter = new PrivateResponseCacheControlFilter();

  @Test
  void marksPersonalSiteResponseAsNoStoreWhenTokenHeaderPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/site/activation");
    request.addHeader("jmiopenatom", "some-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control")).contains("no-store");
  }

  @Test
  void marksPersonalSiteResponseAsNoStoreWhenBearerAuthorizationPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/site/points/me");
    request.addHeader("Authorization", "Bearer some-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control")).contains("no-store");
  }

  @Test
  void marksPersonalSiteResponseAsNoStoreWhenSessionCookiePresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/site/progress");
    request.setCookies(new Cookie("jmiopenatom", "some-token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control")).contains("no-store");
  }

  @Test
  void leavesAnonymousPublicRequestCacheable() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/site/activities");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control")).isNull();
  }

  @Test
  void ignoresPathsOutsideSiteAndPublicNamespaces() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/clubs");
    request.addHeader("jmiopenatom", "some-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader("Cache-Control")).isNull();
  }
}
