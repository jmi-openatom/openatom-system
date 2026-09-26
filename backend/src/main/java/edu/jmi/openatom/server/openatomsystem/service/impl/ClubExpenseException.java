package edu.jmi.openatom.server.openatomsystem.service.impl;

public class ClubExpenseException extends RuntimeException {
  private final int code;

  public ClubExpenseException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
