package edu.jmi.openatom.quest.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.jmi.openatom.quest.config.OauthProperties;
import edu.jmi.openatom.quest.model.OauthUserInfo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OpenAtomOauthClient {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OauthProperties properties;
    private final RestClient oauthRestClient;

    public LoginRequest createLoginRequest() {
        String state = randomValue();
        String nonce = randomValue();
        String verifier = randomValue();
        String challenge = sha256Base64Url(verifier);
        String authorizeUrl = UriComponentsBuilder
            .fromUriString(properties.issuer().replaceAll("/+$", "") + "/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", properties.scopes())
            .queryParam("state", state)
            .queryParam("nonce", nonce)
            .queryParam("code_challenge", challenge)
            .queryParam("code_challenge_method", "S256")
            .build()
            .encode()
            .toUriString();
        return new LoginRequest(authorizeUrl, state, nonce, verifier);
    }

    public TokenResult exchangeCode(String code, String verifier, String expectedNonce) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        form.add("code_verifier", verifier);
        if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
            form.add("client_secret", properties.clientSecret());
        }

        JsonNode token = oauthRestClient.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JsonNode.class);
        if (token == null || token.path("access_token").asText().isBlank() || token.path("id_token").asText().isBlank()) {
            throw new IllegalStateException("OAuth 令牌响应不完整");
        }
        String idTokenSubject = verifyIdToken(token.path("id_token").asText(), expectedNonce);

        JsonNode info = oauthRestClient.get()
            .uri("/oauth/userinfo")
            .headers(headers -> headers.setBearerAuth(token.path("access_token").asText()))
            .retrieve()
            .body(JsonNode.class);
        if (info == null || info.path("sub").asText().isBlank()) {
            throw new IllegalStateException("OAuth 用户信息不完整");
        }
        if (!idTokenSubject.equals(info.path("sub").asText())) {
            throw new IllegalStateException("OIDC ID Token 与 UserInfo 用户不一致");
        }
        OauthUserInfo user = new OauthUserInfo(
            info.path("sub").asText(),
            firstText(info, "nickname", "name", "preferred_username", "username"),
            firstText(info, "avatar"),
            firstText(info, "email"),
            firstText(info, "school"),
            firstText(info, "college"),
            firstText(info, "major"),
            firstText(info, "grade"),
            info.path("lab_role").asInt(0)
        );
        return new TokenResult(token.path("access_token").asText(), token.path("refresh_token").asText(), user);
    }

    private String verifyIdToken(String rawToken, String expectedNonce) {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(properties.issuer().replaceAll("/+$", ""));
        Jwt jwt;
        try {
            jwt = decoder.decode(rawToken);
        } catch (JwtException exception) {
            throw new IllegalStateException("OIDC ID Token 校验失败", exception);
        }
        List<String> audience = jwt.getAudience();
        if (!audience.contains(properties.clientId())
            || !expectedNonce.equals(jwt.getClaimAsString("nonce"))
            || !"id".equals(jwt.getClaimAsString("token_use"))
            || jwt.getSubject() == null
            || jwt.getSubject().isBlank()) {
            throw new IllegalStateException("OIDC ID Token Claims 校验失败");
        }
        return jwt.getSubject();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Base64Url(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 PKCE Challenge", exception);
        }
    }

    public record LoginRequest(String authorizeUrl, String state, String nonce, String verifier) {
    }

    public record TokenResult(String accessToken, String refreshToken, OauthUserInfo user) {
    }
}
