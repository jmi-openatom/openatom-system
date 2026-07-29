package edu.jmi.openatom.mail.domain;

public record ProvisionResponse(String status, String provisionStatus, String address) {
  public static ProvisionResponse from(MailboxAccount account) {
    return new ProvisionResponse(
        account.status(), account.provisionStatus(), account.primaryAddress());
  }
}
