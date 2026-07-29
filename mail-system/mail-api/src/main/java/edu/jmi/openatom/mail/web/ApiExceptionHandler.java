package edu.jmi.openatom.mail.web;

import edu.jmi.openatom.mail.service.MailboxNotFoundException;
import edu.jmi.openatom.mail.service.AttachmentTooLargeException;
import edu.jmi.openatom.mail.service.StalwartClientException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(StalwartClientException.class)
  ResponseEntity<Map<String, Object>> stalwartUnavailable(StalwartClientException exception) {
    return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
  }

  @ExceptionHandler(MailboxNotFoundException.class)
  ResponseEntity<Map<String, Object>> notFound(MailboxNotFoundException exception) {
    return error(HttpStatus.NOT_FOUND, "mailbox_not_found");
  }

  @ExceptionHandler({AttachmentTooLargeException.class, MaxUploadSizeExceededException.class})
  ResponseEntity<Map<String, Object>> attachmentTooLarge(RuntimeException exception) {
    return error(HttpStatus.PAYLOAD_TOO_LARGE, "attachment_too_large");
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  ResponseEntity<Map<String, Object>> invalid(RuntimeException exception) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code) {
    return ResponseEntity.status(status)
        .body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "code", code));
  }
}
