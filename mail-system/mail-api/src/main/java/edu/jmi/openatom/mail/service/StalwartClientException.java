package edu.jmi.openatom.mail.service;

public class StalwartClientException extends RuntimeException {
  public StalwartClientException(String message) {
    super(message);
  }

  public StalwartClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
