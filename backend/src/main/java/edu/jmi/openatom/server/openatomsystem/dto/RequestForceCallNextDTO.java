package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestForceCallNextDTO {
  @NotNull @Positive private Integer expectedInterviewId;
  @NotBlank @Size(max = 500) private String reason;

  public void setReason(String reason) {
    this.reason = reason == null ? null : reason.strip();
  }
}
