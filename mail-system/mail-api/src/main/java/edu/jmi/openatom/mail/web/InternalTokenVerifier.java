package edu.jmi.openatom.mail.web;

import edu.jmi.openatom.mail.config.MailProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

@Component
public class InternalTokenVerifier {
  private final byte[] expected;

  public InternalTokenVerifier(MailProperties properties) {
    expected = properties.getInternalServiceToken().getBytes(StandardCharsets.UTF_8);
  }

  public boolean accepts(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return false;
    }
    byte[] supplied = authorization.substring(7).getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, supplied);
  }
}
