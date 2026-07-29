package edu.jmi.openatom.mail.service;

public class AttachmentTooLargeException extends RuntimeException {
  public AttachmentTooLargeException() {
    super("attachment_too_large");
  }
}
