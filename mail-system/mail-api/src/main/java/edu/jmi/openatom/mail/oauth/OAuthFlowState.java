package edu.jmi.openatom.mail.oauth;

import java.io.Serial;
import java.io.Serializable;

public record OAuthFlowState(String state, String nonce, String verifier) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
