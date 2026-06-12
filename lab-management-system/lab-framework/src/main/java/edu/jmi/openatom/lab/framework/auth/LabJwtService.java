package edu.jmi.openatom.lab.framework.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.lab.framework.entity.LabUser;
import edu.jmi.openatom.lab.framework.properties.LabAuthProperties;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabJwtService {
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final ObjectMapper objectMapper;
  private final LabAuthProperties properties;

  public TokenPair createToken(LabUser user) {
    long expiresAt = System.currentTimeMillis() + properties.getTokenTtlMinutes() * 60_000L;
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("uid", user.getId());
    payload.put("club_uid", user.getClubUserId());
    payload.put("username", user.getUsername());
    payload.put("nickname", user.getNickname());
    payload.put("lab_role", user.getLabRole());
    payload.put("exp", expiresAt / 1000L);

    String token = encode(header) + "." + encode(payload);
    token = token + "." + sign(token);
    LocalDateTime expires =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expiresAt), ZoneId.systemDefault());
    return new TokenPair(token, expires);
  }

  public LabPrincipal parse(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return null;
      }
      String signingInput = parts[0] + "." + parts[1];
      if (!constantEquals(sign(signingInput), parts[2])) {
        return null;
      }
      Map<String, Object> payload =
          objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {});
      Number exp = (Number) payload.get("exp");
      if (exp == null || exp.longValue() * 1000L < System.currentTimeMillis()) {
        return null;
      }
      return new LabPrincipal(
          numberToLong(payload.get("uid")),
          numberToLong(payload.get("club_uid")),
          string(payload.get("username")),
          string(payload.get("nickname")),
          numberToInteger(payload.get("lab_role")));
    } catch (Exception ignored) {
      return null;
    }
  }

  private String encode(Object value) {
    try {
      return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception e) {
      throw new IllegalStateException("JWT编码失败", e);
    }
  }

  private String sign(String input) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return URL_ENCODER.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("JWT签名失败", e);
    }
  }

  private boolean constantEquals(String a, String b) {
    if (a == null || b == null || a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }

  private Long numberToLong(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  private Integer numberToInteger(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
  }

  private String string(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  public record TokenPair(String token, LocalDateTime expiresAt) {}
}
