package edu.jmi.openatom.mail.service;

public class MailboxNotFoundException extends RuntimeException {
  public MailboxNotFoundException(String sub) {
    super("Mailbox not found for subject " + sub);
  }
}
