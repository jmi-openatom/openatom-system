package edu.jmi.openatom.server.openatomsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jwt.JWTClaimsSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class OidcSigningKeyProviderTest {

  @Test
  void jwksContainsOnlyRsaPublicMaterial() {
    OidcSigningKeyProvider provider = provider("current-key");

    Map<String, Object> jwks = provider.jwks();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

    assertEquals(1, keys.size());
    Map<String, Object> key = keys.getFirst();
    assertEquals("RSA", key.get("kty"));
    assertEquals("RS256", key.get("alg"));
    assertEquals("current-key", key.get("kid"));
    assertTrue(key.containsKey("n"));
    assertTrue(key.containsKey("e"));
    assertFalse(key.containsKey("k"));
    assertFalse(key.containsKey("d"));
    assertFalse(key.containsKey("p"));
    assertFalse(key.containsKey("q"));
  }

  @Test
  void signedTokenVerifiesAndTamperingFails() {
    OidcSigningKeyProvider provider = provider("current-key");
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer("https://oauth.example.test")
            .subject("42")
            .audience("mail")
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("token_use", "access")
            .build();

    String token = provider.sign(claims);

    assertEquals("42", provider.verify(token).getSubject());
    String tampered = token.substring(0, token.length() - 2) + "xx";
    assertThrows(IllegalArgumentException.class, () -> provider.verify(tampered));
  }

  @Test
  void previousPublicKeyRemainsAvailableDuringRotation() throws Exception {
    KeyPair previous = generateKeyPair();
    String previousPublic = Base64.getEncoder().encodeToString(previous.getPublic().getEncoded());
    OidcSigningKeyProvider provider =
        new OidcSigningKeyProvider(
            "", "", "current-key", previousPublic, "previous-key", false);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> keys =
        (List<Map<String, Object>>) provider.jwks().get("keys");

    assertEquals(2, keys.size());
    assertTrue(keys.stream().anyMatch(key -> "current-key".equals(key.get("kid"))));
    assertTrue(keys.stream().anyMatch(key -> "previous-key".equals(key.get("kid"))));
  }

  @Test
  void productionRefusesToStartWithoutConfiguredKeys() {
    assertThrows(
        IllegalStateException.class,
        () -> new OidcSigningKeyProvider("", "", "prod-key", "", "", true));
  }

  @Test
  void springSelectsTheConfiguredConstructor() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(OidcSigningKeyProvider.class);
      context.refresh();

      OidcSigningKeyProvider provider = context.getBean(OidcSigningKeyProvider.class);
      assertEquals("openatom-oidc-rs256", provider.jwks().get("keys") instanceof List<?> keys
          ? ((Map<?, ?>) keys.getFirst()).get("kid")
          : null);
    }
  }

  private OidcSigningKeyProvider provider(String keyId) {
    return new OidcSigningKeyProvider("", "", keyId, "", "", false);
  }

  private KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }
}
