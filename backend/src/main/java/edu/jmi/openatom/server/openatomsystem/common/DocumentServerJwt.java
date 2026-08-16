package edu.jmi.openatom.server.openatomsystem.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Minimal HS256 JWT used to authenticate against ONLYOFFICE Document Server
 * (config token and save callbacks) with the shared DOCUMENT_SERVER_JWT_SECRET.
 */
public final class DocumentServerJwt {
  private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64D = Base64.getUrlDecoder();
  private static final ObjectMapper JSON = new ObjectMapper();

  private DocumentServerJwt() {}

  /** Signs a JSON payload (typically the ONLYOFFICE editor config). */
  public static String sign(String secret, JsonNode payload) {
    requireSecret(secret);
    String header = B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String body = B64.encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
    String signingInput = header + "." + body;
    String signature = B64.encodeToString(hmac(secret, signingInput));
    return signingInput + "." + signature;
  }

  /** Signs a plain string payload (used for the short-lived download tokens). */
  public static String signString(String secret, String payload) {
    return sign(secret, JSON.getNodeFactory().textNode(payload));
  }

  /** Verifies the signature and returns the payload as JSON. */
  public static JsonNode verify(String secret, String token) {
    requireSecret(secret);
    String[] parts = token == null ? new String[0] : token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("invalid_jwt");
    }
    String expected = B64.encodeToString(hmac(secret, parts[0] + "." + parts[1]));
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
        parts[2].getBytes(StandardCharsets.UTF_8))) {
      throw new IllegalArgumentException("invalid_jwt_signature");
    }
    try {
      return JSON.readTree(new String(B64D.decode(parts[1]), StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalArgumentException("invalid_jwt_payload", exception);
    }
  }

  private static void requireSecret(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("document_server_not_configured");
    }
  }

  private static byte[] hmac(String secret, String input) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("hmac_unavailable", exception);
    }
  }
}
