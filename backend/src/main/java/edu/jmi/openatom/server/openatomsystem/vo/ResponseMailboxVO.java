package edu.jmi.openatom.server.openatomsystem.vo;

/** Current user's mail-system mailbox status (address lives in the mail system). */
public record ResponseMailboxVO(String address, String status, String provisionStatus) {
  public static ResponseMailboxVO unavailable() {
    return new ResponseMailboxVO("", "UNAVAILABLE", "");
  }
}