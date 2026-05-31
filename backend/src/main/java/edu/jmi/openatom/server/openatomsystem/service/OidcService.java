package edu.jmi.openatom.server.openatomsystem.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface OidcService {
  Map<String, Object> configuration(HttpServletRequest request);

  Map<String, Object> jwks();

  ResponseEntity<Void> authorize(
      String responseType,
      String clientId,
      String redirectUri,
      String scope,
      String state,
      String nonce,
      String codeChallenge,
      String codeChallengeMethod,
      HttpServletRequest request);

  ResponseEntity<Map<String, Object>> token(
      String grantType,
      String code,
      String redirectUri,
      String clientId,
      String clientSecret,
      String codeVerifier,
      String refreshToken,
      HttpServletRequest request);

  ResponseEntity<Map<String, Object>> userInfo(String authorization);

  ResponseEntity<Map<String, Object>> introspect(String token);
}
