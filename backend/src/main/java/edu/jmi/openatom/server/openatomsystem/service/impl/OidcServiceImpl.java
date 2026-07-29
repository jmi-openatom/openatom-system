package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpUtil;
import com.nimbusds.jwt.JWTClaimsSet;
import edu.jmi.openatom.server.openatomsystem.entity.OauthAuthorizationCode;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthAuthorizationCodeMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthClientMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.OidcSigningKeyProvider;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.OidcService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseTokenIntrospectionVO;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OidcServiceImpl implements OidcService {
  private static final long AUTH_CODE_TTL_SECONDS = 5 * 60L;
  private static final long OIDC_TOKEN_TTL_SECONDS = 60 * 60L;
  private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60L;
  private static final String REFRESH_KEY_PREFIX = "openatom:oidc:refresh:";

  private final OauthClientMapper oauthClientMapper;
  private final OauthAuthorizationCodeMapper authorizationCodeMapper;
  private final UserMapper userMapper;
  private final PasswordService passwordService;
  private final OidcSigningKeyProvider signingKeyProvider;

  @Value("${app.oidc.issuer:}")
  private String configuredIssuer;

  @Value("${app.oidc.resource-audience:stalwart}")
  private String resourceAudience;

  @Override
  public Map<String, Object> configuration(HttpServletRequest request) {
    String issuer = issuer(request);
    return ordered(
        "issuer", issuer,
        "authorization_endpoint", issuer + "/oauth/authorize",
        "token_endpoint", issuer + "/oauth/token",
        "userinfo_endpoint", issuer + "/oauth/userinfo",
        "jwks_uri", issuer + "/oauth/jwks",
        "introspection_endpoint", issuer + "/oauth/introspect",
        "response_types_supported", List.of("code"),
        "grant_types_supported", List.of("authorization_code", "refresh_token"),
        "subject_types_supported", List.of("public"),
        "id_token_signing_alg_values_supported", List.of("RS256"),
        "scopes_supported", List.of("openid", "profile", "email", "mail", "roles", "permissions"),
        "token_endpoint_auth_methods_supported", List.of("none", "client_secret_post"),
        "code_challenge_methods_supported", List.of("S256"));
  }

  @Override
  public Map<String, Object> jwks() {
    return signingKeyProvider.jwks();
  }

  @Override
  public ResponseEntity<Void> authorize(
      String responseType,
      String clientId,
      String redirectUri,
      String scope,
      String state,
      String nonce,
      String codeChallenge,
      String codeChallengeMethod,
      HttpServletRequest request) {
    OauthClient client = oauthClientMapper.selectByClientId(clientId);
    if (client == null || !Boolean.TRUE.equals(client.getEnabled())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    if (!isAllowedRedirect(client, redirectUri)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    if (!"code".equals(responseType) || !contains(client.getGrantTypes(), "authorization_code")) {
      return redirectError(redirectUri, "unsupported_response_type", state);
    }
    boolean publicClient = client.getClientSecret() == null || client.getClientSecret().isBlank();
    if ((publicClient && isBlank(codeChallenge))
        || (!isBlank(codeChallenge) && !"S256".equalsIgnoreCase(codeChallengeMethod))) {
      return redirectError(redirectUri, "invalid_request", state);
    }
    if (!StpUtil.isLogin()) {
      String redirect = authorizeUrl(request);
      return ResponseEntity.status(HttpStatus.FOUND)
          .location(URI.create(loginUrl(redirect, request)))
          .build();
    }
    User authorizingUser = userMapper.selectById(StpUtil.getLoginIdAsInt());
    if (!isActiveUser(authorizingUser)) {
      StpUtil.logout();
      return redirectError(redirectUri, "access_denied", state);
    }
    String grantedScope = normalizeScope(scope, client.getScopes());
    String code = secureToken();
    authorizationCodeMapper.insert(
        OauthAuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .userId(authorizingUser.getId())
            .redirectUri(redirectUri)
            .scope(grantedScope)
            .state(state)
            .nonce(nonce)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .expiresAt(Timestamp.from(Instant.now().plusSeconds(AUTH_CODE_TTL_SECONDS)))
            .build());
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(appendParams(redirectUri, code, state))).build();
  }

  @Override
  public ResponseEntity<Map<String, Object>> token(
      String grantType,
      String code,
      String redirectUri,
      String clientId,
      String clientSecret,
      String codeVerifier,
      String refreshToken,
      HttpServletRequest request) {
    OauthClient client = oauthClientMapper.selectByClientId(clientId);
    if (!validClient(client, clientSecret)) return oauthError("invalid_client", HttpStatus.UNAUTHORIZED);
    if ("authorization_code".equals(grantType)) {
      return exchangeCode(client, code, redirectUri, codeVerifier, request);
    }
    if ("refresh_token".equals(grantType)) {
      return exchangeRefreshToken(client, refreshToken, request);
    }
    return oauthError("unsupported_grant_type", HttpStatus.BAD_REQUEST);
  }

  @Override
  public ResponseEntity<Map<String, Object>> userInfo(String authorization) {
    ResponseTokenIntrospectionVO token = introspectToken(authorization);
    if (!Boolean.TRUE.equals(token.getActive())) return oauthError("invalid_token", HttpStatus.UNAUTHORIZED);
    User user = userMapper.selectById(Integer.valueOf(token.getSub()));
    if (user == null) return oauthError("invalid_token", HttpStatus.UNAUTHORIZED);
    return ResponseEntity.ok(userInfo(user, token.getRoles(), token.getPermissions()));
  }

  @Override
  public ResponseEntity<Map<String, Object>> introspect(String token) {
    ResponseTokenIntrospectionVO result = introspectToken(token);
    return ResponseEntity.ok(
        ordered(
            "active", Boolean.TRUE.equals(result.getActive()),
            "sub", result.getSub(),
            "username", result.getUsername(),
            "name", result.getName(),
            "client_id", result.getClientId(),
            "scope", result.getScope(),
            "exp", result.getExp(),
            "roles", result.getRoles(),
            "permissions", result.getPermissions()));
  }

  private ResponseEntity<Map<String, Object>> exchangeCode(
      OauthClient client, String code, String redirectUri, String codeVerifier, HttpServletRequest request) {
    OauthAuthorizationCode authCode = authorizationCodeMapper.selectByCode(code);
    if (authCode == null
        || authCode.getConsumedAt() != null
        || authCode.getExpiresAt().before(Timestamp.from(Instant.now()))
        || !client.getClientId().equals(authCode.getClientId())
        || !authCode.getRedirectUri().equals(redirectUri)) {
      return oauthError("invalid_grant", HttpStatus.BAD_REQUEST);
    }
    if (!verifyPkce(authCode, codeVerifier)) return oauthError("invalid_grant", HttpStatus.BAD_REQUEST);
    authCode.setConsumedAt(Timestamp.from(Instant.now()));
    authorizationCodeMapper.updateById(authCode);
    User user = userMapper.selectById(authCode.getUserId());
    if (!isActiveUser(user)) return oauthError("invalid_grant", HttpStatus.BAD_REQUEST);
    return ResponseEntity.ok(tokens(user, client.getClientId(), authCode.getScope(), authCode.getNonce(), request));
  }

  private ResponseEntity<Map<String, Object>> exchangeRefreshToken(
      OauthClient client, String refreshToken, HttpServletRequest request) {
    String payload = SaManager.getSaTokenDao().get(REFRESH_KEY_PREFIX + refreshToken);
    if (payload == null || payload.isBlank()) return oauthError("invalid_grant", HttpStatus.BAD_REQUEST);
    String[] parts = payload.split("\t", 3);
    if (parts.length < 3 || !client.getClientId().equals(parts[1])) return oauthError("invalid_grant", HttpStatus.BAD_REQUEST);
    User user = userMapper.selectById(Integer.valueOf(parts[0]));
    if (!isActiveUser(user)) return oauthError("invalid_grant", HttpStatus.BAD_REQUEST);
    SaManager.getSaTokenDao().delete(REFRESH_KEY_PREFIX + refreshToken);
    return ResponseEntity.ok(tokens(user, client.getClientId(), parts[2], null, request));
  }

  private Map<String, Object> tokens(User user, String clientId, String scope, String nonce, HttpServletRequest request) {
    List<String> roles = StpUtil.getRoleList(user.getId());
    List<String> permissions = StpUtil.getPermissionList(user.getId());
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusSeconds(OIDC_TOKEN_TTL_SECONDS);
    String tokenIssuer = issuer(request);

    JWTClaimsSet.Builder accessClaims =
        standardClaims(user, tokenIssuer, issuedAt, expiresAt)
            .audience(List.of(clientId, resourceAudience))
            .claim("client_id", clientId)
            .claim("scope", scope)
            .claim("token_use", "access");
    String accessToken = signingKeyProvider.sign(accessClaims.build());

    JWTClaimsSet.Builder idClaims =
        standardClaims(user, tokenIssuer, issuedAt, expiresAt)
            .audience(clientId)
            .claim("client_id", clientId)
            .claim("token_use", "id")
            .claim("auth_time", issuedAt.getEpochSecond());
    if (!isBlank(nonce)) idClaims.claim("nonce", nonce);
    String idToken = signingKeyProvider.sign(idClaims.build());
    String refreshToken = secureToken();
    SaTokenDao dao = SaManager.getSaTokenDao();
    dao.set(REFRESH_KEY_PREFIX + refreshToken, user.getId() + "\t" + clientId + "\t" + scope, REFRESH_TOKEN_TTL_SECONDS);
    return ordered(
        "access_token", accessToken,
        "id_token", idToken,
        "refresh_token", refreshToken,
        "token_type", "Bearer",
        "expires_in", OIDC_TOKEN_TTL_SECONDS,
        "scope", scope,
        "user", userInfo(user, roles, permissions),
        "issuer", issuer(request));
  }

  private Map<String, Object> userInfo(User user, List<String> roles, List<String> permissions) {
    boolean labMember =
        roles.stream().anyMatch(role -> Set.of("super_admin", "club_admin", "formal_member").contains(role));
    return ordered(
        "sub", String.valueOf(user.getId()),
        "club_user_id", user.getId(),
        "preferred_username", user.getUserName(),
        "username", user.getUserName(),
        "name", user.getRealName(),
        "nickname", user.getRealName(),
        "email", user.getEmail(),
        "phone", user.getPhone(),
        "phone_number", user.getPhone(),
        "student_id", user.getStudentId(),
        "avatar", user.getAvatar(),
        "is_lab_member", labMember,
        "lab_role", roles.contains("super_admin") || roles.contains("club_admin") ? 2 : 0,
        "onboarding_completed_at", user.getOnboardingCompletedAt(),
        "activated_at", user.getActivatedAt(),
        "roles", roles,
        "permissions", permissions);
  }

  private ResponseTokenIntrospectionVO introspectToken(String token) {
    try {
      String value = normalizeBearer(token);
      if (value == null) return inactiveToken();
      JWTClaimsSet claims = signingKeyProvider.verify(value);
      Instant now = Instant.now();
      if (!issuer(null).equals(claims.getIssuer())
          || !"access".equals(claims.getStringClaim("token_use"))
          || claims.getExpirationTime() == null
          || !claims.getExpirationTime().toInstant().isAfter(now)
          || (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().toInstant().isAfter(now))) {
        return inactiveToken();
      }
      User user = userMapper.selectById(Integer.valueOf(claims.getSubject()));
      if (user == null
          || (user.getUserStatus() != null && user.getUserStatus() != UserStatus.ACTIVE)) {
        return inactiveToken();
      }
      List<String> roles = StpUtil.getRoleList(user.getId());
      List<String> permissions = StpUtil.getPermissionList(user.getId());
      long expiresIn = Math.max(0, claims.getExpirationTime().toInstant().getEpochSecond() - now.getEpochSecond());
      return ResponseTokenIntrospectionVO.builder()
          .active(true)
          .sub(String.valueOf(user.getId()))
          .username(user.getUserName())
          .name(user.getRealName())
          .clientId(claims.getStringClaim("client_id"))
          .scope(claims.getStringClaim("scope"))
          .exp(claims.getExpirationTime().toInstant().getEpochSecond())
          .expiresIn(expiresIn)
          .roles(roles)
          .permissions(permissions)
          .build();
    } catch (Exception exception) {
      return inactiveToken();
    }
  }

  private JWTClaimsSet.Builder standardClaims(
      User user, String issuer, Instant issuedAt, Instant expiresAt) {
    return new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(String.valueOf(user.getId()))
        .issueTime(Date.from(issuedAt))
        .notBeforeTime(Date.from(issuedAt.minusSeconds(5)))
        .expirationTime(Date.from(expiresAt))
        .jwtID(secureToken())
        .claim("preferred_username", user.getUserName())
        .claim("name", user.getRealName())
        .claim("email", user.getEmail());
  }

  private ResponseTokenIntrospectionVO inactiveToken() {
    return ResponseTokenIntrospectionVO.builder().active(false).build();
  }

  private String normalizeBearer(String token) {
    if (isBlank(token)) return null;
    String value = token.trim();
    if (value.regionMatches(true, 0, "Bearer ", 0, 7)) value = value.substring(7).trim();
    return value.isBlank() ? null : value;
  }

  private boolean validClient(OauthClient client, String clientSecret) {
    if (client == null || !Boolean.TRUE.equals(client.getEnabled())) return false;
    if (client.getClientSecret() == null || client.getClientSecret().isBlank()) return true;
    return passwordService.matches(clientSecret, client.getClientSecret());
  }

  private boolean isActiveUser(User user) {
    return user != null && (user.getUserStatus() == null || user.getUserStatus() == UserStatus.ACTIVE);
  }

  private boolean isAllowedRedirect(OauthClient client, String redirectUri) {
    return split(client.getRedirectUris(), ",").contains(redirectUri);
  }

  private String normalizeScope(String requested, String allowed) {
    Set<String> allowedScopes = split(allowed, " ");
    Set<String> requestedScopes = split(requested == null || requested.isBlank() ? "openid profile" : requested, " ");
    requestedScopes.retainAll(allowedScopes);
    if (!requestedScopes.contains("openid")) requestedScopes.add("openid");
    return String.join(" ", requestedScopes);
  }

  private boolean verifyPkce(OauthAuthorizationCode code, String verifier) {
    if (code.getCodeChallenge() == null || code.getCodeChallenge().isBlank()) return true;
    if (verifier == null || verifier.isBlank()) return false;
    if (!"S256".equalsIgnoreCase(code.getCodeChallengeMethod())) return false;
    return code.getCodeChallenge().equals(sha256Base64Url(verifier));
  }

  private String sha256Base64Url(String value) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      return "";
    }
  }

  private ResponseEntity<Map<String, Object>> oauthError(String error, HttpStatus status) {
    return ResponseEntity.status(status).body(Map.of("error", error));
  }

  private ResponseEntity<Void> redirectError(String redirectUri, String error, String state) {
    String target = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "error=" + encode(error);
    if (state != null && !state.isBlank()) target += "&state=" + encode(state);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
  }

  private String appendParams(String redirectUri, String code, String state) {
    String target = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "code=" + encode(code);
    if (state != null && !state.isBlank()) target += "&state=" + encode(state);
    return target;
  }

  private boolean contains(String source, String value) {
    return split(source, " ").contains(value);
  }

  private Set<String> split(String source, String delimiterRegex) {
    Set<String> values = new LinkedHashSet<>();
    if (source == null || source.isBlank()) return values;
    Arrays.stream(source.split(delimiterRegex))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .forEach(values::add);
    return values;
  }

  private String secureToken() {
    return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
  }

  private String issuer(HttpServletRequest request) {
    if (configuredIssuer != null && !configuredIssuer.isBlank()) return configuredIssuer;
    if (request == null) throw new IllegalStateException("OIDC issuer is not configured");
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
  }

  private String authorizeUrl(HttpServletRequest request) {
    String query = sanitizeAuthorizeQuery(request.getQueryString());
    return issuer(request) + "/oauth/authorize" + (query == null || query.isBlank() ? "" : "?" + query);
  }

  private String sanitizeAuthorizeQuery(String query) {
    if (isBlank(query)) return "";
    return Arrays.stream(query.split("&"))
        .filter(part -> !part.regionMatches(true, 0, "jmiopenatom=", 0, "jmiopenatom=".length()))
        .collect(java.util.stream.Collectors.joining("&"));
  }

  private String loginUrl(String authorizeUrl, HttpServletRequest request) {
    return issuer(request) + "/oauth/login?return_to=" + encode(authorizeUrl);
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Map<String, Object> ordered(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < values.length; i += 2) {
      map.put(String.valueOf(values[i]), values[i + 1]);
    }
    return map;
  }
}
