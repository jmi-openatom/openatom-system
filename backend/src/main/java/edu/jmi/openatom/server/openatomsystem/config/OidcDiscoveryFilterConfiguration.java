package edu.jmi.openatom.server.openatomsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Serves the OIDC discovery document at {@code {issuer}/.well-known/openid-configuration}.
 *
 * <p>Stalwart Mail discovers this identity provider from its issuer URL. The regular MVC
 * controller for the discovery path is shadowed by the Sa-Token servlet filter chain in the
 * current deployment, so this filter answers the request directly with the same document the
 * controller would produce, and it is registered with the highest precedence so nothing can
 * intercept it.
 */
@Configuration
public class OidcDiscoveryFilterConfiguration {

  private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${app.oidc.issuer:}")
  private String issuer;

  @Bean
  public FilterRegistrationBean<Filter> oidcDiscoveryFilterRegistration() {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new OidcDiscoveryFilter());
    registration.addUrlPatterns(DISCOVERY_PATH);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  private class OidcDiscoveryFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      if ("GET".equalsIgnoreCase(httpRequest.getMethod())
          && DISCOVERY_PATH.equals(requestPath(httpRequest))) {
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        httpResponse.setContentType("application/json;charset=utf-8");
        httpResponse.setCharacterEncoding("utf-8");
        httpResponse.getWriter().write(discoveryDocument());
        return;
      }
      chain.doFilter(request, response);
    }
  }

  private String requestPath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    String path = request.getRequestURI();
    if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
      return path.substring(contextPath.length());
    }
    return path;
  }

  private String discoveryDocument() {
    String base =
        (issuer == null || issuer.isBlank()) ? "https://oauth.jmi-openatom.cn/api/v1" : issuer;
    Map<String, Object> document = new LinkedHashMap<>();
    document.put("issuer", base);
    document.put("authorization_endpoint", base + "/oauth/authorize");
    document.put("token_endpoint", base + "/oauth/token");
    document.put("userinfo_endpoint", base + "/oauth/userinfo");
    document.put("jwks_uri", base + "/oauth/jwks");
    document.put("introspection_endpoint", base + "/oauth/introspect");
    document.put("response_types_supported", List.of("code"));
    document.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
    document.put("subject_types_supported", List.of("public"));
    document.put("id_token_signing_alg_values_supported", List.of("RS256"));
    document.put("scopes_supported", List.of("openid", "profile", "email", "mail", "roles", "permissions"));
    document.put("token_endpoint_auth_methods_supported", List.of("none", "client_secret_post"));
    document.put("code_challenge_methods_supported", List.of("S256"));
    try {
      return objectMapper.writeValueAsString(document);
    } catch (Exception exception) {
      return "{}";
    }
  }
}
