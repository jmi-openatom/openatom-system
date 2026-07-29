package edu.jmi.openatom.mail.oauth;

public class OAuthException extends RuntimeException {
  public OAuthException(String message) { super(message); }
  public OAuthException(String message, Throwable cause) { super(message, cause); }
}
