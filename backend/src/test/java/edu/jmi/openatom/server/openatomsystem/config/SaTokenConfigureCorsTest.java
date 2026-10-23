package edu.jmi.openatom.server.openatomsystem.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class SaTokenConfigureCorsTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "cache-control,pragma",
      "authorization,cache-control,jmiopenatom,pragma"
  })
  void allowsBlogNoStorePreflightForAnonymousAndLoggedInReaders(String requestedHeaders)
      throws Exception {
    SaTokenConfigure configure = new SaTokenConfigure(null, null, null, null);
    ReflectionTestUtils.setField(configure, "allowedOriginPatterns",
        new String[] {"https://jmi-openatom.cn"});
    MockHttpServletRequest request = new MockHttpServletRequest(
        "OPTIONS", "/api/v1/site/blog/articles");
    request.addHeader("Origin", "https://jmi-openatom.cn");
    request.addHeader("Access-Control-Request-Method", "GET");
    request.addHeader("Access-Control-Request-Headers", requestedHeaders);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    configure.corsFilterRegistrationBean().getFilter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("https://jmi-openatom.cn");
    assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    assertThat(Arrays.asList(response.getHeader("Access-Control-Allow-Headers").split(",\\s*")))
        .contains(requestedHeaders.split(","));
    // 预检在进入业务接口和登录拦截器前结束。
    assertThat(chain.getRequest()).isNull();
  }
}
