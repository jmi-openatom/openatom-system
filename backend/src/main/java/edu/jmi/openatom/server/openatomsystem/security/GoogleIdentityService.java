package edu.jmi.openatom.server.openatomsystem.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityService {
  private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
  private static final List<String> GOOGLE_ISSUERS =
      List.of("accounts.google.com", "https://accounts.google.com");

  private final String clientId;
  private final NimbusJwtDecoder jwtDecoder;

  public GoogleIdentityService(@Value("${app.google.client-id:}") String clientId) {
    this.clientId = clientId == null ? "" : clientId.trim();
    this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
    OAuth2TokenValidator<Jwt> googleClaimsValidator = jwt -> {
      if (!GOOGLE_ISSUERS.contains(jwt.getIssuer() == null ? null : jwt.getIssuer().toString())) {
        return failure("invalid_issuer", "Google ID Token签发方无效");
      }
      if (this.clientId.isBlank() || !jwt.getAudience().contains(this.clientId)) {
        return failure("invalid_audience", "Google ID Token客户端不匹配");
      }
      return OAuth2TokenValidatorResult.success();
    };
    this.jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), googleClaimsValidator));
  }

  public GoogleIdentity verify(String credential) {
    if (clientId.isBlank()) throw new IllegalStateException("Google登录未配置");
    try {
      Jwt jwt = jwtDecoder.decode(credential);
      String subject = jwt.getSubject();
      String email = jwt.getClaimAsString("email");
      Boolean emailVerified = jwt.getClaim("email_verified");
      if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Google用户标识缺失");
      if (!Boolean.TRUE.equals(emailVerified)) throw new IllegalArgumentException("Google邮箱尚未验证");
      return new GoogleIdentity(
          subject,
          email,
          jwt.getClaimAsString("name"),
          jwt.getClaimAsString("picture"));
    } catch (JwtException exception) {
      throw new IllegalArgumentException("Google登录凭证无效或已过期", exception);
    }
  }

  private static OAuth2TokenValidatorResult failure(String code, String description) {
    return OAuth2TokenValidatorResult.failure(new OAuth2Error(code, description, null));
  }

  public record GoogleIdentity(String subject, String email, String name, String picture) {}
}
