package edu.jmi.openatom.mail.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.mail.config.MailProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class OAuthClient {
  private final MailProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final NimbusJwtDecoder jwtDecoder;

  @Autowired
  public OAuthClient(MailProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  OAuthClient(MailProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(properties.getOauth().getJwksUrl())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
    this.jwtDecoder.setJwtValidator(
        JwtValidators.createDefaultWithIssuer(properties.getOauth().getIssuer()));
  }

  public TokenSet exchangeCode(String code, String verifier, String expectedNonce) {
    String form =
        form(
            "grant_type", "authorization_code",
            "code", code,
            "redirect_uri", properties.getOauth().getRedirectUri(),
            "client_id", properties.getOauth().getClientId(),
            "client_secret", properties.getOauth().getClientSecret(),
            "code_verifier", verifier);
    TokenSet tokens = tokenRequest(form);
    Jwt idToken = decodeIdToken(tokens.idToken(), expectedNonce);
    return tokens.withIdentity(
        idToken.getSubject(),
        idToken.getClaimAsString("name"),
        idToken.getId() == null ? idToken.getIssuedAt().toString() : idToken.getId(),
        rolesOf(idToken));
  }

  public TokenSet refresh(String refreshToken) {
    String form =
        form(
            "grant_type", "refresh_token",
            "refresh_token", refreshToken,
            "client_id", properties.getOauth().getClientId(),
            "client_secret", properties.getOauth().getClientSecret());
    TokenSet refreshed = tokenRequest(form);
    return refreshed.refreshToken() == null || refreshed.refreshToken().isBlank()
        ? refreshed.withRefreshToken(refreshToken)
        : refreshed;
  }

  private Jwt decodeIdToken(String encoded, String expectedNonce) {
    if (encoded == null || encoded.isBlank()) {
      throw new OAuthException("missing_id_token");
    }
    try {
      Jwt jwt = jwtDecoder.decode(encoded);
      List<String> audiences = jwt.getAudience();
      if (!audiences.contains(properties.getOauth().getClientId())) {
        throw new OAuthException("invalid_id_token_audience");
      }
      if (!expectedNonce.equals(jwt.getClaimAsString("nonce"))) {
        throw new OAuthException("invalid_id_token_nonce");
      }
      if (!"id".equals(jwt.getClaimAsString("token_use"))) {
        throw new OAuthException("invalid_id_token_use");
      }
      return jwt;
    } catch (JwtException exception) {
      throw new OAuthException("invalid_id_token", exception);
    }
  }

  private java.util.List<String> rolesOf(Jwt idToken) {
    Object claim = idToken.getClaim("roles");
    if (!(claim instanceof java.util.List<?> roles)) {
      return java.util.List.of();
    }
    java.util.List<String> result = new java.util.ArrayList<>();
    for (Object role : roles) {
      if (role instanceof String value && !value.isBlank()) {
        result.add(value);
      }
    }
    return result;
  }

  private TokenSet tokenRequest(String form) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(properties.getOauth().getTokenUrl()))
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(form))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new OAuthException("oauth_token_http_" + response.statusCode());
      }
      JsonNode body = objectMapper.readTree(response.body());
      String access = required(body, "access_token");
      long expiresIn = body.path("expires_in").asLong(900);
      return new TokenSet(
          access,
          text(body, "refresh_token"),
          text(body, "id_token"),
          Instant.now().plusSeconds(Math.max(30, expiresIn)),
          null,
          null,
          null,
          java.util.List.of());
    } catch (IOException exception) {
      throw new OAuthException("oauth_transport_error", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new OAuthException("oauth_transport_interrupted", exception);
    }
  }

  private String required(JsonNode body, String field) {
    String value = text(body, field);
    if (value == null || value.isBlank()) {
      throw new OAuthException("oauth_response_missing_" + field);
    }
    return value;
  }

  private String text(JsonNode body, String field) {
    JsonNode value = body.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private String form(String... values) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < values.length; index += 2) {
      if (!result.isEmpty()) {
        result.append('&');
      }
      result.append(URLEncoder.encode(values[index], StandardCharsets.UTF_8));
      result.append('=');
      result.append(URLEncoder.encode(values[index + 1], StandardCharsets.UTF_8));
    }
    return result.toString();
  }

  public record TokenSet(
      String accessToken,
      String refreshToken,
      String idToken,
      Instant expiresAt,
      String sub,
      String displayName,
      String identityEventId,
      java.util.List<String> roles) {
    TokenSet withIdentity(
        String sub, String displayName, String identityEventId, java.util.List<String> roles) {
      return new TokenSet(
          accessToken, refreshToken, idToken, expiresAt, sub, displayName, identityEventId, roles);
    }

    TokenSet withRefreshToken(String refresh) {
      return new TokenSet(
          accessToken, refresh, idToken, expiresAt, sub, displayName, identityEventId, roles);
    }
  }
}