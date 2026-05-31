package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.service.OidcService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OidcController {
  private final OidcService oidcService;

  @GetMapping("/.well-known/openid-configuration")
  public Map<String, Object> configuration(HttpServletRequest request) {
    return oidcService.configuration(request);
  }

  @GetMapping("/oauth/jwks")
  public Map<String, Object> jwks() {
    return oidcService.jwks();
  }

  @GetMapping("/oauth/authorize")
  public ResponseEntity<Void> authorize(
      @RequestParam("response_type") String responseType,
      @RequestParam("client_id") String clientId,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam(required = false) String scope,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String nonce,
      @RequestParam(name = "code_challenge", required = false) String codeChallenge,
      @RequestParam(name = "code_challenge_method", required = false) String codeChallengeMethod,
      HttpServletRequest request) {
    return oidcService.authorize(
        responseType, clientId, redirectUri, scope, state, nonce, codeChallenge, codeChallengeMethod, request);
  }

  @PostMapping("/oauth/token")
  public ResponseEntity<Map<String, Object>> token(
      @RequestParam("grant_type") String grantType,
      @RequestParam(required = false) String code,
      @RequestParam(name = "redirect_uri", required = false) String redirectUri,
      @RequestParam(name = "client_id") String clientId,
      @RequestParam(name = "client_secret", required = false) String clientSecret,
      @RequestParam(name = "code_verifier", required = false) String codeVerifier,
      @RequestParam(name = "refresh_token", required = false) String refreshToken,
      HttpServletRequest request) {
    return oidcService.token(
        grantType, code, redirectUri, clientId, clientSecret, codeVerifier, refreshToken, request);
  }

  @GetMapping("/oauth/userinfo")
  public ResponseEntity<Map<String, Object>> userInfo(
      @RequestHeader(name = "Authorization", required = false) String authorization) {
    return oidcService.userInfo(authorization);
  }

  @PostMapping("/oauth/introspect")
  public ResponseEntity<Map<String, Object>> introspect(@RequestParam(required = false) String token) {
    return oidcService.introspect(token);
  }
}
