package edu.jmi.openatom.server.openatomsystem.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Owns the asymmetric signing keys used by the OpenAtom OIDC provider.
 *
 * <p>Production must provide an RSA private/public key pair as base64-encoded DER (PKCS#8 and
 * X.509 respectively). Non-production environments may use an ephemeral key pair so local
 * development never falls back to publishing a symmetric signing secret.
 */
@Component
public class OidcSigningKeyProvider {
  private static final String DEFAULT_KEY_ID = "openatom-oidc-rs256";

  private final String currentKeyId;
  private final RSAPrivateKey privateKey;
  private final Map<String, RSAPublicKey> verificationKeys;

  public OidcSigningKeyProvider(
      @Value("${app.oidc.signing.private-key-base64:}") String privateKeyBase64,
      @Value("${app.oidc.signing.public-key-base64:}") String publicKeyBase64,
      @Value("${app.oidc.signing.key-id:" + DEFAULT_KEY_ID + "}") String keyId,
      @Value("${app.oidc.signing.previous-public-key-base64:}") String previousPublicKeyBase64,
      @Value("${app.oidc.signing.previous-key-id:}") String previousKeyId,
      Environment environment) {
    this(
        privateKeyBase64,
        publicKeyBase64,
        keyId,
        previousPublicKeyBase64,
        previousKeyId,
        environment.acceptsProfiles(Profiles.of("prod")));
  }

  OidcSigningKeyProvider(
      String privateKeyBase64,
      String publicKeyBase64,
      String keyId,
      String previousPublicKeyBase64,
      String previousKeyId,
      boolean production) {
    this.currentKeyId = isBlank(keyId) ? DEFAULT_KEY_ID : keyId.trim();
    KeyPair current = loadOrGenerate(privateKeyBase64, publicKeyBase64, production);
    this.privateKey = (RSAPrivateKey) current.getPrivate();
    this.verificationKeys = new LinkedHashMap<>();
    this.verificationKeys.put(this.currentKeyId, (RSAPublicKey) current.getPublic());

    if (!isBlank(previousPublicKeyBase64)) {
      if (isBlank(previousKeyId)) {
        throw new IllegalStateException(
            "app.oidc.signing.previous-key-id is required when a previous public key is configured");
      }
      if (this.currentKeyId.equals(previousKeyId.trim())) {
        throw new IllegalStateException("OIDC current and previous key ids must be different");
      }
      this.verificationKeys.put(previousKeyId.trim(), parsePublicKey(previousPublicKeyBase64));
    }
  }

  /** Creates an ephemeral RSA provider for isolated tests and local tooling. */
  public static OidcSigningKeyProvider ephemeral(String keyId) {
    return new OidcSigningKeyProvider("", "", keyId, "", "", false);
  }

  public String sign(JWTClaimsSet claims) {
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .keyID(currentKeyId)
                  .type(com.nimbusds.jose.JOSEObjectType.JWT)
                  .build(),
              claims);
      jwt.sign(new RSASSASigner(privateKey));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException("Unable to sign OIDC token", exception);
    }
  }

  public JWTClaimsSet verify(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
        throw new IllegalArgumentException("Unsupported OIDC signing algorithm");
      }
      String keyId = jwt.getHeader().getKeyID();
      RSAPublicKey key = verificationKeys.get(keyId);
      if (key == null || !jwt.verify(new RSASSAVerifier(key))) {
        throw new IllegalArgumentException("Invalid OIDC token signature");
      }
      return jwt.getJWTClaimsSet();
    } catch (ParseException | JOSEException exception) {
      throw new IllegalArgumentException("Invalid OIDC token", exception);
    }
  }

  public Map<String, Object> jwks() {
    List<Map<String, Object>> keys = new ArrayList<>();
    verificationKeys.forEach(
        (keyId, publicKey) ->
            keys.add(
                new RSAKey.Builder(publicKey)
                    .keyID(keyId)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .build()
                    .toPublicJWK()
                    .toJSONObject()));
    return Map.of("keys", keys);
  }

  private KeyPair loadOrGenerate(
      String privateKeyBase64, String publicKeyBase64, boolean production) {
    if (isBlank(privateKeyBase64) && isBlank(publicKeyBase64)) {
      if (production) {
        throw new IllegalStateException(
            "Production OIDC requires APP_OIDC_PRIVATE_KEY_BASE64 and APP_OIDC_PUBLIC_KEY_BASE64");
      }
      return generateKeyPair();
    }
    if (isBlank(privateKeyBase64) || isBlank(publicKeyBase64)) {
      throw new IllegalStateException("Both OIDC private and public signing keys must be configured");
    }
    return new KeyPair(parsePublicKey(publicKeyBase64), parsePrivateKey(privateKeyBase64));
  }

  private KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to generate development OIDC signing key", exception);
    }
  }

  private RSAPrivateKey parsePrivateKey(String encoded) {
    try {
      byte[] bytes = Base64.getDecoder().decode(removeWhitespace(encoded));
      PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
      return (RSAPrivateKey) key;
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid OIDC PKCS#8 private key", exception);
    }
  }

  private RSAPublicKey parsePublicKey(String encoded) {
    try {
      byte[] bytes = Base64.getDecoder().decode(removeWhitespace(encoded));
      PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
      return (RSAPublicKey) key;
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid OIDC X.509 public key", exception);
    }
  }

  private String removeWhitespace(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
